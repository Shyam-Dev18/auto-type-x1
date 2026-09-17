package com.shyam.autotypex1.data.bluetooth

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings

/**
 * USB HID keyboard descriptor and SDP/QoS configuration constants.
 *
 * The 65-byte descriptor is the known-good USB HID keyboard report descriptor
 * verified against the legacy codebase and tested across Windows, Linux, and macOS hosts.
 *
 * Report format (8 bytes):
 * ```
 * Byte 0: Modifier keys (bit flags: LCtrl, LShift, LAlt, LGui, RCtrl, RShift, RAlt, RGui)
 * Byte 1: Reserved (always 0x00)
 * Bytes 2–7: Up to 6 simultaneous key codes (Usage IDs from HID Usage Table §10)
 * ```
 *
 * This object is isolated and contains no Android runtime dependencies beyond constants,
 * making it independently testable.
 */
object KeyboardHidDescriptor {

    // ── SDP Service Record ──────────────────────────────────────────

    const val SDP_NAME = "AutoTypeHID"
    const val SDP_DESCRIPTION = "Bluetooth keyboard typing"
    const val SDP_PROVIDER = "AutoTypeHID"

    // ── Report Configuration ────────────────────────────────────────

    /** Input report ID used with `BluetoothHidDevice.sendReport()`. */
    const val REPORT_ID = 1

    /** Input report size in bytes (modifier + reserved + 6 key codes). */
    const val REPORT_SIZE = 8

    /** HID subclass: keyboard. */
    const val SUBCLASS = BluetoothHidDevice.SUBCLASS1_KEYBOARD

    // ── QoS Configuration ───────────────────────────────────────────

    const val QOS_SERVICE_TYPE = BluetoothHidDeviceAppQosSettings.SERVICE_GUARANTEED
    const val QOS_TOKEN_RATE = 800
    const val QOS_TOKEN_BUCKET_SIZE = 9
    const val QOS_PEAK_BANDWIDTH = 0
    const val QOS_LATENCY = 11250
    const val QOS_DELAY_VARIATION = BluetoothHidDeviceAppQosSettings.MAX

    // ── HID Report Descriptor ───────────────────────────────────────

    /**
     * USB HID keyboard report descriptor (63 bytes).
     *
     * Breakdown:
     * ```
     * Usage Page (Generic Desktop)            05 01
     * Usage (Keyboard)                        09 06
     * Collection (Application)                A1 01
     *   Usage Page (Key Codes)                05 07
     *   Usage Minimum (Left Control)          19 E0
     *   Usage Maximum (Right GUI)             29 E7
     *   Logical Minimum (0)                   15 00
     *   Logical Maximum (1)                   25 01
     *   Report Size (1)                       75 01
     *   Report Count (8)                      95 08
     *   Input (Data, Variable, Absolute)      81 02    ← 8 modifier bits
     *   Report Count (1)                      95 01
     *   Report Size (8)                       75 08
     *   Input (Constant)                      81 01    ← 1 reserved byte
     *   Report Count (5)                      95 05
     *   Report Size (1)                       75 01
     *   Usage Page (LEDs)                     05 08
     *   Usage Minimum (Num Lock)              19 01
     *   Usage Maximum (Kana)                  29 05
     *   Output (Data, Variable, Absolute)     91 02    ← 5 LED bits
     *   Report Count (1)                      95 01
     *   Report Size (3)                       75 03
     *   Output (Constant)                     91 01    ← 3 padding bits
     *   Report Count (6)                      95 06
     *   Report Size (8)                       75 08
     *   Logical Minimum (0)                   15 00
     *   Logical Maximum (101)                 25 65
     *   Usage Page (Key Codes)                05 07
     *   Usage Minimum (0)                     19 00
     *   Usage Maximum (101)                   29 65
     *   Input (Data, Array)                   81 00    ← 6 key code bytes
     * End Collection                          C0
     * ```
     */
    val DESCRIPTOR: ByteArray = intArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x06,       // Usage (Keyboard)
        0xA1, 0x01,       // Collection (Application)
        0x05, 0x07,       //   Usage Page (Key Codes)
        0x19, 0xE0,       //   Usage Minimum (224 = Left Control)
        0x29, 0xE7,       //   Usage Maximum (231 = Right GUI)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x01,       //   Logical Maximum (1)
        0x75, 0x01,       //   Report Size (1 bit)
        0x95, 0x08,       //   Report Count (8 bits = 1 byte of modifiers)
        0x81, 0x02,       //   Input (Data, Variable, Absolute) → modifier byte
        0x95, 0x01,       //   Report Count (1)
        0x75, 0x08,       //   Report Size (8 bits)
        0x81, 0x01,       //   Input (Constant) → reserved byte
        0x95, 0x05,       //   Report Count (5)
        0x75, 0x01,       //   Report Size (1 bit)
        0x05, 0x08,       //   Usage Page (LEDs)
        0x19, 0x01,       //   Usage Minimum (1 = Num Lock)
        0x29, 0x05,       //   Usage Maximum (5 = Kana)
        0x91, 0x02,       //   Output (Data, Variable, Absolute) → 5 LED bits
        0x95, 0x01,       //   Report Count (1)
        0x75, 0x03,       //   Report Size (3 bits)
        0x91, 0x01,       //   Output (Constant) → 3 padding bits
        0x95, 0x06,       //   Report Count (6)
        0x75, 0x08,       //   Report Size (8 bits)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x65,       //   Logical Maximum (101)
        0x05, 0x07,       //   Usage Page (Key Codes)
        0x19, 0x00,       //   Usage Minimum (0)
        0x29, 0x65,       //   Usage Maximum (101)
        0x81, 0x00,       //   Input (Data, Array) → 6 key codes
        0xC0              // End Collection
    ).map { it.toByte() }.toByteArray()
}
