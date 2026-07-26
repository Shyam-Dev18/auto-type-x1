package com.shyam.autotypex1.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScriptEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
}
