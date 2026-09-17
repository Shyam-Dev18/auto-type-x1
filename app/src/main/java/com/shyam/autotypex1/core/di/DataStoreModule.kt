package com.shyam.autotypex1.core.di

import com.shyam.autotypex1.data.repository.DeviceRepositoryImpl
import com.shyam.autotypex1.data.repository.SettingsRepositoryImpl
import com.shyam.autotypex1.domain.repository.DeviceRepository
import com.shyam.autotypex1.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        impl: DeviceRepositoryImpl
    ): DeviceRepository
}
