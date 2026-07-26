package com.shyam.autotypex1.domain.model

/** Bluetooth HID connection state — sealed for exhaustive when + carries data on each variant. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val deviceName: String, val deviceAddress: String) : ConnectionState
    data class Failed(val reason: ConnectionFailure) : ConnectionState
}

enum class ConnectionFailure {
    BLUETOOTH_DISABLED,
    PERMISSION_MISSING,
    HID_ROLE_UNSUPPORTED,
    HOST_REJECTED,
    TIMEOUT,
    UNKNOWN
}
