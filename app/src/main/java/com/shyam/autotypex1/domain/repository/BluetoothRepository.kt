package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import kotlinx.coroutines.flow.Flow

/**
 * Bluetooth Classic operations — domain interface.
 *
 * Phase 1 scope: adapter state, discovery, connection lifecycle.
 * Phase 2 scope: HID registration, key report transmission, and stuck-key prevention.
 *
 * The domain layer never sees `android.bluetooth.*` — only these abstractions.
 * Implementation lives in `data/bluetooth/` and `data/repository/`.
 */
interface BluetoothRepository {

    // ── State observation ────────────────────────────────────────────

    /** Adapter hardware state as a hot [Flow]. */
    fun observeAdapterState(): Flow<BluetoothAdapterState>

    /** Connection lifecycle state as a hot [Flow]. */
    fun observeConnectionState(): Flow<ConnectionState>

    /** Devices found during Classic discovery, as a hot [Flow]. */
    fun observeScannedDevices(): Flow<List<ScannedDevice>>

    /** Whether a discovery scan is currently in progress. */
    fun observeIsDiscovering(): Flow<Boolean>

    // ── Actions ──────────────────────────────────────────────────────

    /**
     * Start Bluetooth Classic discovery.
     *
     * Pre-populates bonded devices, then runs `BluetoothAdapter.startDiscovery()`.
     * Returns [Result.failure] on missing permissions, disabled adapter, or
     * if the system refuses to start discovery.
     */
    suspend fun startDiscovery(): Result<Unit>

    /**
     * Stop any running discovery.
     * Safe to call even if not currently discovering.
     */
    suspend fun stopDiscovery()

    /**
     * Initiate a connection to the device at [address].
     *
     * Uses the HID Device profile proxy. Runs asynchronously with a hard timeout.
     * Connection state transitions are emitted via [observeConnectionState].
     *
     * Returns [Result.failure] for pre-condition failures (permissions, adapter state,
     * duplicate connect). Async failures (timeout, pairing rejection) are emitted as
     * [ConnectionState.Failed] on the state flow.
     */
    suspend fun connect(address: String): Result<Unit>

    /**
     * Disconnect from the currently connected device (if any).
     * Transitions through [ConnectionState.Disconnecting] → [ConnectionState.Disconnected].
     * Safe to call in any state.
     */
    suspend fun disconnect()

    // ── HID Key Operations (Phase 2) ────────────────────────────────

    /**
     * Sends a key-down report for the specified [char].
     *
     * Returns [Result.failure] if not connected, character is unsupported,
     * or sending the report failed.
     */
    suspend fun sendKeyDown(char: Char): Result<Boolean>

    /**
     * Sends a key-up report (all keys released).
     *
     * Returns [Result.failure] if not connected or sending the report failed.
     */
    suspend fun sendKeyUp(): Result<Boolean>

    /**
     * Releases all keys on the connected host (safety net).
     * Guaranteed safe even if not connected.
     */
    suspend fun releaseAllKeys(): Result<Boolean>

    /**
     * Returns whether a remote host is currently connected.
     */
    fun isConnected(): Boolean

    // ── Cleanup ──────────────────────────────────────────────────────

    /**
     * Release all resources: unregister receivers, close sockets/proxies,
     * cancel coroutine jobs, and stop discovery.
     * Must be called when the repository is no longer needed.
     */
    fun release()
}
