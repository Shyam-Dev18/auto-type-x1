package com.shyam.autotypex1.data.bluetooth

/**
 * Maps characters to USB HID keyboard key strokes.
 * Ported verbatim from the old codebase — this mapping is proven correct
 * across Windows/Linux/Mac hosts.
 */
object HidKeyMapper {
    private const val MOD_SHIFT: Byte = 0x02

    private val digitShift = mapOf(
        ')' to '0',
        '!' to '1',
        '@' to '2',
        '#' to '3',
        '$' to '4',
        '%' to '5',
        '^' to '6',
        '&' to '7',
        '*' to '8',
        '(' to '9'
    )

    private val symbolMap = mapOf(
        '-' to HidKeyStroke(0x2D),
        '_' to HidKeyStroke(0x2D, MOD_SHIFT),
        '=' to HidKeyStroke(0x2E),
        '+' to HidKeyStroke(0x2E, MOD_SHIFT),
        '[' to HidKeyStroke(0x2F),
        '{' to HidKeyStroke(0x2F, MOD_SHIFT),
        ']' to HidKeyStroke(0x30),
        '}' to HidKeyStroke(0x30, MOD_SHIFT),
        '\\' to HidKeyStroke(0x31),
        '|' to HidKeyStroke(0x31, MOD_SHIFT),
        ';' to HidKeyStroke(0x33),
        ':' to HidKeyStroke(0x33, MOD_SHIFT),
        '\'' to HidKeyStroke(0x34),
        '"' to HidKeyStroke(0x34, MOD_SHIFT),
        '`' to HidKeyStroke(0x35),
        '~' to HidKeyStroke(0x35, MOD_SHIFT),
        ',' to HidKeyStroke(0x36),
        '<' to HidKeyStroke(0x36, MOD_SHIFT),
        '.' to HidKeyStroke(0x37),
        '>' to HidKeyStroke(0x37, MOD_SHIFT),
        '/' to HidKeyStroke(0x38),
        '?' to HidKeyStroke(0x38, MOD_SHIFT),
        ' ' to HidKeyStroke(0x2C),
        '\n' to HidKeyStroke(0x28),
        '\r' to HidKeyStroke(0x28),
        '\t' to HidKeyStroke(0x2B),
        '\b' to HidKeyStroke(0x2A),
        '\u001B' to HidKeyStroke(0x29),
        '\u007F' to HidKeyStroke(0x4C)
    )

    fun mapChar(char: Char): HidKeyStroke? {
        if (char in 'a'..'z') {
            return HidKeyStroke((0x04 + (char - 'a')).toByte())
        }

        if (char in 'A'..'Z') {
            return HidKeyStroke((0x04 + (char.lowercaseChar() - 'a')).toByte(), MOD_SHIFT)
        }

        if (char in '0'..'9') {
            val code = if (char == '0') 0x27 else (0x1E + (char - '1'))
            return HidKeyStroke(code.toByte())
        }

        digitShift[char]?.let { baseDigit ->
            val code = if (baseDigit == '0') 0x27 else (0x1E + (baseDigit - '1'))
            return HidKeyStroke(code.toByte(), MOD_SHIFT)
        }

        return symbolMap[char]
    }
}
