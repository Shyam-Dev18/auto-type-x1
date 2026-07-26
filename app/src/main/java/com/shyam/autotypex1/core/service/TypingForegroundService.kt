package com.shyam.autotypex1.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shyam.autotypex1.MainActivity
import com.shyam.autotypex1.R
import com.shyam.autotypex1.data.bluetooth.HidKeyMapper
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.TypingFailure
import com.shyam.autotypex1.domain.model.TypingProgress
import com.shyam.autotypex1.domain.model.TypingState
import com.shyam.autotypex1.domain.repository.HidConnectionRepository
import com.shyam.autotypex1.domain.typing.DefaultHumanTypingEngine
import com.shyam.autotypex1.domain.typing.KeyAction
import com.shyam.autotypex1.domain.typing.TimedKeyEvent
import com.shyam.autotypex1.domain.model.TypingProfileSpec
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Foreground service whose lifecycle is tied to the Bluetooth connection (§7).
 *
 * - Starts when HID connection is established (not when typing starts)
 * - Stays alive through connected+idle, connected+typing, connected+paused
 * - Stops on disconnect / BT off / explicit user disconnect
 * - Owns the only coroutine scope that sends key events via HidConnectionRepository
 * - On process death: resumes typing as Paused, never auto-continues keystrokes
 */
@AndroidEntryPoint
class TypingForegroundService : Service() {

