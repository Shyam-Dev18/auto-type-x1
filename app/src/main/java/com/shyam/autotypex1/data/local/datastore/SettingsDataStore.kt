package com.shyam.autotypex1.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * DataStore-backed storage for application settings, UI preferences, and typing profiles.
 */
@Singleton
class SettingsDataStore(
    private val dataStore: DataStore<Preferences>
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context.settingsDataStore)

    companion object {
        val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")
        val KEY_MIN_WPM = intPreferencesKey("min_wpm")
        val KEY_MAX_WPM = intPreferencesKey("max_wpm")
        val KEY_JITTER_PERCENT = intPreferencesKey("jitter_percent")
        val KEY_TYPO_PROBABILITY = doublePreferencesKey("typo_probability")
        val KEY_WORD_DELAY_MS = longPreferencesKey("word_delay_ms")
        val KEY_PUNCTUATION_DELAY_MULTIPLIER = doublePreferencesKey("punctuation_delay_multiplier")
        val KEY_THINKING_PAUSE_PROBABILITY = doublePreferencesKey("thinking_pause_probability")
        val KEY_BURST_VARIATION = doublePreferencesKey("burst_variation")
        val KEY_CORRECTION_PAUSE_MIN_MS = longPreferencesKey("correction_pause_min_ms")
        val KEY_CORRECTION_PAUSE_MAX_MS = longPreferencesKey("correction_pause_max_ms")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * Observes live application settings, ensuring sanitized safe defaults on missing or corrupted values.
     */
    fun observeSettings(): Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            try {
                val defaultProfile = TypingProfile.NORMAL
                val name = prefs.getSafeString(KEY_PROFILE_NAME.name, defaultProfile.name)
                val minWpm = prefs.getSafeInt(KEY_MIN_WPM.name, defaultProfile.minWpm)
                val maxWpm = prefs.getSafeInt(KEY_MAX_WPM.name, defaultProfile.maxWpm)
                val jitterPercent = prefs.getSafeInt(KEY_JITTER_PERCENT.name, defaultProfile.jitterPercent)
                val typoProbability = prefs.getSafeDouble(KEY_TYPO_PROBABILITY.name, defaultProfile.typoProbability)
                val wordDelayMs = prefs.getSafeLong(KEY_WORD_DELAY_MS.name, defaultProfile.wordDelayMs)
                val punctuationDelayMultiplier = prefs.getSafeDouble(
                    KEY_PUNCTUATION_DELAY_MULTIPLIER.name,
                    defaultProfile.punctuationDelayMultiplier
                )
                val thinkingPauseProbability = prefs.getSafeDouble(
                    KEY_THINKING_PAUSE_PROBABILITY.name,
                    defaultProfile.thinkingPauseProbability
                )
                val burstVariation = prefs.getSafeDouble(
                    KEY_BURST_VARIATION.name,
                    defaultProfile.burstVariation
                )
                val corrMin = prefs.getSafeLong(
                    KEY_CORRECTION_PAUSE_MIN_MS.name,
                    defaultProfile.correctionPauseRangeMs.first
                )
                val corrMax = prefs.getSafeLong(
                    KEY_CORRECTION_PAUSE_MAX_MS.name,
                    defaultProfile.correctionPauseRangeMs.last
                )

                val themeModeStr = prefs.getSafeString(KEY_THEME_MODE.name, ThemeMode.SYSTEM.name)
                val themeMode = try {
                    ThemeMode.valueOf(themeModeStr)
                } catch (_: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                }

                val rawProfile = try {
                    TypingProfile(
                        name = name.ifBlank { "Custom" },
                        minWpm = minWpm,
                        maxWpm = maxWpm,
                        jitterPercent = jitterPercent,
                        typoProbability = typoProbability,
                        wordDelayMs = wordDelayMs,
                        punctuationDelayMultiplier = punctuationDelayMultiplier,
                        thinkingPauseProbability = thinkingPauseProbability,
                        burstVariation = burstVariation,
                        correctionPauseRangeMs = corrMin..corrMax
                    )
                } catch (_: IllegalArgumentException) {
                    TypingProfile.custom(
                        name = name.ifBlank { "Custom" },
                        minWpm = minWpm.coerceIn(5, 500),
                        maxWpm = maxWpm.coerceIn(minWpm.coerceIn(5, 500), 500),
                        jitterPercent = jitterPercent.coerceIn(0, 100),
                        typoProbability = typoProbability.coerceIn(0.0, 1.0),
                        wordDelayMs = wordDelayMs.coerceIn(0L, 5000L),
                        punctuationDelayMultiplier = punctuationDelayMultiplier.coerceIn(1.0, 10.0),
                        thinkingPauseProbability = thinkingPauseProbability.coerceIn(0.0, 1.0),
                        burstVariation = burstVariation.coerceIn(0.0, 1.0),
                        correctionPauseRangeMs = corrMin.coerceIn(10L, 2000L)..corrMax.coerceIn(
                            corrMin.coerceIn(10L, 2000L),
                            5000L
                        )
                    )
                }

                AppSettings(
                    profile = rawProfile.sanitized(),
                    themeMode = themeMode
                )
            } catch (_: Exception) {
                AppSettings(
                    profile = TypingProfile.NORMAL.sanitized(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

    /**
     * Retrieves current settings one-shot.
     */
    suspend fun getSettings(): AppSettings = observeSettings().first()

    /**
     * Saves full application settings.
     */
    suspend fun saveSettings(settings: AppSettings) {
        val sanitized = settings.profile.sanitized()
        dataStore.edit { prefs ->
            prefs[KEY_PROFILE_NAME] = sanitized.name
            prefs[KEY_MIN_WPM] = sanitized.minWpm
            prefs[KEY_MAX_WPM] = sanitized.maxWpm
            prefs[KEY_JITTER_PERCENT] = sanitized.jitterPercent
            prefs[KEY_TYPO_PROBABILITY] = sanitized.typoProbability
            prefs[KEY_WORD_DELAY_MS] = sanitized.wordDelayMs
            prefs[KEY_PUNCTUATION_DELAY_MULTIPLIER] = sanitized.punctuationDelayMultiplier
            prefs[KEY_THINKING_PAUSE_PROBABILITY] = sanitized.thinkingPauseProbability
            prefs[KEY_BURST_VARIATION] = sanitized.burstVariation
            prefs[KEY_CORRECTION_PAUSE_MIN_MS] = sanitized.correctionPauseRangeMs.first
            prefs[KEY_CORRECTION_PAUSE_MAX_MS] = sanitized.correctionPauseRangeMs.last
            prefs[KEY_THEME_MODE] = settings.themeMode.name
        }
    }

    /**
     * Updates typing profile values.
     */
    suspend fun updateTypingProfile(profile: TypingProfile) {
        val sanitized = profile.sanitized()
        dataStore.edit { prefs ->
            prefs[KEY_PROFILE_NAME] = sanitized.name
            prefs[KEY_MIN_WPM] = sanitized.minWpm
            prefs[KEY_MAX_WPM] = sanitized.maxWpm
            prefs[KEY_JITTER_PERCENT] = sanitized.jitterPercent
            prefs[KEY_TYPO_PROBABILITY] = sanitized.typoProbability
            prefs[KEY_WORD_DELAY_MS] = sanitized.wordDelayMs
            prefs[KEY_PUNCTUATION_DELAY_MULTIPLIER] = sanitized.punctuationDelayMultiplier
            prefs[KEY_THINKING_PAUSE_PROBABILITY] = sanitized.thinkingPauseProbability
            prefs[KEY_BURST_VARIATION] = sanitized.burstVariation
            prefs[KEY_CORRECTION_PAUSE_MIN_MS] = sanitized.correctionPauseRangeMs.first
            prefs[KEY_CORRECTION_PAUSE_MAX_MS] = sanitized.correctionPauseRangeMs.last
        }
    }

    /**
     * Updates theme mode preference.
     */
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = themeMode.name
        }
    }

    /**
     * Clears all stored preferences, resetting to default.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

private fun Preferences.getSafeDouble(keyName: String, fallback: Double): Double {
    for ((key, value) in asMap()) {
        if (key.name == keyName) {
            return when (value) {
                is Double -> value
                is Float -> value.toDouble()
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: fallback
                else -> fallback
            }
        }
    }
    return fallback
}

private fun Preferences.getSafeInt(keyName: String, fallback: Int): Int {
    for ((key, value) in asMap()) {
        if (key.name == keyName) {
            return when (value) {
                is Int -> value
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: fallback
                else -> fallback
            }
        }
    }
    return fallback
}

private fun Preferences.getSafeLong(keyName: String, fallback: Long): Long {
    for ((key, value) in asMap()) {
        if (key.name == keyName) {
            return when (value) {
                is Long -> value
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: fallback
                else -> fallback
            }
        }
    }
    return fallback
}

private fun Preferences.getSafeString(keyName: String, fallback: String): String {
    for ((key, value) in asMap()) {
        if (key.name == keyName) {
            return value.toString()
        }
    }
    return fallback
}

