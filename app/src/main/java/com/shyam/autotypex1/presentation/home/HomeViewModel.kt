package com.shyam.autotypex1.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import com.shyam.autotypex1.domain.repository.DeviceRepository
import com.shyam.autotypex1.domain.repository.ScriptRepository
import com.shyam.autotypex1.domain.usecase.RequestEnableBluetoothUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val scriptRepository: ScriptRepository,
    private val deviceRepository: DeviceRepository,
    private val requestEnableBluetooth: RequestEnableBluetoothUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bluetoothRepository.observeAdapterState().collect { state ->
                _uiState.update { it.copy(bluetoothState = state) }
            }
        }
        viewModelScope.launch {
            bluetoothRepository.observeConnectionState().collect { state ->
                _uiState.update { current ->
                    val pending = if (state is ConnectionState.Connecting) current.pendingAddress else null
                    current.copy(connectionState = state, pendingAddress = pending)
                }
                if (state is ConnectionState.Connected) {
                    deviceRepository.saveDevice(
                        name = state.deviceName,
                        address = state.deviceAddress
                    )
                }
            }
        }
        viewModelScope.launch {
            scriptRepository.getSelectedScript().collect { script ->
                _uiState.update {
                    it.copy(
                        selectedScriptName = script?.name,
                        selectedScriptId = script?.id
                    )
                }
            }
        }
        viewModelScope.launch {
            deviceRepository.observeKnownDevices().collect { devices ->
                _uiState.update { it.copy(savedDevices = devices) }
            }
        }
        viewModelScope.launch {
            deviceRepository.observeLastConnectedAddress().collect { address ->
                _uiState.update { it.copy(lastConnectedAddress = address) }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnSavedDeviceClick -> connectToDevice(event.address)
            is HomeUiEvent.OnDeleteSavedDevice -> deleteDevice(event.address)
            is HomeUiEvent.OnDisconnectClick -> disconnect()
            is HomeUiEvent.OnReconnectClick -> reconnect()
            is HomeUiEvent.OnBluetoothIconClick -> Unit
            is HomeUiEvent.OnDismissError -> _uiState.update { it.copy(error = null) }
            else -> Unit
        }
    }

    private fun connectToDevice(address: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingAddress = address) }
            val result = bluetoothRepository.connect(address)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        error = result.exceptionOrNull()?.message ?: "Failed to connect to device",
                        pendingAddress = null
                    )
                }
            }
        }
    }

    private fun deleteDevice(address: String) {
        viewModelScope.launch {
            val currentConn = _uiState.value.connectionState
            if (currentConn is ConnectionState.Connected && currentConn.deviceAddress.equals(address, ignoreCase = true)) {
                bluetoothRepository.disconnect()
            }
            deviceRepository.removeDevice(address)
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            bluetoothRepository.disconnect()
        }
    }

    private fun reconnect() {
        val address = _uiState.value.lastConnectedAddress ?: return
        connectToDevice(address)
    }
}
