package com.shyam.autotypex1.presentation.home

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.usecase.RequestEnableBluetoothUseCase
import com.shyam.autotypex1.presentation.fakes.FakeBluetoothRepository
import com.shyam.autotypex1.presentation.fakes.FakeDeviceRepository
import com.shyam.autotypex1.presentation.fakes.FakeScriptRepository
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothRepo: FakeBluetoothRepository
    private lateinit var scriptRepo: FakeScriptRepository
    private lateinit var deviceRepo: FakeDeviceRepository
    private lateinit var requestEnableBluetooth: RequestEnableBluetoothUseCase
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        bluetoothRepo = FakeBluetoothRepository()
        scriptRepo = FakeScriptRepository()
        deviceRepo = FakeDeviceRepository()
        requestEnableBluetooth = RequestEnableBluetoothUseCase()

        viewModel = HomeViewModel(
            bluetoothRepository = bluetoothRepo,
            scriptRepository = scriptRepo,
            deviceRepository = deviceRepo,
            requestEnableBluetooth = requestEnableBluetooth
        )
    }

    @Test
    fun `initial state reflects repos default values`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(BluetoothAdapterState.Enabled, state.bluetoothState)
        assertEquals(ConnectionState.Disconnected, state.connectionState)
        assertTrue(state.savedDevices.isEmpty())
        assertNull(state.selectedScriptName)
        assertNull(state.selectedScriptId)
        assertNull(state.error)
    }

    @Test
    fun `connect to saved device succeeds and updates repo state`() = runTest {
        val targetAddress = "AA:BB:CC:DD:EE:FF"
        viewModel.onEvent(HomeUiEvent.OnSavedDeviceClick(targetAddress))

        assertEquals(targetAddress, bluetoothRepo.lastConnectedAddress)
        val state = viewModel.uiState.value
        assertTrue(state.connectionState is ConnectionState.Connected)
        assertEquals(targetAddress, (state.connectionState as ConnectionState.Connected).deviceAddress)
        assertNull(state.error)
    }

    @Test
    fun `connect failure updates error state`() = runTest {
        val targetAddress = "AA:BB:CC:DD:EE:FF"
        bluetoothRepo.connectResult = Result.failure(RuntimeException("Connection timed out"))

        viewModel.onEvent(HomeUiEvent.OnSavedDeviceClick(targetAddress))

        val state = viewModel.uiState.value
        assertEquals("Connection timed out", state.error)
        assertNull(state.pendingAddress)
    }

    @Test
    fun `disconnect event calls repo disconnect`() = runTest {
        bluetoothRepo.connect("AA:BB:CC:DD:EE:FF")
        viewModel.onEvent(HomeUiEvent.OnDisconnectClick)

        assertTrue(bluetoothRepo.disconnectCalled)
    }

    @Test
    fun `delete saved device removes from repo and disconnects if currently connected`() = runTest {
        val address = "AA:BB:CC:DD:EE:FF"
        deviceRepo.saveDevice("PC 1", address)
        bluetoothRepo.connect(address)

        viewModel.onEvent(HomeUiEvent.OnDeleteSavedDevice(address))

        val knownDevices = deviceRepo.getKnownDevices()
        assertTrue(knownDevices.isEmpty())
        assertTrue(bluetoothRepo.disconnectCalled)
    }

    @Test
    fun `reconnect calls connect with last connected address`() = runTest {
        val address = "AA:BB:CC:DD:EE:FF"
        deviceRepo.saveDevice("PC 1", address)

        viewModel.onEvent(HomeUiEvent.OnReconnectClick)

        assertEquals(address, bluetoothRepo.lastConnectedAddress)
    }

    @Test
    fun `dismiss error clears error in state`() = runTest {
        bluetoothRepo.connectResult = Result.failure(RuntimeException("Fail"))
        viewModel.onEvent(HomeUiEvent.OnSavedDeviceClick("AA:BB:CC:DD:EE:FF"))
        assertEquals("Fail", viewModel.uiState.value.error)

        viewModel.onEvent(HomeUiEvent.OnDismissError)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `selected script update from repo updates uiState`() = runTest {
        val scriptId = scriptRepo.insertScript(
            Script(
                id = 1L,
                name = "Test Script",
                content = "echo hello",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isSelected = true
            )
        )
        scriptRepo.selectScript(scriptId)

        val state = viewModel.uiState.value
        assertEquals("Test Script", state.selectedScriptName)
        assertEquals(scriptId, state.selectedScriptId)
    }
}
