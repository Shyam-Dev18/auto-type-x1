package com.shyam.autotypex1.domain.model

import com.shyam.autotypex1.domain.typing.TypingProfile

/**
 * Domain representation of application settings and preferences.
 *
 * @property profile Configured typing profile (speed, delays, jitter, typos, etc.).
 * @property themeMode Selected UI theme appearance.
 */
data class AppSettings(
    val profile: TypingProfile = TypingProfile.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
