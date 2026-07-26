package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import kotlinx.coroutines.flow.Flow

/**
 * Bluetooth HID connection — domain interface.
 *
 * Implementation wraps BluetoothHidDevice (Bluetooth Classic HID Device profile).
 * The domain layer never sees android.bluetooth.* — only these abstractions.
 */
interface HidConnectionRepository {
    fun observeConnectionState(): Flow<ConnectionState>
    fun observeAdapterState(): Flow<BluetoothAdapterState>
    fun observeScannedDevices(): Flow<List<ScannedDevice>>
    fun observeIsScanning(): Flow<Boolean>

    suspend fun startScan(): Result<Unit>
    suspend fun stopScan(): Result<Unit>
    suspend fun connect(address: String): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun reconnectLast(): Result<Unit>

    suspend fun sendCharacter(char: Char): Result<Boolean>
    suspend fun sendBackspace(): Result<Boolean>
    suspend fun releaseAllKeys(): Result<Boolean>

    fun isConnected(): Boolean
    fun isTypingReady(): Boolean

    fun openBluetoothSettings()
    fun requestEnableBluetooth()
}
