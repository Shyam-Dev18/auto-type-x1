package com.shyam.autotypex1.presentation.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Your Privacy is Sovereign",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Last Updated: July 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "AutoType X1 is built as an offline-first utility. We care deeply about the security of your keystrokes, your scripts, and your personal data. Below is a breakdown of our commitment to your privacy.",
                style = MaterialTheme.typography.bodyMedium
            )

            Section(
                title = "1. Zero Data Collection",
                content = "All script contents, settings, and connected device names are saved strictly on your local device using secure local storage (Android Room Database & Jetpack DataStore). We do not collect, transmit, upload, or analyze any of your data."
            )

            Section(
                title = "2. Absolute Offline Enforcement",
                content = "AutoType X1 has completely stripped the INTERNET permission from its application manifest. The app is physically incapable of transmitting packets to external servers, making it immune to remote data leaks."
            )

            Section(
                title = "3. Purpose of Permissions",
                content = "• Bluetooth / Bluetooth Connect / Bluetooth Scan: Required strictly to detect, pair, and register the Android device as a Human Interface Device (HID) to send keypresses to your host machine.\n" +
                    "• Location Access: On older Android versions, Bluetooth discovery requires location access. This location data is never processed or saved by the application.\n" +
                    "• Foreground Service: Required to maintain a stable Bluetooth HID keyboard connection and continue typing accurately even when the app is minimized."
            )

            Section(
                title = "4. Keystroke Integrity",
                content = "The application functions solely as a keyboard peripheral emulator. It does not monitor, log, or record keys typed on the device outside the active emulation scripts you create."
            )

            Section(
                title = "5. Developer's Trust Clause",
                content = "Why did we completely strip the INTERNET permission? Because we wanted a connection we could actually trust. Plus, local storage has no network lag, no merge conflicts, and zero AWS bills.\n\n" +
                    "Jokes aside, your script contents are compile-only. There are no tracking scripts, no telemetry, and no remote databases. Just pure local Bluetooth keyboard emulations."
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: String
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
