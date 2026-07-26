# Release Checklist — AutoType X1

> This checklist is for **future release execution**. Do not perform these steps now.
> Complete all items before creating a public release.

---

## Pre-Release Verification

- [ ] `versionCode` incremented correctly (must be strictly greater than any previously published version)
- [ ] `versionName` updated in `app/build.gradle.kts`
- [ ] `CHANGELOG.md` updated with release date and all changes
- [ ] All unit tests pass: `./gradlew test`
- [ ] Lint passes with zero errors: `./gradlew lint`
- [ ] Debug build assembles: `./gradlew assembleDebug`
- [ ] Release build compiles: `./gradlew assembleRelease` (requires signing config)
- [ ] Physical Bluetooth HID tests completed (see `MANUAL_TESTING.md`)
- [ ] Privacy policy reviewed for accuracy

## Signing

- [ ] Production keystore generated and backed up securely (offline)
- [ ] `keystore.properties` **NOT** committed to Git
- [ ] Signing credentials configured as environment variables or CI secrets only

## GitHub Release

- [ ] All commits merged to `main`
- [ ] Git tag created: `git tag v1.0.0`
- [ ] GitHub Release created with:
  - [ ] Tag `v1.0.0`
  - [ ] Release title: `AutoType X1 v1.0.0`
  - [ ] Changelog content pasted
  - [ ] Signed APK attached
  - [ ] SHA-256 checksum of APK documented

## F-Droid

- [ ] F-Droid readiness audit confirmed (no proprietary deps, no trackers)
- [ ] fastlane metadata reviewed and updated
- [ ] F-Droid submission / metadata update performed

## Google Play Store

- [ ] Play Console app created
- [ ] Internal test track upload completed
- [ ] Privacy policy URL added in Play Console
- [ ] Data Safety form completed accurately
- [ ] Content rating questionnaire completed
- [ ] Store listing reviewed (title, description, screenshots)
- [ ] Production release submitted

## Post-Release

- [ ] Release tag pushed: `git push origin v1.0.0`
- [ ] CHANGELOG `[Unreleased]` section reset for next release
- [ ] Monitor for crash reports or issues in the first 48 hours
