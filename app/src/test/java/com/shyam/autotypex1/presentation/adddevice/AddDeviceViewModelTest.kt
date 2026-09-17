package com.shyam.autotypex1.presentation.adddevice

import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.presentation.fakes.FakeBluetoothRepository
import com.shyam.autotypex1.presentation.fakes.FakeDeviceRepository
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
class AddDeviceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothRepo: FakeBluetoothRepository
    private lateinit var deviceRepo: FakeDeviceRepository
    private lateinit var viewModel: AddDeviceViewModel

    @Before
    fun setUp() {
        bluetoothRepo = FakeBluetoothRepository()
        deviceRepo = FakeDeviceRepository()
        viewModel = AddDeviceViewModel(
            bluetoothRepository = bluetoothRepo,
            deviceRepository = deviceRepo
        )
    }

    @Test
    fun `init automatically starts discovery`() = runTest {
        assertTrue(bluetoothRepo.startDiscoveryCalled)
        assertTrue(viewModel.uiState.value.isScanning)
    }

    @Test
    fun `scanned devices flow updates uiState`() = runTest {
        val testDevices = listOf(
            ScannedDevice("Device 1", "AA:BB:CC:DD:EE:01", bonded = false),
            ScannedDevice("Device 2", "AA:BB:CC:DD:EE:02", bonded = true)
        )
        bluetoothRepo.scannedDevicesFlow.value = testDevices

        val state = viewModel.uiState.value
        assertEquals(2, state.devices.size)
        assertEquals("Device 1", state.devices[0].name)
        assertEquals("Device 2", state.devices[1].name)
    }

    @Test
    fun `stop scan event stops discovery`() = runTest {
        viewModel.onEvent(AddDeviceUiEvent.OnStopScan)
        assertTrue(bluetoothRepo.stopDiscoveryCalled)
        assertFalse(viewModel.uiState.value.isScanning)
    }

    @Test
    fun `connect event stops discovery and connects to device`() = runTest {
        val address = "AA:BB:CC:DD:EE:FF"
        viewModel.onEvent(AddDeviceUiEvent.OnConnect(address))

        assertTrue(bluetoothRepo.stopDiscoveryCalled)
        assertEquals(address, bluetoothRepo.lastConnectedAddress)
        val state = viewModel.uiState.value
        assertTrue(state.connectionState is ConnectionState.Connected)
        assertEquals(address, (state.connectionState as ConnectionState.Connected).deviceAddress)
    }

    @Test
    fun `connect failure sets error and clears pendingAddress`() = runTest {
        bluetoothRepo.connectResult = Result.failure(RuntimeException("Pairing failed"))
        val address = "AA:BB:CC:DD:EE:FF"

        viewModel.onEvent(AddDeviceUiEvent.OnConnect(address))

        val state = viewModel.uiState.value
        assertEquals("Pairing failed", state.error)
        assertNull(state.pendingAddress)
    }

    @Test
    fun `dismiss error clears error in state`() = runTest {
        bluetoothRepo.connectResult = Result.failure(RuntimeException("Pairing failed"))
        viewModel.onEvent(AddDeviceUiEvent.OnConnect("AA:BB:CC:DD:EE:FF"))
        assertEquals("Pairing failed", viewModel.uiState.value.error)

        viewModel.onEvent(AddDeviceUiEvent.OnDismissError)
        assertNull(viewModel.uiState.value.error)
    }
}
