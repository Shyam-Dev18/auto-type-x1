package com.shyam.autotypex1.presentation.scripteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.repository.ScriptRepository
import com.shyam.autotypex1.domain.typing.CharacterValidator
import com.shyam.autotypex1.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val scriptRepository: ScriptRepository
) : ViewModel() {

    private val scriptIdArg: Long? = savedStateHandle.get<String>(Screen.ScriptEditor.ARG_SCRIPT_ID)?.toLongOrNull()
        ?: savedStateHandle.get<Long>(Screen.ScriptEditor.ARG_SCRIPT_ID)?.takeIf { it > 0L }

    private val _uiState = MutableStateFlow(ScriptEditorUiState(scriptId = scriptIdArg))
    val uiState: StateFlow<ScriptEditorUiState> = _uiState.asStateFlow()

    init {
        scriptIdArg?.let { id ->
            viewModelScope.launch {
                val script = scriptRepository.getScriptById(id)
                if (script != null) {
                    val validation = CharacterValidator.validate(script.content)
                    _uiState.update {
                        it.copy(
                            name = script.name,
                            content = script.content,
                            characterCount = script.characterCount,
                            lineCount = script.lineCount,
                            unsupportedChars = validation.unsupportedChars
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ScriptEditorUiEvent) {
        when (event) {
            is ScriptEditorUiEvent.OnNameChange -> {
                _uiState.update { it.copy(name = event.name, error = null) }
            }
            is ScriptEditorUiEvent.OnContentChange -> {
                val validation = CharacterValidator.validate(event.content)
                val lines = if (event.content.isEmpty()) 0 else event.content.lines().size
                _uiState.update {
                    it.copy(
                        content = event.content,
                        characterCount = event.content.length,
                        lineCount = lines,
                        unsupportedChars = validation.unsupportedChars,
                        error = null
                    )
                }
            }
            is ScriptEditorUiEvent.OnSave -> saveScript()
            is ScriptEditorUiEvent.OnDelete -> deleteScript()
            is ScriptEditorUiEvent.OnDismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun saveScript() {
        val state = _uiState.value
        val cleanName = state.name.trim()
        val cleanContent = state.content

        if (cleanName.isBlank()) {
            _uiState.update { it.copy(error = "Script name cannot be blank") }
            return
        }
        if (cleanContent.isBlank()) {
            _uiState.update { it.copy(error = "Script content cannot be blank") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val id = state.scriptId
            if (id != null && id > 0L) {
                val existing = scriptRepository.getScriptById(id)
                val updated = (existing ?: Script(id = id, name = cleanName, content = cleanContent)).copy(
                    name = cleanName,
                    content = cleanContent,
                    updatedAt = System.currentTimeMillis()
                )
                scriptRepository.updateScript(updated)
            } else {
                val newScript = Script(
                    name = cleanName,
                    content = cleanContent,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val newId = scriptRepository.insertScript(newScript)
                scriptRepository.selectScript(newId)
            }
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }

    private fun deleteScript() {
        val id = _uiState.value.scriptId ?: return
        viewModelScope.launch {
            scriptRepository.deleteScript(id)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
