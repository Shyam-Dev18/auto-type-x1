package com.shyam.autotypex1.presentation.scripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.usecase.DeleteScriptUseCase
import com.shyam.autotypex1.domain.usecase.ObserveScriptsUseCase
import com.shyam.autotypex1.domain.usecase.SelectScriptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScriptsViewModel @Inject constructor(
    private val observeScripts: ObserveScriptsUseCase,
    private val deleteScript: DeleteScriptUseCase,
    private val selectScript: SelectScriptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScriptsUiState())
    val uiState: StateFlow<ScriptsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeScripts().collect { scripts ->
                _uiState.update { it.copy(isLoading = false, scripts = scripts) }
            }
        }
    }

    fun onEvent(event: ScriptsUiEvent) {
        when (event) {
            is ScriptsUiEvent.OnDelete -> viewModelScope.launch { deleteScript(event.scriptId) }
            is ScriptsUiEvent.OnSelect -> viewModelScope.launch { selectScript(event.scriptId) }
            else -> Unit // Navigation handled by NavGraph
        }
    }
}
