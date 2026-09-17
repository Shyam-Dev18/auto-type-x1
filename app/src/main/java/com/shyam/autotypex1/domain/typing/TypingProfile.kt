package com.shyam.autotypex1.domain.typing

/**
 * Configuration model for the human-like typing engine.
 *
 * Defines all user-configurable and internal pacing parameters.
 * Clamps values to safe ranges to avoid negative delays or busy loops.
 *
 * @property name                       Human-readable profile name (e.g. "Normal", "Fast").
 * @property minWpm                     Minimum typing speed in Words Per Minute.
 * @property maxWpm                     Maximum typing speed in Words Per Minute.
 * @property jitterPercent              Log-normal timing jitter percentage (0–50%).
 * @property typoProbability            Probability of injecting an adjacent-key typo (0.0–0.2).
 * @property wordDelayMs                Base pause duration after word boundaries/spaces (in ms).
 * @property punctuationDelayMultiplier Multiplier applied after punctuation characters.
 * @property thinkingPauseProbability   Probability of hesitation/thinking pauses between words.
 * @property burstVariation             Strength of short typing burst accelerations (0.0–0.5).
 * @property correctionPauseRangeMs     Range of realization and correction pauses for typos (in ms).
 * @property enabled                    Whether this profile is active.
 */
data class TypingProfile(
    val name: String,
    val minWpm: Int,
    val maxWpm: Int,
    val jitterPercent: Int,
    val typoProbability: Double,
    val wordDelayMs: Long,
    val punctuationDelayMultiplier: Double,
    val thinkingPauseProbability: Double,
    val burstVariation: Double,
    val correctionPauseRangeMs: LongRange,
    val enabled: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(minWpm > 0) { "minWpm must be > 0" }
        require(maxWpm >= minWpm) { "maxWpm ($maxWpm) must be >= minWpm ($minWpm)" }
        require(jitterPercent >= 0) { "jitterPercent must be >= 0" }
        require(typoProbability in 0.0..1.0) { "typoProbability must be in 0.0..1.0" }
        require(wordDelayMs >= 0L) { "wordDelayMs must be >= 0" }
        require(punctuationDelayMultiplier >= 1.0) { "punctuationDelayMultiplier must be >= 1.0" }
        require(thinkingPauseProbability in 0.0..1.0) { "thinkingPauseProbability must be in 0.0..1.0" }
        require(burstVariation in 0.0..1.0) { "burstVariation must be in 0.0..1.0" }
        require(correctionPauseRangeMs.first >= 0L) { "correctionPauseRangeMs.first must be >= 0" }
        require(correctionPauseRangeMs.last >= correctionPauseRangeMs.first) {
            "correctionPauseRangeMs.last (${correctionPauseRangeMs.last}) must be >= first (${correctionPauseRangeMs.first})"
        }
    }

    /**
     * Returns a copy with all properties clamped to safe execution ranges.
     */
    fun sanitized(): TypingProfile {
        val safeMinWpm = minWpm.coerceIn(5, 500)
        val safeMaxWpm = maxWpm.coerceIn(safeMinWpm, 500)
        val safeJitter = jitterPercent.coerceIn(0, 100)
        val safeTypoProb = typoProbability.coerceIn(0.0, 1.0)
        val safeWordDelay = wordDelayMs.coerceIn(0L, 5000L)
        val safePunctMult = punctuationDelayMultiplier.coerceIn(1.0, 10.0)
        val safeThinkingProb = thinkingPauseProbability.coerceIn(0.0, 1.0)
        val safeBurst = burstVariation.coerceIn(0.0, 1.0)
        val safeCorrMin = correctionPauseRangeMs.first.coerceIn(10L, 2000L)
        val safeCorrMax = correctionPauseRangeMs.last.coerceIn(safeCorrMin, 5000L)

        return copy(
            minWpm = safeMinWpm,
            maxWpm = safeMaxWpm,
            jitterPercent = safeJitter,
            typoProbability = safeTypoProb,
            wordDelayMs = safeWordDelay,
            punctuationDelayMultiplier = safePunctMult,
            thinkingPauseProbability = safeThinkingProb,
            burstVariation = safeBurst,
            correctionPauseRangeMs = safeCorrMin..safeCorrMax
        )
    }

    companion object {
        /**
         * Slow preset: 30–50 WPM, higher jitter and typo rate, longer pauses.
         */
        val SLOW = TypingProfile(
            name = "Slow",
            minWpm = 30,
            maxWpm = 50,
            jitterPercent = 20,
            typoProbability = 0.10,
            wordDelayMs = 150L,
            punctuationDelayMultiplier = 2.5,
            thinkingPauseProbability = 0.05,
            burstVariation = 0.10,
            correctionPauseRangeMs = 150L..300L
        )

        /**
         * Normal preset: 60–90 WPM, balanced human natural typing.
         */
        val NORMAL = TypingProfile(
            name = "Normal",
            minWpm = 60,
            maxWpm = 90,
            jitterPercent = 12,
            typoProbability = 0.05,
            wordDelayMs = 80L,
            punctuationDelayMultiplier = 2.0,
            thinkingPauseProbability = 0.03,
            burstVariation = 0.15,
            correctionPauseRangeMs = 100L..200L
        )

        /**
         * Fast preset: 110–150 WPM, proficient typist with tight pauses.
         */
        val FAST = TypingProfile(
            name = "Fast",
            minWpm = 110,
            maxWpm = 150,
            jitterPercent = 8,
            typoProbability = 0.02,
            wordDelayMs = 40L,
            punctuationDelayMultiplier = 1.5,
            thinkingPauseProbability = 0.01,
            burstVariation = 0.20,
            correctionPauseRangeMs = 50L..120L
        )

        /**
         * Speed Demon preset: 200–300 WPM, ultra-fast automation with minimal delays.
         */
        val SPEED_DEMON = TypingProfile(
            name = "Speed Demon",
            minWpm = 200,
            maxWpm = 300,
            jitterPercent = 3,
            typoProbability = 0.005,
            wordDelayMs = 15L,
            punctuationDelayMultiplier = 1.2,
            thinkingPauseProbability = 0.00,
            burstVariation = 0.10,
            correctionPauseRangeMs = 30L..80L
        )

        /**
         * Default custom profile template starting with Normal defaults.
         */
        fun custom(
            name: String = "Custom",
            minWpm: Int = 60,
            maxWpm: Int = 90,
            jitterPercent: Int = 12,
            typoProbability: Double = 0.05,
            wordDelayMs: Long = 80L,
            punctuationDelayMultiplier: Double = 2.0,
            thinkingPauseProbability: Double = 0.03,
            burstVariation: Double = 0.15,
            correctionPauseRangeMs: LongRange = 100L..200L
        ): TypingProfile = TypingProfile(
            name = name,
            minWpm = minWpm,
            maxWpm = maxWpm,
            jitterPercent = jitterPercent,
            typoProbability = typoProbability,
            wordDelayMs = wordDelayMs,
            punctuationDelayMultiplier = punctuationDelayMultiplier,
            thinkingPauseProbability = thinkingPauseProbability,
            burstVariation = burstVariation,
            correctionPauseRangeMs = correctionPauseRangeMs
        )
    }
}
