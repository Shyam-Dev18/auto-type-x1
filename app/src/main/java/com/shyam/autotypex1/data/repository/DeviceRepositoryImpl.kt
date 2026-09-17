package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.datastore.KnownDevicesDataStore
import com.shyam.autotypex1.domain.model.KnownDevice
import com.shyam.autotypex1.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DeviceRepository] backed by DataStore JSON storage.
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val knownDevicesDataStore: KnownDevicesDataStore
) : DeviceRepository {

    override fun observeKnownDevices(): Flow<List<KnownDevice>> =
        knownDevicesDataStore.observeKnownDevices()

    override fun observeLastConnectedAddress(): Flow<String?> =
        knownDevicesDataStore.observeLastConnectedAddress()

    override suspend fun getKnownDevices(): List<KnownDevice> =
        knownDevicesDataStore.getKnownDevices()

    override suspend fun saveDevice(name: String, address: String) =
        knownDevicesDataStore.saveDevice(name, address)

    override suspend fun removeDevice(address: String) =
        knownDevicesDataStore.removeDevice(address)

    override suspend fun updateLastConnected(address: String) =
        knownDevicesDataStore.updateLastConnected(address)
}
