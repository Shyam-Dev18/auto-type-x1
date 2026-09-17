package com.shyam.autotypex1.domain.model

/**
 * Typed domain errors for Bluetooth operations.
 *
 * Every failure path emits one of these instead of raw exceptions or strings.
 * UI layer converts these to human-readable messages.
 */
sealed interface BluetoothError {
    /** Bluetooth adapter is disabled — user needs to enable it. */
    data object BluetoothDisabled : BluetoothError

    /** A required runtime permission has not been granted. */
    data object PermissionMissing : BluetoothError

    /** Connection attempt exceeded the hard timeout. */
    data object ConnectionTimeout : BluetoothError

    /** Pairing was rejected by the remote device or cancelled by the user. */
    data object PairingFailed : BluetoothError

    /** Bluetooth discovery failed to start. */
    data object DiscoveryFailed : BluetoothError

    /** A connect() call was made while already in Connecting state. */
    data object AlreadyConnecting : BluetoothError

    /** A connect() call was made while already Connected. */
    data object AlreadyConnected : BluetoothError

    /** HID Device profile is not supported on this device. */
    data object HidProfileUnavailable : BluetoothError

    /** Registration of HID device application failed. */
    data object HidRegistrationFailed : BluetoothError

    /** Failed to send HID input report to the host device. */
    data object SendReportFailed : BluetoothError

    /** Attempted an operation requiring an active connection while not connected. */
    data object NotConnected : BluetoothError

    /** Character cannot be mapped to a standard HID keycode. */
    data class UnsupportedCharacter(val char: Char) : BluetoothError

    /** Catch-all for unexpected failures. Carries a diagnostic message. */
    data class Unknown(val message: String) : BluetoothError
}
