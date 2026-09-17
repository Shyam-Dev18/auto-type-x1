package com.shyam.autotypex1.presentation.settings

import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val error: String? = null
)

sealed interface SettingsUiEvent {
    data class OnPresetSelected(val profile: TypingProfile) : SettingsUiEvent
    data class OnWpmRangeChange(val minWpm: Int, val maxWpm: Int) : SettingsUiEvent
    data class OnJitterChange(val jitterPercent: Int) : SettingsUiEvent
    data class OnTypoProbabilityChange(val probability: Double) : SettingsUiEvent
    data class OnWordDelayChange(val delayMs: Long) : SettingsUiEvent
    data class OnPunctuationMultiplierChange(val multiplier: Double) : SettingsUiEvent
    data class OnThinkingPauseChange(val probability: Double) : SettingsUiEvent
    data class OnBurstVariationChange(val variation: Double) : SettingsUiEvent
    data class OnCorrectionPauseRangeChange(val minMs: Long, val maxMs: Long) : SettingsUiEvent
    data class OnThemeModeChange(val themeMode: ThemeMode) : SettingsUiEvent
    data object OnResetToDefaults : SettingsUiEvent
    data object OnDismissError : SettingsUiEvent
}
