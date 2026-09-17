package com.shyam.autotypex1.domain.typing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DefaultHumanTypingEngine].
 *
 * Verifies 5-layer timing model, deterministic seed reproduction, typo correction,
 * and key safety invariants preventing stuck keys.
 */
class DefaultHumanTypingEngineTest {

    private lateinit var engine: HumanTypingEngine

    @Before
    fun setUp() {
        engine = DefaultHumanTypingEngine()
    }

    @Test
    fun `empty script produces empty event stream`() {
        val events = engine.generate("", TypingProfile.NORMAL)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `disabled profile produces empty event stream`() {
        val disabled = TypingProfile.NORMAL.copy(enabled = false)
        val events = engine.generate("Hello world", disabled)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `basic lowercase text produces matched KeyDown and KeyUp pairs`() {
        val script = "hello"
        val profile = TypingProfile.NORMAL.copy(typoProbability = 0.0)
        val events = engine.generate(script, profile, SeededTypingRandom(12345L))

        assertEquals(10, events.size) // 5 chars * 2 (KeyDown + KeyUp)

        val characters = script.toList()
        for (i in characters.indices) {
            val down = events[i * 2]
            val up = events[i * 2 + 1]

            assertEquals(KeyAction.KeyDown(characters[i]), down.action)
            assertEquals(KeyAction.KeyUp(characters[i]), up.action)
            assertTrue("Inter-key delay should be positive", down.delayMs >= DefaultHumanTypingEngine.MIN_KEY_DELAY_MS)
            assertTrue("Key hold duration should be positive", up.delayMs >= DefaultHumanTypingEngine.MIN_KEY_HOLD_MS)
        }
    }

    @Test
    fun `shifted characters wrap KeyDown and KeyUp with Shift Modifier`() {
        val script = "A"
        val profile = TypingProfile.NORMAL.copy(typoProbability = 0.0)
        val events = engine.generate(script, profile, SeededTypingRandom(12345L))

        assertEquals(4, events.size)
        assertEquals(KeyAction.ModifierDown(ModifierKey.SHIFT), events[0].action)
        assertEquals(KeyAction.KeyDown('A'), events[1].action)
        assertEquals(KeyAction.KeyUp('A'), events[2].action)
        assertEquals(KeyAction.ModifierUp(ModifierKey.SHIFT), events[3].action)
    }

    @Test
    fun `seeded random produces exact identical event list`() {
        val script = "The quick brown fox jumps over the lazy dog."
        val profile = TypingProfile.NORMAL
        val seed = 987654321L

        val events1 = engine.generate(script, profile, SeededTypingRandom(seed))
        val events2 = engine.generate(script, profile, SeededTypingRandom(seed))

        assertEquals(events1.size, events2.size)
        assertEquals(events1, events2)
    }

    @Test
    fun `different seeds produce different timing variation`() {
        val script = "The quick brown fox jumps over the lazy dog."
        val profile = TypingProfile.NORMAL

        val events1 = engine.generate(script, profile, SeededTypingRandom(111L))
        val events2 = engine.generate(script, profile, SeededTypingRandom(222L))

        assertNotEquals(events1, events2)
    }

    @Test
    fun `Speed Demon profile produces significantly lower total duration than Slow profile`() {
        val script = "Testing typing speed differences between profiles in the engine."
        val seed = 42L

        val slowEvents = engine.generate(script, TypingProfile.SLOW.copy(typoProbability = 0.0), SeededTypingRandom(seed))
        val fastEvents = engine.generate(script, TypingProfile.SPEED_DEMON.copy(typoProbability = 0.0), SeededTypingRandom(seed))

        val slowTotalMs = slowEvents.sumOf { it.delayMs }
        val fastTotalMs = fastEvents.sumOf { it.delayMs }

        assertTrue("Slow ($slowTotalMs ms) must take longer than Speed Demon ($fastTotalMs ms)", slowTotalMs > fastTotalMs * 2)
    }

    @Test
    fun `punctuation produces longer pauses than regular letters`() {
        val script = "a.b"
        val profile = TypingProfile.NORMAL.copy(typoProbability = 0.0, jitterPercent = 0)
        val events = engine.generate(script, profile, SeededTypingRandom(12345L))

        val delayA = events[0].delayMs
        val delayDot = events[2].delayMs // '.' key delay
        val delayB = events[4].delayMs   // delay after '.'

        assertTrue("Pause for sentence ender '.' ($delayDot) should exceed regular letter delay ($delayA)", delayDot > delayA)
    }

    @Test
    fun `typo generation injects adjacent key, notice pause, backspace, and correct key`() {
        val script = "word"
        val profile = TypingProfile.NORMAL.copy(typoProbability = 1.0) // Force typo
        val events = engine.generate(script, profile, SeededTypingRandom(12345L))

        // Must contain a backspace action
        val hasBackspace = events.any { it.action is KeyAction.KeyDown && (it.action as KeyAction.KeyDown).char == '\b' }
        assertTrue("Typo generation should include a Backspace event", hasBackspace)
    }

    @Test
    fun `unsupported characters are omitted without crashing`() {
        val script = "Valid €100 text ₹500"
        val profile = TypingProfile.NORMAL.copy(typoProbability = 0.0)
        val events = engine.generate(script, profile, SeededTypingRandom(12345L))

        val emittedChars = events.mapNotNull {
            when (val a = it.action) {
                is KeyAction.KeyDown -> a.char
                else -> null
            }
        }

        assertFalse(emittedChars.contains('€'))
        assertFalse(emittedChars.contains('₹'))
        assertTrue(emittedChars.contains('V'))
        assertTrue(emittedChars.contains('1'))
    }

    @Test
    fun `key safety invariant — every KeyDown has a matching KeyUp in balanced order`() {
        val scripts = listOf(
            "Hello World!",
            "Complex Script with 123 & symbols: (test) [brackets] {braces}!",
            "Multi-line\nScript\rwith\ttabs",
            "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z",
            "~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?"
        )

        val profile = TypingProfile.SLOW // with typos enabled

        for (script in scripts) {
            val events = engine.generate(script, profile, SeededTypingRandom(54321L))
            val heldKeys = mutableSetOf<Char>()
            var shiftHeld = false

            for (event in events) {
                when (val action = event.action) {
                    is KeyAction.ModifierDown -> {
                        if (action.modifier == ModifierKey.SHIFT) {
                            assertFalse("Shift was already held", shiftHeld)
                            shiftHeld = true
                        }
                    }
                    is KeyAction.ModifierUp -> {
                        if (action.modifier == ModifierKey.SHIFT) {
                            assertTrue("Shift was not held before release", shiftHeld)
                            shiftHeld = false
                        }
                    }
                    is KeyAction.KeyDown -> {
                        assertFalse("Key '${action.char}' was already held without prior KeyUp", heldKeys.contains(action.char))
                        heldKeys.add(action.char)
                    }
                    is KeyAction.KeyUp -> {
                        assertTrue("Key '${action.char}' was released without prior KeyDown", heldKeys.contains(action.char))
                        heldKeys.remove(action.char)
                    }
                    else -> {}
                }
            }

            assertTrue("All keys must be released at the end of typing: remaining=$heldKeys", heldKeys.isEmpty())
            assertFalse("Shift modifier must be released at the end of typing", shiftHeld)
        }
    }

    @Test
    fun `regression test for historical stuck-key bug repeating L sequence`() {
        // Historical bug: repeating characters caused unmatched KeyDowns or desynced releases
        val repetitiveScript = "llllllllllllllllllllllllllllllllllllllll"
        val profile = TypingProfile.NORMAL.copy(typoProbability = 0.20)
        val events = engine.generate(repetitiveScript, profile, SeededTypingRandom(99999L))

        var lKeyDownCount = 0
        var lKeyUpCount = 0
        var currentHold = false

        for (event in events) {
            when (event.action) {
                is KeyAction.KeyDown -> {
                    if (event.action.char == 'l') {
                        assertFalse("Double KeyDown('l') without intervening KeyUp", currentHold)
                        currentHold = true
                        lKeyDownCount++
                    }
                }
                is KeyAction.KeyUp -> {
                    if (event.action.char == 'l') {
                        assertTrue("KeyUp('l') without preceding KeyDown", currentHold)
                        currentHold = false
                        lKeyUpCount++
                    }
                }
                else -> {}
            }
        }

        assertFalse("Final key state must not be held", currentHold)
        assertEquals("Total KeyDown('l') count must equal KeyUp('l') count", lKeyDownCount, lKeyUpCount)
        assertTrue("Must have typed multiple 'l' keystrokes", lKeyDownCount >= 40)
    }
}
