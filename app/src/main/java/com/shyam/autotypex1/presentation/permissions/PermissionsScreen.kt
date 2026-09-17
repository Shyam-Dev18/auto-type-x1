package com.shyam.autotypex1.presentation.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Permissions screen composable.
 *
 * Explains each required permission, provides Grant/Try Again/Open Settings actions,
 * and re-checks permission state on lifecycle resume (returning from Settings).
 *
 * No business logic — renders state from ViewModel and emits events.
 */
@Composable
fun PermissionsScreen(
    onAllPermissionsGranted: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionResult(results, activity)
    }

    // Re-check permissions when returning from Settings or any other app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionState(activity)
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AutoType X1 needs the following permissions to function as a Bluetooth HID keyboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is PermissionsUiState.Loading -> {
                    // Will be replaced after first ON_RESUME triggers refreshPermissionState
                    Text(
                        text = "Checking permissions…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is PermissionsUiState.Ready -> {
                    // Navigate forward if all granted
                    if (state.allGranted) {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            onAllPermissionsGranted()
                        }
                    }

                    // Bluetooth permissions section
                    if (state.bluetoothPermissions.isNotEmpty()) {
                        SectionHeader(title = "Bluetooth")
                        Spacer(modifier = Modifier.height(8.dp))
                        state.bluetoothPermissions.forEach { item ->
                            PermissionCard(item = item)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Notification permission section
                    state.notificationPermission?.let { item ->
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "Notifications")
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionCard(item = item)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action buttons
                    if (!state.allGranted) {
                        if (state.hasPermanentlyDenied) {
                            // At least one permission is permanently denied — need Settings
                            Text(
                                text = "Some permissions were permanently denied. Please grant them in app settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open Settings")
                            }
                        }

                        // Always show "Grant Permissions" / "Try Again" if anything is missing
                        val missingPermissions = viewModel.getPermissionsToRequest()
                        if (missingPermissions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.hasPermanentlyDenied) {
                                // Secondary action: try requesting the non-permanently-denied ones
                                val nonPermanent = state.bluetoothPermissions
                                    .plus(listOfNotNull(state.notificationPermission))
                                    .filter { it.status == PermissionStatus.DENIED }
                                    .map { it.permission }
                                    .toTypedArray()

                                if (nonPermanent.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { permissionLauncher.launch(nonPermanent) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Try Again for Remaining")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { permissionLauncher.launch(missingPermissions) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Grant Permissions")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PermissionCard(item: PermissionItemState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                PermissionStatus.GRANTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                PermissionStatus.DENIED -> MaterialTheme.colorScheme.surfaceVariant
                PermissionStatus.PERMANENTLY_DENIED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            StatusIndicator(status = item.status)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.status == PermissionStatus.PERMANENTLY_DENIED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Permanently denied — grant in Settings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: PermissionStatus) {
    val (text, color) = when (status) {
        PermissionStatus.GRANTED -> "✓" to MaterialTheme.colorScheme.primary
        PermissionStatus.DENIED -> "○" to MaterialTheme.colorScheme.outline
        PermissionStatus.PERMANENTLY_DENIED -> "✕" to MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = color,
        modifier = Modifier.size(24.dp)
    )
}
