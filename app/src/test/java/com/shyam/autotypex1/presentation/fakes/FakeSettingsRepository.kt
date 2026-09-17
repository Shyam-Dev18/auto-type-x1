package com.shyam.autotypex1.presentation.fakes

import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.repository.SettingsRepository
import com.shyam.autotypex1.domain.typing.TypingProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    initialSettings: AppSettings = AppSettings()
) : SettingsRepository {

    private val settingsFlow = MutableStateFlow(initialSettings)

    override fun observeSettings(): Flow<AppSettings> = settingsFlow.asStateFlow()

    override suspend fun getSettings(): AppSettings = settingsFlow.value

    override suspend fun updateTypingProfile(profile: TypingProfile) {
        settingsFlow.value = settingsFlow.value.copy(profile = profile.sanitized())
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        settingsFlow.value = settingsFlow.value.copy(themeMode = themeMode)
    }

    override suspend fun resetToDefaults() {
        settingsFlow.value = AppSettings()
    }
}
