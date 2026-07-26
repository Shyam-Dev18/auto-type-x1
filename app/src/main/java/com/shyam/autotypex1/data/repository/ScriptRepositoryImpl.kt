package com.shyam.autotypex1.data.repository

import com.shyam.autotypex1.data.local.ScriptDao
import com.shyam.autotypex1.data.local.ScriptEntity
import com.shyam.autotypex1.domain.model.Script
import com.shyam.autotypex1.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepositoryImpl @Inject constructor(
    private val scriptDao: ScriptDao
) : ScriptRepository {

    override fun observeAll(): Flow<List<Script>> =
        scriptDao.getAll().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entity.toDomain()
                } catch (_: Exception) {
                    // Corrupt-row handling: skip malformed rows, don't crash (§8)
                    null
                }
            }
        }

    override fun observeById(id: Long): Flow<Script?> =
        scriptDao.observeById(id).map { it?.toDomain() }

    override fun observeSelected(): Flow<Script?> =
        scriptDao.getSelected().map { it?.toDomain() }

    override suspend fun getById(id: Long): Script? =
        try {
            scriptDao.getById(id)?.toDomain()
        } catch (_: Exception) {
            null
        }

    override suspend fun save(name: String, content: String, existingId: Long?): Result<Long> =
        runCatching {
            val now = System.currentTimeMillis()
            if (existingId != null && existingId > 0) {
                scriptDao.update(existingId, name.trim(), content, now)
                existingId
            } else {
                scriptDao.insert(
                    ScriptEntity(
                        name = name.trim(),
                        content = content,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }

    override suspend fun delete(id: Long): Result<Unit> =
        runCatching { scriptDao.deleteById(id) }

    override suspend fun selectScript(id: Long): Result<Unit> =
        runCatching { scriptDao.selectScript(id) }

    private fun ScriptEntity.toDomain() = Script(
        id = id,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSelected = isSelected
    )
}
