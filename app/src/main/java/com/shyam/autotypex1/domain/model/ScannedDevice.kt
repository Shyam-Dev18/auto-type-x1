package com.shyam.autotypex1.domain.model

/**
 * A Bluetooth device found during Classic discovery.
 *
 * @property name   Human-readable name reported by the device, or "Unknown Device".
 * @property address MAC address (XX:XX:XX:XX:XX:XX).
 * @property bonded  True if the device is already bonded (paired) with this phone.
 */
data class ScannedDevice(
    val name: String,
    val address: String,
    val bonded: Boolean
)
