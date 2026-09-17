# Contributing to AutoType X1

Thank you for considering a contribution to AutoType X1. This document explains how to get started.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Android Studio | Ladybug (2024.x) or newer |
| JDK | 17 (bundled with Android Studio) |
| Android SDK | API 36 (compileSdk) |
| Android device | API 28+ (minSdk = Android 9 Pie) recommended for Bluetooth HID testing |
| Physical Bluetooth hardware | Required for end-to-end HID testing; emulators do not support Bluetooth HID |

---

## Cloning and Opening

```bash
git clone https://github.com/<owner>/auto-type-x1.git
cd auto-type-x1
```

Open in Android Studio: **File → Open → select the cloned directory**.

Android Studio will sync Gradle automatically. No additional steps are required.

---

## Building

Debug build (recommended for development):

```bash
./gradlew assembleDebug
```

Install directly on a connected device:

```bash
./gradlew installDebug
```

---

## Running Tests

Unit tests (no device required):

```bash
./gradlew test
```

Android instrumented tests (device required):

```bash
./gradlew connectedAndroidTest
```

---

## Linting

```bash
./gradlew lint
```

Lint must pass with no errors before any pull request is merged. Warnings are reviewed on a case-by-case basis.

---

## Architecture

AutoType X1 follows Clean Architecture with a Kotlin + Jetpack Compose + Hilt stack:

```
presentation/    ← Compose UI, ViewModels, navigation
domain/          ← Models, use cases, repository interfaces, typing engine
data/            ← Room, DataStore, Bluetooth HID implementation
core/            ← DI modules, foreground service, utilities
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full architecture description.

---

## Coding Style

- **Language:** Kotlin only
- **Formatting:** Follow `.editorconfig` (4-space indent, LF line endings)
- **Naming:** Standard Kotlin/Android conventions (PascalCase classes, camelCase functions/properties)
- **Compose:** State hoisting, collect Flows with `collectAsStateWithLifecycle`
- **Coroutines:** Prefer `viewModelScope` or lifecycle-aware scopes; avoid `GlobalScope`
- **No hardcoded strings** in Compose UI — use `strings.xml` resources

---

## Making Changes

1. Fork the repository and create a descriptive branch:
   ```bash
   git checkout -b fix/bt-reconnect-race-condition
   git checkout -b feat/script-import
   ```
2. Make your changes with focused, atomic commits
3. Run `./gradlew test lint` locally — both must pass
4. Open a Pull Request against `main` with a clear description

---

## Pull Request Guidelines

- Link any related issue in the PR description
- Include a description of **what** changed and **why**
- For Bluetooth/HID changes, describe what physical hardware was tested
- One logical change per PR — avoid bundling unrelated changes
- Do not bump dependency versions without justification
- Do not add new third-party SDKs without discussion

---

## Bluetooth HID Testing

Many changes require physical device testing. Emulators do not support Bluetooth HID. When submitting Bluetooth-related changes, please state in your PR:

- Android version tested
- Phone/device model
- Target host OS (Windows / Linux / macOS / Android)
- Whether pair, connect, disconnect, and typing were verified

---

## Reporting Issues

Use the GitHub Issue templates:

- **Bug report** — for unexpected crashes or incorrect behavior
- **Feature request** — for new functionality proposals

---

## Dependency Changes

Adding or removing dependencies requires discussion. Criteria for new dependencies:

- Must be open-source with a compatible license (Apache 2.0, MIT, or BSD preferred)
- Must not introduce network access, analytics, or trackers
- Must be actively maintained
- Must be necessary — prefer solving problems with existing dependencies

---

## Code of Conduct

All contributors are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
