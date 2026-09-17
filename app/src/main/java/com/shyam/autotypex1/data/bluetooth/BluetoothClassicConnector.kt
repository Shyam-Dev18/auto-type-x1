package com.shyam.autotypex1.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.shyam.autotypex1.domain.model.BluetoothError
import com.shyam.autotypex1.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages Bluetooth HID Device profile proxy connection lifecycle and HID keyboard input.
 *
 * Uses `BluetoothProfile.HID_DEVICE` proxy directly.
 * Handles HID application registration, host connection, input report transmission,
 * and clean resource teardown with stuck-key prevention.
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
 * Disconnect semantics:
 * - **Full disconnect** (user-initiated): `hid.disconnect()` → wait `STATE_DISCONNECTED` →
 *   `unregisterApp()` → wait `onAppStatusChanged(false)` → `Disconnected`.
 *   Removes the SDP record so the host cannot auto-reconnect.
 * - **Switch disconnect** (connecting to new device): `hid.disconnect()` → wait
 *   `STATE_DISCONNECTED` → `Disconnected`. App stays registered, ready for next `connect()`.
 *
 * Guarantees:
 * - Timeout cancels and closes the connection so a late callback cannot become Connected
 * - Duplicate connect calls return typed errors (AlreadyConnecting / AlreadyConnected)
 * - Explicit sendKeyDown and sendKeyUp separation with stuck-key safety net (releaseAllKeys)
 * - Bond state receiver is always unregistered on release
 * - Profile proxy and HID app registration are cleanly unregistered and closed on release
 * - All coroutine jobs are cancelled on release
 */
