package com.shyam.autotypex1.data.bluetooth

/**
 * USB HID keyboard key stroke — keyCode + modifier byte.
 * Ported from old codebase (proven correct).
 */
data class HidKeyStroke(
    val keyCode: Byte,
    val modifier: Byte = 0
)
