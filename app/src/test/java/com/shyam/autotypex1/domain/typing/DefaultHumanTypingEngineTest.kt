package com.shyam.autotypex1.domain.typing

import com.shyam.autotypex1.domain.model.TypingProfileSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultHumanTypingEngineTest {

    private val engine = DefaultHumanTypingEngine()

    @Test
    fun testDeterminismWithSameSeed() {
        val content = "This is a deterministic human typing engine test."
        val spec = TypingProfileSpec(
            baseWpm = 45..55,
            jitterPercent = 15,
            typoProbability = 0.15f,
            wordGapMs = 100
        )
        val seed = 42L

        val run1 = engine.generate(content, spec, seed)
        val run2 = engine.generate(content, spec, seed)

        assertEquals("Runs with same seed must produce identical event count", run1.size, run2.size)
        run1.zip(run2).forEachIndexed { index, (event1, event2) ->
            assertEquals("Event $index action must match", event1.action, event2.action)
            assertEquals("Event $index delay must match", event1.delayMs, event2.delayMs)
        }
    }

    @Test
    fun testDifferentSeedsProduceDifferentJitter() {
        val content = "This is a human typing engine test with different seeds."
        val spec = TypingProfileSpec(
            baseWpm = 40..60,
            jitterPercent = 20,
            typoProbability = 0.2f,
            wordGapMs = 80
        )

        val run1 = engine.generate(content, spec, 100L)
        val run2 = engine.generate(content, spec, 200L)

        // They might have different typo structures and different delays, resulting in different sizes/delays
        var matchingDelays = 0
        run1.zip(run2).forEach { (e1, e2) ->
            if (e1.delayMs == e2.delayMs) matchingDelays++
        }

        // Delays shouldn't be completely identical
        assertNotEquals("Delays across different seeds must vary due to random jitter", run1.size, matchingDelays)
    }

    @Test
    fun testTypoAndBackspaceCorrection() {
        val content = "Hello"
        val spec = TypingProfileSpec(
            baseWpm = 40..60,
            jitterPercent = 10,
            typoProbability = 1.0f, // Force typos on every possible character
            wordGapMs = 100
        )
        // With typo probability = 1.0, the generator should inject typos and issue Backspaces
        val events = engine.generate(content, spec, 12345L)

        val hasBackspace = events.any { it.action is KeyAction.Backspace }
        assertTrue("High typo probability must result in backspace correction events", hasBackspace)
    }

    @Test
    fun testEmptyContentProducesZeroEvents() {
        val spec = TypingProfileSpec(
            baseWpm = 40..60,
            jitterPercent = 10,
            typoProbability = 0.1f,
            wordGapMs = 100
        )
        val events = engine.generate("", spec, 123L)
        assertTrue("Empty content should result in empty event list", events.isEmpty())
    }
}
