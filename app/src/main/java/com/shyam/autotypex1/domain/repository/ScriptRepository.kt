package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.Script
import kotlinx.coroutines.flow.Flow

/** Script persistence — domain interface, implemented in data layer. */
interface ScriptRepository {
    fun observeAll(): Flow<List<Script>>
    fun observeById(id: Long): Flow<Script?>
    fun observeSelected(): Flow<Script?>
    suspend fun getById(id: Long): Script?
    suspend fun save(name: String, content: String, existingId: Long? = null): Result<Long>
    suspend fun delete(id: Long): Result<Unit>
    suspend fun selectScript(id: Long): Result<Unit>
}
