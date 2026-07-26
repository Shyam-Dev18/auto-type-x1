package com.shyam.autotypex1.domain.model

data class TypingProfileSpec(
    val baseWpm: IntRange,
    val jitterPercent: Int,
    val typoProbability: Float,
    val wordGapMs: Int
) {
    companion object {
        fun forProfile(profile: TypingProfile): TypingProfileSpec = when (profile) {
            TypingProfile.SLOW -> TypingProfileSpec(
                baseWpm = 45..65,
                jitterPercent = 15,
                typoProbability = 0.15f,
                wordGapMs = 120
            )
            TypingProfile.NORMAL -> TypingProfileSpec(
                baseWpm = 95..125,
                jitterPercent = 10,
                typoProbability = 0.08f,
                wordGapMs = 60
            )
            TypingProfile.FAST -> TypingProfileSpec(
                baseWpm = 180..240,
                jitterPercent = 5,
                typoProbability = 0.02f,
                wordGapMs = 25
            )
            TypingProfile.LIGHTNING -> TypingProfileSpec(
                baseWpm = 2000..3000,
                jitterPercent = 0,
                typoProbability = 0.00f,
                wordGapMs = 0
            )
            TypingProfile.CUSTOM -> TypingProfileSpec(
                baseWpm = 95..125,
                jitterPercent = 10,
                typoProbability = 0.08f,
                wordGapMs = 60
            )
        }
    }
}
