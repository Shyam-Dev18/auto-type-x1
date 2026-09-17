package com.shyam.autotypex1.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory test fake for DataStore<Preferences>.
 */
class FakeDataStore(
    initialPreferences: Preferences = emptyPreferences()
) : DataStore<Preferences> {

    private val stateFlow = MutableStateFlow(initialPreferences)
    private val mutex = Mutex()

    override val data: Flow<Preferences> = stateFlow.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return mutex.withLock {
            val current = stateFlow.value
            val updated = transform(current.toMutablePreferences())
            stateFlow.value = updated
            updated
        }
    }
}
