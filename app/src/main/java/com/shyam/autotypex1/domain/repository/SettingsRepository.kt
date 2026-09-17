package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for application settings and typing profiles.
 */
interface SettingsRepository {
    /**
     * Observes live application settings stream.
     */
    fun observeSettings(): Flow<AppSettings>

    /**
     * Retrieves current application settings (one-shot).
     */
    suspend fun getSettings(): AppSettings

    /**
     * Saves a new or updated typing profile.
     */
    suspend fun updateTypingProfile(profile: TypingProfile)

    /**
     * Updates the app theme mode.
     */
    suspend fun updateThemeMode(themeMode: ThemeMode)

    /**
     * Resets settings back to default values.
     */
    suspend fun resetToDefaults()
}
