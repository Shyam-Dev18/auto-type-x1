package com.shyam.autotypex1.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors the Bluetooth adapter hardware state via a [BroadcastReceiver].
 *
 * Maps Android's 5 adapter states (ON, OFF, TURNING_ON, TURNING_OFF, ERROR)
 * to our 3-state [BluetoothAdapterState] enum:
 * - STATE_ON → [BluetoothAdapterState.Enabled]
 * - STATE_OFF / STATE_TURNING_ON / STATE_TURNING_OFF → [BluetoothAdapterState.Disabled]
 * - everything else → [BluetoothAdapterState.Unavailable]
 *
 * Lifecycle: call [release] to unregister the receiver when done.
 */
class BluetoothAdapterStateMonitor(
    private val context: Context,
    private val adapter: BluetoothAdapter?
) {

    private val _adapterState = MutableStateFlow(readCurrentState())
    val adapterState: StateFlow<BluetoothAdapterState> = _adapterState.asStateFlow()

    private var receiverRegistered = false

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(
                BluetoothAdapter.EXTRA_STATE,
                BluetoothAdapter.ERROR
            )
            _adapterState.value = mapAdapterState(state)
        }
    }

    init {
        registerReceiver()
    }

    /**
     * Force-refresh the adapter state from the system.
     * Useful after returning from Settings or after a lifecycle resume.
     */
    fun refresh() {
        _adapterState.value = readCurrentState()
    }

    /**
     * Unregister the receiver. Must be called to prevent leaks.
     * Safe to call multiple times.
     */
    fun release() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(stateReceiver)
            } catch (_: IllegalArgumentException) {
                // Already unregistered — safe to ignore
            }
            receiverRegistered = false
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(stateReceiver, filter)
        receiverRegistered = true
    }

    private fun readCurrentState(): BluetoothAdapterState {
        val btAdapter = adapter ?: return BluetoothAdapterState.Unavailable
        return mapAdapterState(btAdapter.state)
    }

    companion object {
        /**
         * Maps an Android `BluetoothAdapter.STATE_*` int to our 3-state enum.
         * Exposed as a companion function for testability.
         */
        fun mapAdapterState(state: Int): BluetoothAdapterState = when (state) {
            BluetoothAdapter.STATE_ON -> BluetoothAdapterState.Enabled
            BluetoothAdapter.STATE_OFF,
            BluetoothAdapter.STATE_TURNING_ON,
            BluetoothAdapter.STATE_TURNING_OFF -> BluetoothAdapterState.Disabled
            else -> BluetoothAdapterState.Unavailable
        }
    }
}
