package com.shyam.autotypex1.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for HidKeyMapper — verifies correctness of the USB HID key-code
 * mapping for the US QWERTY layout.
 *
 * HID keyboard usage IDs are defined in the USB HID Usage Tables specification,
 * Section 10 "Keyboard/Keypad Page".
 */
class HidKeyMapperTest {

    // ── Lowercase letters ────────────────────────────────────────────────────

    @Test fun `a maps to HID 0x04 no modifier`() {
        val stroke = HidKeyMapper.mapChar('a')
        assertNotNull(stroke)
        assertEquals(0x04.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `z maps to HID 0x1D no modifier`() {
        val stroke = HidKeyMapper.mapChar('z')
        assertNotNull(stroke)
        assertEquals(0x1D.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `all lowercase letters map without Shift modifier`() {
        for (ch in 'a'..'z') {
            val stroke = HidKeyMapper.mapChar(ch)
            assertNotNull("Expected mapping for '$ch'", stroke)
            assertEquals("'$ch' must not use Shift", 0x00.toByte(), stroke!!.modifier)
        }
    }

    // ── Uppercase letters ────────────────────────────────────────────────────

    @Test fun `A maps to HID 0x04 with Shift modifier`() {
        val stroke = HidKeyMapper.mapChar('A')
        assertNotNull(stroke)
        assertEquals(0x04.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier) // MOD_SHIFT
    }

    @Test fun `Z maps to HID 0x1D with Shift modifier`() {
        val stroke = HidKeyMapper.mapChar('Z')
        assertNotNull(stroke)
        assertEquals(0x1D.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    @Test fun `all uppercase letters map with Shift modifier`() {
        for (ch in 'A'..'Z') {
            val stroke = HidKeyMapper.mapChar(ch)
            assertNotNull("Expected mapping for '$ch'", stroke)
            assertEquals("'$ch' must use Shift", 0x02.toByte(), stroke!!.modifier)
        }
    }

    @Test fun `uppercase matches lowercase keyCode`() {
        for (i in 0..25) {
            val lower = 'a' + i
            val upper = 'A' + i
            val lowerStroke = HidKeyMapper.mapChar(lower)!!
            val upperStroke = HidKeyMapper.mapChar(upper)!!
            assertEquals(
                "Upper '$upper' must have same keyCode as lower '$lower'",
                lowerStroke.keyCode, upperStroke.keyCode
            )
        }
    }

    // ── Digits ───────────────────────────────────────────────────────────────

    @Test fun `digit 1 maps to HID 0x1E no modifier`() {
        val stroke = HidKeyMapper.mapChar('1')
        assertNotNull(stroke)
        assertEquals(0x1E.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `digit 0 maps to HID 0x27 no modifier`() {
        val stroke = HidKeyMapper.mapChar('0')
        assertNotNull(stroke)
        assertEquals(0x27.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `all digits 0-9 map without Shift modifier`() {
        for (ch in '0'..'9') {
            val stroke = HidKeyMapper.mapChar(ch)
            assertNotNull("Expected mapping for '$ch'", stroke)
            assertEquals("'$ch' must not use Shift", 0x00.toByte(), stroke!!.modifier)
        }
    }

    // ── Shift-digit symbols ──────────────────────────────────────────────────

    @Test fun `exclamation maps to HID 0x1E with Shift (Shift+1)`() {
        val stroke = HidKeyMapper.mapChar('!')
        assertNotNull(stroke)
        assertEquals(0x1E.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    @Test fun `at-sign maps to HID 0x1F with Shift (Shift+2)`() {
        val stroke = HidKeyMapper.mapChar('@')
        assertNotNull(stroke)
        assertEquals(0x1F.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    @Test fun `close-paren maps to HID 0x27 with Shift (Shift+0)`() {
        val stroke = HidKeyMapper.mapChar(')')
        assertNotNull(stroke)
        assertEquals(0x27.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    // ── Whitespace and control ───────────────────────────────────────────────

    @Test fun `space maps to HID 0x2C`() {
        val stroke = HidKeyMapper.mapChar(' ')
        assertNotNull(stroke)
        assertEquals(0x2C.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `newline maps to HID 0x28 (Enter)`() {
        val stroke = HidKeyMapper.mapChar('\n')
        assertNotNull(stroke)
        assertEquals(0x28.toByte(), stroke!!.keyCode)
    }

    @Test fun `carriage return maps to HID 0x28 (Enter)`() {
        val stroke = HidKeyMapper.mapChar('\r')
        assertNotNull(stroke)
        assertEquals(0x28.toByte(), stroke!!.keyCode)
    }

    @Test fun `tab maps to HID 0x2B`() {
        val stroke = HidKeyMapper.mapChar('\t')
        assertNotNull(stroke)
        assertEquals(0x2B.toByte(), stroke!!.keyCode)
    }

    @Test fun `backspace maps to HID 0x2A`() {
        val stroke = HidKeyMapper.mapChar('\b')
        assertNotNull(stroke)
        assertEquals(0x2A.toByte(), stroke!!.keyCode)
    }

    // ── Punctuation and symbols ──────────────────────────────────────────────

    @Test fun `hyphen maps to HID 0x2D no modifier`() {
        val stroke = HidKeyMapper.mapChar('-')
        assertNotNull(stroke)
        assertEquals(0x2D.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `underscore maps to HID 0x2D with Shift`() {
        val stroke = HidKeyMapper.mapChar('_')
        assertNotNull(stroke)
        assertEquals(0x2D.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    @Test fun `period maps to HID 0x37 no modifier`() {
        val stroke = HidKeyMapper.mapChar('.')
        assertNotNull(stroke)
        assertEquals(0x37.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `comma maps to HID 0x36 no modifier`() {
        val stroke = HidKeyMapper.mapChar(',')
        assertNotNull(stroke)
        assertEquals(0x36.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `forward slash maps to HID 0x38 no modifier`() {
        val stroke = HidKeyMapper.mapChar('/')
        assertNotNull(stroke)
        assertEquals(0x38.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `question mark maps to HID 0x38 with Shift`() {
        val stroke = HidKeyMapper.mapChar('?')
        assertNotNull(stroke)
        assertEquals(0x38.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    @Test fun `colon maps to HID 0x33 with Shift`() {
        val stroke = HidKeyMapper.mapChar(':')
        assertNotNull(stroke)
        assertEquals(0x33.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    @Test fun `semicolon maps to HID 0x33 no modifier`() {
        val stroke = HidKeyMapper.mapChar(';')
        assertNotNull(stroke)
        assertEquals(0x33.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `equals maps to HID 0x2E no modifier`() {
        val stroke = HidKeyMapper.mapChar('=')
        assertNotNull(stroke)
        assertEquals(0x2E.toByte(), stroke!!.keyCode)
        assertEquals(0x00.toByte(), stroke.modifier)
    }

    @Test fun `plus maps to HID 0x2E with Shift`() {
        val stroke = HidKeyMapper.mapChar('+')
        assertNotNull(stroke)
        assertEquals(0x2E.toByte(), stroke!!.keyCode)
        assertEquals(0x02.toByte(), stroke.modifier)
    }

    // ── Unsupported characters ───────────────────────────────────────────────

    @Test fun `euro sign returns null (unsupported)`() {
        assertNull(HidKeyMapper.mapChar('€'))
    }

    @Test fun `chinese character returns null (unsupported)`() {
        assertNull(HidKeyMapper.mapChar('中'))
    }

    @Test fun `null character returns null`() {
        assertNull(HidKeyMapper.mapChar('\u0000'))
    }

    @Test fun `emoji returns null`() {
        // Emoji are multi-char code points — surrogate is not mappable
        assertNull(HidKeyMapper.mapChar('\uD83D'))
    }

    // ── Symmetry: Shift pairs share the same keyCode ─────────────────────────

    @Test fun `Shift pairs share keyCode for bracket and brace`() {
        val open = HidKeyMapper.mapChar('[')!!
        val openShifted = HidKeyMapper.mapChar('{')!!
        assertEquals(open.keyCode, openShifted.keyCode)
        assertEquals(0x00.toByte(), open.modifier)
        assertEquals(0x02.toByte(), openShifted.modifier)
    }

    @Test fun `Shift pairs share keyCode for single and double quote`() {
        val single = HidKeyMapper.mapChar('\'')!!
        val double = HidKeyMapper.mapChar('"')!!
        assertEquals(single.keyCode, double.keyCode)
        assertEquals(0x00.toByte(), single.modifier)
        assertEquals(0x02.toByte(), double.modifier)
    }

    @Test fun `Shift pairs share keyCode for backtick and tilde`() {
        val backtick = HidKeyMapper.mapChar('`')!!
        val tilde = HidKeyMapper.mapChar('~')!!
        assertEquals(backtick.keyCode, tilde.keyCode)
        assertEquals(0x00.toByte(), backtick.modifier)
        assertEquals(0x02.toByte(), tilde.modifier)
    }
}
