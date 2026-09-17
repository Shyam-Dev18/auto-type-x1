package com.shyam.autotypex1.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shyam.autotypex1.domain.typing.TypingController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver for handling notification action clicks (Pause, Resume, Stop).
 * Works reliably across all Android versions in background, screen-off, and foreground states.
 */
@AndroidEntryPoint
class TypingActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var typingController: TypingController

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                when (intent?.action) {
                    ACTION_PAUSE -> typingController.pauseTyping()
                    ACTION_RESUME -> typingController.resumeTyping()
                    ACTION_STOP -> typingController.stopTyping()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.shyam.autotypex1.action.NOTIFICATION_PAUSE"
        const val ACTION_RESUME = "com.shyam.autotypex1.action.NOTIFICATION_RESUME"
        const val ACTION_STOP = "com.shyam.autotypex1.action.NOTIFICATION_STOP"
    }
}
