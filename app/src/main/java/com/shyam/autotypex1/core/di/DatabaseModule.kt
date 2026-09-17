package com.shyam.autotypex1.core.di

import android.content.Context
import androidx.room.Room
import com.shyam.autotypex1.data.local.room.AppDatabase
import com.shyam.autotypex1.data.local.room.ScriptDao
import com.shyam.autotypex1.data.repository.ScriptRepositoryImpl
import com.shyam.autotypex1.domain.repository.ScriptRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME
    ).build()

    @Provides
    @Singleton
    fun provideScriptDao(database: AppDatabase): ScriptDao =
        database.scriptDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScriptRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindScriptRepository(
        impl: ScriptRepositoryImpl
    ): ScriptRepository
}
