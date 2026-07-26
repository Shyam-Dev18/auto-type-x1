package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** App settings — domain interface, implemented via DataStore in data layer. */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings): Result<Unit>
}
