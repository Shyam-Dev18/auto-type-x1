package com.shyam.autotypex1.presentation.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    onNavigateToScripts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTyping: () -> Unit,
    onNavigateToAddDevice: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(HomeUiEvent.OnDismissError)
        }
    }

    val enableBluetoothLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { }

    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.activity.compose.BackHandler(enabled = true) {
        (context as? android.app.Activity)?.finish()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = com.shyam.autotypex1.R.drawable.ic_launcher_foreground),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "AUTO TYPE X1",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Bluetooth Keyboard Emulator",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        IconButton(
                            onClick = { viewModel.onEvent(HomeUiEvent.OnAboutClick); onNavigateToAbout() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "About",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        IconButton(
                            onClick = { viewModel.onEvent(HomeUiEvent.OnSettingsClick); onNavigateToSettings() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Connection Status Card ──
            ConnectionStatusCard(
                connectionState = state.connectionState,
                bluetoothState = state.bluetoothState,
                onBluetoothClick = {
                    enableBluetoothLauncher.launch(
                        android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    )
                },
                onDisconnectClick = { viewModel.onEvent(HomeUiEvent.OnDisconnectClick) }
            )

            // ── Quick Actions ──
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

            // ── Typing CTA Card ──
            val isConnected = state.connectionState is ConnectionState.Connected
            val isReady = isConnected && state.selectedScriptName != null
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onEvent(HomeUiEvent.OnTypingClick); onNavigateToTyping() },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(
                    1.dp,
                    if (isReady) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isReady) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isReady) "Ready to Type" else "Typing Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isReady) "Active: ${state.selectedScriptName} • Tap to open controls"
                            else if (!isConnected) "Device not connected • Tap to view"
                            else "Select a script to start typing",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Reconnect Last Device ──
            if (state.lastConnectedAddress != null && state.connectionState is ConnectionState.Disconnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onEvent(HomeUiEvent.OnReconnectClick) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Reconnect Last Device",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Tap to reconnect to ${state.lastConnectedAddress}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // ── Saved Devices Scrollable Section ──
            if (state.savedDevices.isNotEmpty()) {
                Text(
                    "Saved Devices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.savedDevices, key = { it.address }) { device ->
                        val isCurrentDevice = state.connectionState is ConnectionState.Connected &&
                                (state.connectionState as ConnectionState.Connected).deviceAddress.equals(device.address, ignoreCase = true)

                        Card(
                            modifier = Modifier
                                 .fillMaxWidth()
                                 .clickable(enabled = state.pendingAddress != device.address) {
                                     viewModel.onEvent(HomeUiEvent.OnSavedDeviceClick(device.address))
                                 },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentDevice) ConnectedGreen.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isCurrentDevice) Icons.Default.BluetoothConnected
                                    else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isCurrentDevice) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        device.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        device.address + if (isCurrentDevice) " • Connected" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCurrentDevice) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (state.pendingAddress == device.address) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                } else {
                                    IconButton(onClick = { viewModel.onEvent(HomeUiEvent.OnDeleteSavedDevice(device.address)) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = ErrorRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
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
        is ConnectionState.Connecting -> Icons.AutoMirrored.Filled.BluetoothSearching to "Connecting to device…"
        is ConnectionState.Disconnecting -> Icons.AutoMirrored.Filled.BluetoothSearching to "Disconnecting…"
        is ConnectionState.Failed -> Icons.Default.BluetoothDisabled to "Connection failed"
        is ConnectionState.Disconnected -> {
            if (bluetoothState == BluetoothAdapterState.Disabled || bluetoothState == BluetoothAdapterState.Unavailable) {
                Icons.Default.BluetoothDisabled to "Bluetooth is off"
            } else {
                Icons.Default.LinkOff to "Not connected"
            }
        }
    }

    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting || connectionState is ConnectionState.Disconnecting
    val isFailed = connectionState is ConnectionState.Failed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 110.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            when {
                isConnected -> ConnectedGreen.copy(alpha = 0.5f)
                isConnecting -> ConnectingAmber.copy(alpha = 0.5f)
                isFailed -> ErrorRed.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    lineHeight = 23.sp
                )
                if (connectionState is ConnectionState.Connected) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        connectionState.deviceAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (connectionState is ConnectionState.Connected) {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Disconnect", fontWeight = FontWeight.Bold)
                }
            } else if (bluetoothState == BluetoothAdapterState.Disabled) {
                Button(
                    onClick = onBluetoothClick,
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text("Turn On", fontWeight = FontWeight.Bold)
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
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
