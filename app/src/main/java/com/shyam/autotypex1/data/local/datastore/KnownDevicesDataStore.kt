package com.shyam.autotypex1.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shyam.autotypex1.domain.model.KnownDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.knownDevicesDataStore: DataStore<Preferences> by preferencesDataStore(name = "known_devices")

/**
 * DataStore-backed storage for known/paired Bluetooth devices.
 * Uses JSON-based serialization for safety against spaces, special characters, and corruption.
 */
@Singleton
class KnownDevicesDataStore(
    private val dataStore: DataStore<Preferences>
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context.knownDevicesDataStore)

    companion object {
        val KEY_KNOWN_DEVICES = stringPreferencesKey("known_devices_json")
        val KEY_LAST_CONNECTED_ADDRESS = stringPreferencesKey("last_connected_address")
    }

    /**
     * Observes all known devices sorted by most recent connection time descending.
     */
    fun observeKnownDevices(): Flow<List<KnownDevice>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            parseDevices(prefs[KEY_KNOWN_DEVICES])
        }

    /**
     * Observes the address of the most recently connected device.
     */
    fun observeLastConnectedAddress(): Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            prefs[KEY_LAST_CONNECTED_ADDRESS]
        }

    /**
     * Retrieves all known devices (one-shot).
     */
    suspend fun getKnownDevices(): List<KnownDevice> = observeKnownDevices().first()

    /**
     * Saves or updates a known device with the current timestamp.
     */
    suspend fun saveDevice(name: String, address: String) {
        val cleanAddress = address.trim()
        if (cleanAddress.isBlank()) return
        val cleanName = name.trim().ifBlank { "Unknown Device" }

        dataStore.edit { prefs ->
            val existing = parseDevices(prefs[KEY_KNOWN_DEVICES]).associateBy { it.address }.toMutableMap()
            existing[cleanAddress] = KnownDevice(
                address = cleanAddress,
                name = cleanName,
                lastConnectedAt = System.currentTimeMillis()
            )
            prefs[KEY_KNOWN_DEVICES] = encodeDevices(existing.values.toList())
            prefs[KEY_LAST_CONNECTED_ADDRESS] = cleanAddress
        }
    }

    /**
     * Removes a known device by its unique address.
     */
    suspend fun removeDevice(address: String) {
        val cleanAddress = address.trim()
        if (cleanAddress.isBlank()) return

        dataStore.edit { prefs ->
            val existing = parseDevices(prefs[KEY_KNOWN_DEVICES]).filterNot { it.address.equals(cleanAddress, ignoreCase = true) }
            prefs[KEY_KNOWN_DEVICES] = encodeDevices(existing)
            if (prefs[KEY_LAST_CONNECTED_ADDRESS]?.equals(cleanAddress, ignoreCase = true) == true) {
                prefs.remove(KEY_LAST_CONNECTED_ADDRESS)
            }
        }
    }

    /**
     * Updates the last-connected timestamp for an existing device.
     */
    suspend fun updateLastConnected(address: String) {
        val cleanAddress = address.trim()
        if (cleanAddress.isBlank()) return

        dataStore.edit { prefs ->
            val existing = parseDevices(prefs[KEY_KNOWN_DEVICES]).associateBy { it.address }.toMutableMap()
            val current = existing[cleanAddress]
            if (current != null) {
                existing[cleanAddress] = current.copy(lastConnectedAt = System.currentTimeMillis())
            } else {
                existing[cleanAddress] = KnownDevice(
                    address = cleanAddress,
                    name = "Unknown Device",
                    lastConnectedAt = System.currentTimeMillis()
                )
            }
            prefs[KEY_KNOWN_DEVICES] = encodeDevices(existing.values.toList())
            prefs[KEY_LAST_CONNECTED_ADDRESS] = cleanAddress
        }
    }

    /**
     * Safely deserializes JSON string into a list of KnownDevice objects.
     * Prevents duplicates, ignores malformed entries, and falls back to empty list on error.
     */
    fun parseDevices(raw: String?): List<KnownDevice> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<KnownDevice>()
            val seenAddresses = mutableSetOf<String>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val address = obj.optString("address", "").trim()
                val rawName = obj.optString("name", "")
                val lastConnectedAt = obj.optLong("lastConnectedAt", 0L)
                if (address.isNotBlank() && seenAddresses.add(address.lowercase())) {
                    list.add(
                        KnownDevice(
                            address = address,
                            name = rawName.ifBlank { "Unknown Device" },
                            lastConnectedAt = if (lastConnectedAt > 0L) lastConnectedAt else System.currentTimeMillis()
                        )
                    )
                }
            }
            list.sortedByDescending { it.lastConnectedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Serializes a list of KnownDevice objects into a JSON array string.
     */
    fun encodeDevices(devices: List<KnownDevice>): String {
        val jsonArray = JSONArray()
        devices.sortedByDescending { it.lastConnectedAt }.forEach { device ->
            val obj = JSONObject()
            obj.put("address", device.address)
            obj.put("name", device.name)
            obj.put("lastConnectedAt", device.lastConnectedAt)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
