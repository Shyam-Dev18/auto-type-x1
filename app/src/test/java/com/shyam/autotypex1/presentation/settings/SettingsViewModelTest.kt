package com.shyam.autotypex1.presentation.settings

import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile
import com.shyam.autotypex1.presentation.fakes.FakeSettingsRepository
import com.shyam.autotypex1.presentation.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        settingsRepo = FakeSettingsRepository()
        viewModel = SettingsViewModel(settingsRepository = settingsRepo)
    }

    @Test
    fun `initial state reflects default settings`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("Normal", state.settings.profile.name)
        assertEquals(ThemeMode.SYSTEM, state.settings.themeMode)
    }

    @Test
    fun `preset selection updates repository profile`() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnPresetSelected(TypingProfile.FAST))

        val current = settingsRepo.observeSettings().first()
        assertEquals("Fast", current.profile.name)
        assertEquals(TypingProfile.FAST.minWpm, current.profile.minWpm)
        assertEquals(TypingProfile.FAST.maxWpm, current.profile.maxWpm)
    }

    @Test
    fun `wpm range change updates to custom profile`() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnWpmRangeChange(minWpm = 35, maxWpm = 65))

        val current = settingsRepo.observeSettings().first()
        assertEquals("Custom", current.profile.name)
        assertEquals(35, current.profile.minWpm)
        assertEquals(65, current.profile.maxWpm)
    }

    @Test
    fun `jitter change updates to custom profile`() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnJitterChange(30))

        val current = settingsRepo.observeSettings().first()
        assertEquals("Custom", current.profile.name)
        assertEquals(30, current.profile.jitterPercent)
    }

    @Test
    fun `theme mode change updates settings repository`() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnThemeModeChange(ThemeMode.DARK))

        val current = settingsRepo.observeSettings().first()
        assertEquals(ThemeMode.DARK, current.themeMode)
    }

    @Test
    fun `reset to defaults resets profile and theme`() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnThemeModeChange(ThemeMode.DARK))
        viewModel.onEvent(SettingsUiEvent.OnPresetSelected(TypingProfile.SPEED_DEMON))

        viewModel.onEvent(SettingsUiEvent.OnResetToDefaults)

        val current = settingsRepo.observeSettings().first()
        assertEquals("Normal", current.profile.name)
        assertEquals(ThemeMode.SYSTEM, current.themeMode)
    }
}
