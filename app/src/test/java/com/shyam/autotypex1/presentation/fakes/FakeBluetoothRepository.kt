package com.shyam.autotypex1.presentation.fakes

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBluetoothRepository : BluetoothRepository {
    val adapterStateFlow = MutableStateFlow(BluetoothAdapterState.Enabled)
    val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val scannedDevicesFlow = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val bondedDevicesFlow = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val isDiscoveringFlow = MutableStateFlow(false)

    var connectResult: Result<Unit> = Result.success(Unit)
    var isConnectedReturnValue: Boolean = false
    var lastConnectedAddress: String? = null
    var disconnectCalled = false
    var startDiscoveryCalled = false
    var stopDiscoveryCalled = false

    override fun observeAdapterState(): Flow<BluetoothAdapterState> = adapterStateFlow.asStateFlow()
    override fun observeConnectionState(): Flow<ConnectionState> = connectionStateFlow.asStateFlow()
    override fun observeScannedDevices(): Flow<List<ScannedDevice>> = scannedDevicesFlow.asStateFlow()
    override fun observeIsDiscovering(): Flow<Boolean> = isDiscoveringFlow.asStateFlow()

    override suspend fun startDiscovery(): Result<Unit> {
        startDiscoveryCalled = true
        isDiscoveringFlow.value = true
        return Result.success(Unit)
    }

    override suspend fun stopDiscovery() {
        stopDiscoveryCalled = true
        isDiscoveringFlow.value = false
    }

    override suspend fun connect(address: String): Result<Unit> {
        lastConnectedAddress = address
        if (connectResult.isSuccess) {
            isConnectedReturnValue = true
            connectionStateFlow.value = ConnectionState.Connected(
                deviceName = "Test Device",
                deviceAddress = address
            )
        }
        return connectResult
    }

    override suspend fun disconnect() {
        disconnectCalled = true
        isConnectedReturnValue = false
        connectionStateFlow.value = ConnectionState.Disconnected
    }

    override suspend fun sendKeyDown(char: Char): Result<Boolean> = Result.success(true)
    override suspend fun sendKeyUp(): Result<Boolean> = Result.success(true)
    override suspend fun releaseAllKeys(): Result<Boolean> = Result.success(true)
    override fun isConnected(): Boolean = isConnectedReturnValue
    override fun release() {
        disconnectCalled = true
    }
}
