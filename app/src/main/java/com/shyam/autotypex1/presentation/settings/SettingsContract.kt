package com.shyam.autotypex1.presentation.settings

import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isSaving: Boolean = false,
    val error: String? = null
)

sealed interface SettingsUiEvent {
    data class OnProfileChange(val profile: com.shyam.autotypex1.domain.model.TypingProfile) : SettingsUiEvent
    data class OnWpmRangeChange(val min: Int, val max: Int) : SettingsUiEvent
    data class OnJitterChange(val percent: Int) : SettingsUiEvent
    data class OnTypoProbabilityChange(val probability: Float) : SettingsUiEvent
    data class OnWordGapChange(val gapMs: Int) : SettingsUiEvent
    data class OnThemeModeChange(val mode: ThemeMode) : SettingsUiEvent
}
