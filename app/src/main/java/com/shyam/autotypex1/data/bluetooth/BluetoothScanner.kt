package com.shyam.autotypex1.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.shyam.autotypex1.domain.model.ScannedDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bluetooth Classic device discovery with proper async lifecycle.
 *
 * Discovery lifecycle:
 * 1. Register BroadcastReceiver for ACTION_FOUND + ACTION_DISCOVERY_FINISHED
 * 2. Pre-populate with bonded (paired) devices
 * 3. Call `startDiscovery()`
 * 4. On finish / cancel / timeout → **always unregister receiver**
 *
 * Guarantees:
 * - Receiver is always unregistered on stop, finish, or release
 * - Discovery is cancelled before starting a new one
 * - Safe to call [stopDiscovery] and [release] multiple times
 *
 * Permission checks are the caller's responsibility (BluetoothRepositoryImpl).
 */
class BluetoothScanner(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {

    private val discoveredDevices = linkedMapOf<String, ScannedDevice>()

    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedDevice>> = _scannedDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = extractDevice(intent) ?: return
                    val address = device.address ?: return
                    val name = try {
                        device.name ?: "Unknown Device"
                    } catch (_: SecurityException) {
                        "Unknown Device"
                    }
                    val bonded = try {
                        device.bondState == BluetoothDevice.BOND_BONDED
                    } catch (_: SecurityException) {
                        false
                    }
                    discoveredDevices[address] = ScannedDevice(
                        name = name,
                        address = address,
                        bonded = bonded
                    )
                    emitSorted()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                    unregisterReceiver()
                }
            }
        }
    }

    /**
     * Start Classic discovery.
     *
     * @throws SecurityException if scan permissions are missing (caller should check first)
     * @throws IllegalStateException if adapter is null or disabled, or discovery fails to start
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val btAdapter = adapter
            ?: throw IllegalStateException("No Bluetooth adapter")
        if (!btAdapter.isEnabled) {
            throw IllegalStateException("Bluetooth is disabled")
        }

        // Cancel any existing discovery
        if (btAdapter.isDiscovering) {
            btAdapter.cancelDiscovery()
        }

        // Clean state
        discoveredDevices.clear()
        _isDiscovering.value = true

        // Register receiver BEFORE starting discovery
        registerReceiver()

        // Pre-populate with bonded devices
        try {
            btAdapter.bondedDevices?.forEach { bonded ->
                discoveredDevices[bonded.address] = ScannedDevice(
                    name = bonded.name ?: "Unknown Device",
                    address = bonded.address,
                    bonded = true
                )
            }
            emitSorted()
        } catch (_: SecurityException) {
            // Missing BLUETOOTH_CONNECT — bonded list unavailable, continue with discovery only
        }

        // Start system discovery
        if (!btAdapter.startDiscovery()) {
            _isDiscovering.value = false
            unregisterReceiver()
            throw IllegalStateException("Failed to start discovery")
        }
    }

    /**
     * Stop any running discovery and unregister the receiver.
     * Safe to call even if not currently discovering.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            adapter?.takeIf { it.isDiscovering }?.cancelDiscovery()
        } catch (_: SecurityException) {
            // Best effort — missing permission at cancel time
        }
        _isDiscovering.value = false
        unregisterReceiver()
    }

    /**
     * Release all resources. Must be called to prevent receiver leaks.
     * Safe to call multiple times.
     */
    fun release() {
        stopDiscovery()
        discoveredDevices.clear()
        _scannedDevices.value = emptyList()
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)
        receiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(discoveryReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
        receiverRegistered = false
    }

    private fun emitSorted() {
        _scannedDevices.value = discoveredDevices.values
            .toList()
            .sortedWith(compareByDescending<ScannedDevice> { it.bonded }.thenBy { it.name.lowercase() })
    }

    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}
