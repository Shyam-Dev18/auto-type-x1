package com.shyam.autotypex1.presentation.adddevice

import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice

data class AddDeviceUiState(
    val devices: List<ScannedDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val pendingAddress: String? = null,
    val error: String? = null
)

sealed interface AddDeviceUiEvent {
    data object OnStartScan : AddDeviceUiEvent
    data object OnStopScan : AddDeviceUiEvent
    data class OnConnect(val address: String) : AddDeviceUiEvent
    data object OnBack : AddDeviceUiEvent
    data object OnDismissError : AddDeviceUiEvent
}
