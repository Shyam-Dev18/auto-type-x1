package com.shyam.autotypex1.domain.model

/**
 * Bluetooth adapter hardware state — three explicit states.
 *
 * Transitional states (TURNING_ON, TURNING_OFF) are collapsed into [Disabled]
 * since they provide no actionable UI difference and would complicate the state machine.
 *
 * [Unavailable] means the device has no Bluetooth hardware at all.
 */
enum class BluetoothAdapterState {
    /** No Bluetooth adapter present on this device. */
    Unavailable,

    /** Adapter exists but is turned off (or transitioning). */
    Disabled,

    /** Adapter is on and ready for use. */
    Enabled
}
