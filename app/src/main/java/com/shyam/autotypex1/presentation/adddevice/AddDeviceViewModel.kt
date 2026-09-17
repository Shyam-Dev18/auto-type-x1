package com.shyam.autotypex1.presentation.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import com.shyam.autotypex1.domain.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bluetoothRepository.observeScannedDevices().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
        viewModelScope.launch {
            bluetoothRepository.observeIsDiscovering().collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
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

        // Auto-start discovery when entering screen
        onEvent(AddDeviceUiEvent.OnStartScan)
    }

    fun onEvent(event: AddDeviceUiEvent) {
        when (event) {
            is AddDeviceUiEvent.OnStartScan -> {
                viewModelScope.launch {
                    val result = bluetoothRepository.startDiscovery()
                    if (result.isFailure) {
                        _uiState.update {
                            it.copy(error = result.exceptionOrNull()?.message ?: "Failed to start scan")
                        }
                    }
                }
            }
            is AddDeviceUiEvent.OnStopScan -> {
                viewModelScope.launch {
                    bluetoothRepository.stopDiscovery()
                }
            }
            is AddDeviceUiEvent.OnConnect -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(pendingAddress = event.address) }
                    bluetoothRepository.stopDiscovery()
                    val result = bluetoothRepository.connect(event.address)
                    if (result.isFailure) {
                        _uiState.update {
                            it.copy(
                                error = result.exceptionOrNull()?.message ?: "Connection failed",
                                pendingAddress = null
                            )
                        }
                    }
                }
            }
            is AddDeviceUiEvent.OnBack -> {
                viewModelScope.launch {
                    bluetoothRepository.stopDiscovery()
                }
            }
            is AddDeviceUiEvent.OnDismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            bluetoothRepository.stopDiscovery()
        }
    }
}
