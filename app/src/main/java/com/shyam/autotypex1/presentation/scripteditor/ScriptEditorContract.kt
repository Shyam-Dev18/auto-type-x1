package com.shyam.autotypex1.presentation.scripteditor

data class ScriptEditorUiState(
    val scriptId: Long? = null,
    val name: String = "",
    val content: String = "",
    val characterCount: Int = 0,
    val lineCount: Int = 0,
    val unsupportedChars: Set<Char> = emptySet(),
    val saved: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface ScriptEditorUiEvent {
    data class OnNameChange(val name: String) : ScriptEditorUiEvent
    data class OnContentChange(val content: String) : ScriptEditorUiEvent
    data object OnSave : ScriptEditorUiEvent
    data object OnDelete : ScriptEditorUiEvent
    data object OnDismissError : ScriptEditorUiEvent
}
