package com.shyam.autotypex1.domain.typing

import com.shyam.autotypex1.domain.model.TypingProfileSpec
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Default implementation of HumanTypingEngine.
 *
 * Produces a deterministic, pre-computed plan of [TimedKeyEvent]s for a given
 * script, typing profile, and random seed. The output is fully determined by
 * the inputs — same seed always produces the same event list.
 *
 * Rules (from §4 specification):
 * - Per-keystroke delay from baseWpm with log-normal jitter
 * - After space: 1.5–2.5× normal delay
 * - After punctuation (. ? ! ,): 2–4× normal delay
 * - Per-word typo injection via QWERTY adjacency
 * - Deterministic given a seed
 * - No fatigue drift, no thinking-pauses beyond punctuation gaps
 */
class DefaultHumanTypingEngine : HumanTypingEngine {

    override fun generate(
        script: String,
        spec: TypingProfileSpec,
        randomSeed: Long
    ): List<TimedKeyEvent> {
        if (script.isEmpty()) return emptyList()

        val random = Random(randomSeed)
        val events = mutableListOf<TimedKeyEvent>()

        // Calculate base delay from WPM midpoint.
        // WPM assumes 5 chars per word. delay_per_char_ms = 60000 / (wpm * 5)
        val midWpm = (spec.baseWpm.first + spec.baseWpm.last) / 2.0
        val baseDelayMs = (60_000.0 / (midWpm * 5.0))

        val words = splitIntoWords(script)

        for ((wordIndex, word) in words.withIndex()) {
            // Decide if this word gets a typo (per-word, not per-character)
            val typoCharIndex = if (word.length > 1 && random.nextFloat() < spec.typoProbability) {
                // Pick a random position in the word for the typo (not the first char)
                random.nextInt(1, word.length)
            } else {
                -1
            }

            for ((charIndex, char) in word.withIndex()) {
                // Inject typo before the correct character
                if (charIndex == typoCharIndex && char.isLetter()) {
                    val typoChar = QwertyAdjacency.getAdjacentChar(char, random)

                    // Type the wrong character
                    val typoDelay = computeKeystrokeDelay(baseDelayMs, spec.jitterPercent, random)
                    events.add(TimedKeyEvent(typoDelay, KeyAction.KeyDown(typoChar)))
                    events.add(TimedKeyEvent(0L, KeyAction.KeyUp(typoChar)))

                    // Pause ("notice" delay: 150–300ms)
                    val noticeDelay = random.nextLong(150L, 301L)

                    // Backspace
                    events.add(TimedKeyEvent(noticeDelay, KeyAction.Backspace))

                    // Short pause before correct character
                    val correctionDelay = random.nextLong(50L, 120L)
                    events.add(TimedKeyEvent(correctionDelay, KeyAction.KeyDown(char)))
                    events.add(TimedKeyEvent(0L, KeyAction.KeyUp(char)))
                } else {
                    // Normal character
                    val delay = computeCharDelay(char, baseDelayMs, spec, random)
                    events.add(TimedKeyEvent(delay, KeyAction.KeyDown(char)))
                    events.add(TimedKeyEvent(0L, KeyAction.KeyUp(char)))
                }
            }
        }

        return events
    }

    /**
     * Splits the script preserving whitespace and punctuation as part of the preceding word.
     * This ensures word boundaries are natural and typos are applied per-word.
     */
    private fun splitIntoWords(script: String): List<String> {
        val words = mutableListOf<String>()
        val current = StringBuilder()

        for (char in script) {
            current.append(char)
            // A word boundary is after a space or newline
            if (char == ' ' || char == '\n' || char == '\r' || char == '\t') {
                words.add(current.toString())
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            words.add(current.toString())
        }

        return words
    }

    /**
     * Computes the delay for a character, applying punctuation and space multipliers.
     */
    private fun computeCharDelay(
        char: Char,
        baseDelayMs: Double,
        spec: TypingProfileSpec,
        random: Random
    ): Long {
        val keystrokeDelay = computeKeystrokeDelay(baseDelayMs, spec.jitterPercent, random)
        if (spec.jitterPercent <= 0) {
            return keystrokeDelay
        }

        return when {
            // After sentence-ending punctuation or comma: 2–4× delay
            char in setOf('.', '?', '!', ',') -> {
                val multiplier = 2.0 + random.nextDouble() * 2.0
                (keystrokeDelay * multiplier).toLong()
            }
            // After space: 1.5–2.5× delay (word gap)
            char == ' ' -> {
                val multiplier = 1.5 + random.nextDouble()
                val wordGap = spec.wordGapMs.coerceIn(0, 1000)
                (keystrokeDelay * multiplier + wordGap).toLong()
            }
            // After newline: extra gap similar to punctuation
            char == '\n' || char == '\r' -> {
                val multiplier = 2.0 + random.nextDouble() * 2.0
                (keystrokeDelay * multiplier).toLong()
            }
            else -> keystrokeDelay
        }
    }

    /**
     * Produces a log-normally distributed keystroke delay.
     *
     * Log-normal jitter reads as more human than uniform random — it produces
     * occasional longer pauses (the long tail) while keeping most keystrokes
     * close to the base delay.
     */
    private fun computeKeystrokeDelay(
        baseDelayMs: Double,
        jitterPercent: Int,
        random: Random
    ): Long {
        if (jitterPercent <= 0) return baseDelayMs.toLong()

        val jitterRatio = jitterPercent / 100.0

        // Box-Muller transform for standard normal
        val u1 = random.nextDouble().coerceAtLeast(1e-10)
        val u2 = random.nextDouble()
        val stdNormal = sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)

        // Apply as log-normal: scale by jitter ratio
        val logNormalMultiplier = kotlin.math.exp(stdNormal * jitterRatio * 0.5)

        val delay = (baseDelayMs * logNormalMultiplier).toLong()
        return delay.coerceIn(0L, (baseDelayMs * 5).toLong()) // Safety bounds
    }
}
