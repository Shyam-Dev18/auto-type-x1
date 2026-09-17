package com.shyam.autotypex1.presentation.fakes

import com.shyam.autotypex1.domain.model.KnownDevice
import com.shyam.autotypex1.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeDeviceRepository : DeviceRepository {
    private val devicesFlow = MutableStateFlow<Map<String, KnownDevice>>(emptyMap())
    private val lastConnectedFlow = MutableStateFlow<String?>(null)

    override fun observeKnownDevices(): Flow<List<KnownDevice>> =
        devicesFlow.map { it.values.sortedByDescending { d -> d.lastConnectedAt } }

    override fun observeLastConnectedAddress(): Flow<String?> =
        lastConnectedFlow.asStateFlow()

    override suspend fun getKnownDevices(): List<KnownDevice> =
        devicesFlow.value.values.sortedByDescending { it.lastConnectedAt }

    override suspend fun saveDevice(name: String, address: String) {
        val current = devicesFlow.value.toMutableMap()
        current[address] = KnownDevice(
            address = address,
            name = name,
            lastConnectedAt = System.currentTimeMillis()
        )
        devicesFlow.value = current
        lastConnectedFlow.value = address
    }

    override suspend fun removeDevice(address: String) {
        val current = devicesFlow.value.toMutableMap()
        current.remove(address)
        devicesFlow.value = current
        if (lastConnectedFlow.value == address) {
            lastConnectedFlow.value = null
        }
    }

    override suspend fun updateLastConnected(address: String) {
        val current = devicesFlow.value.toMutableMap()
        val existing = current[address]
        if (existing != null) {
            current[address] = existing.copy(lastConnectedAt = System.currentTimeMillis())
            devicesFlow.value = current
        }
        lastConnectedFlow.value = address
    }
}
