package com.shyam.autotypex1.core.di

import com.shyam.autotypex1.data.repository.DeviceRepositoryImpl
import com.shyam.autotypex1.data.repository.ScriptRepositoryImpl
import com.shyam.autotypex1.data.repository.SettingsRepositoryImpl
import com.shyam.autotypex1.domain.repository.DeviceRepository
import com.shyam.autotypex1.domain.repository.ScriptRepository
import com.shyam.autotypex1.domain.repository.SettingsRepository
import com.shyam.autotypex1.domain.typing.DefaultHumanTypingEngine
import com.shyam.autotypex1.domain.typing.HumanTypingEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScriptRepository(impl: ScriptRepositoryImpl): ScriptRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides
        @Singleton
        fun provideHumanTypingEngine(): HumanTypingEngine = DefaultHumanTypingEngine()
    }
}
