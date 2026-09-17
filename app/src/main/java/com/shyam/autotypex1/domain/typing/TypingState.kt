package com.shyam.autotypex1.domain.typing

/**
 * Explicit sealed state machine for typing execution.
 */
sealed interface TypingState {
    /** No typing operation is active. */
    data object Idle : TypingState

    /** Preparing script, initializing engine, and verifying Bluetooth readiness. */
    data object Preparing : TypingState

    /** Actively executing keystrokes. */
    data class Typing(val progress: TypingProgress) : TypingState

    /** Typing is paused by user. Keystrokes halted, all keys released. */
    data class Paused(val progress: TypingProgress) : TypingState

    /** Cancellation requested, releasing keys and winding down. */
    data object Cancelling : TypingState

    /** Typing finished successfully. */
    data class Completed(val totalChars: Int, val elapsedMs: Long) : TypingState

    /** Typing was cancelled by the user or system before completion. */
    data class Cancelled(val progress: TypingProgress) : TypingState

    /** Typing failed due to an explicit [TypingFailure]. */
    data class Failed(val failure: TypingFailure, val progress: TypingProgress? = null) : TypingState
}

/**
 * Progress snapshot during typing execution.
 *
 * @property currentEvent      1-based index of the event currently being processed.
 * @property totalEvents       Total number of timed key events in the sequence.
 * @property currentCharIndex  1-based index of character reached in the script.
 * @property totalChars        Total number of characters in the sanitized script.
 * @property percentage        Completion percentage in [0.0f, 100.0f].
 * @property elapsedMs         Elapsed time in milliseconds since typing started.
 */
data class TypingProgress(
    val currentEvent: Int = 0,
    val totalEvents: Int = 0,
    val currentCharIndex: Int = 0,
    val totalChars: Int = 0,
    val percentage: Float = 0.0f,
    val elapsedMs: Long = 0L
)

/**
 * Typed failure reasons for typing operations.
 */
sealed interface TypingFailure {
    /** Remote host disconnected during typing. */
    data object BluetoothDisconnected : TypingFailure

    /** HID report dispatch failed to Bluetooth stack. */
    data object HidSendFailed : TypingFailure

    /** Operation was cancelled. */
    data object TypingCancelled : TypingFailure

    /** Bluetooth adapter was turned off during or before typing. */
    data object BluetoothDisabled : TypingFailure

    /** No active HID connection to a host device. */
    data object NoHidConnection : TypingFailure

    /** Script contains no supported characters or is empty. */
    data object ScriptEmpty : TypingFailure

    /** Controller state transition was invalid. */
    data class InvalidState(val message: String) : TypingFailure
}
