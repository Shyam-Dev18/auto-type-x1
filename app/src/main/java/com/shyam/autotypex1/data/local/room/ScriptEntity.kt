package com.shyam.autotypex1.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shyam.autotypex1.domain.model.Script

/**
 * Room database entity representing a script.
 */
@Entity(
    tableName = "scripts",
    indices = [Index(value = ["id"], unique = true)]
)
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSelected: Boolean = false
) {
    fun toDomain(): Script = Script(
        id = id,
        name = name,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSelected = isSelected
    )

    companion object {
        fun fromDomain(script: Script): ScriptEntity = ScriptEntity(
            id = script.id,
            name = script.name,
            content = script.content,
            createdAt = script.createdAt,
            updatedAt = script.updatedAt,
            isSelected = script.isSelected
        )
    }
}
