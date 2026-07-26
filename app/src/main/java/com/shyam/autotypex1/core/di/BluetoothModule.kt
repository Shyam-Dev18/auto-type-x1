package com.shyam.autotypex1.core.di

import android.content.Context
import com.shyam.autotypex1.data.bluetooth.BluetoothHidRepositoryImpl
import com.shyam.autotypex1.data.local.KnownDevicesDataStore
import com.shyam.autotypex1.domain.repository.HidConnectionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideHidConnectionRepository(
        @ApplicationContext context: Context,
        knownDevicesDataStore: KnownDevicesDataStore
    ): HidConnectionRepository = BluetoothHidRepositoryImpl(context, knownDevicesDataStore)
}
