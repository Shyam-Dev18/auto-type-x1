package com.shyam.autotypex1.domain.typing

/**
 * Transforms a text script into a timed stream of explicit key events.
 *
 * Emits [TimedKeyEvent]s with human-like timing layers, natural variation,
 * context-sensitive pauses, and bounded typo generation/correction.
 */
interface HumanTypingEngine {
    /**
     * Generates a complete list of timed key events for [script].
     *
     * @param script   The text script to type.
     * @param profile  Typing configuration parameters.
     * @param random   Randomness provider (use [SeededTypingRandom] for deterministic tests).
     * @return Ordered list of [TimedKeyEvent]s.
     */
    fun generate(
        script: String,
        profile: TypingProfile = TypingProfile.NORMAL,
        random: TypingRandom = DefaultTypingRandom()
    ): List<TimedKeyEvent>

    /**
     * Generates a lazy [Sequence] of timed key events for [script].
     * Recommended for long scripts to avoid allocating large in-memory lists.
     */
    fun generateSequence(
        script: String,
        profile: TypingProfile = TypingProfile.NORMAL,
        random: TypingRandom = DefaultTypingRandom()
    ): Sequence<TimedKeyEvent>
}