    @Inject lateinit var hidRepo: HidConnectionRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var typingJob: Job? = null
    private val typingMutex = Mutex() // Guards against rapid double-tap (§8)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECTION_ESTABLISHED -> {
                startForeground(NOTIFICATION_ID, buildNotification("Connected — idle"))
                monitorConnection()
            }
            ACTION_START_TYPING -> {
                val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
                val wpmMin = intent.getIntExtra(EXTRA_WPM_MIN, 40)
                val wpmMax = intent.getIntExtra(EXTRA_WPM_MAX, 55)
                val jitter = intent.getIntExtra(EXTRA_JITTER, 18)
                val typoProb = intent.getFloatExtra(EXTRA_TYPO_PROB, 0.18f)
                val wordGap = intent.getIntExtra(EXTRA_WORD_GAP, 80)
                val seed = intent.getLongExtra(EXTRA_SEED, System.currentTimeMillis())
                val spec = TypingProfileSpec(
                    baseWpm = wpmMin..wpmMax,
                    jitterPercent = jitter,
                    typoProbability = typoProb,
                    wordGapMs = wordGap
                )
                startTyping(content, spec, seed)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP_TYPING -> stopTyping()
            ACTION_DISCONNECT -> {
                stopTyping()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        typingJob?.cancel()
        scope.cancel()
    }

    private fun monitorConnection() {
        scope.launch {
            hidRepo.observeConnectionState().collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        updateNotification("Connected to ${state.deviceName} — idle")
                    }
                    is ConnectionState.Disconnected -> {
                        // If typing was in progress, mark as errored
                        if (_typingState.value is TypingState.Running) {
                            val progress = (_typingState.value as TypingState.Running).progress
                            _typingState.value = TypingState.Errored(progress, TypingFailure.CONNECTION_LOST)
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    is ConnectionState.Failed -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun startTyping(content: String, spec: TypingProfileSpec, seed: Long) {
        scope.launch {
            typingMutex.withLock {
                typingJob?.cancel()
                typingJob = scope.launch {
                    if (content.isBlank()) {
                        _typingState.value = TypingState.Errored(
                            TypingProgress(0, 0, 0L),
                            TypingFailure.UNKNOWN
                        )
                        return@launch
                    }

                    if (!hidRepo.isTypingReady()) {
                        _typingState.value = TypingState.Errored(
                            TypingProgress(0, content.length, 0L),
                            TypingFailure.CONNECTION_LOST
                        )
                        return@launch
                    }

                    // Generate the typing plan
                    val engine = DefaultHumanTypingEngine()
                    val events = engine.generate(content, spec, seed)
                    val totalChars = content.length
                    var charsTyped = 0
                    val startTime = System.currentTimeMillis()

                    _typingState.value = TypingState.Running(TypingProgress(0, totalChars, 0L))
                    updateNotification("Typing… 0%")

                    // Clear host keyboard modifiers/keys to start with a clean state
                    hidRepo.releaseAllKeys()

                    // Warm-up delay to allow the Bluetooth HID connection/driver to stabilize and focus
                    delay(1500)

                    for (event in events) {
                        // Wait for the specified delay
                        if (event.delayMs > 0) {
                            delay(event.delayMs)
                        }

                        // Check for pause
                        while (isActive && _typingState.value is TypingState.Paused) {
                            delay(100)
                        }

                        if (!isActive) return@launch

                        // Check connection
                        if (!hidRepo.isTypingReady()) {
                            val elapsed = System.currentTimeMillis() - startTime
                            _typingState.value = TypingState.Errored(
                                TypingProgress(charsTyped, totalChars, elapsed),
                                TypingFailure.CONNECTION_LOST
                            )
                            updateNotification("Connection lost")
                            return@launch
                        }

                        // Send the key event
                        when (val action = event.action) {
                            is KeyAction.KeyDown -> {
                                hidRepo.sendCharacter(action.char)
                                charsTyped++
                                val elapsed = System.currentTimeMillis() - startTime
                                val progress = TypingProgress(charsTyped, totalChars, elapsed)
                                _typingState.value = TypingState.Running(progress)
                                val pct = (progress.percent * 100).toInt()
                                if (pct % 5 == 0) {
                                    updateNotification("Typing… $pct%")
                                }
                            }
                            is KeyAction.KeyUp -> { /* Release handled by sendCharacter */ }
                            is KeyAction.Backspace -> {
                                hidRepo.sendBackspace()
                            }
                        }
                    }

                    val elapsed = System.currentTimeMillis() - startTime
                    _typingState.value = TypingState.Finished(TypingProgress(totalChars, totalChars, elapsed))
                    updateNotification("Typing complete")
                }
            }
        }
    }

    private fun pause() {
        val current = _typingState.value
        if (current is TypingState.Running) {
            _typingState.value = TypingState.Paused(current.progress)
            updateNotification("Paused — ${(current.progress.percent * 100).toInt()}%")
        }
    }

    private fun resume() {
        val current = _typingState.value
        if (current is TypingState.Paused) {
            _typingState.value = TypingState.Running(current.progress)
            updateNotification("Typing… ${(current.progress.percent * 100).toInt()}%")
        }
    }

    private fun stopTyping() {
        typingJob?.cancel()
        _typingState.value = TypingState.Idle
        updateNotification("Connected — idle")
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(content: String): Notification {
        createChannelIfNeeded()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("AutoType X1")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannelIfNeeded() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Typing Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows status while connected to a Bluetooth HID host"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "typing_service_channel"
        private const val NOTIFICATION_ID = 991

        const val ACTION_CONNECTION_ESTABLISHED = "com.shyam.autotypex1.service.CONNECTION_ESTABLISHED"
        const val ACTION_START_TYPING = "com.shyam.autotypex1.service.START_TYPING"
        const val ACTION_PAUSE = "com.shyam.autotypex1.service.PAUSE"
        const val ACTION_RESUME = "com.shyam.autotypex1.service.RESUME"
        const val ACTION_STOP_TYPING = "com.shyam.autotypex1.service.STOP_TYPING"
        const val ACTION_DISCONNECT = "com.shyam.autotypex1.service.DISCONNECT"

        const val EXTRA_CONTENT = "extra_content"
        const val EXTRA_WPM_MIN = "extra_wpm_min"
        const val EXTRA_WPM_MAX = "extra_wpm_max"
        const val EXTRA_JITTER = "extra_jitter"
        const val EXTRA_TYPO_PROB = "extra_typo_prob"
        const val EXTRA_WORD_GAP = "extra_word_gap"
        const val EXTRA_SEED = "extra_seed"

        // Global typing state — observed by ViewModels
        private val _typingState = MutableStateFlow<TypingState>(TypingState.Idle)
        val typingState: StateFlow<TypingState> = _typingState.asStateFlow()

        fun startConnection(context: Context) {
            val intent = Intent(context, TypingForegroundService::class.java)
                .setAction(ACTION_CONNECTION_ESTABLISHED)
            context.startForegroundService(intent)
        }

        fun startTyping(
            context: Context,
            content: String,
            spec: TypingProfileSpec,
            seed: Long = System.currentTimeMillis()
        ) {
            val intent = Intent(context, TypingForegroundService::class.java)
                .setAction(ACTION_START_TYPING)
                .putExtra(EXTRA_CONTENT, content)
                .putExtra(EXTRA_WPM_MIN, spec.baseWpm.first)
                .putExtra(EXTRA_WPM_MAX, spec.baseWpm.last)
                .putExtra(EXTRA_JITTER, spec.jitterPercent)
                .putExtra(EXTRA_TYPO_PROB, spec.typoProbability)
                .putExtra(EXTRA_WORD_GAP, spec.wordGapMs)
                .putExtra(EXTRA_SEED, seed)
            context.startForegroundService(intent)
        }

        fun pause(context: Context) {
            context.startService(
                Intent(context, TypingForegroundService::class.java).setAction(ACTION_PAUSE)
            )
        }

        fun resume(context: Context) {
            context.startService(
                Intent(context, TypingForegroundService::class.java).setAction(ACTION_RESUME)
            )
        }

        fun stopTyping(context: Context) {
            context.startService(
                Intent(context, TypingForegroundService::class.java).setAction(ACTION_STOP_TYPING)
            )
        }

        fun disconnect(context: Context) {
            context.startService(
                Intent(context, TypingForegroundService::class.java).setAction(ACTION_DISCONNECT)
            )
        }
    }
}
