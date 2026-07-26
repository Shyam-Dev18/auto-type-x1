package com.shyam.autotypex1.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shyam.autotypex1.domain.model.KnownDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.knownDevicesDataStore by preferencesDataStore(name = "known_devices")

/**
 * DataStore-based persistence for known (previously paired) Bluetooth devices.
 * Replaces the old SharedPreferences-based KnownDevicesStore.
 *
 * Devices are stored as a JSON-like string: "name\taddress\ttimestamp\n..." 
 * This is simple, avoids adding a JSON library for a trivial data shape,
 * and is fully deterministic for testing.
 */
@Singleton
class KnownDevicesDataStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_KNOWN_DEVICES = stringPreferencesKey("known_devices_list")
        private val KEY_LAST_CONNECTED = stringPreferencesKey("last_connected_address")
    }

    fun observeKnownDevices(): Flow<List<KnownDevice>> =
        context.knownDevicesDataStore.data.map { prefs ->
            parseDevices(prefs[KEY_KNOWN_DEVICES])
        }

    fun observeLastConnectedAddress(): Flow<String?> =
        context.knownDevicesDataStore.data.map { prefs ->
            prefs[KEY_LAST_CONNECTED]
        }

    suspend fun saveDevice(name: String, address: String) {
        context.knownDevicesDataStore.edit { prefs ->
            val existing = parseDevices(prefs[KEY_KNOWN_DEVICES]).associateBy { it.address }.toMutableMap()
            existing[address] = KnownDevice(
                name = name,
                address = address,
                lastConnectedAt = System.currentTimeMillis()
            )
            prefs[KEY_KNOWN_DEVICES] = encodeDevices(existing.values.toList())
            prefs[KEY_LAST_CONNECTED] = address
        }
    }

    suspend fun removeDevice(address: String) {
        context.knownDevicesDataStore.edit { prefs ->
            val existing = parseDevices(prefs[KEY_KNOWN_DEVICES]).filterNot { it.address == address }
            prefs[KEY_KNOWN_DEVICES] = encodeDevices(existing)
            if (prefs[KEY_LAST_CONNECTED] == address) {
                prefs.remove(KEY_LAST_CONNECTED)
            }
        }
    }

    suspend fun updateLastConnected(address: String) {
        context.knownDevicesDataStore.edit { prefs ->
            val existing = parseDevices(prefs[KEY_KNOWN_DEVICES]).associateBy { it.address }.toMutableMap()
            existing[address]?.let {
                existing[address] = it.copy(lastConnectedAt = System.currentTimeMillis())
            }
            prefs[KEY_KNOWN_DEVICES] = encodeDevices(existing.values.toList())
            prefs[KEY_LAST_CONNECTED] = address
        }
    }

    private fun parseDevices(raw: String?): List<KnownDevice> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n").mapNotNull { row ->
            val parts = row.split("\t")
            if (parts.size != 3) return@mapNotNull null
            val name = parts[0].trim()
            val address = parts[1].trim()
            val timestamp = parts[2].trim().toLongOrNull() ?: return@mapNotNull null
            if (name.isBlank() || address.isBlank()) return@mapNotNull null
            KnownDevice(name = name, address = address, lastConnectedAt = timestamp)
        }
    }

    private fun encodeDevices(devices: List<KnownDevice>): String =
        devices.sortedByDescending { it.lastConnectedAt }
            .joinToString("\n") { "${it.name}\t${it.address}\t${it.lastConnectedAt}" }
}
