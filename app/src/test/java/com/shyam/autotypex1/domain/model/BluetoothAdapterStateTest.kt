package com.shyam.autotypex1.domain.model

import android.bluetooth.BluetoothAdapter
import com.shyam.autotypex1.data.bluetooth.BluetoothAdapterStateMonitor
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [BluetoothAdapterState] mapping logic.
 *
 * Verifies that all Android adapter state integers map correctly to
 * our simplified 3-state enum.
 */
class BluetoothAdapterStateTest {

    @Test
    fun `STATE_ON maps to Enabled`() {
        assertEquals(
            BluetoothAdapterState.Enabled,
            BluetoothAdapterStateMonitor.mapAdapterState(BluetoothAdapter.STATE_ON)
        )
    }

    @Test
    fun `STATE_OFF maps to Disabled`() {
        assertEquals(
            BluetoothAdapterState.Disabled,
            BluetoothAdapterStateMonitor.mapAdapterState(BluetoothAdapter.STATE_OFF)
        )
    }

    @Test
    fun `STATE_TURNING_ON maps to Disabled (transitional state collapsed)`() {
        assertEquals(
            BluetoothAdapterState.Disabled,
            BluetoothAdapterStateMonitor.mapAdapterState(BluetoothAdapter.STATE_TURNING_ON)
        )
    }

    @Test
    fun `STATE_TURNING_OFF maps to Disabled (transitional state collapsed)`() {
        assertEquals(
            BluetoothAdapterState.Disabled,
            BluetoothAdapterStateMonitor.mapAdapterState(BluetoothAdapter.STATE_TURNING_OFF)
        )
    }

    @Test
    fun `ERROR maps to Unavailable`() {
        assertEquals(
            BluetoothAdapterState.Unavailable,
            BluetoothAdapterStateMonitor.mapAdapterState(BluetoothAdapter.ERROR)
        )
    }

    @Test
    fun `unknown state int maps to Unavailable`() {
        assertEquals(
            BluetoothAdapterState.Unavailable,
            BluetoothAdapterStateMonitor.mapAdapterState(-999)
        )
    }

    @Test
    fun `enum has exactly 3 values`() {
        assertEquals(3, BluetoothAdapterState.entries.size)
    }

    @Test
    fun `enum values are in expected order`() {
        val values = BluetoothAdapterState.entries
        assertEquals(BluetoothAdapterState.Unavailable, values[0])
        assertEquals(BluetoothAdapterState.Disabled, values[1])
        assertEquals(BluetoothAdapterState.Enabled, values[2])
    }
}
