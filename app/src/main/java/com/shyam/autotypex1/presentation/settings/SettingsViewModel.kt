package com.shyam.autotypex1.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.repository.SettingsRepository
import com.shyam.autotypex1.domain.typing.TypingProfile
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
        val currentProfile = _uiState.value.settings.profile
        when (event) {
            is SettingsUiEvent.OnPresetSelected -> {
                viewModelScope.launch {
                    settingsRepository.updateTypingProfile(event.profile)
                }
            }
            is SettingsUiEvent.OnWpmRangeChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    minWpm = event.minWpm.coerceAtLeast(5),
                    maxWpm = event.maxWpm.coerceAtLeast(event.minWpm.coerceAtLeast(5))
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnJitterChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    jitterPercent = event.jitterPercent.coerceIn(0, 100)
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnTypoProbabilityChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    typoProbability = event.probability.coerceIn(0.0, 1.0)
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnWordDelayChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    wordDelayMs = event.delayMs.coerceIn(0L, 5000L)
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnPunctuationMultiplierChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    punctuationDelayMultiplier = event.multiplier.coerceIn(1.0, 10.0)
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnThinkingPauseChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    thinkingPauseProbability = event.probability.coerceIn(0.0, 1.0)
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnBurstVariationChange -> {
                val updated = currentProfile.copy(
                    name = "Custom",
                    burstVariation = event.variation.coerceIn(0.0, 1.0)
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnCorrectionPauseRangeChange -> {
                val safeMin = event.minMs.coerceIn(10L, 2000L)
                val safeMax = event.maxMs.coerceIn(safeMin, 5000L)
                val updated = currentProfile.copy(
                    name = "Custom",
                    correctionPauseRangeMs = safeMin..safeMax
                )
                viewModelScope.launch { settingsRepository.updateTypingProfile(updated) }
            }
            is SettingsUiEvent.OnThemeModeChange -> {
                viewModelScope.launch {
                    settingsRepository.updateThemeMode(event.themeMode)
                }
            }
            is SettingsUiEvent.OnResetToDefaults -> {
                viewModelScope.launch {
                    settingsRepository.resetToDefaults()
                }
            }
            is SettingsUiEvent.OnDismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }
}
