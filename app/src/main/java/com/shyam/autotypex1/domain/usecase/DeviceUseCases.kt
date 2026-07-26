package com.shyam.autotypex1.domain.usecase

import com.shyam.autotypex1.domain.model.KnownDevice
import com.shyam.autotypex1.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveKnownDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(): Flow<List<KnownDevice>> = deviceRepository.observeKnownDevices()
}

class ObserveLastConnectedAddressUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    operator fun invoke(): Flow<String?> = deviceRepository.observeLastConnectedAddress()
}

class RemoveKnownDeviceUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(address: String): Result<Unit> = deviceRepository.removeDevice(address)
}
