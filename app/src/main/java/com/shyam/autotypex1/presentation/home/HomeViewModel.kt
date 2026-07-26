package com.shyam.autotypex1.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shyam.autotypex1.core.service.TypingForegroundService
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.usecase.ConnectDeviceUseCase
import com.shyam.autotypex1.domain.usecase.DisconnectDeviceUseCase
import com.shyam.autotypex1.domain.usecase.ObserveBluetoothStateUseCase
import com.shyam.autotypex1.domain.usecase.ObserveConnectionStateUseCase
import com.shyam.autotypex1.domain.usecase.ObserveKnownDevicesUseCase
import com.shyam.autotypex1.domain.usecase.ObserveLastConnectedAddressUseCase
import com.shyam.autotypex1.domain.usecase.ObserveSelectedScriptUseCase
import com.shyam.autotypex1.domain.usecase.ReconnectLastDeviceUseCase
import com.shyam.autotypex1.domain.usecase.RemoveKnownDeviceUseCase
import com.shyam.autotypex1.domain.usecase.RequestEnableBluetoothUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val app: Application,
    private val observeBluetoothState: ObserveBluetoothStateUseCase,
    private val observeConnectionState: ObserveConnectionStateUseCase,
    private val observeSelectedScript: ObserveSelectedScriptUseCase,
    private val observeKnownDevices: ObserveKnownDevicesUseCase,
    private val observeLastConnected: ObserveLastConnectedAddressUseCase,
    private val connectDevice: ConnectDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
    private val reconnectLast: ReconnectLastDeviceUseCase,
    private val removeKnownDevice: RemoveKnownDeviceUseCase,
    private val requestEnableBluetooth: RequestEnableBluetoothUseCase
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeBluetoothState().collect { state ->
                _uiState.update { it.copy(bluetoothState = state) }
            }
        }
        viewModelScope.launch {
            observeConnectionState().collect { state ->
                _uiState.update { it.copy(connectionState = state, pendingAddress = null) }
                // Start foreground service on connect
                if (state is ConnectionState.Connected) {
                    TypingForegroundService.startConnection(app)
                }
            }
        }
        viewModelScope.launch {
            observeSelectedScript().collect { script ->
                _uiState.update { it.copy(selectedScriptName = script?.name) }
            }
        }
        viewModelScope.launch {
            observeKnownDevices().collect { devices ->
                _uiState.update { it.copy(savedDevices = devices) }
            }
        }
        viewModelScope.launch {
            observeLastConnected().collect { address ->
                _uiState.update { it.copy(lastConnectedAddress = address) }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnSavedDeviceClick -> connectToDevice(event.address)
            is HomeUiEvent.OnDeleteSavedDevice -> deleteDevice(event.address)
            is HomeUiEvent.OnDisconnectClick -> disconnect()
            is HomeUiEvent.OnAddDeviceClick -> disconnect()
            is HomeUiEvent.OnReconnectClick -> reconnect()
            is HomeUiEvent.OnBluetoothIconClick -> requestEnableBluetooth()
            is HomeUiEvent.OnDismissError -> _uiState.update { it.copy(error = null) }
            // Navigation events handled by the NavGraph
            else -> Unit
        }
    }

    private fun connectToDevice(address: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingAddress = address) }
            connectDevice(address).onFailure { e ->
                _uiState.update { it.copy(error = e.message, pendingAddress = null) }
            }
        }
    }

    private fun deleteDevice(address: String) {
        _uiState.update { 
            it.copy(savedDevices = it.savedDevices.filterNot { d -> d.address == address }) 
        }
        viewModelScope.launch {
            removeKnownDevice(address)
        }
    }

    private fun disconnect() {
        viewModelScope.launch {
            disconnectDevice()
        }
    }

    private fun reconnect() {
        viewModelScope.launch {
            reconnectLast().onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
