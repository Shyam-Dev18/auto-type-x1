package com.shyam.autotypex1.presentation.typing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.core.service.TypingForegroundService
import com.shyam.autotypex1.domain.model.TypingState
import com.shyam.autotypex1.domain.usecase.ObserveSelectedScriptUseCase
import com.shyam.autotypex1.domain.usecase.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TypingViewModel @Inject constructor(
    private val app: Application,
    private val observeSelectedScript: ObserveSelectedScriptUseCase,
    private val observeSettings: ObserveSettingsUseCase
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(TypingUiState())
    val uiState: StateFlow<TypingUiState> = _uiState.asStateFlow()

    private var currentSpec: com.shyam.autotypex1.domain.model.TypingProfileSpec? = null

    init {
        viewModelScope.launch {
            combine(
                observeSelectedScript(),
                observeSettings(),
                TypingForegroundService.typingState
            ) { script, settings, typingState ->
                currentSpec = settings.typingSpec
                TypingUiState(
                    scriptName = script?.name ?: "No Script Selected",
                    scriptContent = script?.content ?: "",
                    typingState = typingState,
                    isReady = script != null,
                    error = null
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onEvent(event: TypingUiEvent) {
        val state = _uiState.value
        when (event) {
            TypingUiEvent.OnStart -> {
                val spec = currentSpec ?: return
                if (state.isReady && state.scriptContent.isNotEmpty()) {
                    TypingForegroundService.startTyping(app, state.scriptContent, spec)
                } else {
                    _uiState.update { it.copy(error = "Script content is empty or not selected") }
                }
            }
            TypingUiEvent.OnPause -> {
                TypingForegroundService.pause(app)
            }
            TypingUiEvent.OnResume -> {
                TypingForegroundService.resume(app)
            }
            TypingUiEvent.OnStop -> {
                TypingForegroundService.stopTyping(app)
            }
            TypingUiEvent.OnDismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }
}
