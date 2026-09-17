package com.shyam.autotypex1.core.service

import android.content.Context
import com.shyam.autotypex1.domain.typing.TypingServiceLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [TypingServiceLauncher] using [ConnectionForegroundService].
 *
 * Connection lifecycle methods ([startConnection] / [stopConnection]) manage the
 * foreground service that keeps the process alive while a BT HID device is connected.
 *
 * Typing lifecycle methods ([startTyping] / [pause] / [resume] / [stop]) send commands
 * to the already-running service.
 */
@Singleton
class AndroidTypingServiceLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TypingServiceLauncher {

    override fun startConnection(deviceName: String) {
        ConnectionForegroundService.startConnection(context, deviceName)
    }

    override fun stopConnection() {
        ConnectionForegroundService.stopConnection(context)
    }

    override fun startTyping(script: String, profileName: String, seed: Long?) {
        ConnectionForegroundService.startTyping(
            context = context,
            script = script,
            profileName = profileName,
            seed = seed
        )
    }

    override fun pause() {
        ConnectionForegroundService.pause(context)
    }

    override fun resume() {
        ConnectionForegroundService.resume(context)
    }

    override fun stop() {
        ConnectionForegroundService.stop(context)
    }
}
