package com.shyam.autotypex1.domain.typing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [TimedKeyEvent] and [KeyAction].
 */
class TimedKeyEventTest {

    @Test
    fun `event data class equality holds`() {
        val event1 = TimedKeyEvent(50L, KeyAction.KeyDown('a'))
        val event2 = TimedKeyEvent(50L, KeyAction.KeyDown('a'))
        val event3 = TimedKeyEvent(60L, KeyAction.KeyDown('a'))
        val event4 = TimedKeyEvent(50L, KeyAction.KeyUp('a'))

        assertEquals(event1, event2)
        assertNotEquals(event1, event3)
        assertNotEquals(event1, event4)
    }

    @Test
    fun `modifier actions preserve modifier keys`() {
        val modDown = KeyAction.ModifierDown(ModifierKey.SHIFT)
        val modUp = KeyAction.ModifierUp(ModifierKey.SHIFT)

        assertEquals(ModifierKey.SHIFT, modDown.modifier)
        assertEquals(ModifierKey.SHIFT, modUp.modifier)
    }
}
