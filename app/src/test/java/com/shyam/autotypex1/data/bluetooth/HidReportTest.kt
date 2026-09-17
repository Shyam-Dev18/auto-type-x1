package com.shyam.autotypex1.data.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for HID report generation in [HidDeviceManager].
 */
class HidReportTest {

    @Test
    fun `createKeyDownReport produces 8 bytes with modifier and keycode in correct positions`() {
        val stroke = HidKeyStroke(keyCode = 0x04, modifier = HidKeyStroke.MOD_LEFT_SHIFT)
        val report = HidDeviceManager.createKeyDownReport(stroke)

        assertEquals(8, report.size)
        assertEquals(HidKeyStroke.MOD_LEFT_SHIFT, report[0]) // Byte 0: Modifier
        assertEquals(0x00.toByte(), report[1])               // Byte 1: Reserved
        assertEquals(0x04.toByte(), report[2])               // Byte 2: Key code
        assertEquals(0x00.toByte(), report[3])               // Bytes 3-7: Empty
        assertEquals(0x00.toByte(), report[4])
        assertEquals(0x00.toByte(), report[5])
        assertEquals(0x00.toByte(), report[6])
        assertEquals(0x00.toByte(), report[7])
    }

    @Test
    fun `createKeyUpReport produces 8 bytes of all zeros`() {
        val report = HidDeviceManager.createKeyUpReport()

        val expected = ByteArray(8)
        assertEquals(8, report.size)
        assertArrayEquals(expected, report)
    }

    @Test
    fun `report size constant matches generated reports`() {
        val downReport = HidDeviceManager.createKeyDownReport(HidKeyStroke(0x04))
        val upReport = HidDeviceManager.createKeyUpReport()

        assertEquals(KeyboardHidDescriptor.REPORT_SIZE, downReport.size)
        assertEquals(KeyboardHidDescriptor.REPORT_SIZE, upReport.size)
    }
}
