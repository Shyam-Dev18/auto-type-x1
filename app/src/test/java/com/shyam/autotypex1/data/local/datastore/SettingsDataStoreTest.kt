package com.shyam.autotypex1.data.local.datastore

import androidx.datastore.preferences.core.edit
import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsDataStoreTest {

    private lateinit var fakeDataStore: FakeDataStore
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        fakeDataStore = FakeDataStore()
        settingsDataStore = SettingsDataStore(fakeDataStore)
    }

    @Test
    fun initialSettings_returnsNormalProfileAndSystemTheme() = runTest {
        val settings = settingsDataStore.getSettings()

        assertEquals("Normal", settings.profile.name)
        assertEquals(TypingProfile.NORMAL.minWpm, settings.profile.minWpm)
        assertEquals(TypingProfile.NORMAL.maxWpm, settings.profile.maxWpm)
        assertEquals(TypingProfile.NORMAL.jitterPercent, settings.profile.jitterPercent)
        assertEquals(TypingProfile.NORMAL.typoProbability, settings.profile.typoProbability, 0.0001)
        assertEquals(TypingProfile.NORMAL.wordDelayMs, settings.profile.wordDelayMs)
        assertEquals(
            TypingProfile.NORMAL.punctuationDelayMultiplier,
            settings.profile.punctuationDelayMultiplier,
            0.0001
        )
        assertEquals(
            TypingProfile.NORMAL.thinkingPauseProbability,
            settings.profile.thinkingPauseProbability,
            0.0001
        )
        assertEquals(TypingProfile.NORMAL.burstVariation, settings.profile.burstVariation, 0.0001)
        assertEquals(TypingProfile.NORMAL.correctionPauseRangeMs, settings.profile.correctionPauseRangeMs)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    @Test
    fun saveSettings_persistsAllTypingProfileFieldsAndTheme() = runTest {
        val customProfile = TypingProfile(
            name = "My Custom Profile",
            minWpm = 45,
            maxWpm = 75,
            jitterPercent = 18,
            typoProbability = 0.08,
            wordDelayMs = 120L,
            punctuationDelayMultiplier = 2.8,
            thinkingPauseProbability = 0.04,
            burstVariation = 0.22,
            correctionPauseRangeMs = 80L..250L
        )

        val newSettings = AppSettings(
            profile = customProfile,
            themeMode = ThemeMode.DARK
        )

        settingsDataStore.saveSettings(newSettings)
        val loaded = settingsDataStore.getSettings()

        assertEquals("My Custom Profile", loaded.profile.name)
        assertEquals(45, loaded.profile.minWpm)
        assertEquals(75, loaded.profile.maxWpm)
        assertEquals(18, loaded.profile.jitterPercent)
        assertEquals(0.08, loaded.profile.typoProbability, 0.0001)
        assertEquals(120L, loaded.profile.wordDelayMs)
        assertEquals(2.8, loaded.profile.punctuationDelayMultiplier, 0.0001)
        assertEquals(0.04, loaded.profile.thinkingPauseProbability, 0.0001)
        assertEquals(0.22, loaded.profile.burstVariation, 0.0001)
        assertEquals(80L..250L, loaded.profile.correctionPauseRangeMs)
        assertEquals(ThemeMode.DARK, loaded.themeMode)
    }

    @Test
    fun updateTypingProfile_updatesProfilePreservingTheme() = runTest {
        settingsDataStore.updateThemeMode(ThemeMode.LIGHT)
        settingsDataStore.updateTypingProfile(TypingProfile.FAST)

        val loaded = settingsDataStore.getSettings()
        assertEquals(TypingProfile.FAST.name, loaded.profile.name)
        assertEquals(TypingProfile.FAST.minWpm, loaded.profile.minWpm)
        assertEquals(TypingProfile.FAST.maxWpm, loaded.profile.maxWpm)
        assertEquals(ThemeMode.LIGHT, loaded.themeMode)
    }

    @Test
    fun speedDemonPreset_persistedCorrectly() = runTest {
        settingsDataStore.updateTypingProfile(TypingProfile.SPEED_DEMON)

        val loaded = settingsDataStore.getSettings()
        assertEquals("Speed Demon", loaded.profile.name)
        assertEquals(200, loaded.profile.minWpm)
        assertEquals(300, loaded.profile.maxWpm)
        assertEquals(3, loaded.profile.jitterPercent)
        assertEquals(0.005, loaded.profile.typoProbability, 0.0001)
    }

    @Test
    fun corruptOrOutOfBoundsValues_clampedToSafeRanges() = runTest {
        fakeDataStore.edit { prefs ->
            prefs[SettingsDataStore.KEY_MIN_Wpm_Corrupt()] = -50
            prefs[SettingsDataStore.KEY_MAX_Wpm_Corrupt()] = 2
            prefs[SettingsDataStore.KEY_JITTER_Corrupt()] = -10
            prefs[SettingsDataStore.KEY_TYPO_Corrupt()] = 5.0 // > 1.0
            prefs[SettingsDataStore.KEY_WORD_DELAY_Corrupt()] = -999L
            prefs[SettingsDataStore.KEY_PUNCT_Corrupt()] = 0.5 // < 1.0
            prefs[SettingsDataStore.KEY_THEME_MODE] = "INVALID_THEME_ENUM"
        }

        val loaded = settingsDataStore.getSettings()

        // Verify clamped within safe bounds
        assertTrue(loaded.profile.minWpm >= 5)
        assertTrue(loaded.profile.maxWpm >= loaded.profile.minWpm)
        assertTrue(loaded.profile.jitterPercent >= 0)
        assertTrue(loaded.profile.typoProbability in 0.0..1.0)
        assertTrue(loaded.profile.wordDelayMs >= 0L)
        assertTrue(loaded.profile.punctuationDelayMultiplier >= 1.0)
        assertEquals(ThemeMode.SYSTEM, loaded.themeMode)
    }

    @Test
    fun resetToDefaults_clearsAllKeys() = runTest {
        settingsDataStore.saveSettings(
            AppSettings(
                profile = TypingProfile.FAST,
                themeMode = ThemeMode.DARK
            )
        )

        settingsDataStore.resetToDefaults()
        val loaded = settingsDataStore.getSettings()

        assertEquals("Normal", loaded.profile.name)
        assertEquals(ThemeMode.SYSTEM, loaded.themeMode)
    }

    // Helper extensions for test corruption injection
    private fun SettingsDataStore.Companion.KEY_MIN_Wpm_Corrupt() = KEY_MIN_WPM
    private fun SettingsDataStore.Companion.KEY_MAX_Wpm_Corrupt() = KEY_MAX_WPM
    private fun SettingsDataStore.Companion.KEY_JITTER_Corrupt() = KEY_JITTER_PERCENT
    private fun SettingsDataStore.Companion.KEY_TYPO_Corrupt() = KEY_TYPO_PROBABILITY
    private fun SettingsDataStore.Companion.KEY_WORD_DELAY_Corrupt() = KEY_WORD_DELAY_MS
    private fun SettingsDataStore.Companion.KEY_PUNCT_Corrupt() = KEY_PUNCTUATION_DELAY_MULTIPLIER
}
