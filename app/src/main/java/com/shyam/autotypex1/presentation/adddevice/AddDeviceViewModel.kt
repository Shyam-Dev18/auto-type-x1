package com.shyam.autotypex1.presentation.adddevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.domain.usecase.ConnectDeviceUseCase
import com.shyam.autotypex1.domain.usecase.ObserveConnectionStateUseCase
import com.shyam.autotypex1.domain.usecase.ObserveIsScanningUseCase
import com.shyam.autotypex1.domain.usecase.ObserveScannedDevicesUseCase
import com.shyam.autotypex1.domain.usecase.StartScanUseCase
import com.shyam.autotypex1.domain.usecase.StopScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val observeScannedDevices: ObserveScannedDevicesUseCase,
    private val observeIsScanning: ObserveIsScanningUseCase,
    private val observeConnectionState: ObserveConnectionStateUseCase,
    private val startScan: StartScanUseCase,
    private val stopScan: StopScanUseCase,
    private val connectDevice: ConnectDeviceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeScannedDevices().collect { devices ->
                _uiState.update { it.copy(devices = devices) }
            }
        }
        viewModelScope.launch {
            observeIsScanning().collect { scanning ->
                _uiState.update { it.copy(isScanning = scanning) }
            }
        }
        viewModelScope.launch {
            observeConnectionState().collect { state ->
                _uiState.update { it.copy(connectionState = state, pendingAddress = null) }
            }
        }
    }

    fun onEvent(event: AddDeviceUiEvent) {
        when (event) {
            is AddDeviceUiEvent.OnStartScan -> viewModelScope.launch {
                startScan().onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            }
            is AddDeviceUiEvent.OnStopScan -> viewModelScope.launch {
                stopScan()
            }
            is AddDeviceUiEvent.OnConnect -> viewModelScope.launch {
                _uiState.update { it.copy(pendingAddress = event.address) }
                connectDevice(event.address).onFailure { e ->
                    _uiState.update { it.copy(error = e.message, pendingAddress = null) }
                }
            }
            is AddDeviceUiEvent.OnBack -> viewModelScope.launch { stopScan() }
        }
    }
}
