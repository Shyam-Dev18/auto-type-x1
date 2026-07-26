package com.shyam.autotypex1.presentation.scripteditor

data class ScriptEditorUiState(
    val scriptId: Long? = null,
    val name: String = "",
    val content: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

sealed interface ScriptEditorUiEvent {
    data class OnNameChange(val value: String) : ScriptEditorUiEvent
    data class OnContentChange(val value: String) : ScriptEditorUiEvent
    data object OnSave : ScriptEditorUiEvent
}
