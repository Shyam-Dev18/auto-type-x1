# Manual Testing Matrix — AutoType X1

> **Status Summary:** Verified on real physical Android hardware against Windows and Android hosts.

---

## Verified Host OS Compatibility

| Host OS | Status | Notes |
|---|---|---|
| **Windows 10 / 11** | **PASS** | Manually tested on physical hardware — paired, connected, typed scripts cleanly |
| **Android (Target phone)** | **PASS** | Manually tested on physical hardware — HID keystrokes received cleanly |
| **Linux (X11 / Wayland)** | **UNTESTED** | Expected compatible via Bluetooth HID, but not yet verified on hardware |
| **macOS** | **UNTESTED** | Expected compatible via Bluetooth HID, but not yet verified on hardware |

---

## Verified Test Scenarios

| Scenario | Result | Hardware Tested | Notes |
|---|---|---|---|
| **Connect and Type** | **PASS** | Physical Android phone → Windows 11 | Initial connection established and keystrokes received |
| **Disconnect & Reconnect** | **PASS** | Physical Android phone → Windows 11 | Device reconnected successfully after toggle |
| **Long Script Execution** | **PASS** | Physical Android phone → Windows 11 | High character count script typed without corruption |
| **Multiline Script Execution** | **PASS** | Physical Android phone → Windows 11 | Newline characters (`\n` / `\r`) transmitted as Enter keys |
| **Cancel Typing Mid-Script** | **PASS** | Physical Android phone → Windows 11 | Typing stopped immediately without stuck key reports |

---

## Staged Test Scenarios (Pending Hardware Availability)

### Android App Runtime & Permissions

| Test | Expected | Status |
|---|---|---|
| Fresh install permissions | PermissionsScreen shown, runtime dialogs triggered | Untested |
| Deny Bluetooth permission | Permission dialog re-fires without crashing | Untested |
| Android 13+ Notification | Foreground notification displayed cleanly | Untested |

### Edge Cases

| Test | Expected | Status |
|---|---|---|
| Unsupported Unicode (e.g. €, 中) | Skipped gracefully without crashing | Untested |
| Host power off during typing | Typing stops, foreground service state updates to Disconnected | Untested |
