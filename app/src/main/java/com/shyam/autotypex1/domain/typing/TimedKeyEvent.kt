package com.shyam.autotypex1.domain.typing

data class TimedKeyEvent(
    val delayMs: Long,
    val action: KeyAction
)

sealed interface KeyAction {
    data class KeyDown(val char: Char) : KeyAction
    data class KeyUp(val char: Char) : KeyAction
    data object Backspace : KeyAction
}
