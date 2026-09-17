package com.shyam.autotypex1.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [HidKeyStroke].
 */
class HidKeyStrokeTest {

    @Test
    fun `default modifier is zero`() {
        val stroke = HidKeyStroke(keyCode = 0x04)
        assertEquals(0.toByte(), stroke.modifier)
        assertEquals(0x04.toByte(), stroke.keyCode)
    }

    @Test
    fun `explicit modifier is preserved`() {
        val stroke = HidKeyStroke(keyCode = 0x04, modifier = HidKeyStroke.MOD_LEFT_SHIFT)
        assertEquals(HidKeyStroke.MOD_LEFT_SHIFT, stroke.modifier)
        assertEquals(0x04.toByte(), stroke.keyCode)
    }

    @Test
    fun `equality works on value comparison`() {
        val stroke1 = HidKeyStroke(0x04, 0x02)
        val stroke2 = HidKeyStroke(0x04, 0x02)
        val stroke3 = HidKeyStroke(0x04, 0x00)
        val stroke4 = HidKeyStroke(0x05, 0x02)

        assertEquals(stroke1, stroke2)
        assertNotEquals(stroke1, stroke3)
        assertNotEquals(stroke1, stroke4)
    }

    @Test
    fun `constants are properly defined`() {
        assertEquals(0x00.toByte(), HidKeyStroke.MOD_NONE)
        assertEquals(0x01.toByte(), HidKeyStroke.MOD_LEFT_CTRL)
        assertEquals(0x02.toByte(), HidKeyStroke.MOD_LEFT_SHIFT)
        assertEquals(0x04.toByte(), HidKeyStroke.MOD_LEFT_ALT)
        assertEquals(0x08.toByte(), HidKeyStroke.MOD_LEFT_GUI)
        assertEquals(0x10.toByte(), HidKeyStroke.MOD_RIGHT_CTRL)
        assertEquals(0x20.toByte(), HidKeyStroke.MOD_RIGHT_SHIFT)
        assertEquals(0x40.toByte(), HidKeyStroke.MOD_RIGHT_ALT)

        assertEquals(0x28.toByte(), HidKeyStroke.KEY_ENTER)
        assertEquals(0x29.toByte(), HidKeyStroke.KEY_ESCAPE)
        assertEquals(0x2A.toByte(), HidKeyStroke.KEY_BACKSPACE)
        assertEquals(0x2B.toByte(), HidKeyStroke.KEY_TAB)
        assertEquals(0x2C.toByte(), HidKeyStroke.KEY_SPACE)
        assertEquals(0x4C.toByte(), HidKeyStroke.KEY_DELETE)
    }
}
