package com.shyam.autotypex1.presentation.fakes

import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeScriptRepository : ScriptRepository {
    private val scriptsMap = MutableStateFlow<Map<Long, Script>>(emptyMap())
    private var nextId = 1L

    override fun getAllScripts(): Flow<List<Script>> =
        scriptsMap.map { it.values.sortedByDescending { s -> s.updatedAt } }

    override fun getSelectedScript(): Flow<Script?> =
        scriptsMap.map { it.values.firstOrNull { s -> s.isSelected } }

    override fun observeScriptById(id: Long): Flow<Script?> =
        scriptsMap.map { it[id] }

    override suspend fun getScriptById(id: Long): Script? =
        scriptsMap.value[id]

    override suspend fun insertScript(script: Script): Long {
        val id = if (script.id == 0L) nextId++ else script.id
        val newScript = script.copy(id = id)
        scriptsMap.value = scriptsMap.value + (id to newScript)
        return id
    }

    override suspend fun updateScript(script: Script) {
        if (scriptsMap.value.containsKey(script.id)) {
            scriptsMap.value = scriptsMap.value + (script.id to script)
        }
    }

    override suspend fun deleteScript(id: Long) {
        scriptsMap.value = scriptsMap.value - id
    }

    override suspend fun selectScript(id: Long) {
        val current = scriptsMap.value.toMutableMap()
        for ((k, v) in current) {
            current[k] = v.copy(isSelected = (k == id))
        }
        scriptsMap.value = current
    }

    override suspend fun clearSelection() {
        val current = scriptsMap.value.toMutableMap()
        for ((k, v) in current) {
            current[k] = v.copy(isSelected = false)
        }
        scriptsMap.value = current
    }
}
