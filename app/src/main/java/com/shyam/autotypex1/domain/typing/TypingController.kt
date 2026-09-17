package com.shyam.autotypex1.domain.typing

import com.shyam.autotypex1.domain.repository.BluetoothRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Orchestrates the execution of typing sessions.
 *
 * Responsibilities:
 * - Bridges [HumanTypingEngine] event generation with [BluetoothRepository] HID dispatch.
 * - Enforces sequential execution, respecting every timed delay.
 * - Tracks held keys and modifiers in memory.
 * - Guarantees full key release ([BluetoothRepository.releaseAllKeys]) on EVERY termination path.
 * - Handles structured cancellation without swallowing [CancellationException].
 * - Prevents multiple concurrent typing sessions.
 * - Does NOT log script text or individual keystrokes.
 */
class TypingController(
    private val engine: HumanTypingEngine,
    private val bluetoothRepository: BluetoothRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val _typingState = MutableStateFlow<TypingState>(TypingState.Idle)
    val typingState: StateFlow<TypingState> = _typingState.asStateFlow()

    private val _progress = MutableStateFlow(TypingProgress())
    val progress: StateFlow<TypingProgress> = _progress.asStateFlow()

    private val mutex = Mutex()
    private var activeJob: Job? = null
    private val heldKeys = mutableSetOf<Char>()
    @Volatile
    private var isPaused = false
    @Volatile
    private var pauseStartTimestamp = 0L
    @Volatile
    private var totalPausedMs = 0L

    fun isCurrentlyPaused(): Boolean = isPaused || _typingState.value is TypingState.Paused

    /**
     * Starts a new typing session.
     *
     * @param script   The text to type.
     * @param profile  Typing configuration profile.
     * @param seed     Optional seed for reproducible testing.
     * @param scope    CoroutineScope in which the typing loop runs.
     * @return [Result.success] if typing was successfully started, [Result.failure] otherwise.
     */
    suspend fun startTyping(
        script: String,
        profile: TypingProfile = TypingProfile.NORMAL,
        seed: Long? = null,
        scope: CoroutineScope
    ): Result<Unit> = mutex.withLock {
        // Prevent concurrent sessions
        when (_typingState.value) {
            is TypingState.Typing, is TypingState.Preparing, is TypingState.Paused -> {
                return Result.failure(IllegalStateException("A typing session is already active"))
            }
            else -> Unit
        }

        // Connection check
        if (!bluetoothRepository.isConnected()) {
            _typingState.value = TypingState.Failed(TypingFailure.NoHidConnection)
            return Result.failure(IllegalStateException("No active HID connection"))
        }

        // Validation check
        val validation = CharacterValidator.validate(script)
        if (validation.sanitizedScript.isEmpty()) {
            _typingState.value = TypingState.Failed(TypingFailure.ScriptEmpty)
            return Result.failure(IllegalArgumentException("Script contains no supported characters"))
        }

        isPaused = false
        pauseStartTimestamp = 0L
        totalPausedMs = 0L
        _typingState.value = TypingState.Preparing
        _progress.value = TypingProgress(totalChars = validation.sanitizedScript.length)

        activeJob = scope.launch(dispatcher) {
            val random = seed?.let { SeededTypingRandom(it) } ?: DefaultTypingRandom()
            val events = engine.generate(script, profile, random)
            val totalEvents = events.size
            val totalChars = validation.sanitizedScript.length
            val startTime = System.currentTimeMillis()

            var currentEventIdx = 0
            var currentCharIdx = 0

            try {
                for (event in events) {
                    ensureActive()

                    // Handle pause suspension before key event
                    while (isPaused || _typingState.value is TypingState.Paused) {
                        heldKeys.clear()
                        bluetoothRepository.releaseAllKeys()
                        delay(50)
                        ensureActive()
                    }

                    // Verify connection before each event
                    if (!bluetoothRepository.isConnected()) {
                        _typingState.value = TypingState.Failed(
                            TypingFailure.BluetoothDisconnected,
                            _progress.value
                        )
                        return@launch
                    }

                    if (event.delayMs > 0) {
                        delay(event.delayMs)
                    }
                    ensureActive()

                    // Handle pause suspension if paused during delay
                    while (isPaused || _typingState.value is TypingState.Paused) {
                        heldKeys.clear()
                        bluetoothRepository.releaseAllKeys()
                        delay(50)
                        ensureActive()
                    }

                    // Execute key action
                    val sendResult = when (val action = event.action) {
                        is KeyAction.KeyDown -> {
                            heldKeys.add(action.char)
                            currentCharIdx++
                            bluetoothRepository.sendKeyDown(action.char)
                        }
                        is KeyAction.KeyUp -> {
                            heldKeys.remove(action.char)
                            bluetoothRepository.sendKeyUp()
                        }
                        is KeyAction.ModifierDown -> {
                            // Shift modifier is packed into subsequent KeyDown report
                            Result.success(true)
                        }
                        is KeyAction.ModifierUp -> {
                            bluetoothRepository.sendKeyUp()
                        }
                        is KeyAction.Backspace -> {
                            val down = bluetoothRepository.sendKeyDown('\b')
                            delay(20)
                            val up = bluetoothRepository.sendKeyUp()
                            if (down.isSuccess && up.isSuccess) {
                                Result.success(true)
                            } else {
                                Result.failure(
                                    down.exceptionOrNull() ?: up.exceptionOrNull() ?: Exception("Backspace send failed")
                                )
                            }
                        }
                    }

                    if (sendResult.isFailure) {
                        val failureReason = if (!bluetoothRepository.isConnected()) {
                            TypingFailure.BluetoothDisconnected
                        } else {
                            TypingFailure.HidSendFailed
                        }
                        _typingState.value = TypingState.Failed(
                            failureReason,
                            _progress.value
                        )
                        return@launch
                    }

                    currentEventIdx++
                    val elapsed = (System.currentTimeMillis() - startTime - totalPausedMs).coerceAtLeast(0L)
                    val percentage = if (totalEvents > 0) {
                        (currentEventIdx.toFloat() / totalEvents) * 100f
                    } else {
                        100f
                    }

                    val currentProg = TypingProgress(
                        currentEvent = currentEventIdx,
                        totalEvents = totalEvents,
                        currentCharIndex = currentCharIdx.coerceAtMost(totalChars),
                        totalChars = totalChars,
                        percentage = percentage.coerceIn(0f, 100f),
                        elapsedMs = elapsed
                    )
                    _progress.value = currentProg
                    if (!isPaused && _typingState.value !is TypingState.Paused) {
                        _typingState.value = TypingState.Typing(currentProg)
                    }
                }

                val totalElapsed = (System.currentTimeMillis() - startTime - totalPausedMs).coerceAtLeast(0L)
                _typingState.value = TypingState.Completed(totalChars, totalElapsed)
            } catch (e: CancellationException) {
                _typingState.value = TypingState.Idle
                _progress.value = TypingProgress()
                throw e
            } catch (e: Exception) {
                _typingState.value = TypingState.Failed(
                    TypingFailure.InvalidState(e.message ?: "Unexpected typing error"),
                    _progress.value
                )
            } finally {
                withContext(NonCancellable) {
                    heldKeys.clear()
                    bluetoothRepository.releaseAllKeys()
                }
            }
        }

        return Result.success(Unit)
    }

    /**
     * Pauses the active typing session.
     * Guarantees all keys are released while paused.
     */
    suspend fun pauseTyping() = mutex.withLock {
        if (_typingState.value is TypingState.Typing || _typingState.value is TypingState.Preparing) {
            isPaused = true
            pauseStartTimestamp = System.currentTimeMillis()
            heldKeys.clear()
            bluetoothRepository.releaseAllKeys()
            _typingState.value = TypingState.Paused(_progress.value)
        }
    }

    /**
     * Resumes the paused typing session from the exact point of pause.
     */
    suspend fun resumeTyping() = mutex.withLock {
        if (isPaused || _typingState.value is TypingState.Paused) {
            isPaused = false
            if (pauseStartTimestamp > 0L) {
                totalPausedMs += (System.currentTimeMillis() - pauseStartTimestamp)
                pauseStartTimestamp = 0L
            }
            _typingState.value = TypingState.Typing(_progress.value)
        }
    }

    /**
     * Cancels any active typing session, guarantees release of all keys, and resets to Idle.
     */
    suspend fun stopTyping() = mutex.withLock {
        isPaused = false
        pauseStartTimestamp = 0L
        totalPausedMs = 0L
        val job = activeJob
        activeJob = null
        if (job != null && job.isActive) {
            _typingState.value = TypingState.Cancelling
            job.cancel()
            try {
                job.join()
            } catch (_: Exception) {
                // Ignore join errors
            }
        }
        heldKeys.clear()
        bluetoothRepository.releaseAllKeys()
        _progress.value = TypingProgress()
        _typingState.value = TypingState.Idle
    }

    /**
     * Resets the controller state to [TypingState.Idle].
     */
    fun reset() {
        _typingState.value = TypingState.Idle
        _progress.value = TypingProgress()
    }
}
