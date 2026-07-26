package com.shyam.autotypex1.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: ScriptEntity): Long

    @Query("UPDATE scripts SET name = :name, content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun update(id: Long, name: String, content: String, updatedAt: Long)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ScriptEntity?>

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScriptEntity?

    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE isSelected = 1 LIMIT 1")
    fun getSelected(): Flow<ScriptEntity?>

    /**
     * Atomically selects a script: clears any previous selection and sets the new one.
     * Single @Transaction method so partial writes are structurally impossible.
     */
    @Transaction
    suspend fun selectScript(id: Long) {
        clearSelection()
        markSelected(id)
    }

    @Query("UPDATE scripts SET isSelected = 0 WHERE isSelected = 1")
    suspend fun clearSelection()

    @Query("UPDATE scripts SET isSelected = 1 WHERE id = :id")
    suspend fun markSelected(id: Long)
}
