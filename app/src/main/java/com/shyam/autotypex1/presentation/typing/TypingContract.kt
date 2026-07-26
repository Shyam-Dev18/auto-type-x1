package com.shyam.autotypex1.presentation.typing

import com.shyam.autotypex1.domain.model.TypingState

data class TypingUiState(
    val scriptName: String = "",
    val scriptContent: String = "",
    val typingState: TypingState = TypingState.Idle,
    val isReady: Boolean = false,
    val error: String? = null
)

sealed interface TypingUiEvent {
    data object OnStart : TypingUiEvent
    data object OnPause : TypingUiEvent
    data object OnResume : TypingUiEvent
    data object OnStop : TypingUiEvent
    data object OnDismissError : TypingUiEvent
}
