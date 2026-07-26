package com.shyam.autotypex1.presentation.scripteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.usecase.LoadScriptUseCase
import com.shyam.autotypex1.domain.usecase.SaveScriptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loadScript: LoadScriptUseCase,
    private val saveScript: SaveScriptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptEditorUiState())
    val uiState: StateFlow<ScriptEditorUiState> = _uiState.asStateFlow()

    init {
        val id = savedStateHandle.get<String>("id")?.toLongOrNull()
        if (id != null && id > 0) {
            _uiState.update { it.copy(scriptId = id) }
            viewModelScope.launch {
                val script = loadScript(id)
                if (script != null) {
                    _uiState.update { it.copy(name = script.name, content = script.content) }
                }
            }
        }
    }

    fun onEvent(event: ScriptEditorUiEvent) {
        when (event) {
            is ScriptEditorUiEvent.OnNameChange -> _uiState.update { it.copy(name = event.value) }
            is ScriptEditorUiEvent.OnContentChange -> _uiState.update { it.copy(content = event.value) }
            is ScriptEditorUiEvent.OnSave -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.name.isBlank() || state.content.isBlank()) {
            _uiState.update { it.copy(error = "Name and content are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            saveScript(state.name, state.content, state.scriptId)
                .onSuccess { _uiState.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e -> _uiState.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}
