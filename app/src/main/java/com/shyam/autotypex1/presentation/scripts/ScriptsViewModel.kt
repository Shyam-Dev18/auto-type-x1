package com.shyam.autotypex1.presentation.scripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.repository.ScriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptsViewModel @Inject constructor(
    private val scriptRepository: ScriptRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptsUiState())
    val uiState: StateFlow<ScriptsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            scriptRepository.getAllScripts().collect { scripts ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        scripts = scripts
                    )
                }
            }
        }
    }

    fun onEvent(event: ScriptsUiEvent) {
        when (event) {
            is ScriptsUiEvent.OnSelect -> {
                viewModelScope.launch {
                    scriptRepository.selectScript(event.scriptId)
                }
            }
            is ScriptsUiEvent.OnDelete -> {
                viewModelScope.launch {
                    scriptRepository.deleteScript(event.scriptId)
                }
            }
            is ScriptsUiEvent.OnDismissError -> {
                _uiState.update { it.copy(error = null) }
            }
            else -> Unit
        }
    }
}
