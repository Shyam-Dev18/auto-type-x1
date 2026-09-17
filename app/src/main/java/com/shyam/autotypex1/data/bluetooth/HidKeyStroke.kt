package com.shyam.autotypex1.data.bluetooth

/**
 * A USB HID keyboard key stroke: a key code + optional modifier byte.
 *
 * @property keyCode  HID Usage ID from the Keyboard/Keypad Usage Page (§10).
 *                    e.g. 0x04 = 'a', 0x1E = '1', 0x28 = Enter.
 * @property modifier Modifier bit flags (byte 0 of the HID input report).
 *                    0x00 = none, 0x02 = Left Shift, 0x01 = Left Control, etc.
 */
data class HidKeyStroke(
    val keyCode: Byte,
    val modifier: Byte = 0
) {
    companion object {
        // Modifier bit flags (byte 0 of the 8-byte input report)
        const val MOD_NONE: Byte = 0x00
        const val MOD_LEFT_CTRL: Byte = 0x01
        const val MOD_LEFT_SHIFT: Byte = 0x02
        const val MOD_LEFT_ALT: Byte = 0x04
        const val MOD_LEFT_GUI: Byte = 0x08
        const val MOD_RIGHT_CTRL: Byte = 0x10
        const val MOD_RIGHT_SHIFT: Byte = 0x20
        const val MOD_RIGHT_ALT: Byte = 0x40

        // Common special key codes
        const val KEY_ENTER: Byte = 0x28
        const val KEY_ESCAPE: Byte = 0x29
        const val KEY_BACKSPACE: Byte = 0x2A
        const val KEY_TAB: Byte = 0x2B
        const val KEY_SPACE: Byte = 0x2C
        const val KEY_DELETE: Byte = 0x4C
    }
}
