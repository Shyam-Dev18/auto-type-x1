package com.shyam.autotypex1.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scripts",
    indices = [Index(value = ["id"], unique = true)]
)
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSelected: Boolean = false
)
