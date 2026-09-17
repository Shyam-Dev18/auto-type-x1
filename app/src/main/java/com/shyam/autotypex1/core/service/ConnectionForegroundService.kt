package com.shyam.autotypex1.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.shyam.autotypex1.MainActivity
import com.shyam.autotypex1.R
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import com.shyam.autotypex1.domain.typing.TypingController
import com.shyam.autotypex1.domain.typing.TypingProfile
import com.shyam.autotypex1.domain.typing.TypingProgress
import com.shyam.autotypex1.domain.typing.TypingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Connection-scoped Foreground Service that keeps the process alive
 * whenever a Bluetooth HID device is connected.
 *
 * Lifecycle:
 * - Starts when [ConnectionState.Connected] is reached (via user's Connect tap).
 * - Stops when [ConnectionState.Disconnected] is reached after full unregisterApp() teardown.
 * - Survives backgrounding, screen-off, and task-swipe from recents.
 *
 * Notification states (single [NOTIFICATION_ID], updated in-place):
 * - **Connected idle**: "Connected to <deviceName>" + Disconnect action
 * - **Typing**: "Typing to <deviceName> (X%)" + Pause/Resume + Stop actions + progress bar
 * - **Paused**: "Typing Paused (X%)" + Resume + Stop actions
 *
 * Does NOT contain typing algorithms, randomization, or HID report generation.
 * Purely coordinates Android service lifecycle, notification updates, and [TypingController].
 */
@AndroidEntryPoint
class ConnectionForegroundService : Service() {

    @Inject
    lateinit var typingController: TypingController

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notificationManager: NotificationManager
    private var currentDeviceName: String = "Bluetooth Device"

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        observeStateChanges()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CONNECTION -> {
                currentDeviceName = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "Bluetooth Device"

                val notification = buildConnectedIdleNotification(currentDeviceName)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_STOP_CONNECTION -> {
                // Stop any active typing first
                serviceScope.launch {
                    val state = typingController.typingState.value
                    if (state is TypingState.Typing || state is TypingState.Paused || state is TypingState.Preparing) {
                        typingController.stopTyping()
                    }
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_TYPING -> {
                val script = intent.getStringExtra(EXTRA_SCRIPT).orEmpty()
                val profileName = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Normal"
                val profile = when (profileName.lowercase()) {
                    "slow" -> TypingProfile.SLOW
                    "fast" -> TypingProfile.FAST
                    "speed demon", "speed_demon" -> TypingProfile.SPEED_DEMON
                    else -> TypingProfile.NORMAL
                }
                val seed = if (intent.hasExtra(EXTRA_SEED)) intent.getLongExtra(EXTRA_SEED, 0L) else null

                serviceScope.launch {
                    typingController.startTyping(
                        script = script,
                        profile = profile,
                        seed = seed,
                        scope = serviceScope
                    )
                }
            }
            ACTION_PAUSE_TYPING -> {
                serviceScope.launch {
                    typingController.pauseTyping()
                }
            }
            ACTION_RESUME_TYPING -> {
                serviceScope.launch {
                    typingController.resumeTyping()
                }
            }
            ACTION_STOP_TYPING -> {
                serviceScope.launch {
                    typingController.stopTyping()
                }
            }
            ACTION_DISCONNECT -> {
                serviceScope.launch {
                    val state = typingController.typingState.value
                    if (state is TypingState.Typing || state is TypingState.Paused || state is TypingState.Preparing) {
                        typingController.stopTyping()
                    }
                    bluetoothRepository.disconnect()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * The service must NOT stop when the task is swiped from recents.
     * This keeps the Bluetooth connection alive like a music player.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Intentionally empty — do NOT call stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        // Release any held keys on service death (safety net)
        CoroutineScope(Dispatchers.IO).launch {
            bluetoothRepository.releaseAllKeys()
        }
    }

    // ── State observation ────────────────────────────────────────────

    /**
     * Observes typing state and connection state to update the notification in-place.
     * Throttles high-frequency progress updates to avoid Android OS notification rate-limiting,
     * while guaranteeing instant unthrottled updates on state transitions (Pause, Resume, Stop).
     */
    private fun observeStateChanges() {
        var lastNotifyTime = 0L
        var lastProgressPct = -1

        serviceScope.launch {
            combine(
                typingController.typingState,
                typingController.progress,
                bluetoothRepository.observeConnectionState()
            ) { typingState, progress, connState ->
                Triple(typingState, progress, connState)
            }.collect { (typingState, progress, connState) ->
                val deviceName = (connState as? ConnectionState.Connected)?.deviceName ?: currentDeviceName

                when {
                    // Connection lost — stop the service
                    connState is ConnectionState.Disconnected -> {
                        val ts = typingController.typingState.value
                        if (ts is TypingState.Typing || ts is TypingState.Paused || ts is TypingState.Preparing) {
                            typingController.stopTyping()
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    // Typing preparing — immediate
                    typingState is TypingState.Preparing -> {
                        lastNotifyTime = 0L
                        lastProgressPct = -1
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            buildTypingNotification(
                                deviceName = deviceName,
                                title = "Preparing...",
                                text = "Starting typing session • $deviceName",
                                progressPercent = 0,
                                isPaused = false
                            )
                        )
                    }
                    // Typing active — throttled (every 500ms or 2% step)
                    typingState is TypingState.Typing -> {
                        val now = System.currentTimeMillis()
                        val currentPct = progress.percentage.toInt()
                        if (now - lastNotifyTime >= 500L || currentPct - lastProgressPct >= 2 || lastProgressPct < 0) {
                            lastNotifyTime = now
                            lastProgressPct = currentPct
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                buildTypingNotification(
                                    deviceName = deviceName,
                                    title = "Typing in Progress ($currentPct%)",
                                    text = "${progress.currentCharIndex} / ${progress.totalChars} chars • $deviceName",
                                    progressPercent = currentPct,
                                    isPaused = false
                                )
                            )
                        }
                    }
                    // Typing paused — ALWAYS immediate update
                    typingState is TypingState.Paused -> {
                        lastNotifyTime = 0L
                        val currentPct = progress.percentage.toInt()
                        lastProgressPct = currentPct
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            buildTypingNotification(
                                deviceName = deviceName,
                                title = "Typing Paused ($currentPct%)",
                                text = "${progress.currentCharIndex} / ${progress.totalChars} chars • $deviceName",
                                progressPercent = currentPct,
                                isPaused = true
                            )
                        )
                    }
                    // Idle, Completed, Cancelled, Failed, or Stopping — ALWAYS immediate idle notification
                    connState is ConnectionState.Connected -> {
                        lastNotifyTime = 0L
                        lastProgressPct = -1
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            buildConnectedIdleNotification(deviceName)
                        )
                    }
                }
            }
        }
    }

    // ── Notification builders ────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains Bluetooth HID connection and shows typing progress"
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Builds the "Connected, not typing" notification without quick action buttons.
     */
    private fun buildConnectedIdleNotification(deviceName: String): Notification {
        val openAppPendingIntent = createOpenAppPendingIntent()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Connected to $deviceName")
            .setContentText("Ready to type • Tap to open")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setProgress(0, 0, false)
            .build()
    }

    /**
     * Builds the "Typing in progress" notification with Pause/Resume + Stop actions.
     * Uses BroadcastReceiver PendingIntents for 100% reliability across all Android versions.
     */
    private fun buildTypingNotification(
        deviceName: String,
        title: String,
        text: String,
        progressPercent: Int,
        isPaused: Boolean
    ): Notification {
        val openAppPendingIntent = createOpenAppPendingIntent()

        val pauseIntent = Intent(this, TypingActionReceiver::class.java).apply {
            action = TypingActionReceiver.ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_PAUSE, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val resumeIntent = Intent(this, TypingActionReceiver::class.java).apply {
            action = TypingActionReceiver.ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_RESUME, resumeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TypingActionReceiver::class.java).apply {
            action = TypingActionReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, REQUEST_CODE_STOP, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                if (isPaused) resumePendingIntent else pausePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )

        if (progressPercent in 0..100) {
            builder.setProgress(100, progressPercent, false)
        }

        return builder.build()
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            this, REQUEST_CODE_OPEN_APP, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "autotype_connection_channel"
        const val CHANNEL_NAME = "Connection Service"

        const val ACTION_START_CONNECTION = "com.shyam.autotypex1.action.START_CONNECTION"
        const val ACTION_STOP_CONNECTION = "com.shyam.autotypex1.action.STOP_CONNECTION"
        const val ACTION_START_TYPING = "com.shyam.autotypex1.action.START_TYPING"
        const val ACTION_PAUSE_TYPING = "com.shyam.autotypex1.action.PAUSE_TYPING"
        const val ACTION_RESUME_TYPING = "com.shyam.autotypex1.action.RESUME_TYPING"
        const val ACTION_STOP_TYPING = "com.shyam.autotypex1.action.STOP_TYPING"
        const val ACTION_DISCONNECT = "com.shyam.autotypex1.action.DISCONNECT"

        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_SCRIPT = "extra_script"
        const val EXTRA_PROFILE_NAME = "extra_profile_name"
        const val EXTRA_SEED = "extra_seed"

        private const val REQUEST_CODE_OPEN_APP = 0
        private const val REQUEST_CODE_STOP = 1
        private const val REQUEST_CODE_PAUSE = 2
        private const val REQUEST_CODE_RESUME = 3

        /**
         * Start the foreground service on device connection.
         * Must be called from a foreground-permitted context (user's Connect tap)
         * to satisfy API 31+ restrictions.
         */
        fun startConnection(context: Context, deviceName: String) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_START_CONNECTION
                putExtra(EXTRA_DEVICE_NAME, deviceName)
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop the foreground service on full disconnect.
         */
        fun stopConnection(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_STOP_CONNECTION
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
                // Service may already be stopped
            }
        }

        fun startTyping(context: Context, script: String, profileName: String = "Normal", seed: Long? = null) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_START_TYPING
                putExtra(EXTRA_SCRIPT, script)
                putExtra(EXTRA_PROFILE_NAME, profileName)
                if (seed != null) putExtra(EXTRA_SEED, seed)
            }
            context.startService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_PAUSE_TYPING
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_RESUME_TYPING
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_STOP_TYPING
            }
            context.startService(intent)
        }
    }
}
