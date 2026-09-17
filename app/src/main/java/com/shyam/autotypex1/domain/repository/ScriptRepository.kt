package com.shyam.autotypex1.domain.repository

import com.shyam.autotypex1.domain.model.Script
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing text scripts.
 */
interface ScriptRepository {
    /**
     * Observes all scripts ordered by last updated timestamp descending.
     */
    fun getAllScripts(): Flow<List<Script>>

    /**
     * Observes the currently selected active script, if any.
     */
    fun getSelectedScript(): Flow<Script?>

    /**
     * Observes a single script by its unique ID.
     */
    fun observeScriptById(id: Long): Flow<Script?>

    /**
     * Retrieves a single script by ID (one-shot).
     */
    suspend fun getScriptById(id: Long): Script?

    /**
     * Inserts a new script. Returns the generated ID.
     */
    suspend fun insertScript(script: Script): Long

    /**
     * Updates an existing script.
     */
    suspend fun updateScript(script: Script)

    /**
     * Deletes a script by ID.
     */
    suspend fun deleteScript(id: Long)

    /**
     * Atomically selects a script by ID, clearing any previous selection.
     */
    suspend fun selectScript(id: Long)

    /**
     * Clears any currently selected script.
     */
    suspend fun clearSelection()
}
