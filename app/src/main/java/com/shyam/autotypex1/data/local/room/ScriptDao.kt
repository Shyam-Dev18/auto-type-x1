package com.shyam.autotypex1.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room scripts database.
 */
@Dao
interface ScriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(script: ScriptEntity): Long

    @Update
    suspend fun update(script: ScriptEntity)

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
     * Annotated with @Transaction so partial state transitions are impossible.
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
