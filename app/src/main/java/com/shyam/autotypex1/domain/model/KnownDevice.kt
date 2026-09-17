package com.shyam.autotypex1.domain.model

/**
 * Domain model representing a previously connected / paired Bluetooth HID device.
 *
 * @property address MAC address / unique Bluetooth hardware address.
 * @property name Human-readable device name advertised by the target host.
 * @property lastConnectedAt Epoch timestamp (ms) when last successfully connected.
 */
data class KnownDevice(
    val address: String,
    val name: String,
    val lastConnectedAt: Long = System.currentTimeMillis()
)
