package com.shyam.autotypex1.domain.typing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TypingProfile].
 */
class TypingProfileTest {

    @Test
    fun `presets are configured with valid ascending speeds`() {
        assertTrue(TypingProfile.SLOW.minWpm < TypingProfile.SLOW.maxWpm)
        assertTrue(TypingProfile.NORMAL.minWpm < TypingProfile.NORMAL.maxWpm)
        assertTrue(TypingProfile.FAST.minWpm < TypingProfile.FAST.maxWpm)
        assertTrue(TypingProfile.SPEED_DEMON.minWpm < TypingProfile.SPEED_DEMON.maxWpm)

        assertTrue(TypingProfile.SLOW.maxWpm <= TypingProfile.NORMAL.minWpm)
        assertTrue(TypingProfile.NORMAL.maxWpm <= TypingProfile.FAST.minWpm)
        assertTrue(TypingProfile.FAST.maxWpm <= TypingProfile.SPEED_DEMON.minWpm)
    }

    @Test
    fun `presets have descending typo probabilities`() {
        assertTrue(TypingProfile.SLOW.typoProbability > TypingProfile.NORMAL.typoProbability)
        assertTrue(TypingProfile.NORMAL.typoProbability > TypingProfile.FAST.typoProbability)
        assertTrue(TypingProfile.FAST.typoProbability >= TypingProfile.SPEED_DEMON.typoProbability)
    }

    @Test
    fun `sanitized clamps out-of-bounds parameters safely`() {
        val unsafe = TypingProfile(
            name = "Extreme",
            minWpm = 1,
            maxWpm = 1000,
            jitterPercent = 150,
            typoProbability = 1.0,
            wordDelayMs = 10000L,
            punctuationDelayMultiplier = 20.0,
            thinkingPauseProbability = 1.0,
            burstVariation = 1.0,
            correctionPauseRangeMs = 5L..6000L
        )

        val safe = unsafe.sanitized()
        assertEquals(5, safe.minWpm)
        assertEquals(500, safe.maxWpm)
        assertEquals(100, safe.jitterPercent)
        assertEquals(1.0, safe.typoProbability, 0.001)
        assertEquals(5000L, safe.wordDelayMs)
        assertEquals(10.0, safe.punctuationDelayMultiplier, 0.001)
        assertEquals(1.0, safe.thinkingPauseProbability, 0.001)
        assertEquals(1.0, safe.burstVariation, 0.001)
        assertEquals(10L..5000L, safe.correctionPauseRangeMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank profile name throws exception`() {
        TypingProfile(
            name = "",
            minWpm = 60,
            maxWpm = 90,
            jitterPercent = 10,
            typoProbability = 0.05,
            wordDelayMs = 50L,
            punctuationDelayMultiplier = 2.0,
            thinkingPauseProbability = 0.02,
            burstVariation = 0.1,
            correctionPauseRangeMs = 100L..200L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxWpm lower than minWpm throws exception`() {
        TypingProfile(
            name = "Invalid",
            minWpm = 100,
            maxWpm = 50,
            jitterPercent = 10,
            typoProbability = 0.05,
            wordDelayMs = 50L,
            punctuationDelayMultiplier = 2.0,
            thinkingPauseProbability = 0.02,
            burstVariation = 0.1,
            correctionPauseRangeMs = 100L..200L
        )
    }

    @Test
    fun `custom helper produces expected profile`() {
        val custom = TypingProfile.custom(name = "My Profile", minWpm = 75, maxWpm = 105)
        assertEquals("My Profile", custom.name)
        assertEquals(75, custom.minWpm)
        assertEquals(105, custom.maxWpm)
        assertTrue(custom.enabled)
    }
}
