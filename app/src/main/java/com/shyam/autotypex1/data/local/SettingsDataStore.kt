package com.shyam.autotypex1.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.model.TypingProfile
import com.shyam.autotypex1.domain.model.TypingProfileSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_PROFILE = stringPreferencesKey("profile")
        private val KEY_BASE_WPM_MIN = intPreferencesKey("base_wpm_min")
        private val KEY_BASE_WPM_MAX = intPreferencesKey("base_wpm_max")
        private val KEY_JITTER_PERCENT = intPreferencesKey("jitter_percent")
        private val KEY_TYPO_PROBABILITY = floatPreferencesKey("typo_probability")
        private val KEY_WORD_GAP_MS = intPreferencesKey("word_gap_ms")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    fun observeSettings(): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val profile = try {
            TypingProfile.valueOf(prefs[KEY_PROFILE] ?: "NORMAL")
        } catch (_: IllegalArgumentException) {
            TypingProfile.NORMAL
        }

        val themeMode = try {
            ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: "SYSTEM")
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }

        val defaultSpec = TypingProfileSpec.forProfile(profile)
        val spec = TypingProfileSpec(
            baseWpm = (prefs[KEY_BASE_WPM_MIN] ?: defaultSpec.baseWpm.first)..(prefs[KEY_BASE_WPM_MAX] ?: defaultSpec.baseWpm.last),
            jitterPercent = prefs[KEY_JITTER_PERCENT] ?: defaultSpec.jitterPercent,
            typoProbability = prefs[KEY_TYPO_PROBABILITY] ?: defaultSpec.typoProbability,
            wordGapMs = prefs[KEY_WORD_GAP_MS] ?: defaultSpec.wordGapMs
        )

        AppSettings(profile = profile, typingSpec = spec, themeMode = themeMode)
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_PROFILE] = settings.profile.name
            prefs[KEY_BASE_WPM_MIN] = settings.typingSpec.baseWpm.first
            prefs[KEY_BASE_WPM_MAX] = settings.typingSpec.baseWpm.last
            prefs[KEY_JITTER_PERCENT] = settings.typingSpec.jitterPercent
            prefs[KEY_TYPO_PROBABILITY] = settings.typingSpec.typoProbability
            prefs[KEY_WORD_GAP_MS] = settings.typingSpec.wordGapMs
            prefs[KEY_THEME_MODE] = settings.themeMode.name
        }
    }
}
