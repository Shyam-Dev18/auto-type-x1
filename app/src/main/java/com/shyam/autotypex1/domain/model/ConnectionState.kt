package com.shyam.autotypex1.domain.model

/**
 * Bluetooth connection lifecycle — explicit sealed states.
 *
 * State machine:
 * ```
 * Disconnected ──→ Connecting ──→ Connected
 *       ↑              │               │
 *       │              ↓               ↓
 *       │           Failed        Disconnecting
 *       │              │               │
 *       └──────────────┴───────────────┘
 * ```
 *
 * - [Connecting] is the only state from which [Connected] or [Failed] can be reached.
 * - [Disconnecting] is entered when an active connection is being torn down.
 * - [Failed] carries a typed [BluetoothError] — never a raw string.
 * - [Connected] carries device identity so the UI can display it.
 */
sealed interface ConnectionState {
    /** No active connection or connection attempt. */
    data object Disconnected : ConnectionState

    /** Connection attempt in progress (may be waiting for pairing, profile proxy, etc.). */
    data object Connecting : ConnectionState

    /** Successfully connected to the remote device. */
    data class Connected(
        val deviceName: String,
        val deviceAddress: String
    ) : ConnectionState

    /** Connection attempt failed with a typed error. */
    data class Failed(val error: BluetoothError) : ConnectionState

    /** Active connection is being torn down. */
    data object Disconnecting : ConnectionState
}
