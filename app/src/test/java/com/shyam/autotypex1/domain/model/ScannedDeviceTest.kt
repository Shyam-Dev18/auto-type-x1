package com.shyam.autotypex1.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ScannedDevice] data class.
 */
class ScannedDeviceTest {

    @Test
    fun `data class carries all fields`() {
        val device = ScannedDevice(
            name = "Test PC",
            address = "AA:BB:CC:DD:EE:FF",
            bonded = true
        )
        assertEquals("Test PC", device.name)
        assertEquals("AA:BB:CC:DD:EE:FF", device.address)
        assertTrue(device.bonded)
    }

    @Test
    fun `unbonded device`() {
        val device = ScannedDevice("Unknown", "11:22:33:44:55:66", bonded = false)
        assertFalse(device.bonded)
    }

    @Test
    fun `equality based on all fields`() {
        val a = ScannedDevice("PC", "AA:BB:CC:DD:EE:FF", bonded = true)
        val b = ScannedDevice("PC", "AA:BB:CC:DD:EE:FF", bonded = true)
        val c = ScannedDevice("PC", "AA:BB:CC:DD:EE:FF", bonded = false)
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `copy with changed bonded state`() {
        val original = ScannedDevice("PC", "AA:BB:CC:DD:EE:FF", bonded = false)
        val updated = original.copy(bonded = true)
        assertTrue(updated.bonded)
        assertEquals(original.name, updated.name)
        assertEquals(original.address, updated.address)
    }

    @Test
    fun `different addresses are not equal`() {
        val a = ScannedDevice("PC", "AA:BB:CC:DD:EE:FF", bonded = true)
        val b = ScannedDevice("PC", "11:22:33:44:55:66", bonded = true)
        assertNotEquals(a, b)
    }

    @Test
    fun `different names are not equal`() {
        val a = ScannedDevice("PC-A", "AA:BB:CC:DD:EE:FF", bonded = true)
        val b = ScannedDevice("PC-B", "AA:BB:CC:DD:EE:FF", bonded = true)
        assertNotEquals(a, b)
    }
}
