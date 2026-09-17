package com.shyam.autotypex1.domain.model

import com.shyam.autotypex1.data.bluetooth.BluetoothOperationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [BluetoothError] sealed interface.
 *
 * Verifies all error types instantiate correctly and carry expected data.
 */
class BluetoothErrorTest {

    @Test
    fun `BluetoothDisabled is a singleton`() {
        val a: BluetoothError = BluetoothError.BluetoothDisabled
        val b: BluetoothError = BluetoothError.BluetoothDisabled
        assertEquals(a, b)
    }

    @Test
    fun `PermissionMissing is a singleton`() {
        assertEquals(BluetoothError.PermissionMissing, BluetoothError.PermissionMissing)
    }

    @Test
    fun `ConnectionTimeout is a singleton`() {
        assertEquals(BluetoothError.ConnectionTimeout, BluetoothError.ConnectionTimeout)
    }

    @Test
    fun `PairingFailed is a singleton`() {
        assertEquals(BluetoothError.PairingFailed, BluetoothError.PairingFailed)
    }

    @Test
    fun `DiscoveryFailed is a singleton`() {
        assertEquals(BluetoothError.DiscoveryFailed, BluetoothError.DiscoveryFailed)
    }

    @Test
    fun `AlreadyConnecting is a singleton`() {
        assertEquals(BluetoothError.AlreadyConnecting, BluetoothError.AlreadyConnecting)
    }

    @Test
    fun `AlreadyConnected is a singleton`() {
        assertEquals(BluetoothError.AlreadyConnected, BluetoothError.AlreadyConnected)
    }

    @Test
    fun `HidProfileUnavailable is a singleton`() {
        assertEquals(BluetoothError.HidProfileUnavailable, BluetoothError.HidProfileUnavailable)
    }

    @Test
    fun `HidRegistrationFailed is a singleton`() {
        assertEquals(BluetoothError.HidRegistrationFailed, BluetoothError.HidRegistrationFailed)
    }

    @Test
    fun `SendReportFailed is a singleton`() {
        assertEquals(BluetoothError.SendReportFailed, BluetoothError.SendReportFailed)
    }

    @Test
    fun `NotConnected is a singleton`() {
        assertEquals(BluetoothError.NotConnected, BluetoothError.NotConnected)
    }

    @Test
    fun `UnsupportedCharacter carries char`() {
        val error = BluetoothError.UnsupportedCharacter('€')
        assertEquals('€', error.char)
    }

    @Test
    fun `UnsupportedCharacter equality by char`() {
        val a = BluetoothError.UnsupportedCharacter('€')
        val b = BluetoothError.UnsupportedCharacter('€')
        val c = BluetoothError.UnsupportedCharacter('£')
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `Unknown error carries message`() {
        val error = BluetoothError.Unknown("Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun `Unknown errors with different messages are not equal`() {
        val a = BluetoothError.Unknown("Error A")
        val b = BluetoothError.Unknown("Error B")
        assertNotEquals(a, b)
    }

    @Test
    fun `Unknown errors with same message are equal`() {
        val a = BluetoothError.Unknown("Same error")
        val b = BluetoothError.Unknown("Same error")
        assertEquals(a, b)
    }

    @Test
    fun `exhaustive when covers all error types`() {
        val errors: List<BluetoothError> = listOf(
            BluetoothError.BluetoothDisabled,
            BluetoothError.PermissionMissing,
            BluetoothError.ConnectionTimeout,
            BluetoothError.PairingFailed,
            BluetoothError.DiscoveryFailed,
            BluetoothError.AlreadyConnecting,
            BluetoothError.AlreadyConnected,
            BluetoothError.HidProfileUnavailable,
            BluetoothError.HidRegistrationFailed,
            BluetoothError.SendReportFailed,
            BluetoothError.NotConnected,
            BluetoothError.UnsupportedCharacter('~'),
            BluetoothError.Unknown("test")
        )

        errors.forEach { error ->
            val label = when (error) {
                is BluetoothError.BluetoothDisabled -> "disabled"
                is BluetoothError.PermissionMissing -> "permission"
                is BluetoothError.ConnectionTimeout -> "timeout"
                is BluetoothError.PairingFailed -> "pairing"
                is BluetoothError.DiscoveryFailed -> "discovery"
                is BluetoothError.AlreadyConnecting -> "already connecting"
                is BluetoothError.AlreadyConnected -> "already connected"
                is BluetoothError.HidProfileUnavailable -> "hid unavailable"
                is BluetoothError.HidRegistrationFailed -> "hid registration failed"
                is BluetoothError.SendReportFailed -> "send report failed"
                is BluetoothError.NotConnected -> "not connected"
                is BluetoothError.UnsupportedCharacter -> "unsupported char: ${error.char}"
                is BluetoothError.Unknown -> "unknown: ${error.message}"
            }
            assertTrue(label.isNotEmpty())
        }
    }

    @Test
    fun `BluetoothOperationException wraps BluetoothError correctly`() {
        val error = BluetoothError.ConnectionTimeout
        val exception = BluetoothOperationException(error)
        assertEquals(error, exception.error)
        assertTrue(exception.message!!.contains("ConnectionTimeout"))
    }

    @Test
    fun `different error types are not equal to each other`() {
        assertNotEquals(BluetoothError.BluetoothDisabled as BluetoothError, BluetoothError.PermissionMissing as BluetoothError)
        assertNotEquals(BluetoothError.ConnectionTimeout as BluetoothError, BluetoothError.PairingFailed as BluetoothError)
        assertNotEquals(BluetoothError.NotConnected as BluetoothError, BluetoothError.SendReportFailed as BluetoothError)
    }
}
