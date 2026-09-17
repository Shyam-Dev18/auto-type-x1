package com.shyam.autotypex1.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages HID Device profile app registration, input report generation, and safe cleanup.
 *
 * Responsibilities:
 * - Registers the SDP service record and QoS settings with the Android Bluetooth HID Device service.
 * - Generates 8-byte standard USB HID keyboard input reports.
 * - Provides discrete [sendKeyDown], [sendKeyUp], and [releaseAllKeys] methods.
 * - Protects against stuck keys by guaranteeing full-zero release reports on disconnect/cleanup.
 */
class HidDeviceManager {

    /** Whether the HID app is currently registered with the Bluetooth stack. */
    val isAppRegistered = AtomicBoolean(false)

    /**
     * Registers this application as a Bluetooth HID keyboard device.
     */
    @SuppressLint("MissingPermission")
    fun registerApp(
        hidDevice: BluetoothHidDevice,
        executor: Executor,
        callback: BluetoothHidDevice.Callback
    ): Boolean {
        return try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                KeyboardHidDescriptor.SDP_NAME,
                KeyboardHidDescriptor.SDP_DESCRIPTION,
                KeyboardHidDescriptor.SDP_PROVIDER,
                KeyboardHidDescriptor.SUBCLASS,
                KeyboardHidDescriptor.DESCRIPTOR
            )

            val qos = BluetoothHidDeviceAppQosSettings(
                KeyboardHidDescriptor.QOS_SERVICE_TYPE,
                KeyboardHidDescriptor.QOS_TOKEN_RATE,
                KeyboardHidDescriptor.QOS_TOKEN_BUCKET_SIZE,
                KeyboardHidDescriptor.QOS_PEAK_BANDWIDTH,
                KeyboardHidDescriptor.QOS_LATENCY,
                KeyboardHidDescriptor.QOS_DELAY_VARIATION
            )

            hidDevice.registerApp(sdp, null, qos, executor, callback)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Unregisters the HID application from the Bluetooth stack.
     */
    @SuppressLint("MissingPermission")
    fun unregisterApp(hidDevice: BluetoothHidDevice?) {
        if (hidDevice == null) return
        try {
            if (isAppRegistered.compareAndSet(true, false)) {
                hidDevice.unregisterApp()
            }
        } catch (_: Exception) {
            // Best effort
        }
    }

    /**
     * Sends a key-down report for the specified [stroke] to [host].
     *
     * @return true if report was dispatched to the Bluetooth stack.
     */
    @SuppressLint("MissingPermission")
    fun sendKeyDown(
        hidDevice: BluetoothHidDevice,
        host: BluetoothDevice,
        stroke: HidKeyStroke
    ): Boolean {
        return try {
            val report = createKeyDownReport(stroke)
            hidDevice.sendReport(host, KeyboardHidDescriptor.REPORT_ID, report)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sends a key-up report (all zeros) to [host].
     *
     * @return true if report was dispatched to the Bluetooth stack.
     */
    @SuppressLint("MissingPermission")
    fun sendKeyUp(
        hidDevice: BluetoothHidDevice,
        host: BluetoothDevice
    ): Boolean {
        return try {
            val report = createKeyUpReport()
            hidDevice.sendReport(host, KeyboardHidDescriptor.REPORT_ID, report)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Safety net: sends a full-zero report to release any stuck keys.
     * Safe to call even if [hidDevice] or [host] is null.
     */
    @SuppressLint("MissingPermission")
    fun releaseAllKeys(
        hidDevice: BluetoothHidDevice?,
        host: BluetoothDevice?
    ): Boolean {
        if (hidDevice == null || host == null) return true
        return try {
            val report = createKeyUpReport()
            hidDevice.sendReport(host, KeyboardHidDescriptor.REPORT_ID, report)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Full cleanup: release all keys and unregister the HID app.
     */
    fun release(hidDevice: BluetoothHidDevice?, host: BluetoothDevice?) {
        releaseAllKeys(hidDevice, host)
        unregisterApp(hidDevice)
    }

    companion object {
        /**
         * Creates an 8-byte HID keyboard input report with the given [stroke] pressed.
         * Byte 0: modifier flags
         * Byte 1: reserved (0x00)
         * Byte 2: key code
         * Bytes 3-7: zeros (no other simultaneous keys)
         */
        fun createKeyDownReport(stroke: HidKeyStroke): ByteArray {
            val report = ByteArray(KeyboardHidDescriptor.REPORT_SIZE)
            report[0] = stroke.modifier
            report[1] = 0x00
            report[2] = stroke.keyCode
            return report
        }

        /**
         * Creates an 8-byte HID keyboard input report with all keys released (zeros).
         */
        fun createKeyUpReport(): ByteArray {
            return ByteArray(KeyboardHidDescriptor.REPORT_SIZE)
        }
    }
}
