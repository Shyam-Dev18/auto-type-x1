package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.KnownDevicesDataStore
import com.shyam.autotypex1.domain.model.KnownDevice
import com.shyam.autotypex1.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val dataStore: KnownDevicesDataStore
) : DeviceRepository {

    override fun observeKnownDevices(): Flow<List<KnownDevice>> =
        dataStore.observeKnownDevices()

    override fun observeLastConnectedAddress(): Flow<String?> =
        dataStore.observeLastConnectedAddress()

    override suspend fun saveDevice(name: String, address: String): Result<Unit> =
        runCatching { dataStore.saveDevice(name, address) }

    override suspend fun removeDevice(address: String): Result<Unit> =
        runCatching { dataStore.removeDevice(address) }

    override suspend fun updateLastConnected(address: String): Result<Unit> =
        runCatching { dataStore.updateLastConnected(address) }
}
