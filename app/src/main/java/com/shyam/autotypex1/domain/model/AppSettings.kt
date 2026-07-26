package com.shyam.autotypex1.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val profile: TypingProfile = TypingProfile.NORMAL,
    val typingSpec: TypingProfileSpec = TypingProfileSpec.forProfile(TypingProfile.NORMAL),
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
