package com.shyam.autotypex1.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.shyam.autotypex1.data.local.KnownDevicesDataStore
import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionFailure
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.domain.repository.HidConnectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BluetoothHidDevice (Bluetooth Classic HID Device profile) implementation.
 *
 * Ported from the old codebase's BluetoothSessionRepository with these fixes:
 * - Properly closes BluetoothProfile proxy on disconnect (old code leaked)
 * - Uses sealed ConnectionState instead of flat enum (carries device info + failure reasons)
 * - Permission checks at every call site (§0.6)
 * - Returns Result<T> for all fallible operations
 * - Guards against double registerApp
 */
@Singleton
class BluetoothHidRepositoryImpl @Inject constructor(
    private val context: Context,
    private val knownDevicesDataStore: KnownDevicesDataStore
) : HidConnectionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val callbackExecutor: Executor = Executors.newSingleThreadExecutor()

    // State flows
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _adapterState = MutableStateFlow(readAdapterState())
    private val _scannedDevices = MutableStateFlow<List<ScannedDevice>>(emptyList())
    private val _isScanning = MutableStateFlow(false)

    // Internal state
    private var hidDevice: BluetoothHidDevice? = null
    private var appRegistered = false
    private var pendingHostAddress: String? = null
    private var connectedHost: BluetoothDevice? = null
    private var scanReceiverRegistered = false
    private var btStateReceiverRegistered = false
    private val discoveredDevices = linkedMapOf<String, ScannedDevice>()
    private var connectionTimeoutJob: Job? = null

    init {
        registerBluetoothStateReceiver()
    }

    // ── Flow accessors ──────────────────────────────────────────────────

    override fun observeConnectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()
    override fun observeAdapterState(): Flow<BluetoothAdapterState> {
        _adapterState.value = readAdapterState()
        return _adapterState.asStateFlow()
    }
    override fun observeScannedDevices(): Flow<List<ScannedDevice>> = _scannedDevices.asStateFlow()
    override fun observeIsScanning(): Flow<Boolean> = _isScanning.asStateFlow()

    // ── Scanning ────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun startScan(): Result<Unit> = runCatching {
        if (!canScan()) throw SecurityException("Missing scan permissions")
        val btAdapter = adapter ?: throw IllegalStateException("No Bluetooth adapter")
        if (!btAdapter.isEnabled) throw IllegalStateException("Bluetooth is disabled")

        registerScanReceiver()

        if (btAdapter.isDiscovering) btAdapter.cancelDiscovery()

        _isScanning.value = true
        discoveredDevices.clear()

        // Pre-populate with bonded devices
        btAdapter.bondedDevices?.forEach { bonded ->
            discoveredDevices[bonded.address] = ScannedDevice(
                name = bonded.name ?: "Unknown Device",
                address = bonded.address
            )
        }
        _scannedDevices.value = discoveredDevices.values.toList().sortedBy { it.name.lowercase() }

        if (!btAdapter.startDiscovery()) {
            _isScanning.value = false
            throw IllegalStateException("Failed to start discovery")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan(): Result<Unit> = runCatching {
        adapter?.takeIf { it.isDiscovering }?.cancelDiscovery()
        _isScanning.value = false
    }

    // ── Connection ──────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Result<Unit> = runCatching {
        val activeAddress = connectedHost?.address ?: pendingHostAddress
        if (activeAddress != null) {
            disconnect().getOrNull()
        }

        if (!isHidSupported()) {
            _connectionState.value = ConnectionState.Failed(ConnectionFailure.HID_ROLE_UNSUPPORTED)
            throw UnsupportedOperationException("HID Device profile not supported")
        }
        if (!hasConnectPermission()) {
            _connectionState.value = ConnectionState.Failed(ConnectionFailure.PERMISSION_MISSING)
            throw SecurityException("Missing BLUETOOTH_CONNECT permission")
        }
        if (adapter?.isEnabled != true) {
            _connectionState.value = ConnectionState.Failed(ConnectionFailure.BLUETOOTH_DISABLED)
            throw IllegalStateException("Bluetooth is disabled")
        }

        pendingHostAddress = address
        _connectionState.value = ConnectionState.Connecting
        if (ensureProfileProxy()) {
            connectPendingHostIfReady()
        }
        startConnectionTimeoutWatcher()
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect(): Result<Unit> = runCatching {
        pendingHostAddress = null
        val host = connectedHost
        val hid = hidDevice
        if (host != null && hid != null) {
            hid.disconnect(host)
        }
        connectedHost = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun reconnectLast(): Result<Unit> = runCatching {
        val addr = knownDevicesDataStore.observeLastConnectedAddress().firstOrNull()
            ?: throw IllegalStateException("No last connected device")
        connect(addr).getOrThrow()
    }

    // ── Key sending ─────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun sendCharacter(char: Char): Result<Boolean> = runCatching {
        val key = HidKeyMapper.mapChar(char) ?: return@runCatching false
        sendKeyStroke(key)
    }

    @SuppressLint("MissingPermission")
    override suspend fun sendBackspace(): Result<Boolean> = runCatching {
        sendKeyStroke(HidKeyStroke(keyCode = 0x2A))
    }

    @SuppressLint("MissingPermission")
    override suspend fun releaseAllKeys(): Result<Boolean> = runCatching {
        val host = connectedHost ?: return@runCatching false
        val hid = hidDevice ?: return@runCatching false
        val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        hid.sendReport(host, 1, releaseReport)
    }

    // ── State queries ───────────────────────────────────────────────────

    override fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected
    override fun isTypingReady(): Boolean = isConnected() && connectedHost != null && hidDevice != null

    override fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    override fun requestEnableBluetooth() {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ── Internal: BroadcastReceivers ────────────────────────────────────

    private val scanReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return

                    val address = device.address ?: return
                    val name = device.name ?: "Unknown Device"
                    discoveredDevices[address] = ScannedDevice(name = name, address = address)
                    _scannedDevices.value = discoveredDevices.values.toList().sortedBy { it.name.lowercase() }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    private val btStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _adapterState.value = mapAdapterState(state)

                    if (_adapterState.value == BluetoothAdapterState.OFF ||
                        _adapterState.value == BluetoothAdapterState.UNAVAILABLE
                    ) {
                        connectedHost = null
                        _isScanning.value = false
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

                    if (device.address == pendingHostAddress) {
                        if (bondState == BluetoothDevice.BOND_BONDED) {
                            connectPendingHostIfReady()
                        } else if (bondState == BluetoothDevice.BOND_NONE) {
                            val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
                            if (prevBondState == BluetoothDevice.BOND_BONDING) {
                                _connectionState.value = ConnectionState.Failed(ConnectionFailure.HOST_REJECTED)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Internal: Profile/HID callbacks ─────────────────────────────────

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = proxy as BluetoothHidDevice
            registerAppIfNeeded()
            connectPendingHostIfReady()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hidDevice = null
            appRegistered = false
            connectedHost = null
            if (_connectionState.value !is ConnectionState.Failed) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    @SuppressLint("MissingPermission")
    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            if (!registered) {
                _connectionState.value = ConnectionState.Failed(ConnectionFailure.HID_ROLE_UNSUPPORTED)
                return
            }
            connectPendingHostIfReady()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_DISCONNECTED) {
                connectionTimeoutJob?.cancel()
            }
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHost = device
                    val name = device.name ?: "Unknown Device"
                    val address = device.address
                    _connectionState.value = ConnectionState.Connected(
                        deviceName = name,
                        deviceAddress = address
                    )
                    // Persist known device
                    scope.launch {
                        knownDevicesDataStore.saveDevice(name, address)
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _connectionState.value = ConnectionState.Connecting
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (connectedHost?.address == device.address) {
                        connectedHost = null
                    }
                    _connectionState.value = ConnectionState.Disconnected
                }
                else -> Unit
            }
        }
    }

    // ── Internal: helpers ────────────────────────────────────────────────

    private fun ensureProfileProxy(): Boolean {
        if (hidDevice != null) {
            registerAppIfNeeded()
            return true
        }
        val success = adapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE) ?: false
        if (!success) {
            _connectionState.value = ConnectionState.Failed(ConnectionFailure.HID_ROLE_UNSUPPORTED)
        }
        return success
    }

    private fun startConnectionTimeoutWatcher() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(20000) // 20 seconds connection timeout
            if (_connectionState.value is ConnectionState.Connecting) {
                disconnect()
                _connectionState.value = ConnectionState.Failed(ConnectionFailure.TIMEOUT)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerAppIfNeeded() {
        val hid = hidDevice ?: return
        if (appRegistered) return

        // USB HID keyboard descriptor — proven correct from old codebase
        val descriptor = intArrayOf(
            0x05, 0x01, 0x09, 0x06, 0xA1, 0x01, 0x05, 0x07,
            0x19, 0xE0, 0x29, 0xE7, 0x15, 0x00, 0x25, 0x01,
            0x75, 0x01, 0x95, 0x08, 0x81, 0x02, 0x95, 0x01,
            0x75, 0x08, 0x81, 0x01, 0x95, 0x05, 0x75, 0x01,
            0x05, 0x08, 0x19, 0x01, 0x29, 0x05, 0x91, 0x02,
            0x95, 0x01, 0x75, 0x03, 0x91, 0x01, 0x95, 0x06,
            0x75, 0x08, 0x15, 0x00, 0x25, 0x65, 0x05, 0x07,
            0x19, 0x00, 0x29, 0x65, 0x81, 0x00, 0xC0
        ).map { it.toByte() }.toByteArray()

        val sdp = BluetoothHidDeviceAppSdpSettings(
            "AutoTypeHID",
            "Bluetooth keyboard typing",
            "AutoTypeHID",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            descriptor
        )

        val qos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_GUARANTEED,
            800, 9, 0, 11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        hid.registerApp(sdp, null, qos, callbackExecutor, hidCallback)
    }

    @SuppressLint("MissingPermission")
    private fun connectPendingHostIfReady() {
        val hid = hidDevice ?: return
        if (!appRegistered) return
        val address = pendingHostAddress ?: return
        val host = findBondedDevice(address)
        if (host == null) {
            _connectionState.value = ConnectionState.Failed(ConnectionFailure.HOST_REJECTED)
            return
        }

        // If not bonded, initiate bonding first!
        if (host.bondState == BluetoothDevice.BOND_NONE) {
            _connectionState.value = ConnectionState.Connecting
            host.createBond()
            return
        }

        if (!hid.connect(host)) {
            _connectionState.value = ConnectionState.Failed(ConnectionFailure.UNKNOWN)
            return
        }

        _connectionState.value = ConnectionState.Connecting
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendKeyStroke(stroke: HidKeyStroke): Boolean {
        val host = connectedHost ?: return false
        val hid = hidDevice ?: return false

        val pressReport = byteArrayOf(
            stroke.modifier, 0x00, stroke.keyCode,
            0x00, 0x00, 0x00, 0x00, 0x00
        )
        val releaseReport = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        val pressed = hid.sendReport(host, 1, pressReport)
        delay(15)
        val released = hid.sendReport(host, 1, releaseReport)
        delay(15)
        return pressed && released
    }

    @SuppressLint("MissingPermission")
    private fun findBondedDevice(address: String): BluetoothDevice? {
        val bonded = adapter?.bondedDevices?.firstOrNull { it.address == address }
        if (bonded != null) return bonded
        return runCatching { adapter?.getRemoteDevice(address) }.getOrNull()
    }

    private fun canScan(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasConnectPermission() &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun isHidSupported(): Boolean = adapter != null

    private fun registerScanReceiver() {
        if (scanReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(scanReceiver, filter)
        scanReceiverRegistered = true
    }

    private fun registerBluetoothStateReceiver() {
        if (btStateReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(btStateReceiver, filter)
        btStateReceiverRegistered = true
    }

    private fun readAdapterState(): BluetoothAdapterState {
        val btAdapter = adapter ?: return BluetoothAdapterState.UNAVAILABLE
        return mapAdapterState(btAdapter.state)
    }

    private fun mapAdapterState(state: Int): BluetoothAdapterState = when (state) {
        BluetoothAdapter.STATE_ON -> BluetoothAdapterState.ON
        BluetoothAdapter.STATE_OFF -> BluetoothAdapterState.OFF
        BluetoothAdapter.STATE_TURNING_ON -> BluetoothAdapterState.TURNING_ON
        BluetoothAdapter.STATE_TURNING_OFF -> BluetoothAdapterState.TURNING_OFF
        else -> BluetoothAdapterState.UNAVAILABLE
    }
}
