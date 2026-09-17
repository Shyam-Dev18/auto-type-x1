package com.shyam.autotypex1.presentation.typing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shyam.autotypex1.domain.typing.TypingFailure
import com.shyam.autotypex1.domain.typing.TypingState
import com.shyam.autotypex1.presentation.theme.ConnectedGreen
import com.shyam.autotypex1.presentation.theme.ConnectingAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingScreen(
    onBack: () -> Unit,
    viewModel: TypingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(TypingUiEvent.OnDismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Typing Session", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Script & Profile Info Card ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ACTIVE SCRIPT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Profile: ${state.profileName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.scriptName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.scriptContent.length > 150) state.scriptContent.take(150) + "…"
                        else state.scriptContent.ifEmpty { "No script content selected" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(0.5f))

            // ── State & Progress Indicator ──
            val isTyping = state.typingState is TypingState.Typing || state.typingState is TypingState.Preparing
            val isPaused = state.typingState is TypingState.Paused
            val isCompleted = state.typingState is TypingState.Completed
            val isCancelled = state.typingState is TypingState.Cancelled
            val isFailed = state.typingState is TypingState.Failed

            val statusTitle = when (state.typingState) {
                is TypingState.Idle -> if (state.isReady) "Ready to Type" else if (!state.isConnected) "Device Not Connected" else "Script Required"
                is TypingState.Preparing -> "Preparing Keystrokes…"
                is TypingState.Typing -> "Typing in Progress…"
                is TypingState.Paused -> "Typing Paused"
                is TypingState.Cancelling -> "Stopping…"
                is TypingState.Cancelled -> "Session Stopped"
                is TypingState.Completed -> "Typing Completed!"
                is TypingState.Failed -> "Typing Failed"
            }

            val statusColor = when {
                isCompleted -> ConnectedGreen
                isPaused -> ConnectingAmber
                isTyping -> MaterialTheme.colorScheme.primary
                isCancelled -> MaterialTheme.colorScheme.tertiary
                isFailed -> MaterialTheme.colorScheme.error
                !state.isConnected -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!state.isConnected && state.typingState is TypingState.Idle) {
                    Icon(
                        Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    statusTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    textAlign = TextAlign.Center
                )
            }

            if (state.typingState is TypingState.Idle) {
                Text(
                    if (state.isReady) "Press Start Typing below to begin hardware key emulation."
                    else if (!state.isConnected) "Without a device connected, typing is disabled. Please connect to a PC/Mac on the Home screen."
                    else "Select a script on the Scripts screen to start typing.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tip: For background typing, set Battery to Unrestricted in App Info → Battery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val progressFraction = (state.progress.percentage / 100f).coerceIn(0f, 1f)
                val pct = state.progress.percentage.toInt()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp)),
                        color = statusColor
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${state.progress.currentCharIndex} / ${state.progress.totalChars} characters",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("$pct%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Elapsed Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val secs = state.progress.elapsedMs / 1000
                            Text(
                                "${secs / 60}m ${secs % 60}s",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Error Banner ──
            AnimatedVisibility(visible = isFailed) {
                val failure = (state.typingState as? TypingState.Failed)?.failure
                val reasonText = when (failure) {
                    is TypingFailure.NoHidConnection -> "No active HID connection to target host"
                    is TypingFailure.ScriptEmpty -> "Script contains no supported characters"
                    is TypingFailure.BluetoothDisconnected -> "Bluetooth disconnected during typing"
                    is TypingFailure.BluetoothDisabled -> "Bluetooth was disabled"
                    is TypingFailure.HidSendFailed -> "Failed to send HID report"
                    is TypingFailure.TypingCancelled -> "Typing session was cancelled"
                    is TypingFailure.InvalidState -> failure.message
                    null -> "Unknown error"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        reasonText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Action Buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                when {
                    isTyping -> {
                        OutlinedButton(
                            onClick = { viewModel.onEvent(TypingUiEvent.OnPause) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pause", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Button(
                            onClick = { viewModel.onEvent(TypingUiEvent.OnStop) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    isPaused -> {
                        Button(
                            onClick = { viewModel.onEvent(TypingUiEvent.OnResume) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Resume", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.onEvent(TypingUiEvent.OnStop) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Stop", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    isCompleted || isCancelled || isFailed -> {
                        Button(
                            onClick = { viewModel.onEvent(TypingUiEvent.OnReset) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reset Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.onEvent(TypingUiEvent.OnStart) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = state.isReady, // GUARANTEE: Disabled if not connected
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start Typing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
