package com.shyam.autotypex1.domain.typing

/**
 * Interface to start and stop the connection foreground service and control typing.
 * Decouples presentation/data logic from Android Service APIs for testability.
 *
 * Connection lifecycle:
 * - [startConnection] / [stopConnection]: manage the foreground service that keeps
 *   the process alive while a Bluetooth HID device is connected.
 *
 * Typing lifecycle:
 * - [startTyping] / [pause] / [resume] / [stop]: control typing sessions within
 *   the already-running foreground service.
 */
interface TypingServiceLauncher {
    /** Start the foreground service with a connected-device notification. */
    fun startConnection(deviceName: String)
    /** Stop the foreground service (called after full disconnect + unregister). */
    fun stopConnection()

    fun startTyping(script: String, profileName: String, seed: Long? = null)
    fun pause()
    fun resume()
    fun stop()
}
