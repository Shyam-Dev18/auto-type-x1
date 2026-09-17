package com.shyam.autotypex1.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for AutoType X1 persistence.
 */
@Database(
    entities = [ScriptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao

    companion object {
        const val DATABASE_NAME = "autotypex1_database"
    }
}
