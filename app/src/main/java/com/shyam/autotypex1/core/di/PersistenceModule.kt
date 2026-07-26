package com.shyam.autotypex1.core.di

import android.content.Context
import androidx.room.Room
import com.shyam.autotypex1.data.local.AppDatabase
import com.shyam.autotypex1.data.local.KnownDevicesDataStore
import com.shyam.autotypex1.data.local.ScriptDao
import com.shyam.autotypex1.data.local.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "autotype_scripts.db"
        ).build() // No fallbackToDestructiveMigration — real migrations required

    @Provides
    fun provideScriptDao(database: AppDatabase): ScriptDao = database.scriptDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore =
        SettingsDataStore(context)

    @Provides
    @Singleton
    fun provideKnownDevicesDataStore(@ApplicationContext context: Context): KnownDevicesDataStore =
        KnownDevicesDataStore(context)
}
