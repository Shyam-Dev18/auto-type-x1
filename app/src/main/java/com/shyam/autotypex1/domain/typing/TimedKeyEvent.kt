package com.shyam.autotypex1.domain.typing

/**
 * An immutable timed key event emitted by the typing engine.
 *
 * @property delayMs The delay in milliseconds that should precede this key action.
 * @property action  The specific explicit key action to execute.
 */
data class TimedKeyEvent(
    val delayMs: Long,
    val action: KeyAction
)

/**
 * Explicit key actions. The engine never emits high-level string payloads;
 * it strictly breaks typing down into explicit key press, release, and modifier actions.
 */
sealed interface KeyAction {
    /** Press a character key down. */
    data class KeyDown(val char: Char) : KeyAction

    /** Release a character key. */
    data class KeyUp(val char: Char) : KeyAction

    /** Press a modifier key down. */
    data class ModifierDown(val modifier: ModifierKey) : KeyAction

    /** Release a modifier key. */
    data class ModifierUp(val modifier: ModifierKey) : KeyAction

    /** Convenience key action for backspace (press and release or discrete action). */
    data object Backspace : KeyAction
}

/**
 * Modifier keys recognized by the typing engine.
 */
enum class ModifierKey {
    SHIFT,
    CTRL,
    ALT,
    GUI
}
