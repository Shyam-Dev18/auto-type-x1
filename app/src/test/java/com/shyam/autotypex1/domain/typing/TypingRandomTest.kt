package com.shyam.autotypex1.domain.typing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [TypingRandom], [DefaultTypingRandom], and [SeededTypingRandom].
 */
class TypingRandomTest {

    @Test
    fun `SeededTypingRandom produces identical sequence for same seed`() {
        val seed = 424242L
        val random1 = SeededTypingRandom(seed)
        val random2 = SeededTypingRandom(seed)

        val doubles1 = List(100) { random1.nextDouble() }
        val doubles2 = List(100) { random2.nextDouble() }
        assertEquals(doubles1, doubles2)

        val gaussians1 = List(100) { random1.nextGaussian() }
        val gaussians2 = List(100) { random2.nextGaussian() }
        assertEquals(gaussians1, gaussians2)

        val ints1 = List(100) { random1.nextInt(10, 50) }
        val ints2 = List(100) { random2.nextInt(10, 50) }
        assertEquals(ints1, ints2)
    }

    @Test
    fun `different seeds produce different sequences`() {
        val random1 = SeededTypingRandom(12345L)
        val random2 = SeededTypingRandom(67890L)

        val doubles1 = List(50) { random1.nextDouble() }
        val doubles2 = List(50) { random2.nextDouble() }
        assertNotEquals(doubles1, doubles2)
    }

    @Test
    fun `generated numbers remain strictly within requested bounds`() {
        val random = DefaultTypingRandom()

        repeat(500) {
            val d = random.nextDouble(10.0, 20.0)
            assertTrue("Double out of bounds: $d", d >= 10.0 && d < 20.0)

            val i = random.nextInt(5, 15)
            assertTrue("Int out of bounds: $i", i in 5..14)

            val l = random.nextLong(100L, 200L)
            assertTrue("Long out of bounds: $l", l in 100L..199L)

            val f = random.nextFloat()
            assertTrue("Float out of bounds: $f", f in 0.0f..1.0f)
        }
    }
}
