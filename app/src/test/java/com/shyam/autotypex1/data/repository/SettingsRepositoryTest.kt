package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.datastore.FakeDataStore
import com.shyam.autotypex1.data.local.datastore.SettingsDataStore
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setUp() {
        val fakeDataStore = FakeDataStore()
        settingsDataStore = SettingsDataStore(fakeDataStore)
        settingsRepository = SettingsRepositoryImpl(settingsDataStore)
    }

    @Test
    fun getSettings_returnsInitialDefaultSettings() = runTest {
        val settings = settingsRepository.getSettings()

        assertEquals("Normal", settings.profile.name)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    @Test
    fun updateTypingProfile_andObserve() = runTest {
        settingsRepository.updateTypingProfile(TypingProfile.FAST)

        val settings = settingsRepository.observeSettings().first()
        assertEquals("Fast", settings.profile.name)
        assertEquals(TypingProfile.FAST.minWpm, settings.profile.minWpm)
    }

    @Test
    fun updateThemeMode_andGetSettings() = runTest {
        settingsRepository.updateThemeMode(ThemeMode.DARK)

        val settings = settingsRepository.getSettings()
        assertEquals(ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun resetToDefaults_restoresNormalProfileAndSystemTheme() = runTest {
        settingsRepository.updateTypingProfile(TypingProfile.SLOW)
        settingsRepository.updateThemeMode(ThemeMode.LIGHT)

        settingsRepository.resetToDefaults()

        val settings = settingsRepository.getSettings()
        assertEquals("Normal", settings.profile.name)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }
}
