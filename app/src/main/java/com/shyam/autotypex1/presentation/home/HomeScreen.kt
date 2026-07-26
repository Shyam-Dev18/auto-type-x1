package com.shyam.autotypex1.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shyam.autotypex1.domain.model.BluetoothAdapterState
import com.shyam.autotypex1.domain.model.ConnectionState
import com.shyam.autotypex1.presentation.theme.ConnectedGreen
import com.shyam.autotypex1.presentation.theme.ConnectingAmber
import com.shyam.autotypex1.presentation.theme.DisconnectedGray
import com.shyam.autotypex1.presentation.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScripts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTyping: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(HomeUiEvent.OnDismissError)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "AUTO TYPE X1",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Bluetooth Keyboard Emulator",
                            style = MaterialTheme.typography.bodySmall,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(HomeUiEvent.OnAboutClick); onNavigateToAbout() }) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                    IconButton(onClick = { viewModel.onEvent(HomeUiEvent.OnSettingsClick); onNavigateToSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Connection Status Card ──
            item {
                ConnectionStatusCard(
                    connectionState = state.connectionState,
                    bluetoothState = state.bluetoothState,
                    onBluetoothClick = { viewModel.onEvent(HomeUiEvent.OnBluetoothIconClick) },
                    onDisconnectClick = { viewModel.onEvent(HomeUiEvent.OnDisconnectClick) }
                )
            }

            // ── Quick Actions ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Add,
                        title = "Add Device",
                        subtitle = "Scan & connect",
                        onClick = { viewModel.onEvent(HomeUiEvent.OnAddDeviceClick); onNavigateToAddDevice() }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Code,
                        title = "Scripts",
                        subtitle = state.selectedScriptName ?: "None selected",
                        onClick = { viewModel.onEvent(HomeUiEvent.OnScriptsClick); onNavigateToScripts() }
                    )
                }
            }

            item {
                // Typing card — prominent CTA
                TypingQuickAction(
                    isReady = state.connectionState is ConnectionState.Connected && state.selectedScriptName != null,
                    scriptName = state.selectedScriptName,
                    onClick = { viewModel.onEvent(HomeUiEvent.OnTypingClick); onNavigateToTyping() }
                )
            }

            // ── Reconnect last ──
            if (state.lastConnectedAddress != null && state.connectionState is ConnectionState.Disconnected) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onEvent(HomeUiEvent.OnReconnectClick) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reconnect", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Tap to reconnect to last device",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // ── Saved devices ──
            if (state.savedDevices.isNotEmpty()) {
                item {
                    Text(
                        "Saved Devices",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.savedDevices, key = { it.address }) { device ->
                    val clickEnabled = state.pendingAddress != device.address
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = clickEnabled) {
                                viewModel.onEvent(HomeUiEvent.OnSavedDeviceClick(device.address))
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (state.connectionState is ConnectionState.Connected &&
                                    (state.connectionState as ConnectionState.Connected).deviceAddress == device.address
                                ) Icons.Default.BluetoothConnected
                                else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (state.connectionState is ConnectionState.Connected &&
                                    (state.connectionState as ConnectionState.Connected).deviceAddress == device.address
                                ) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.pendingAddress == device.address) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { viewModel.onEvent(HomeUiEvent.OnDeleteSavedDevice(device.address)) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ErrorRed)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    bluetoothState: BluetoothAdapterState,
    onBluetoothClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    val statusColor by animateColorAsState(
        when (connectionState) {
            is ConnectionState.Connected -> ConnectedGreen
            is ConnectionState.Connecting -> ConnectingAmber
            is ConnectionState.Failed -> ErrorRed
            else -> DisconnectedGray
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColor"
    )

    val (icon, text) = when (connectionState) {
        is ConnectionState.Connected -> Icons.Default.BluetoothConnected to "Connected to ${connectionState.deviceName}"
        is ConnectionState.Connecting -> Icons.Default.BluetoothSearching to "Connecting…"
        is ConnectionState.Failed -> Icons.Default.BluetoothDisabled to "Connection failed"
        is ConnectionState.Disconnected -> {
            if (bluetoothState == BluetoothAdapterState.OFF || bluetoothState == BluetoothAdapterState.UNAVAILABLE) {
                Icons.Default.BluetoothDisabled to "Bluetooth is off"
            } else {
                Icons.Default.LinkOff to "Not connected"
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .clickable(onClick = onBluetoothClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (connectionState is ConnectionState.Connected) {
                    Text(
                        connectionState.deviceAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = onDisconnectClick,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TypingQuickAction(
    isReady: Boolean,
    scriptName: String?,
    onClick: () -> Unit
) {
    val containerColor = if (isReady)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isReady, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isReady) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Keyboard,
                contentDescription = null,
                tint = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isReady) "Start Typing" else "Typing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        scriptName == null -> "Select a script first"
                        !isReady -> "Connect a device first"
                        else -> "Ready — \"$scriptName\""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isReady) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
