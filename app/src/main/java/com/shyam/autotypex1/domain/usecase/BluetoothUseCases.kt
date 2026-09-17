package com.shyam.autotypex1.domain.usecase

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the Bluetooth adapter hardware state.
 */
class ObserveAdapterStateUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<BluetoothAdapterState> = repository.observeAdapterState()
}

/**
 * Observes the Bluetooth connection lifecycle state.
 */
class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<ConnectionState> = repository.observeConnectionState()
}

/**
 * Observes devices found during Classic discovery.
 */
class ObserveScannedDevicesUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<List<ScannedDevice>> = repository.observeScannedDevices()
}

/**
 * Observes whether a discovery scan is currently running.
 */
class ObserveIsDiscoveringUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.observeIsDiscovering()
}

/**
 * Starts Bluetooth Classic discovery.
 */
class StartDiscoveryUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.startDiscovery()
}

/**
 * Stops any running Bluetooth Classic discovery.
 */
class StopDiscoveryUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke() = repository.stopDiscovery()
}

/**
 * Initiates a Bluetooth connection to the device at the given address.
 */
class ConnectDeviceUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> = repository.connect(address)
}

/**
 * Disconnects from the currently connected device (if any).
 */
class DisconnectDeviceUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke() = repository.disconnect()
}

/**
 * Sends a key-down event for the specified character over HID.
 */
class SendKeyDownUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke(char: Char): Result<Boolean> = repository.sendKeyDown(char)
}

/**
 * Sends a key-up event (key release) over HID.
 */
class SendKeyUpUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Boolean> = repository.sendKeyUp()
}

/**
 * Releases all keys over HID as a safety net against stuck keys.
 */
class ReleaseAllKeysUseCase @Inject constructor(
    private val repository: BluetoothRepository
) {
    suspend operator fun invoke(): Result<Boolean> = repository.releaseAllKeys()
}
