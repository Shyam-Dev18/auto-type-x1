package com.shyam.autotypex1.domain.usecase

import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.domain.model.ScannedDevice
import com.shyam.autotypex1.domain.repository.HidConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    operator fun invoke(): Flow<ConnectionState> = hidRepo.observeConnectionState()
}

class ObserveBluetoothStateUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    operator fun invoke(): Flow<BluetoothAdapterState> = hidRepo.observeAdapterState()
}

class ObserveScannedDevicesUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    operator fun invoke(): Flow<List<ScannedDevice>> = hidRepo.observeScannedDevices()
}

class ObserveIsScanningUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    operator fun invoke(): Flow<Boolean> = hidRepo.observeIsScanning()
}

class StartScanUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    suspend operator fun invoke(): Result<Unit> = hidRepo.startScan()
}

class StopScanUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    suspend operator fun invoke(): Result<Unit> = hidRepo.stopScan()
}

class ConnectDeviceUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> = hidRepo.connect(address)
}

class DisconnectDeviceUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    suspend operator fun invoke(): Result<Unit> = hidRepo.disconnect()
}

class ReconnectLastDeviceUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    suspend operator fun invoke(): Result<Unit> = hidRepo.reconnectLast()
}

class OpenBluetoothSettingsUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    operator fun invoke() = hidRepo.openBluetoothSettings()
}

class RequestEnableBluetoothUseCase @Inject constructor(
    private val hidRepo: HidConnectionRepository
) {
    operator fun invoke() = hidRepo.requestEnableBluetooth()
}
