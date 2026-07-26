package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.SettingsDataStore
import com.shyam.autotypex1.domain.model.AppSettings
import com.shyam.autotypex1.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.observeSettings()

    override suspend fun updateSettings(settings: AppSettings): Result<Unit> =
        runCatching { dataStore.saveSettings(settings) }
}
