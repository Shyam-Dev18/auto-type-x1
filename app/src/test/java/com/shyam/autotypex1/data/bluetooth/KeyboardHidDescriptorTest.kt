package com.shyam.autotypex1.data.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [KeyboardHidDescriptor].
 *
 * Verifies descriptor byte length, byte-for-byte fidelity with the verified legacy descriptor,
 * and SDP/QoS constants.
 */
class KeyboardHidDescriptorTest {

    private val legacyExpectedBytes: ByteArray = intArrayOf(
        0x05, 0x01, 0x09, 0x06, 0xA1, 0x01, 0x05, 0x07,
        0x19, 0xE0, 0x29, 0xE7, 0x15, 0x00, 0x25, 0x01,
        0x75, 0x01, 0x95, 0x08, 0x81, 0x02, 0x95, 0x01,
        0x75, 0x08, 0x81, 0x01, 0x95, 0x05, 0x75, 0x01,
        0x05, 0x08, 0x19, 0x01, 0x29, 0x05, 0x91, 0x02,
        0x95, 0x01, 0x75, 0x03, 0x91, 0x01, 0x95, 0x06,
        0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07,
        0x19, 0x00, 0x29, 0x65, 0x81, 0x00, 0xC0
    ).map { it.toByte() }.toByteArray()

    @Test
    fun `descriptor is exactly 63 bytes`() {
        assertEquals(63, KeyboardHidDescriptor.DESCRIPTOR.size)
    }

    @Test
    fun `descriptor bytes match legacy verified descriptor byte for byte`() {
        assertArrayEquals(legacyExpectedBytes, KeyboardHidDescriptor.DESCRIPTOR)
    }

    @Test
    fun `report constants are valid`() {
        assertEquals(1, KeyboardHidDescriptor.REPORT_ID)
        assertEquals(8, KeyboardHidDescriptor.REPORT_SIZE)
    }

    @Test
    fun `SDP constants are non-blank`() {
        assert(KeyboardHidDescriptor.SDP_NAME.isNotBlank())
        assert(KeyboardHidDescriptor.SDP_DESCRIPTION.isNotBlank())
        assert(KeyboardHidDescriptor.SDP_PROVIDER.isNotBlank())
    }
}
