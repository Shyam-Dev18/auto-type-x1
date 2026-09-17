# Store Listing — AutoType X1

> Draft listing copy based on implemented functionality.
> Do not publish until review and screenshot assets are ready.

---

## App Title

```
AutoType X1
```

---

## Short Description (80 characters max)

```
Bluetooth HID keyboard emulator with human-like typing profiles.
```

---

## Full Description (4000 characters max for Play Store)

```
AutoType X1 turns your Android device into a physical Bluetooth keyboard that types into any paired computer — Windows, Linux, or macOS — with configurable human-like timing.

HOW IT WORKS
Connect your phone to a computer over Bluetooth. AutoType X1 registers as a Bluetooth HID keyboard device. Create typing scripts in the app, select a profile, and start typing. The text is transmitted directly as hardware keystrokes — exactly as if you typed it physically.

HUMAN-LIKE TYPING PROFILES
AutoType X1 includes four built-in typing profiles to simulate natural human input:

• Natural — Balanced speed with subtle jitter and occasional realistic typos
• Casual — Relaxed typing with natural imperfections  
• Professional — Consistent and efficient with minimal errors
• Speed Demon — Fast and accurate, minimal hesitation

Each profile controls words-per-minute, timing variation, and typo probability with self-correction behavior.

KEY FEATURES
• Bluetooth Classic HID keyboard emulation (no root required)
• Supports Windows, Linux, macOS, and Android as host targets
• Multiline script support with Enter key transmission
• Full US QWERTY layout: a–z, A–Z, 0–9, symbols, and punctuation
• Background typing via foreground service — screen can be locked
• Save multiple named scripts for reuse
• Reconnect to previously paired devices
• Material 3 UI with light, dark, and system theme support
• Fully offline — no internet, no accounts, no tracking

PRIVACY
AutoType X1 collects no data. Scripts are stored only on your device. No telemetry, no analytics, no cloud sync. The app cannot communicate with the internet — it holds no INTERNET permission.

REQUIREMENTS
• Android 9 (API 28) or higher
• Bluetooth Classic support (all standard Android phones)
• The target computer must accept Bluetooth HID connections
```

---

## Feature Summary (F-Droid / shorter formats)

```
• Physical Bluetooth HID keyboard emulation
• Human-like typing profiles with configurable speed and typos
• Multiline script management
• Background typing service
• Fully offline — no internet, no tracking
• Material 3 with dark mode support
```

---

## Permission Explanations (Play Console Data Safety)

### BLUETOOTH_CONNECT
Required to communicate with paired Bluetooth host devices and transmit keystrokes.

### BLUETOOTH_SCAN
Required to discover nearby Bluetooth devices for initial pairing.

### ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
Required by Android 11 and below for Bluetooth Classic device discovery. Not used for geographic location tracking.

### FOREGROUND_SERVICE / FOREGROUND_SERVICE_CONNECTED_DEVICE
Required to keep the typing service active while the app is in the background or screen is locked.

### POST_NOTIFICATIONS
Required on Android 13+ to display the persistent foreground service notification.

---

## Known Limitations

- Supports US QWERTY layout only (non-US keyboard layouts on the host may receive incorrect characters)
- Does not support arbitrary Unicode — emoji and non-Latin scripts are not transmitted
- Requires Bluetooth Classic — Bluetooth LE-only hosts are not supported
- Bluetooth HID Device profile availability depends on phone manufacturer; some budget devices may not support it
- Emulators do not support Bluetooth HID

---

## Screenshot Plan

> Capture on a physical device running AutoType X1 1.1.0.
> Screenshots should use dark mode and a high-end device frame (optional).

| # | Screen | Notes |
|---|---|---|
| 1 | Home screen — connected state | Show device name in connected state |
| 2 | Scripts list | Show 2–3 sample scripts |
| 3 | Script editor | Show a realistic script being edited |
| 4 | Typing screen — in progress | Show typing animation/progress |
| 5 | Add Device screen | Show scanned device list |
| 6 | Settings screen | Show profile and theme options |
| 7 | Permissions screen | Show on first launch |

---

## Feature Graphic Requirements

- Size: 1024 × 500 px (Play Store)
- No device frame required
- Should show app name "AutoType X1" and a simple visual suggesting keyboard/Bluetooth
- Assets: **PENDING** — to be designed before store submission
