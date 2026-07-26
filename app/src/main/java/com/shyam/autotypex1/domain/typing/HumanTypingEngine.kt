package com.shyam.autotypex1.domain.typing

/**
 * Pure function interface for generating a human-like typing plan.
 * Deterministic given the same inputs — same script + spec + seed always
 * produces the same event list.
 */
fun interface HumanTypingEngine {
    fun generate(script: String, spec: com.shyam.autotypex1.domain.model.TypingProfileSpec, randomSeed: Long): List<TimedKeyEvent>
}
