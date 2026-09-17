package com.shyam.autotypex1.domain.typing

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Abstraction for random number generation in the typing engine.
 *
 * Allows production execution with [DefaultTypingRandom] and deterministic,
 * 100% reproducible testing with [SeededTypingRandom].
 */
interface TypingRandom {
    /** Returns a random Double in [0.0, 1.0). */
    fun nextDouble(): Double

    /** Returns a random Double in [from, until). */
    fun nextDouble(from: Double, until: Double): Double

    /** Returns a random Int in [from, until). */
    fun nextInt(from: Int, until: Int): Int

    /** Returns a random Long in [from, until). */
    fun nextLong(from: Long, until: Long): Long

    /** Returns a random Float in [0.0f, 1.0f). */
    fun nextFloat(): Float

    /** Returns a standard normally distributed Double (mean = 0.0, std = 1.0). */
    fun nextGaussian(): Double
}

/**
 * Production implementation of [TypingRandom].
 */
class DefaultTypingRandom(
    seed: Long = System.currentTimeMillis()
) : TypingRandom {
    private val random = Random(seed)

    override fun nextDouble(): Double = random.nextDouble()

    override fun nextDouble(from: Double, until: Double): Double =
        random.nextDouble(from, until)

    override fun nextInt(from: Int, until: Int): Int =
        random.nextInt(from, until)

    override fun nextLong(from: Long, until: Long): Long =
        random.nextLong(from, until)

    override fun nextFloat(): Float = random.nextFloat()

    override fun nextGaussian(): Double {
        val u1 = random.nextDouble().coerceAtLeast(1e-10)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
    }
}

/**
 * Deterministic implementation of [TypingRandom] for unit testing.
 * Same seed guarantees exact reproduction of every random value.
 */
class SeededTypingRandom(
    val seed: Long
) : TypingRandom {
    private val random = Random(seed)

    override fun nextDouble(): Double = random.nextDouble()

    override fun nextDouble(from: Double, until: Double): Double =
        random.nextDouble(from, until)

    override fun nextInt(from: Int, until: Int): Int =
        random.nextInt(from, until)

    override fun nextLong(from: Long, until: Long): Long =
        random.nextLong(from, until)

    override fun nextFloat(): Float = random.nextFloat()

    override fun nextGaussian(): Double {
        val u1 = random.nextDouble().coerceAtLeast(1e-10)
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
    }
}
