package com.shyam.autotypex1.domain.model

/** Typing execution state — sealed for exhaustive matching, carries progress/failure data. */
sealed interface TypingState {
    data object Idle : TypingState
    data class Running(val progress: TypingProgress) : TypingState
    data class Paused(val progress: TypingProgress) : TypingState
    data class Finished(val progress: TypingProgress) : TypingState
    data class Errored(val progress: TypingProgress, val reason: TypingFailure) : TypingState
}

enum class TypingFailure {
    CONNECTION_LOST,
    PERMISSION_REVOKED,
    SERVICE_KILLED,
    UNKNOWN
}

data class TypingProgress(
    val charsTyped: Int,
    val totalChars: Int,
    val elapsedMs: Long
) {
    val percent: Float
        get() = if (totalChars > 0) charsTyped.toFloat() / totalChars else 0f
}
