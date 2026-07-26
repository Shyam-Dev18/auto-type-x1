package com.shyam.autotypex1.presentation.scripts

import com.shyam.autotypex1.domain.model.Script

data class ScriptsUiState(
    val isLoading: Boolean = true,
    val scripts: List<Script> = emptyList()
)

sealed interface ScriptsUiEvent {
    data object OnCreateNew : ScriptsUiEvent
    data class OnEdit(val scriptId: Long) : ScriptsUiEvent
    data class OnDelete(val scriptId: Long) : ScriptsUiEvent
    data class OnSelect(val scriptId: Long) : ScriptsUiEvent
}
