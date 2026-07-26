package com.shyam.autotypex1.presentation.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.model.TypingProfile
import androidx.compose.foundation.horizontalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spec = state.settings.typingSpec
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Section: Typing Profile Preset ──
            Text(
                "Typing Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TypingProfile.values().forEach { prof ->
                            FilterChip(
                                selected = state.settings.profile == prof,
                                onClick = { viewModel.onEvent(SettingsUiEvent.OnProfileChange(prof)) },
                                label = {
                                    Text(
                                        when (prof) {
                                            TypingProfile.SLOW -> "Slow"
                                            TypingProfile.NORMAL -> "Normal"
                                            TypingProfile.FAST -> "Fast"
                                            TypingProfile.LIGHTNING -> "Lightning"
                                            TypingProfile.CUSTOM -> "Custom"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // ── Section: Human Typing Profile ──
            Text(
                "Typing Speed & Jitter (Custom)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // WPM Range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Base Speed (WPM)", fontWeight = FontWeight.SemiBold)
                        }
                        Text("${spec.baseWpm.first} - ${spec.baseWpm.last}", fontWeight = FontWeight.Bold)
                    }
                    RangeSlider(
                        value = spec.baseWpm.first.toFloat()..spec.baseWpm.last.toFloat(),
                        onValueChange = { range ->
                            viewModel.onEvent(
                                SettingsUiEvent.OnWpmRangeChange(
                                    range.start.toInt(),
                                    range.endInclusive.toInt()
                                )
                            )
                        },
                        valueRange = 10f..3000f
                    )

                    Spacer(Modifier.height(16.dp))

                    // Jitter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Typing Jitter (%)", fontWeight = FontWeight.SemiBold)
                        Text("${spec.jitterPercent}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = spec.jitterPercent.toFloat(),
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnJitterChange(it.toInt())) },
                        valueRange = 0f..100f
                    )

                    Spacer(Modifier.height(16.dp))

                    // Typo Probability
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Typo Rate (%)", fontWeight = FontWeight.SemiBold)
                        Text("${(spec.typoProbability * 100).toInt()}%", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = spec.typoProbability,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnTypoProbabilityChange(it)) },
                        valueRange = 0f..0.5f
                    )

                    Spacer(Modifier.height(16.dp))

                    // Word Gap
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Post-Word Delay (ms)", fontWeight = FontWeight.SemiBold)
                        Text("${spec.wordGapMs}ms", fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = spec.wordGapMs.toFloat(),
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnWordGapChange(it.toInt())) },
                        valueRange = 0f..1000f,
                        steps = 100
                    )
                }
            }

            // ── Section: Theme Mode ──
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Theme Mode", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = state.settings.themeMode == mode,
                                onClick = { viewModel.onEvent(SettingsUiEvent.OnThemeModeChange(mode)) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
