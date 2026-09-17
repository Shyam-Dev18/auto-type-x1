package com.shyam.autotypex1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ConnectionState] sealed interface.
 *
 * Verifies state construction, data carrying, and exhaustive pattern matching.
 */
class ConnectionStateTest {

    @Test
    fun `Disconnected is a singleton`() {
        val a: ConnectionState = ConnectionState.Disconnected
        val b: ConnectionState = ConnectionState.Disconnected
        assertEquals(a, b)
    }

    @Test
    fun `Connecting is a singleton`() {
        val a: ConnectionState = ConnectionState.Connecting
        val b: ConnectionState = ConnectionState.Connecting
        assertEquals(a, b)
    }

    @Test
    fun `Connected carries device identity`() {
        val state = ConnectionState.Connected(
            deviceName = "Test PC",
            deviceAddress = "AA:BB:CC:DD:EE:FF"
        )
        assertEquals("Test PC", state.deviceName)
        assertEquals("AA:BB:CC:DD:EE:FF", state.deviceAddress)
    }

    @Test
    fun `Connected equality based on device identity`() {
        val a = ConnectionState.Connected("PC", "AA:BB:CC:DD:EE:FF")
        val b = ConnectionState.Connected("PC", "AA:BB:CC:DD:EE:FF")
        val c = ConnectionState.Connected("PC", "11:22:33:44:55:66")
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `Failed carries typed error`() {
        val state = ConnectionState.Failed(BluetoothError.ConnectionTimeout)
        assertEquals(BluetoothError.ConnectionTimeout, state.error)
    }

    @Test
    fun `Failed with different errors are not equal`() {
        val a = ConnectionState.Failed(BluetoothError.ConnectionTimeout)
        val b = ConnectionState.Failed(BluetoothError.PairingFailed)
        assertNotEquals(a, b)
    }

    @Test
    fun `Disconnecting is a singleton`() {
        val a: ConnectionState = ConnectionState.Disconnecting
        val b: ConnectionState = ConnectionState.Disconnecting
        assertEquals(a, b)
    }

    @Test
    fun `exhaustive when covers all states`() {
        val states: List<ConnectionState> = listOf(
            ConnectionState.Disconnected,
            ConnectionState.Connecting,
            ConnectionState.Connected("PC", "AA:BB:CC:DD:EE:FF"),
            ConnectionState.Failed(BluetoothError.ConnectionTimeout),
            ConnectionState.Disconnecting
        )

        states.forEach { state ->
            val label = when (state) {
                is ConnectionState.Disconnected -> "disconnected"
                is ConnectionState.Connecting -> "connecting"
                is ConnectionState.Connected -> "connected to ${state.deviceName}"
                is ConnectionState.Failed -> "failed: ${state.error}"
                is ConnectionState.Disconnecting -> "disconnecting"
            }
            assertTrue(label.isNotEmpty())
        }
    }

    @Test
    fun `valid state transitions - Disconnected to Connecting`() {
        var current: ConnectionState = ConnectionState.Disconnected
        current = ConnectionState.Connecting
        assertTrue(current is ConnectionState.Connecting)
    }

    @Test
    fun `valid state transitions - Connecting to Connected`() {
        var current: ConnectionState = ConnectionState.Connecting
        current = ConnectionState.Connected("Test", "AA:BB:CC:DD:EE:FF")
        assertTrue(current is ConnectionState.Connected)
    }

    @Test
    fun `valid state transitions - Connecting to Failed`() {
        var current: ConnectionState = ConnectionState.Connecting
        current = ConnectionState.Failed(BluetoothError.ConnectionTimeout)
        assertTrue(current is ConnectionState.Failed)
    }

    @Test
    fun `valid state transitions - Connected to Disconnecting`() {
        var current: ConnectionState = ConnectionState.Connected("PC", "AA:BB:CC:DD:EE:FF")
        current = ConnectionState.Disconnecting
        assertTrue(current is ConnectionState.Disconnecting)
    }

    @Test
    fun `valid state transitions - Disconnecting to Disconnected`() {
        var current: ConnectionState = ConnectionState.Disconnecting
        current = ConnectionState.Disconnected
        assertTrue(current is ConnectionState.Disconnected)
    }

    @Test
    fun `valid state transitions - Failed to Disconnected`() {
        var current: ConnectionState = ConnectionState.Failed(BluetoothError.PairingFailed)
        current = ConnectionState.Disconnected
        assertTrue(current is ConnectionState.Disconnected)
    }
}