class BluetoothClassicConnector(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
    private val scope: CoroutineScope,
    private val hidDeviceManager: HidDeviceManager = HidDeviceManager(),
    private val connectionEventListener: ConnectionEventListener? = null
) {

    companion object {
        /** Hard connection timeout in milliseconds. */
        const val CONNECTION_TIMEOUT_MS = 20_000L
        /** Timeout for unregisterApp callback. */
        const val UNREGISTER_TIMEOUT_MS = 3_000L
    }

    // ── Public state ────────────────────────────────────────────────

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // ── Internal state ──────────────────────────────────────────────

    /** The HID Device profile proxy, set when the profile service connects. */
    @Volatile
    var hidDevice: BluetoothHidDevice? = null
        private set

    /** The currently connected remote host device. */
    @Volatile
    var connectedHost: BluetoothDevice? = null
        private set

    /** Address of the device we're trying to connect to. */
    @Volatile
    private var pendingHostAddress: String? = null

    /**
     * Tracks the user's connection intent to distinguish full disconnect,
     * switch-device disconnect, and active connection attempts.
     */
    enum class UserConnectionIntent {
        /** User explicitly requested a full disconnect (unregister SDP). */
        DISCONNECTED,
        /** User requested a connect. */
        CONNECT_REQUESTED,
        /** Internal: switching from one device to another without unregistering. */
        SWITCH_DEVICE
    }

    @Volatile
    private var userIntent = UserConnectionIntent.DISCONNECTED

    /** Guard against stale timeout firing after a successful connect or explicit disconnect. */
    private var connectionGeneration = 0

    private var timeoutJob: Job? = null
    private var unregisterTimeoutJob: Job? = null
    private var bondReceiverRegistered = false
    private val released = AtomicBoolean(false)
    private val callbackExecutor: Executor = Executors.newSingleThreadExecutor()

    /**
     * Guards the unregister phase of a full disconnect cycle.
     *
     * Set to `true` (via compareAndSet) when [initiateUnregister] fires for the first time
     * in a disconnect cycle. While active, all `STATE_DISCONNECTING` and `STATE_DISCONNECTED`
     * callbacks from rejected stray reconnects are suppressed — they are noise produced by
     * the host re-paging during the async gap between `unregisterApp()` and
     * `onAppStatusChanged(false)`.
     *
     * Reset to `false` at the start of every [disconnect] and [connect] call, and on
     * completion of the unregister phase (in `onAppStatusChanged(false)` or the fallback
     * timeout).
     */
    private val unregisterPhaseActive = AtomicBoolean(false)

    // ── Connection event listener ───────────────────────────────────

    /**
     * Callback interface for connection lifecycle events.
     * Used by the repository to start/stop the foreground service.
     */
    interface ConnectionEventListener {
        fun onConnected(deviceName: String, deviceAddress: String)
        fun onDisconnected()
    }

    // ── HID profile callback ────────────────────────────────────────

    /**
     * HID Device profile callback. Handles registration and connection state transitions
     * from the Bluetooth stack.
     */
    @SuppressLint("MissingPermission")
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            hidDeviceManager.isAppRegistered.set(registered)

            if (registered) {
                // If user requested connection and we have a pending host, proceed now that app is registered
                if (userIntent == UserConnectionIntent.CONNECT_REQUESTED &&
                    pendingHostAddress != null &&
                    _connectionState.value is ConnectionState.Connecting
                ) {
                    connectPendingHost()
                }
            } else {
                // App was unregistered

                // If this is the completion of a user-initiated full disconnect teardown,
                // finalize the Disconnected state
                if (userIntent == UserConnectionIntent.DISCONNECTED) {
                    unregisterTimeoutJob?.cancel()
                    unregisterTimeoutJob = null
                    unregisterPhaseActive.set(false)
                    connectedHost = null
                    pendingHostAddress = null
                    _connectionState.value = ConnectionState.Disconnected
                    connectionEventListener?.onDisconnected()
                    return
                }

                // If app unregistered unexpectedly while connecting, fail the attempt
                if (_connectionState.value is ConnectionState.Connecting) {
                    timeoutJob?.cancel()
                    timeoutJob = null
                    _connectionState.value = ConnectionState.Failed(BluetoothError.HidRegistrationFailed)
                    pendingHostAddress = null
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (released.get()) return

            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    // If user explicitly disconnected, reject any incoming reconnect
                    if (userIntent == UserConnectionIntent.DISCONNECTED) {
                        hidDeviceManager.releaseAllKeys(hidDevice, device)
                        try {
                            hidDevice?.disconnect(device)
                        } catch (_: Exception) {}
                        // Don't emit any state — we're in the teardown/unregister phase
                        return
                    }

                    // Cancel timeout — connection succeeded
                    timeoutJob?.cancel()
                    timeoutJob = null

                    // Only accept if this is the device we're actively connecting to
                    val pending = pendingHostAddress
                    if (pending == null || !device.address.equals(pending, ignoreCase = true)) {
                        // Unsolicited device attempted connection while not requested
                        try {
                            hidDevice?.disconnect(device)
                        } catch (_: Exception) {}
                        return
                    }

                    connectedHost = device
                    pendingHostAddress = null

                    val name = try {
                        device.name ?: "Unknown Device"
                    } catch (_: SecurityException) {
                        "Unknown Device"
                    }
                    _connectionState.value = ConnectionState.Connected(
                        deviceName = name,
                        deviceAddress = device.address
                    )
                    connectionEventListener?.onConnected(name, device.address)
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    // If user disconnected, reject reconnect attempts
                    if (userIntent == UserConnectionIntent.DISCONNECTED) {
                        try {
                            hidDevice?.disconnect(device)
                        } catch (_: Exception) {}
                        return
                    }
                    _connectionState.value = ConnectionState.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTING -> {
                    // During the unregister phase of a full disconnect, the host may
                    // re-page and get rejected, producing STATE_DISCONNECTING noise.
                    // Suppress it — the UI is already showing Disconnecting from the
                    // original disconnect() call.
                    if (userIntent == UserConnectionIntent.DISCONNECTED && unregisterPhaseActive.get()) {
                        return
                    }
                    _connectionState.value = ConnectionState.Disconnecting
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // During the unregister phase, suppress all STATE_DISCONNECTED
                    // callbacks — they are noise from rejected stray reconnects.
                    // The real terminal emission happens in onAppStatusChanged(false).
                    if (userIntent == UserConnectionIntent.DISCONNECTED && unregisterPhaseActive.get()) {
                        return
                    }

                    // Safety net: ensure any keys are released
                    hidDeviceManager.releaseAllKeys(hidDevice, device)

                    // Cancel timeout if still running
                    timeoutJob?.cancel()
                    timeoutJob = null

                    if (connectedHost?.address.equals(device.address, ignoreCase = true)) {
                        connectedHost = null
                    }
                    if (pendingHostAddress?.equals(device.address, ignoreCase = true) == true) {
                        pendingHostAddress = null
                    }

                    when (userIntent) {
                        UserConnectionIntent.DISCONNECTED -> {
                            // First STATE_DISCONNECTED in this disconnect cycle:
                            // proceed to unregister the HID app to remove the SDP
                            // record and prevent host auto-reconnect.
                            // initiateUnregister() sets unregisterPhaseActive, so
                            // any subsequent STATE_DISCONNECTED from rejected stray
                            // reconnects will be suppressed by the guard above.
                            initiateUnregister()
                        }
                        UserConnectionIntent.SWITCH_DEVICE -> {
                            // Switch-device disconnect: stay registered, just emit Disconnected
                            _connectionState.value = ConnectionState.Disconnected
                        }
                        UserConnectionIntent.CONNECT_REQUESTED -> {
                            // Connection attempt was dropped by the stack
                            _connectionState.value = ConnectionState.Disconnected
                            connectionEventListener?.onDisconnected()
                        }
                    }
                }
            }
        }
    }

    // ── Profile service listener ────────────────────────────────────

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            if (released.get()) {
                // We were released while waiting for the proxy — close immediately
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
                return
            }
            val hid = proxy as BluetoothHidDevice
            hidDevice = hid

            // Register HID application descriptor & QoS
            hidDeviceManager.registerApp(hid, callbackExecutor, hidCallback)

            // If we have a pending connection and app registration is already active, proceed
            if (pendingHostAddress != null && hidDeviceManager.isAppRegistered.get()) {
                connectPendingHost()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDeviceManager.releaseAllKeys(hidDevice, connectedHost)
            hidDeviceManager.isAppRegistered.set(false)
            hidDevice = null
            connectedHost = null
            if (_connectionState.value !is ConnectionState.Failed) {
                _connectionState.value = ConnectionState.Disconnected
                connectionEventListener?.onDisconnected()
            }
        }
    }

    // ── Bond state receiver ─────────────────────────────────────────

    private val bondStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
            if (released.get()) return

            val device = extractDevice(intent) ?: return
            if (device.address != pendingHostAddress) return

            val bondState = intent.getIntExtra(
                BluetoothDevice.EXTRA_BOND_STATE,
                BluetoothDevice.ERROR
            )
            val prevBondState = intent.getIntExtra(
                BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                BluetoothDevice.ERROR
            )

            when (bondState) {
                BluetoothDevice.BOND_BONDED -> {
                    // Pairing succeeded — now attempt the HID connection
                    connectPendingHost()
                }
                BluetoothDevice.BOND_NONE -> {
                    if (prevBondState == BluetoothDevice.BOND_BONDING) {
                        // Pairing was rejected or cancelled
                        timeoutJob?.cancel()
                        timeoutJob = null
                        _connectionState.value = ConnectionState.Failed(BluetoothError.PairingFailed)
                        pendingHostAddress = null
                    }
                }
            }
        }
    }

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Initiate a connection to the device at [address].
     *
     * If currently connected or connecting to a device, performs a switch-device
     * disconnect (keeps app registered) then connects to the new device.
     *
     * If app is not registered (after a previous full disconnect), re-registers
     * before connecting.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> {
        if (released.get()) {
            return Result.failure(IllegalStateException("Connector has been released"))
        }

        // If currently connected or connecting, do a switch-device disconnect
        // (keeps registration alive for the next connect)
        if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) {
            switchDisconnect()
        }

        val btAdapter = adapter ?: run {
            _connectionState.value = ConnectionState.Failed(BluetoothError.HidProfileUnavailable)
            return Result.failure(
                BluetoothOperationException(BluetoothError.HidProfileUnavailable)
            )
        }

        if (!btAdapter.isEnabled) {
            _connectionState.value = ConnectionState.Failed(BluetoothError.BluetoothDisabled)
            return Result.failure(
                BluetoothOperationException(BluetoothError.BluetoothDisabled)
            )
        }

        // Set up connection attempt
        userIntent = UserConnectionIntent.CONNECT_REQUESTED
        unregisterPhaseActive.set(false)
        pendingHostAddress = address
        connectionGeneration++
        _connectionState.value = ConnectionState.Connecting

        // Register bond state receiver
        registerBondReceiver()

        // Get profile proxy (async — profileListener.onServiceConnected will fire)
        val proxyReady = ensureProfileProxy()
        if (!proxyReady) {
            _connectionState.value = ConnectionState.Failed(BluetoothError.HidProfileUnavailable)
            pendingHostAddress = null
            userIntent = UserConnectionIntent.DISCONNECTED
            return Result.failure(
                BluetoothOperationException(BluetoothError.HidProfileUnavailable)
            )
        }

        // If proxy is ready but app is not registered (after a previous full disconnect),
        // re-register. onAppStatusChanged(true) will call connectPendingHost().
        val hid = hidDevice
        if (hid != null && !hidDeviceManager.isAppRegistered.get()) {
            hidDeviceManager.registerApp(hid, callbackExecutor, hidCallback)
        } else if (hid != null && hidDeviceManager.isAppRegistered.get()) {
            // Proxy ready and app registered — connect immediately
            connectPendingHost()
        }

        // Start timeout
        startTimeoutWatcher()

        return Result.success(Unit)
    }

    /**
     * Full user-initiated disconnect with SDP teardown.
     *
     * Performs a two-step teardown:
     * 1. `hid.disconnect(device)` — waits for `STATE_DISCONNECTED`
     * 2. `hid.unregisterApp()` — waits for `onAppStatusChanged(false)`
     *
     * After unregisterApp completes, the SDP record is removed and the host
     * cannot auto-reconnect. ConnectionState.Disconnected is emitted only
     * after the full teardown completes.
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        userIntent = UserConnectionIntent.DISCONNECTED
        unregisterPhaseActive.set(false)
        timeoutJob?.cancel()
        timeoutJob = null
        pendingHostAddress = null

        val host = connectedHost
        val hid = hidDevice

        if (host != null && hid != null) {
            // Safety net: release all keys before disconnecting
            hidDeviceManager.releaseAllKeys(hid, host)
            _connectionState.value = ConnectionState.Disconnecting
            try {
                hid.disconnect(host)
            } catch (_: SecurityException) {
                // Best effort — fall through to unregister
            }
            // onConnectionStateChanged(STATE_DISCONNECTED) will call initiateUnregister()
            // Fallback: if STATE_DISCONNECTED callback never fires
            val gen = connectionGeneration
            scope.launch {
                delay(UNREGISTER_TIMEOUT_MS)
                if (gen == connectionGeneration && _connectionState.value is ConnectionState.Disconnecting) {
                    // Force unregister
                    connectedHost = null
                    initiateUnregister()
                }
            }
        } else if (hid != null && hidDeviceManager.isAppRegistered.get()) {
            // No active host connection but app is still registered — just unregister
            _connectionState.value = ConnectionState.Disconnecting
            initiateUnregister()
        } else {
            // Nothing to tear down
            connectedHost = null
            _connectionState.value = ConnectionState.Disconnected
            connectionEventListener?.onDisconnected()
        }
    }

    /**
     * Switch-device disconnect: disconnects the current host WITHOUT unregistering
     * the HID app, so the next connect() can proceed immediately.
     */
    @SuppressLint("MissingPermission")
    private suspend fun switchDisconnect(timeoutMs: Long = 2500L) {
        if (_connectionState.value is ConnectionState.Disconnected) return

        userIntent = UserConnectionIntent.SWITCH_DEVICE
        timeoutJob?.cancel()
        timeoutJob = null
        pendingHostAddress = null

        val host = connectedHost
        val hid = hidDevice

        if (host != null && hid != null) {
            hidDeviceManager.releaseAllKeys(hid, host)
            _connectionState.value = ConnectionState.Disconnecting
            try {
                hid.disconnect(host)
            } catch (_: SecurityException) {
                // Best effort
            }
            // Wait for STATE_DISCONNECTED callback
            val startTime = System.currentTimeMillis()
            while (_connectionState.value !is ConnectionState.Disconnected && System.currentTimeMillis() - startTime < timeoutMs) {
                delay(50L)
            }
            if (_connectionState.value !is ConnectionState.Disconnected) {
                connectedHost = null
                _connectionState.value = ConnectionState.Disconnected
            }
        } else {
            connectedHost = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Release all resources. Must be called to prevent leaks.
     * Safe to call multiple times.
     */
    @SuppressLint("MissingPermission")
    fun release() {
        if (!released.compareAndSet(false, true)) return
        userIntent = UserConnectionIntent.DISCONNECTED

        // Cancel timeouts
        timeoutJob?.cancel()
        timeoutJob = null
        unregisterTimeoutJob?.cancel()
        unregisterTimeoutJob = null

        val host = connectedHost
        val hid = hidDevice

        // Release keys and unregister HID app
        hidDeviceManager.release(hid, host)

        // Disconnect if connected
        if (host != null && hid != null) {
            try {
                hid.disconnect(host)
            } catch (_: Exception) { /* best effort */ }
        }

        // Close profile proxy
        if (hid != null) {
            try {
                adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
            } catch (_: Exception) { /* best effort */ }
        }

        // Unregister bond receiver
        unregisterBondReceiver()

        // Reset state
        hidDevice = null
        connectedHost = null
        pendingHostAddress = null
        _connectionState.value = ConnectionState.Disconnected
    }

    // ── HID Key Operations ──────────────────────────────────────────

    /**
     * Sends a key-down report for the specified [stroke].
     */
    fun sendKeyDown(stroke: HidKeyStroke): Result<Boolean> {
        if (released.get()) {
            return Result.failure(BluetoothOperationException(BluetoothError.NotConnected))
        }
        val hid = hidDevice ?: return Result.failure(
            BluetoothOperationException(BluetoothError.NotConnected)
        )
        val host = connectedHost ?: return Result.failure(
            BluetoothOperationException(BluetoothError.NotConnected)
        )

        val sent = hidDeviceManager.sendKeyDown(hid, host, stroke)
        return if (sent) {
            Result.success(true)
        } else {
            Result.failure(BluetoothOperationException(BluetoothError.SendReportFailed))
        }
    }

    /**
     * Sends a key-up report (all zeros) to release currently pressed key.
     */
    fun sendKeyUp(): Result<Boolean> {
        if (released.get()) {
            return Result.failure(BluetoothOperationException(BluetoothError.NotConnected))
        }
        val hid = hidDevice ?: return Result.failure(
            BluetoothOperationException(BluetoothError.NotConnected)
        )
        val host = connectedHost ?: return Result.failure(
            BluetoothOperationException(BluetoothError.NotConnected)
        )

        val sent = hidDeviceManager.sendKeyUp(hid, host)
        return if (sent) {
            Result.success(true)
        } else {
            Result.failure(BluetoothOperationException(BluetoothError.SendReportFailed))
        }
    }

    /**
     * Releases all pressed keys (safety net).
     */
    fun releaseAllKeys(): Result<Boolean> {
        val hid = hidDevice
        val host = connectedHost
        val sent = hidDeviceManager.releaseAllKeys(hid, host)
        return Result.success(sent)
    }

    /**
     * Returns true if a remote host is currently connected.
     */
    fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected && connectedHost != null
    }

    // ── Internal helpers ────────────────────────────────────────────

    /**
     * Initiates HID app unregistration as part of the full disconnect teardown.
     *
     * Uses [unregisterPhaseActive] as a one-shot gate: the first call in a disconnect
     * cycle sets the flag and proceeds; any re-entrant call (from a stray reconnect's
     * STATE_DISCONNECTED or the fallback timeout racing against the real callback) is
     * a no-op.
     *
     * After unregisterApp completes (via [onAppStatusChanged] callback or the fallback
     * timeout), the SDP record is removed and [ConnectionState.Disconnected] is emitted.
     */
    private fun initiateUnregister() {
        // One-shot gate: only the first caller in this disconnect cycle proceeds.
        if (!unregisterPhaseActive.compareAndSet(false, true)) return

        val hid = hidDevice
        if (hid != null && hidDeviceManager.isAppRegistered.get()) {
            hidDeviceManager.unregisterApp(hid)
            // onAppStatusChanged(false) will emit Disconnected and notify listener
            // Fallback timeout in case the callback never fires
            unregisterTimeoutJob?.cancel()
            unregisterTimeoutJob = scope.launch {
                delay(UNREGISTER_TIMEOUT_MS)
                if (userIntent == UserConnectionIntent.DISCONNECTED &&
                    _connectionState.value !is ConnectionState.Disconnected
                ) {
                    unregisterPhaseActive.set(false)
                    connectedHost = null
                    pendingHostAddress = null
                    hidDeviceManager.isAppRegistered.set(false)
                    _connectionState.value = ConnectionState.Disconnected
                    connectionEventListener?.onDisconnected()
                }
            }
        } else {
            // App not registered or no proxy — finalize directly
            connectedHost = null
            pendingHostAddress = null
            _connectionState.value = ConnectionState.Disconnected
            connectionEventListener?.onDisconnected()
        }
    }

    private fun ensureProfileProxy(): Boolean {
        if (hidDevice != null) return true
        return adapter?.getProfileProxy(
            context, profileListener, BluetoothProfile.HID_DEVICE
        ) ?: false
    }

    @SuppressLint("MissingPermission")
    private fun connectPendingHost() {
        val hid = hidDevice ?: return
        val address = pendingHostAddress ?: return

        // If HID app is not registered yet, wait for onAppStatusChanged callback
        if (!hidDeviceManager.isAppRegistered.get()) {
            return
        }

        val host = findDevice(address) ?: run {
            _connectionState.value = ConnectionState.Failed(BluetoothError.PairingFailed)
            pendingHostAddress = null
            timeoutJob?.cancel()
            return
        }

        // If not bonded, initiate bonding (bondStateReceiver will call us again)
        try {
            if (host.bondState == BluetoothDevice.BOND_NONE) {
                _connectionState.value = ConnectionState.Connecting
                host.createBond()
                return
            }
        } catch (_: SecurityException) {
            _connectionState.value = ConnectionState.Failed(BluetoothError.PermissionMissing)
            pendingHostAddress = null
            timeoutJob?.cancel()
            return
        }

        // Bonded — attempt HID connection
        try {
            if (!hid.connect(host)) {
                _connectionState.value = ConnectionState.Failed(
                    BluetoothError.Unknown("HID connect() returned false")
                )
                pendingHostAddress = null
                timeoutJob?.cancel()
            }
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Failed(BluetoothError.PermissionMissing)
            pendingHostAddress = null
            timeoutJob?.cancel()
        }
    }

    private fun startTimeoutWatcher() {
        timeoutJob?.cancel()
        val gen = connectionGeneration
        timeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT_MS)
            // Only fire if we're still on the same connection generation and still connecting
            if (gen == connectionGeneration && _connectionState.value is ConnectionState.Connecting) {
                // Timeout: disconnect and close everything so late callbacks can't promote to Connected
                val host = connectedHost ?: findDevice(pendingHostAddress ?: "")
                val hid = hidDevice
                if (host != null && hid != null) {
                    try {
                        hidDeviceManager.releaseAllKeys(hid, host)
                        @SuppressLint("MissingPermission")
                        hid.disconnect(host)
                    } catch (_: Exception) { /* best effort */ }
                }
                connectedHost = null
                pendingHostAddress = null
                _connectionState.value = ConnectionState.Failed(BluetoothError.ConnectionTimeout)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findDevice(address: String): BluetoothDevice? {
        if (address.isBlank()) return null
        try {
            // Check bonded devices first
            val bonded = adapter?.bondedDevices?.firstOrNull { it.address == address }
            if (bonded != null) return bonded
        } catch (_: SecurityException) {
            // Missing BLUETOOTH_CONNECT
        }
        // Fall back to getRemoteDevice
        return try {
            adapter?.getRemoteDevice(address)
        } catch (_: Exception) {
            null
        }
    }

    private fun registerBondReceiver() {
        if (bondReceiverRegistered) return
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bondStateReceiver, filter)
        bondReceiverRegistered = true
    }

    private fun unregisterBondReceiver() {
        if (!bondReceiverRegistered) return
        try {
            context.unregisterReceiver(bondStateReceiver)
        } catch (_: IllegalArgumentException) {
            // Already unregistered
        }
        bondReceiverRegistered = false
    }

    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }
}

/**
 * Exception wrapper for [BluetoothError] so it can be used in [Result.failure].
 */
class BluetoothOperationException(val error: BluetoothError) :
    Exception("Bluetooth operation failed: $error")
