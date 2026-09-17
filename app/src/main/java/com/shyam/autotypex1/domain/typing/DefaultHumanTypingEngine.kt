package com.shyam.autotypex1.domain.typing

import kotlin.math.exp

/**
 * Production implementation of [HumanTypingEngine].
 *
 * Implements the full 5-layer timing model specified in `TypingEngine.md`:
 * 1. Base WPM character timing (`60000 / (WPM * 5)`).
 * 2. Log-normal right-skewed jitter variation.
 * 3. Smooth local speed drift.
 * 4. Intermittent burst acceleration runs.
 * 5. Context-sensitive pauses (geometry transitions, punctuation, word boundaries, thinking pauses).
 *
 * Guarantees:
 * - Deterministic output given the same [SeededTypingRandom] seed.
 * - Every [KeyAction.KeyDown] has a matching [KeyAction.KeyUp].
 * - Every [KeyAction.ModifierDown] has a matching [KeyAction.ModifierUp].
 * - Stuck-key invariant holds across all code paths.
 * - Zero Android runtime dependencies.
 */
class DefaultHumanTypingEngine : HumanTypingEngine {

    override fun generate(
        script: String,
        profile: TypingProfile,
        random: TypingRandom
    ): List<TimedKeyEvent> {
        return generateSequence(script, profile, random).toList()
    }

    override fun generateSequence(
        script: String,
        profile: TypingProfile,
        random: TypingRandom
    ): Sequence<TimedKeyEvent> = sequence {
        val sanitizedProfile = profile.sanitized()
        val validation = CharacterValidator.validate(script)
        val text = validation.sanitizedScript

        if (text.isEmpty() || !sanitizedProfile.enabled) {
            return@sequence
        }

        // Pick session target WPM from range [minWpm, maxWpm]
        val targetWpm = if (sanitizedProfile.maxWpm > sanitizedProfile.minWpm) {
            random.nextInt(sanitizedProfile.minWpm, sanitizedProfile.maxWpm + 1)
        } else {
            sanitizedProfile.minWpm
        }
        val baseDelayMs = 60_000.0 / (targetWpm * 5.0)

        // State trackers for drift, burst, and transition
        var localDrift = 1.0
        var burstRemaining = 0
        var burstMultiplier = 1.0
        var prevChar: Char? = null

        val words = splitPreservingWhitespace(text)

        for (word in words) {
            // Drift slightly per word: +/- 0.03
            val driftDelta = random.nextDouble(-0.03, 0.03)
            localDrift = (localDrift + driftDelta).coerceIn(0.85, 1.15)

            // Random thinking/hesitation pause before starting a word
            val isThinkingPause = random.nextDouble() < sanitizedProfile.thinkingPauseProbability
            var isFirstCharInWord = true

            // Decide if an eligible character in this word gets a typo (single typo per word max)
            val eligibleIndices = word.indices.filter { word[it].isLetter() }
            val typoCharIndex = if (eligibleIndices.isNotEmpty() && random.nextDouble() < sanitizedProfile.typoProbability) {
                eligibleIndices[random.nextInt(0, eligibleIndices.size)]
            } else {
                -1
            }

            for ((index, char) in word.withIndex()) {
                // Burst state management
                if (burstRemaining > 0) {
                    burstRemaining--
                } else if (random.nextDouble() < 0.15) {
                    // Trigger a short burst of 2–8 characters
                    burstRemaining = random.nextInt(2, 9)
                    burstMultiplier = (1.0 - (sanitizedProfile.burstVariation * random.nextDouble(0.5, 1.0))).coerceIn(0.60, 1.0)
                } else {
                    burstMultiplier = 1.0
                }

                // Compute base interval for this character
                val charDelay = computeCharacterDelay(
                    char = char,
                    prevChar = prevChar,
                    baseDelayMs = baseDelayMs,
                    localDrift = localDrift,
                    burstMultiplier = burstMultiplier,
                    profile = sanitizedProfile,
                    random = random,
                    addThinkingPause = (isFirstCharInWord && isThinkingPause)
                )
                isFirstCharInWord = false

                // Typo branch
                if (index == typoCharIndex && char.isLetter()) {
                    val typoChar = QwertyAdjacency.getAdjacentChar(char, random)

                    // 1. Type wrong character
                    emitKeyEvents(typoChar, charDelay, random)

                    // 2. Realization pause (notice delay)
                    val noticeDelay = random.nextLong(
                        sanitizedProfile.correctionPauseRangeMs.first,
                        sanitizedProfile.correctionPauseRangeMs.last + 1
                    )

                    // 3. Backspace
                    emitBackspace(noticeDelay, random)

                    // 4. Correction pause before typing correct character
                    val correctionPause = random.nextLong(30L, 100L)

                    // 5. Type correct character
                    emitKeyEvents(char, correctionPause, random)
                } else {
                    // Normal character
                    emitKeyEvents(char, charDelay, random)
                }

                prevChar = char
            }
        }
    }

