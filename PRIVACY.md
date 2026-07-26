# Privacy Policy — AutoType X1

**Last updated:** 2026-07-27  
**Application:** AutoType X1  
**Package:** `com.shyam.autotypex1`  
**Developer:** Shyam

---

## 1. Overview

AutoType X1 is a **fully offline** Android application that emulates a physical Bluetooth HID keyboard. It connects to nearby computers and devices over Bluetooth Classic and transmits keystrokes. The application requires no internet access and operates entirely on your local device.

---

## 2. Data We Collect

**AutoType X1 collects no data externally.**

There is no analytics, telemetry, crash reporting, advertising, or any other mechanism that transmits information off your device.

### 2.1 Data stored locally on your device

The following data is stored only on your device and is never transmitted:

| Data | Where stored | Purpose |
|---|---|---|
| Scripts (name, content, timestamps) | Room SQLite database | Typing automation scripts you create |
| App settings (theme, typing profiles) | DataStore (local file) | User preferences |
| Known Bluetooth device names and MAC addresses | DataStore (local file) | Reconnection to previously paired devices |

### 2.2 Bluetooth information

AutoType X1 discovers nearby Bluetooth devices and stores the name and MAC address of devices you successfully connect to. This information is:

- **Stored locally on your device only**
- **Never transmitted to any server**
- **Never shared with any third party**
- **Excluded from device backups** (MAC addresses are hardware-specific and meaningless on a new device)

---

## 3. Data We Do Not Collect

AutoType X1 does **not** collect or transmit:

- Personal information or user accounts
- Device identifiers (IMEI, advertising ID, etc.)
- Location data (location permission on Android ≤ 11 is required only for Bluetooth Classic discovery and is not used for geographic tracking)
- Script contents
- Keystroke logs or typed text
- Usage analytics
- Crash reports
- Any form of telemetry

---

## 4. Internet Access

AutoType X1 does **not** hold the `INTERNET` permission. It is architecturally incapable of communicating with any remote server. This is enforced at the OS level, not merely by policy.

---

## 5. Permissions Used

| Permission | Why required | Revocable |
|---|---|---|
| `BLUETOOTH` / `BLUETOOTH_ADMIN` (Android ≤ 11) | Connect/discover via Bluetooth Classic | No (install-time) |
| `BLUETOOTH_CONNECT` (Android 12+) | Connect to paired Bluetooth HID hosts | Yes (runtime) |
| `BLUETOOTH_SCAN` (Android 12+) | Discover nearby Bluetooth devices | Yes (runtime) |
| `ACCESS_FINE_LOCATION` (Android ≤ 11) | Required by Android OS for Bluetooth Classic device discovery | Yes (runtime) |
| `ACCESS_COARSE_LOCATION` | Requested as safety net; not used for geographic purposes | Yes (runtime) |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keep the typing service active in the background | No (install-time) |
| `POST_NOTIFICATIONS` (Android 13+) | Show the persistent foreground service notification | Yes (runtime) |

Denying a runtime permission prevents the associated feature (Bluetooth connection or notifications) from working. The app does not crash; it displays an appropriate error state and allows retrying.

---

## 6. Third-Party SDKs and Libraries

AutoType X1 uses only **open-source Android libraries** provided by Google/JetBrains as part of the standard Android ecosystem:

- Jetpack Compose, Navigation, Lifecycle — UI and navigation
- Hilt (Dagger) — Dependency injection
- Room — Local SQLite database
- DataStore — Local key-value preferences
- Kotlin Coroutines — Asynchronous operations

None of these libraries transmit data or contain trackers.

---

## 7. Data Backup

Scripts and app settings are **included in Android cloud backup** (Google One Backup) if you have enabled it in Android settings. Bluetooth device lists are **excluded** from backup because they are device-specific.

You can disable Android backup for AutoType X1 in Android Settings → Backup.

---

## 8. Data Deletion

- **Delete a script:** Use the delete option in the Scripts screen. Deletion is immediate and permanent.
- **Forget a known device:** (feature pending; currently clear via Settings → App Info → Clear Data)
- **Clear all data:** Android Settings → Apps → AutoType X1 → Storage → Clear Data removes all local data including scripts, settings, and known devices.
- **Uninstalling the app** removes all locally stored data.

---

## 9. Children's Privacy

AutoType X1 is a developer/automation utility. It is not directed at children under 13 and does not knowingly collect any information from children.

---

## 10. Changes to This Policy

If this privacy policy changes in the future, the updated policy will be distributed with the application update and noted in the changelog.

---

## 11. Contact

For privacy questions or concerns, open an issue on the public GitHub repository. The repository URL will be published upon first public release.
