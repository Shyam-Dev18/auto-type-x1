package com.shyam.autotypex1.domain.typing

/**
 * Validates scripts for US QWERTY compatibility before typing execution begins.
 *
 * Discovers any unsupported characters up-front, reports them clearly to the user,
 * and produces a sanitized script with unsupported characters omitted.
 */
object CharacterValidator {

    private val supportedSymbols: Set<Char> = setOf(
        '-', '_', '=', '+', '[', '{', ']', '}', '\\', '|',
        ';', ':', '\'', '"', '`', '~', ',', '<', '.', '>',
        '/', '?', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')'
    )

    private val supportedControls: Set<Char> = setOf(
        ' ', '\n', '\r', '\t', '\b', '\u001B', '\u007F'
    )

    private val shiftedSymbols: Set<Char> = setOf(
        '_', '+', '{', '}', '|', ':', '"', '~', '<', '>', '?',
        '!', '@', '#', '$', '%', '^', '&', '*', '(', ')'
    )

    /**
     * Checks if a character is supported by the US QWERTY HID layout.
     */
    fun isSupported(char: Char): Boolean {
        return char in 'a'..'z' ||
            char in 'A'..'Z' ||
            char in '0'..'9' ||
            char in supportedSymbols ||
            char in supportedControls
    }

    /**
     * Checks if typing this character requires holding the SHIFT modifier key.
     */
    fun isShifted(char: Char): Boolean {
        return char in 'A'..'Z' || char in shiftedSymbols
    }

    /**
     * Validates the entire [script] up-front.
     *
     * @return [ValidationResult] containing supported characters, any unsupported characters found,
     *         and a formatted user warning message if unsupported characters were detected.
     */
    fun validate(script: String): ValidationResult {
        if (script.isEmpty()) {
            return ValidationResult(
                isValid = true,
                sanitizedScript = "",
                unsupportedChars = emptySet(),
                userMessage = null
            )
        }

        val unsupported = mutableSetOf<Char>()
        val sanitized = StringBuilder(script.length)

        for (char in script) {
            if (isSupported(char)) {
                sanitized.append(char)
            } else {
                unsupported.add(char)
            }
        }

        val message = if (unsupported.isNotEmpty()) {
            val charListStr = unsupported.joinToString(", ") { "'$it'" }
            "Unsupported character(s): $charListStr — These characters will be omitted while typing."
        } else {
            null
        }

        return ValidationResult(
            isValid = unsupported.isEmpty(),
            sanitizedScript = sanitized.toString(),
            unsupportedChars = unsupported,
            userMessage = message
        )
    }
}

/**
 * Result of script validation.
 *
 * @property isValid          True if all characters in the script are supported.
 * @property sanitizedScript  The script with all unsupported characters omitted.
 * @property unsupportedChars Set of unique unsupported characters found in the script.
 * @property userMessage      User-facing explanation when unsupported characters are found.
 */
data class ValidationResult(
    val isValid: Boolean,
    val sanitizedScript: String,
    val unsupportedChars: Set<Char>,
    val userMessage: String?
)
