package com.shyam.autotypex1.core.di

import android.content.Context
import com.shyam.autotypex1.core.permissions.PermissionManager
import com.shyam.autotypex1.core.service.AndroidTypingServiceLauncher
import com.shyam.autotypex1.data.repository.BluetoothRepositoryImpl
import com.shyam.autotypex1.domain.repository.BluetoothRepository
import com.shyam.autotypex1.domain.typing.HumanTypingEngine
import com.shyam.autotypex1.domain.typing.DefaultHumanTypingEngine
import com.shyam.autotypex1.domain.typing.TypingController
import com.shyam.autotypex1.domain.typing.TypingServiceLauncher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Bluetooth and typing dependencies.
 *
 * Dependency order:
 * 1. [TypingServiceLauncher] (depends only on Context)
 * 2. [BluetoothRepository] (depends on Context, PermissionManager, TypingServiceLauncher)
 * 3. [HumanTypingEngine] (no dependencies)
 * 4. [TypingController] (depends on HumanTypingEngine, BluetoothRepository)
 */
@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideTypingServiceLauncher(
        launcher: AndroidTypingServiceLauncher
    ): TypingServiceLauncher = launcher

    @Provides
    @Singleton
    fun provideBluetoothRepository(
        @ApplicationContext context: Context,
        permissionManager: PermissionManager,
        serviceLauncher: TypingServiceLauncher
    ): BluetoothRepository = BluetoothRepositoryImpl(context, permissionManager, serviceLauncher)

    @Provides
    @Singleton
    fun provideHumanTypingEngine(): HumanTypingEngine =
        DefaultHumanTypingEngine()

    @Provides
    @Singleton
    fun provideTypingController(
        engine: HumanTypingEngine,
        bluetoothRepository: BluetoothRepository
    ): TypingController =
        TypingController(engine, bluetoothRepository)
}
