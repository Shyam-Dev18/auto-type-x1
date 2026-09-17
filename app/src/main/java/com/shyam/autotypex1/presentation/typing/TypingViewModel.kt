package com.shyam.autotypex1.presentation.typing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import com.shyam.autotypex1.domain.repository.ScriptRepository
import com.shyam.autotypex1.domain.repository.SettingsRepository
import com.shyam.autotypex1.domain.typing.TypingController
import com.shyam.autotypex1.domain.typing.TypingServiceLauncher
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
    private val scriptRepository: ScriptRepository,
    private val settingsRepository: SettingsRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val typingController: TypingController,
    private val serviceLauncher: TypingServiceLauncher
) : ViewModel() {

    private val _uiState = MutableStateFlow(TypingUiState())
    val uiState: StateFlow<TypingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                scriptRepository.getSelectedScript(),
                settingsRepository.observeSettings(),
                bluetoothRepository.observeConnectionState(),
                typingController.typingState,
                typingController.progress
            ) { script, settings, connectionState, typingState, progress ->
                val isConnected = connectionState is ConnectionState.Connected
                val isReady = isConnected && script != null && script.content.isNotBlank()

                TypingUiState(
                    scriptName = script?.name ?: "No Script Selected",
                    scriptContent = script?.content ?: "",
                    profileName = settings.profile.name,
                    typingState = typingState,
                    progress = progress,
                    isConnected = isConnected,
                    isReady = isReady,
                    error = null
                )
            }.collect { state ->
                _uiState.update { current ->
                    state.copy(error = current.error)
                }
            }
        }
    }

    fun onEvent(event: TypingUiEvent) {
        when (event) {
            TypingUiEvent.OnStart -> {
                val state = _uiState.value
                if (!state.isConnected) {
                    _uiState.update { it.copy(error = "Device is not connected") }
                    return
                }
                if (state.scriptContent.isBlank()) {
                    _uiState.update { it.copy(error = "Script content is empty") }
                    return
                }
                serviceLauncher.startTyping(
                    script = state.scriptContent,
                    profileName = state.profileName
                )
            }
            TypingUiEvent.OnPause -> {
                serviceLauncher.pause()
            }
            TypingUiEvent.OnResume -> {
                serviceLauncher.resume()
            }
            TypingUiEvent.OnStop -> {
                serviceLauncher.stop()
            }
            TypingUiEvent.OnReset -> {
                typingController.reset()
            }
            TypingUiEvent.OnDismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }
}
