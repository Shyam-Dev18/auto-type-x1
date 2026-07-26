package com.shyam.autotypex1.domain.model

data class Script(
    val id: Long,
    val name: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isSelected: Boolean = false
)
