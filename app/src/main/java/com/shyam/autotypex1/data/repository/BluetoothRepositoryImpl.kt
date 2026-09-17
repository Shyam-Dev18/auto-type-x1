package com.shyam.autotypex1.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import com.shyam.autotypex1.core.permissions.PermissionManager
import com.shyam.autotypex1.data.bluetooth.BluetoothAdapterStateMonitor
import com.shyam.autotypex1.data.bluetooth.BluetoothClassicConnector
import com.shyam.autotypex1.data.bluetooth.BluetoothOperationException
import com.shyam.autotypex1.data.bluetooth.BluetoothScanner
import com.shyam.autotypex1.data.bluetooth.HidKeyMapper
import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.BluetoothError
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import com.shyam.autotypex1.domain.typing.TypingServiceLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow

/**
 * Composes [BluetoothAdapterStateMonitor], [BluetoothScanner], and
 * [BluetoothClassicConnector] into a single [BluetoothRepository] implementation.
 *
 * Permission checks happen at this level before delegating to components.
 * Components themselves do not check permissions — they trust the caller.
 *
 * Implements [BluetoothClassicConnector.ConnectionEventListener] to bridge
 * connection lifecycle events to the foreground service via [TypingServiceLauncher].
 */
class BluetoothRepositoryImpl(
    context: Context,
    private val permissionManager: PermissionManager,
    private val serviceLauncher: TypingServiceLauncher
) : BluetoothRepository, BluetoothClassicConnector.ConnectionEventListener {

    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val adapterMonitor = BluetoothAdapterStateMonitor(context, adapter)
    private val scanner = BluetoothScanner(context, adapter)
    private val connector = BluetoothClassicConnector(
        context = context,
        adapter = adapter,
        scope = scope,
        connectionEventListener = this
    )

    // ── ConnectionEventListener ─────────────────────────────────────

    override fun onConnected(deviceName: String, deviceAddress: String) {
        serviceLauncher.startConnection(deviceName)
    }

    override fun onDisconnected() {
        serviceLauncher.stopConnection()
    }

    // ── State observation ────────────────────────────────────────────

    override fun observeAdapterState(): Flow<BluetoothAdapterState> =
        adapterMonitor.adapterState

    override fun observeConnectionState(): Flow<ConnectionState> =
        connector.connectionState

    override fun observeScannedDevices(): Flow<List<ScannedDevice>> =
        scanner.scannedDevices

    override fun observeIsDiscovering(): Flow<Boolean> =
        scanner.isDiscovering

    // ── Actions ──────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override suspend fun startDiscovery(): Result<Unit> {
        // Pre-condition: permissions
        if (permissionManager.getMissingBluetoothPermissions().isNotEmpty()) {
            return Result.failure(
                BluetoothOperationException(BluetoothError.PermissionMissing)
            )
        }

        // Pre-condition: adapter enabled
        if (adapterMonitor.adapterState.value != BluetoothAdapterState.Enabled) {
            return Result.failure(
                BluetoothOperationException(BluetoothError.BluetoothDisabled)
            )
        }

        return try {
            scanner.startDiscovery()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(BluetoothOperationException(BluetoothError.PermissionMissing))
        } catch (e: IllegalStateException) {
            Result.failure(BluetoothOperationException(BluetoothError.DiscoveryFailed))
        }
    }

    override suspend fun stopDiscovery() {
        scanner.stopDiscovery()
    }

    override suspend fun connect(address: String): Result<Unit> {
        // Pre-condition: permissions
        if (permissionManager.getMissingBluetoothPermissions().isNotEmpty()) {
            return Result.failure(
                BluetoothOperationException(BluetoothError.PermissionMissing)
            )
        }

        // Pre-condition: adapter enabled
        if (adapterMonitor.adapterState.value != BluetoothAdapterState.Enabled) {
            return Result.failure(
                BluetoothOperationException(BluetoothError.BluetoothDisabled)
            )
        }

        // Stop discovery before connecting (Android best practice)
        scanner.stopDiscovery()

        return connector.connect(address)
    }

    override suspend fun disconnect() {
        connector.disconnect()
    }

    // ── HID Key Operations ──────────────────────────────────────────

    override suspend fun sendKeyDown(char: Char): Result<Boolean> {
        val stroke = HidKeyMapper.mapChar(char) ?: return Result.failure(
            BluetoothOperationException(BluetoothError.UnsupportedCharacter(char))
        )
        return connector.sendKeyDown(stroke)
    }

    override suspend fun sendKeyUp(): Result<Boolean> {
        return connector.sendKeyUp()
    }

    override suspend fun releaseAllKeys(): Result<Boolean> {
        return connector.releaseAllKeys()
    }

    override fun isConnected(): Boolean {
        return connector.isConnected()
    }

    // ── Cleanup ──────────────────────────────────────────────────────

    override fun release() {
        adapterMonitor.release()
        scanner.release()
        connector.release()
    }
}
