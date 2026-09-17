package com.shyam.autotypex1.presentation.typing

import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.typing.DefaultHumanTypingEngine
import com.shyam.autotypex1.domain.typing.TypingController
import com.shyam.autotypex1.domain.typing.TypingProfile
import com.shyam.autotypex1.domain.typing.TypingState
import com.shyam.autotypex1.presentation.fakes.FakeBluetoothRepository
import com.shyam.autotypex1.presentation.fakes.FakeScriptRepository
import com.shyam.autotypex1.presentation.fakes.FakeSettingsRepository
import com.shyam.autotypex1.presentation.fakes.FakeTypingServiceLauncher
import com.shyam.autotypex1.presentation.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TypingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var scriptRepo: FakeScriptRepository
    private lateinit var settingsRepo: FakeSettingsRepository
    private lateinit var bluetoothRepo: FakeBluetoothRepository
    private lateinit var typingController: TypingController
    private lateinit var serviceLauncher: FakeTypingServiceLauncher
    private lateinit var viewModel: TypingViewModel

    @Before
    fun setUp() {
        scriptRepo = FakeScriptRepository()
        settingsRepo = FakeSettingsRepository()
        bluetoothRepo = FakeBluetoothRepository()
        typingController = TypingController(
            engine = DefaultHumanTypingEngine(),
            bluetoothRepository = bluetoothRepo
        )
        serviceLauncher = FakeTypingServiceLauncher()

        viewModel = TypingViewModel(
            scriptRepository = scriptRepo,
            settingsRepository = settingsRepo,
            bluetoothRepository = bluetoothRepo,
            typingController = typingController,
            serviceLauncher = serviceLauncher
        )
    }

    @Test
    fun `initial state is not ready when disconnected and no script`() = runTest {
        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertFalse(state.isReady)
        assertEquals("No Script Selected", state.scriptName)
        assertEquals(TypingState.Idle, state.typingState)
    }

    @Test
    fun `isReady becomes true when device connected and script selected`() = runTest {
        val scriptId = scriptRepo.insertScript(
            Script(
                id = 1L,
                name = "My Script",
                content = "Hello World",
                createdAt = 1000L,
                updatedAt = 1000L,
                isSelected = true
            )
        )
        scriptRepo.selectScript(scriptId)
        bluetoothRepo.connect("AA:BB:CC:DD:EE:FF")

        val state = viewModel.uiState.value
        assertTrue(state.isConnected)
        assertTrue(state.isReady)
        assertEquals("My Script", state.scriptName)
        assertEquals("Hello World", state.scriptContent)
    }

    @Test
    fun `onStart triggers serviceLauncher when ready`() = runTest {
        val scriptId = scriptRepo.insertScript(
            Script(
                id = 1L,
                name = "My Script",
                content = "Hello World",
                createdAt = 1000L,
                updatedAt = 1000L,
                isSelected = true
            )
        )
        scriptRepo.selectScript(scriptId)
        bluetoothRepo.connect("AA:BB:CC:DD:EE:FF")

        viewModel.onEvent(TypingUiEvent.OnStart)

        assertTrue(serviceLauncher.startCalled)
        assertEquals("Hello World", serviceLauncher.lastScript)
    }

    @Test
    fun `onStart shows error when disconnected`() = runTest {
        val scriptId = scriptRepo.insertScript(
            Script(
                id = 1L,
                name = "My Script",
                content = "Hello World",
                createdAt = 1000L,
                updatedAt = 1000L,
                isSelected = true
            )
        )
        scriptRepo.selectScript(scriptId)

        viewModel.onEvent(TypingUiEvent.OnStart)

        assertFalse(serviceLauncher.startCalled)
        assertEquals("Device is not connected", viewModel.uiState.value.error)
    }

    @Test
    fun `onStart shows error when script is empty`() = runTest {
        bluetoothRepo.connect("AA:BB:CC:DD:EE:FF")

        viewModel.onEvent(TypingUiEvent.OnStart)

        assertFalse(serviceLauncher.startCalled)
        assertEquals("Script content is empty", viewModel.uiState.value.error)
    }

    @Test
    fun `onPause triggers serviceLauncher pause`() = runTest {
        viewModel.onEvent(TypingUiEvent.OnPause)
        assertTrue(serviceLauncher.pauseCalled)
    }

    @Test
    fun `onResume triggers serviceLauncher resume`() = runTest {
        viewModel.onEvent(TypingUiEvent.OnResume)
        assertTrue(serviceLauncher.resumeCalled)
    }

    @Test
    fun `onStop triggers serviceLauncher stop`() = runTest {
        viewModel.onEvent(TypingUiEvent.OnStop)
        assertTrue(serviceLauncher.stopCalled)
    }

    @Test
    fun `dismiss error clears error`() = runTest {
        viewModel.onEvent(TypingUiEvent.OnStart)
        assertEquals("Device is not connected", viewModel.uiState.value.error)

        viewModel.onEvent(TypingUiEvent.OnDismissError)
        assertNull(viewModel.uiState.value.error)
    }
}
