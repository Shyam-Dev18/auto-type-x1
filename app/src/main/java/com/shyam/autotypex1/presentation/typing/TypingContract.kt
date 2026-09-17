package com.shyam.autotypex1.presentation.typing

import com.shyam.autotypex1.domain.typing.TypingProgress
import com.shyam.autotypex1.domain.typing.TypingState

data class TypingUiState(
    val scriptName: String = "",
    val scriptContent: String = "",
    val profileName: String = "Normal",
    val typingState: TypingState = TypingState.Idle,
    val progress: TypingProgress = TypingProgress(),
    val isConnected: Boolean = false,
    val isReady: Boolean = false,
    val error: String? = null
)

sealed interface TypingUiEvent {
    data object OnStart : TypingUiEvent
    data object OnPause : TypingUiEvent
    data object OnResume : TypingUiEvent
    data object OnStop : TypingUiEvent
    data object OnReset : TypingUiEvent
    data object OnDismissError : TypingUiEvent
}
