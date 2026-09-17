package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.room.ScriptDao
import com.shyam.autotypex1.data.local.room.ScriptEntity
import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ScriptRepository] backed by Room database.
 */
@Singleton
class ScriptRepositoryImpl @Inject constructor(
    private val scriptDao: ScriptDao
) : ScriptRepository {

    override fun getAllScripts(): Flow<List<Script>> =
        scriptDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getSelectedScript(): Flow<Script?> =
        scriptDao.getSelected().map { it?.toDomain() }

    override fun observeScriptById(id: Long): Flow<Script?> =
        scriptDao.observeById(id).map { it?.toDomain() }

    override suspend fun getScriptById(id: Long): Script? =
        scriptDao.getById(id)?.toDomain()

    override suspend fun insertScript(script: Script): Long =
        scriptDao.insert(ScriptEntity.fromDomain(script))

    override suspend fun updateScript(script: Script) =
        scriptDao.update(ScriptEntity.fromDomain(script))

    override suspend fun deleteScript(id: Long) =
        scriptDao.deleteById(id)

    override suspend fun selectScript(id: Long) =
        scriptDao.selectScript(id)

    override suspend fun clearSelection() =
        scriptDao.clearSelection()
}
