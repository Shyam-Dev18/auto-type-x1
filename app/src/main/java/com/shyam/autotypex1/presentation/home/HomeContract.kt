package com.shyam.autotypex1.presentation.home

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.KnownDevice

data class HomeUiState(
    val bluetoothState: BluetoothAdapterState = BluetoothAdapterState.Unavailable,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val selectedScriptName: String? = null,
    val selectedScriptId: Long? = null,
    val savedDevices: List<KnownDevice> = emptyList(),
    val lastConnectedAddress: String? = null,
    val pendingAddress: String? = null,
    val error: String? = null
)

sealed interface HomeUiEvent {
    data object OnScriptsClick : HomeUiEvent
    data object OnSettingsClick : HomeUiEvent
    data object OnTypingClick : HomeUiEvent
    data object OnAddDeviceClick : HomeUiEvent
    data object OnReconnectClick : HomeUiEvent
    data object OnBluetoothIconClick : HomeUiEvent
    data object OnAboutClick : HomeUiEvent
    data class OnSavedDeviceClick(val address: String) : HomeUiEvent
    data class OnDeleteSavedDevice(val address: String) : HomeUiEvent
    data object OnDisconnectClick : HomeUiEvent
    data object OnDismissError : HomeUiEvent
}
