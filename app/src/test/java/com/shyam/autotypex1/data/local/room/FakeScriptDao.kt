package com.shyam.autotypex1.data.local.room

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakeScriptDao : ScriptDao {

    private val scriptsFlow = MutableStateFlow<Map<Long, ScriptEntity>>(emptyMap())
    private val nextId = AtomicLong(1L)

    override suspend fun insert(script: ScriptEntity): Long {
        val id = if (script.id == 0L) nextId.getAndIncrement() else script.id
        val entity = script.copy(id = id)
        val current = scriptsFlow.value.toMutableMap()
        current[id] = entity
        scriptsFlow.value = current
        return id
    }

    override suspend fun update(script: ScriptEntity) {
        val current = scriptsFlow.value.toMutableMap()
        if (current.containsKey(script.id)) {
            current[script.id] = script
            scriptsFlow.value = current
        }
    }

    override suspend fun deleteById(id: Long) {
        val current = scriptsFlow.value.toMutableMap()
        current.remove(id)
        scriptsFlow.value = current
    }

    override fun observeById(id: Long): Flow<ScriptEntity?> =
        scriptsFlow.map { it[id] }

    override suspend fun getById(id: Long): ScriptEntity? =
        scriptsFlow.value[id]

    override fun getAll(): Flow<List<ScriptEntity>> =
        scriptsFlow.map { map ->
            map.values.sortedByDescending { it.updatedAt }
        }

    override fun getSelected(): Flow<ScriptEntity?> =
        scriptsFlow.map { map ->
            map.values.firstOrNull { it.isSelected }
        }

    override suspend fun clearSelection() {
        val current = scriptsFlow.value.toMutableMap()
        for ((id, script) in current) {
            if (script.isSelected) {
                current[id] = script.copy(isSelected = false)
            }
        }
        scriptsFlow.value = current
    }

    override suspend fun markSelected(id: Long) {
        val current = scriptsFlow.value.toMutableMap()
        val script = current[id]
        if (script != null) {
            current[id] = script.copy(isSelected = true)
            scriptsFlow.value = current
        }
    }
}
