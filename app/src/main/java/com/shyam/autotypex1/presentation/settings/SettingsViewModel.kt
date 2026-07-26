package com.shyam.autotypex1.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        val currentSettings = _uiState.value.settings
        val updated = when (event) {
            is SettingsUiEvent.OnProfileChange -> {
                currentSettings.copy(
                    profile = event.profile,
                    typingSpec = com.shyam.autotypex1.domain.model.TypingProfileSpec.forProfile(event.profile)
                )
            }
            is SettingsUiEvent.OnWpmRangeChange -> {
                currentSettings.copy(
                    profile = com.shyam.autotypex1.domain.model.TypingProfile.CUSTOM,
                    typingSpec = currentSettings.typingSpec.copy(
                        baseWpm = event.min..event.max
                    )
                )
            }
            is SettingsUiEvent.OnJitterChange -> {
                currentSettings.copy(
                    profile = com.shyam.autotypex1.domain.model.TypingProfile.CUSTOM,
                    typingSpec = currentSettings.typingSpec.copy(
                        jitterPercent = event.percent
                    )
                )
            }
            is SettingsUiEvent.OnTypoProbabilityChange -> {
                currentSettings.copy(
                    profile = com.shyam.autotypex1.domain.model.TypingProfile.CUSTOM,
                    typingSpec = currentSettings.typingSpec.copy(
                        typoProbability = event.probability
                    )
                )
            }
            is SettingsUiEvent.OnWordGapChange -> {
                currentSettings.copy(
                    profile = com.shyam.autotypex1.domain.model.TypingProfile.CUSTOM,
                    typingSpec = currentSettings.typingSpec.copy(
                        wordGapMs = event.gapMs
                    )
                )
            }
            is SettingsUiEvent.OnThemeModeChange -> {
                currentSettings.copy(themeMode = event.mode)
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            settingsRepository.updateSettings(updated)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, settings = updated) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
        }
    }
}