    /**
     * Splits text into token chunks preserving whitespace and punctuation.
     */
    private fun splitPreservingWhitespace(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()

        for (char in text) {
            current.append(char)
            if (char == ' ' || char == '\n' || char == '\r' || char == '\t') {
                tokens.add(current.toString())
                current.clear()
            }
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        return tokens
    }

    /**
     * Computes the total inter-key delay for [char] combining all 5 timing layers.
     */
    private fun computeCharacterDelay(
        char: Char,
        prevChar: Char?,
        baseDelayMs: Double,
        localDrift: Double,
        burstMultiplier: Double,
        profile: TypingProfile,
        random: TypingRandom,
        addThinkingPause: Boolean
    ): Long {
        // Layer 2: Log-normal jitter
        val jitterRatio = profile.jitterPercent / 100.0
        val stdNormal = random.nextGaussian()
        val jitterMultiplier = if (jitterRatio > 0) {
            exp(stdNormal * jitterRatio * 0.5)
        } else {
            1.0
        }

        // Layer 5a: Geometry transition
        val transitionFactor = if (prevChar != null) {
            QwertyAdjacency.transitionFactor(prevChar, char)
        } else {
            1.0
        }

        val layeredDelay = baseDelayMs * jitterMultiplier * localDrift * burstMultiplier * transitionFactor

        // Layer 5b: Contextual pause adjustments
        val contextualExtra = when {
            char in setOf('.', '!', '?') -> {
                baseDelayMs * (profile.punctuationDelayMultiplier - 1.0) * random.nextDouble(1.5, 2.5)
            }
            char in setOf(',', ';', ':', '-', '(', ')', '[', ']', '{', '}', '\'', '"') -> {
                baseDelayMs * (profile.punctuationDelayMultiplier - 1.0) * random.nextDouble(0.8, 1.4)
            }
            char == ' ' -> {
                profile.wordDelayMs * random.nextDouble(0.85, 1.25)
            }
            char == '\n' || char == '\r' -> {
                baseDelayMs * profile.punctuationDelayMultiplier * random.nextDouble(1.5, 2.5)
            }
            else -> 0.0
        }

        val thinkingExtra = if (addThinkingPause) {
            random.nextLong(200L, 450L).toDouble()
        } else {
            0.0
        }

        val finalDelay = (layeredDelay + contextualExtra + thinkingExtra).toLong()
        return finalDelay.coerceIn(MIN_KEY_DELAY_MS, MAX_KEY_DELAY_MS)
    }

    /**
     * Emits a balanced sequence of explicit key events for [char].
     * If [char] is shifted, emits:
     *   ModifierDown(SHIFT) -> KeyDown(char) -> KeyUp(char) -> ModifierUp(SHIFT)
     * If [char] is unshifted, emits:
     *   KeyDown(char) -> KeyUp(char)
     */
    private suspend fun SequenceScope<TimedKeyEvent>.emitKeyEvents(
        char: Char,
        interKeyDelay: Long,
        random: TypingRandom
    ) {
        val isShifted = CharacterValidator.isShifted(char)
        val holdDuration = random.nextLong(MIN_KEY_HOLD_MS, MAX_KEY_HOLD_MS)

        if (isShifted) {
            val leadDelay = random.nextLong(8L, 20L)
            val trailDelay = random.nextLong(8L, 16L)

            // Shift pressed
            yield(TimedKeyEvent(interKeyDelay, KeyAction.ModifierDown(ModifierKey.SHIFT)))
            // Key pressed
            yield(TimedKeyEvent(leadDelay, KeyAction.KeyDown(char)))
            // Key released
            yield(TimedKeyEvent(holdDuration, KeyAction.KeyUp(char)))
            // Shift released
            yield(TimedKeyEvent(trailDelay, KeyAction.ModifierUp(ModifierKey.SHIFT)))
        } else {
            // Key pressed
            yield(TimedKeyEvent(interKeyDelay, KeyAction.KeyDown(char)))
            // Key released
            yield(TimedKeyEvent(holdDuration, KeyAction.KeyUp(char)))
        }
    }

    /**
     * Emits a balanced Backspace key action sequence.
     */
    private suspend fun SequenceScope<TimedKeyEvent>.emitBackspace(
        delayBeforeBackspace: Long,
        random: TypingRandom
    ) {
        val holdDuration = random.nextLong(MIN_KEY_HOLD_MS, MAX_KEY_HOLD_MS)
        yield(TimedKeyEvent(delayBeforeBackspace, KeyAction.KeyDown('\b')))
        yield(TimedKeyEvent(holdDuration, KeyAction.KeyUp('\b')))
    }

    companion object {
        const val MIN_KEY_DELAY_MS = 8L
        const val MAX_KEY_DELAY_MS = 4000L
        const val MIN_KEY_HOLD_MS = 15L
        const val MAX_KEY_HOLD_MS = 35L
    }
}
