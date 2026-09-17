package com.shyam.autotypex1.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [HidKeyMapper].
 *
 * Verifies lowercase/uppercase letters, digits, shifted digits, symbols, whitespace,
 * control characters, and rejection of unsupported characters.
 */
class HidKeyMapperTest {

    private val modShift = HidKeyStroke.MOD_LEFT_SHIFT

    @Test
    fun `lowercase letters map to 0x04 through 0x1D without modifier`() {
        for (ch in 'a'..'z') {
            val stroke = HidKeyMapper.mapChar(ch)
            assertNotNull("Character '$ch' should be supported", stroke)
            val expectedCode = (0x04 + (ch - 'a')).toByte()
            assertEquals("Key code for '$ch'", expectedCode, stroke!!.keyCode)
            assertEquals("Modifier for '$ch'", 0.toByte(), stroke.modifier)
        }
    }

    @Test
    fun `uppercase letters map to same key codes as lowercase with shift modifier`() {
        for (ch in 'A'..'Z') {
            val stroke = HidKeyMapper.mapChar(ch)
            assertNotNull("Character '$ch' should be supported", stroke)
            val expectedCode = (0x04 + (ch.lowercaseChar() - 'a')).toByte()
            assertEquals("Key code for '$ch'", expectedCode, stroke!!.keyCode)
            assertEquals("Modifier for '$ch'", modShift, stroke.modifier)
        }
    }

    @Test
    fun `digits 0 through 9 map to correct key codes without modifier`() {
        // 1..9 map to 0x1E..0x26, 0 maps to 0x27
        val stroke0 = HidKeyMapper.mapChar('0')
        assertNotNull(stroke0)
        assertEquals(0x27.toByte(), stroke0!!.keyCode)
        assertEquals(0.toByte(), stroke0.modifier)

        for (digit in '1'..'9') {
            val stroke = HidKeyMapper.mapChar(digit)
            assertNotNull(stroke)
            val expectedCode = (0x1E + (digit - '1')).toByte()
            assertEquals("Key code for '$digit'", expectedCode, stroke!!.keyCode)
            assertEquals("Modifier for '$digit'", 0.toByte(), stroke.modifier)
        }
    }

    @Test
    fun `shifted digits map to corresponding digit key codes with shift modifier`() {
        val shiftedMap = mapOf(
            '!' to (0x1E.toByte()),
            '@' to (0x1F.toByte()),
            '#' to (0x20.toByte()),
            '$' to (0x21.toByte()),
            '%' to (0x22.toByte()),
            '^' to (0x23.toByte()),
            '&' to (0x24.toByte()),
            '*' to (0x25.toByte()),
            '(' to (0x26.toByte()),
            ')' to (0x27.toByte())
        )

        shiftedMap.forEach { (char, expectedCode) ->
            val stroke = HidKeyMapper.mapChar(char)
            assertNotNull("Character '$char' should be supported", stroke)
            assertEquals("Key code for '$char'", expectedCode, stroke!!.keyCode)
            assertEquals("Modifier for '$char'", modShift, stroke.modifier)
        }
    }

    @Test
    fun `symbols and punctuation map accurately`() {
        val unshiftedSymbols = mapOf(
            '-' to 0x2D.toByte(),
            '=' to 0x2E.toByte(),
            '[' to 0x2F.toByte(),
            ']' to 0x30.toByte(),
            '\\' to 0x31.toByte(),
            ';' to 0x33.toByte(),
            '\'' to 0x34.toByte(),
            '`' to 0x35.toByte(),
            ',' to 0x36.toByte(),
            '.' to 0x37.toByte(),
            '/' to 0x38.toByte()
        )

        unshiftedSymbols.forEach { (char, expectedCode) ->
            val stroke = HidKeyMapper.mapChar(char)
            assertNotNull("Character '$char' should be supported", stroke)
            assertEquals("Key code for '$char'", expectedCode, stroke!!.keyCode)
            assertEquals("Modifier for '$char'", 0.toByte(), stroke.modifier)
        }

        val shiftedSymbols = mapOf(
            '_' to 0x2D.toByte(),
            '+' to 0x2E.toByte(),
            '{' to 0x2F.toByte(),
            '}' to 0x30.toByte(),
            '|' to 0x31.toByte(),
            ':' to 0x33.toByte(),
            '"' to 0x34.toByte(),
            '~' to 0x35.toByte(),
            '<' to 0x36.toByte(),
            '>' to 0x37.toByte(),
            '?' to 0x38.toByte()
        )

        shiftedSymbols.forEach { (char, expectedCode) ->
            val stroke = HidKeyMapper.mapChar(char)
            assertNotNull("Character '$char' should be supported", stroke)
            assertEquals("Key code for '$char'", expectedCode, stroke!!.keyCode)
            assertEquals("Modifier for '$char'", modShift, stroke.modifier)
        }
    }

    @Test
    fun `special and control characters map correctly`() {
        assertEquals(HidKeyStroke(0x2C.toByte(), 0), HidKeyMapper.mapChar(' '))
        assertEquals(HidKeyStroke(0x28.toByte(), 0), HidKeyMapper.mapChar('\n'))
        assertEquals(HidKeyStroke(0x28.toByte(), 0), HidKeyMapper.mapChar('\r'))
        assertEquals(HidKeyStroke(0x2B.toByte(), 0), HidKeyMapper.mapChar('\t'))
        assertEquals(HidKeyStroke(0x2A.toByte(), 0), HidKeyMapper.mapChar('\b'))
        assertEquals(HidKeyStroke(0x29.toByte(), 0), HidKeyMapper.mapChar('\u001B'))
        assertEquals(HidKeyStroke(0x4C.toByte(), 0), HidKeyMapper.mapChar('\u007F'))
    }

    @Test
    fun `unsupported characters return null`() {
        assertNull(HidKeyMapper.mapChar('§'))
        assertNull(HidKeyMapper.mapChar('©'))
        assertNull(HidKeyMapper.mapChar('✓'))
        assertNull(HidKeyMapper.mapChar('ä'))
        assertNull(HidKeyMapper.mapChar('€'))
        assertNull(HidKeyMapper.mapChar('中'))
        assertNull(HidKeyMapper.mapChar('文'))
        assertNull(HidKeyMapper.mapChar('\u0000'))
    }
}
