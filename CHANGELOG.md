# Changelog

All notable changes to AutoType X1 are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.1.0] — 2026-09-15

### Added
- Single Source of Truth architecture for typing state and reactive notification synchronization
- Dedicated `TypingActionReceiver` BroadcastReceiver for background notification actions (Pause, Resume, Stop)
- Custom neon glowing Bluetooth Keyboard vector launcher icons (`ic_launcher_foreground.xml`, `ic_launcher_background.xml`)
- Version-aware notification throttling to prevent system notification flood
- Dynamic black status bar text for light mode in notch/status bar area
- Dynamic `BuildConfig.VERSION_NAME` in About screen
- Subtle elevated card styling, borders, and depth across Home Screen components

### Changed
- Updated `versionName` to `1.1.0` and `versionCode` to `2`
- Polished Splash Screen tagline to "Offline, Zero Data & Ads"
- Shortened battery optimization guidance note in Typing screen
- Cleaned up obsolete SDK checks in foreground service
- Configured production-ready ProGuard/R8 rules and GitHub repository files
- `.gitattributes` for consistent line-ending normalization across platforms
- `.editorconfig` with Kotlin/Android formatting defaults
- `PRIVACY.md` — complete privacy policy documenting local-only data storage
- `SECURITY.md` — responsible disclosure policy using GitHub private vulnerability reporting
- `CONTRIBUTING.md` — contributor guide with verified build and test commands
- `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1
- `CHANGELOG.md` — this file
- `docs/ARCHITECTURE.md` — layered architecture description and major data flows
- `docs/STORE_LISTING.md` — draft store listing copy for Play Store and F-Droid
- `docs/RELEASE_CHECKLIST.md` — pre-release checklist for future release phases
- `docs/MANUAL_TESTING.md` — manual Bluetooth HID testing matrix
- `.github/ISSUE_TEMPLATE/` — structured bug report and feature request templates
- `.github/pull_request_template.md` — pull request checklist
- `.github/workflows/android.yml` — CI workflow (lint, unit tests, debug build)
- `.github/dependabot.yml` — automated dependency update configuration
- fastlane metadata structure for Play Store / F-Droid listing

### Changed
- `proguard-rules.pro` — corrected obsolete `com.shyam.core` / `com.shyam.data` package references to `com.shyam.autotypex1`; added correct Hilt, Room, and Bluetooth HID keep rules
- `backup_rules.xml` — changed from template to intentional: scripts backed up, known Bluetooth devices excluded
- `data_extraction_rules.xml` — changed from template to intentional: same policy as backup rules
- `.gitignore` — added `.kotlin/`, `*.hprof`, `*.apk`, `*.aab`, `signing.properties`, `.env*`
- `AboutScreen.kt` — version now reads from `BuildConfig.VERSION_NAME` instead of hardcoded string
- `build.gradle.kts` — enabled `buildConfig = true`; reset `versionCode` to `1` and `versionName` to `1.0.0` for clean public release
- Permission re-request flow in `PermissionsScreen.kt` — dialog re-fires automatically on denial rather than dead-ending
- Notification small icon — changed from `R.mipmap.ic_launcher` (adaptive icon, renders incorrectly in status bar) to `R.drawable.ic_launcher_foreground` (vector, renders correctly)

### Fixed
- `MainActivity.kt` and `AutoTypeApp.kt` package declarations had accidental double-append `com.shyam.autotypex1.autotypex1` — corrected to `com.shyam.autotypex1`

---

## [1.0.0] — 2026-07-26

### Added
- Bluetooth HID keyboard emulation over Bluetooth Classic (API 28+)
- Human-like typing profiles (Natural, Casual, Professional, Speed Demon) with configurable WPM, jitter, typo rate
- QWERTY adjacency-based realistic typo simulation and self-correction
- Script management: create, edit, delete, and select typing scripts
- Foreground typing service with persistent notification showing connection status
- Add Device screen: scan and connect to Bluetooth hosts
- Saved device reconnection
- Settings: theme (light/dark/system), default typing profile
- Permissions screen with runtime Bluetooth and notification permission handling
- About screen with version info and privacy policy link
- Privacy Policy screen (in-app)
- Material 3 UI with light/dark mode support
- Fully offline — no internet access, no telemetry, no analytics

[Unreleased]: https://github.com/Shyam-Dev18/auto-type-x1/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/Shyam-Dev18/auto-type-x1/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Shyam-Dev18/auto-type-x1/releases/tag/v1.0.0
