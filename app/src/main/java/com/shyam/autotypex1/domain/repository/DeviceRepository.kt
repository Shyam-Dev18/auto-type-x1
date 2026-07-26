package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.KnownDevice
import kotlinx.coroutines.flow.Flow

/** Known/saved Bluetooth devices — domain interface, implemented in data layer. */
interface DeviceRepository {
    fun observeKnownDevices(): Flow<List<KnownDevice>>
    fun observeLastConnectedAddress(): Flow<String?>
    suspend fun saveDevice(name: String, address: String): Result<Unit>
    suspend fun removeDevice(address: String): Result<Unit>
    suspend fun updateLastConnected(address: String): Result<Unit>
}
