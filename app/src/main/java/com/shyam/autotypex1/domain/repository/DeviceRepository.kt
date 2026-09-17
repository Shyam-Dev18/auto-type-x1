package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.KnownDevice
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing known/paired Bluetooth devices.
 */
interface DeviceRepository {
    /**
     * Observes the list of known devices sorted by most recently connected.
     */
    fun observeKnownDevices(): Flow<List<KnownDevice>>

    /**
     * Observes the address of the last connected device.
     */
    fun observeLastConnectedAddress(): Flow<String?>

    /**
     * Retrieves the list of known devices (one-shot).
     */
    suspend fun getKnownDevices(): List<KnownDevice>

    /**
     * Saves or updates a known device with the current timestamp.
     */
    suspend fun saveDevice(name: String, address: String)

    /**
     * Removes a known device by address.
     */
    suspend fun removeDevice(address: String)

    /**
     * Updates the last-connected timestamp for a device.
     */
    suspend fun updateLastConnected(address: String)
}
