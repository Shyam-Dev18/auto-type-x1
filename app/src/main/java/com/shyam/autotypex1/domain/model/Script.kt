package com.shyam.autotypex1.domain.model

/**
 * Domain model representing a text script to be typed.
 *
 * @property id Unique database ID (0 for unsaved/new scripts).
 * @property name Title or label for the script.
 * @property content The raw text content to be typed via HID.
 * @property createdAt Epoch timestamp (ms) when created.
 * @property updatedAt Epoch timestamp (ms) when last modified.
 * @property isSelected Whether this script is currently selected as active.
 */
data class Script(
    val id: Long = 0L,
    val name: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSelected: Boolean = false
) {
    val characterCount: Int
        get() = content.length

    val lineCount: Int
        get() = if (content.isEmpty()) 0 else content.lines().size
}
