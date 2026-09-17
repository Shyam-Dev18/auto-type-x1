package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.datastore.SettingsDataStore
import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.repository.SettingsRepository
import com.shyam.autotypex1.domain.typing.TypingProfile
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [SettingsRepository] backed by DataStore Preferences.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        settingsDataStore.observeSettings()

    override suspend fun getSettings(): AppSettings =
        settingsDataStore.getSettings()

    override suspend fun updateTypingProfile(profile: TypingProfile) =
        settingsDataStore.updateTypingProfile(profile)

    override suspend fun updateThemeMode(themeMode: ThemeMode) =
        settingsDataStore.updateThemeMode(themeMode)

    override suspend fun resetToDefaults() =
        settingsDataStore.resetToDefaults()
}
