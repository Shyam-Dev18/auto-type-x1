package com.shyam.autotypex1.domain.typing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [CharacterValidator].
 */
class CharacterValidatorTest {

    @Test
    fun `letters, numbers, and basic punctuation are supported`() {
        for (c in 'a'..'z') assertTrue(CharacterValidator.isSupported(c))
        for (c in 'A'..'Z') assertTrue(CharacterValidator.isSupported(c))
        for (c in '0'..'9') assertTrue(CharacterValidator.isSupported(c))

        val symbols = listOf('-', '_', '=', '+', '[', '{', ']', '}', '\\', '|', ';', ':', '\'', '"', '`', '~', ',', '<', '.', '>', '/', '?', ' ', '\n', '\t')
        for (s in symbols) assertTrue("Symbol '$s' should be supported", CharacterValidator.isSupported(s))
    }

    @Test
    fun `shifted characters are properly identified`() {
        for (c in 'A'..'Z') assertTrue(CharacterValidator.isShifted(c))
        for (c in 'a'..'z') assertFalse(CharacterValidator.isShifted(c))
        for (c in '0'..'9') assertFalse(CharacterValidator.isShifted(c))

        val shiftedSymbols = listOf('!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '{', '}', '|', ':', '"', '~', '<', '>', '?')
        for (s in shiftedSymbols) assertTrue("Shifted symbol '$s' should be recognized", CharacterValidator.isShifted(s))
    }

    @Test
    fun `unsupported characters are omitted and reported in validation result`() {
        val script = "Hello ₹100 & €50 world! 中文"
        val result = CharacterValidator.validate(script)

        assertFalse(result.isValid)
        assertEquals("Hello 100 & 50 world! ", result.sanitizedScript)
        assertTrue(result.unsupportedChars.contains('₹'))
        assertTrue(result.unsupportedChars.contains('€'))
        assertTrue(result.unsupportedChars.contains('中'))
        assertTrue(result.unsupportedChars.contains('文'))
        assertTrue(result.userMessage!!.contains("Unsupported character(s)"))
    }

    @Test
    fun `clean script produces valid result with null message`() {
        val script = "The quick brown fox jumps over the lazy dog 123!?"
        val result = CharacterValidator.validate(script)

        assertTrue(result.isValid)
        assertEquals(script, result.sanitizedScript)
        assertTrue(result.unsupportedChars.isEmpty())
        assertNull(result.userMessage)
    }

    @Test
    fun `empty script is valid`() {
        val result = CharacterValidator.validate("")
        assertTrue(result.isValid)
        assertEquals("", result.sanitizedScript)
    }
}
