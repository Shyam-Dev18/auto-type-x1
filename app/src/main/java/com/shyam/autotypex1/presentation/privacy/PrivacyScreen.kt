package com.shyam.autotypex1.presentation.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Your Privacy is Sovereign",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                "Last Updated: September 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                "AutoType X1 is built as an offline-first utility. We care deeply about the security of your keystrokes, your scripts, and your personal data. Below is our strict commitment to your privacy.",
                style = MaterialTheme.typography.bodyMedium
            )

            PrivacySection(
                title = "1. Zero Data Collection & Zero Telemetry",
                content = "All script contents, typing profiles, and connected device names are saved strictly on your local device using secure local storage (Android Room Database & Jetpack DataStore). We do not collect, transmit, upload, or analyze any of your data."
            )

            PrivacySection(
                title = "2. Absolute Offline Enforcement",
                content = "AutoType X1 does not declare or request the INTERNET permission in its application manifest. The app is physically incapable of transmitting data to external servers."
            )

            PrivacySection(
                title = "3. Purpose of Permissions",
                content = "• Bluetooth / Bluetooth Connect / Bluetooth Scan: Required strictly to detect, pair, and register the Android device as a Human Interface Device (HID) to send keypresses to your host PC/Mac.\n" +
                    "• Notification Permission: Required on Android 13+ solely to maintain the active foreground service notification showing typing progress."
            )

            PrivacySection(
                title = "4. Keystroke Integrity & No Logging",
                content = "The application functions solely as a keyboard peripheral emulator. It never logs script text or individual keystrokes to logcat, files, or external stores."
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PrivacySection(
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
