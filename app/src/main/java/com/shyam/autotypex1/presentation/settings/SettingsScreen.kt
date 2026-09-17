package com.shyam.autotypex1.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.typing.TypingProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.settings.profile
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SettingsUiEvent.OnDismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Section 1: Typing Profile Presets ──
            SettingsCardSection(
                title = "Typing Profile Presets",
                icon = Icons.Default.Tune
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(
                        "Slow" to TypingProfile.SLOW,
                        "Normal" to TypingProfile.NORMAL,
                        "Fast" to TypingProfile.FAST,
                        "Speed Demon" to TypingProfile.SPEED_DEMON
                    )

                    presets.forEach { (label, preset) ->
                        val isSelected = profile.name.equals(preset.name, ignoreCase = true)
                        ChoiceChipOption(
                            label = label,
                            isSelected = isSelected,
                            onClick = { viewModel.onEvent(SettingsUiEvent.OnPresetSelected(preset)) }
                        )
                    }
                }
            }

            // ── Section 2: Typing Speed (WPM Range) ──
            SettingsCardSection(
                title = "Typing Speed",
                icon = Icons.Default.Speed
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Target Speed Range",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    PillValueBadge("${profile.minWpm} - ${profile.maxWpm} WPM")
                }
                Spacer(Modifier.height(4.dp))
                RangeSlider(
                    value = profile.minWpm.toFloat()..profile.maxWpm.toFloat(),
                    onValueChange = { range ->
                        viewModel.onEvent(
                            SettingsUiEvent.OnWpmRangeChange(
                                range.start.toInt(),
                                range.endInclusive.toInt()
                            )
                        )
                    },
                    valueRange = 5f..350f
                )
            }

            // ── Section 3: Human Dynamics & Timing Jitter ──
            SettingsCardSection(
                title = "Human Dynamics & Variation",
                icon = Icons.Default.Build
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Timing Jitter
                    SliderSettingRow(
                        label = "Timing Jitter",
                        badgeValue = "${profile.jitterPercent}%",
                        value = profile.jitterPercent.toFloat(),
                        valueRange = 0f..50f,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnJitterChange(it.toInt())) }
                    )

                    // Typo Probability
                    SliderSettingRow(
                        label = "Typo & Correction Rate",
                        badgeValue = "${(profile.typoProbability * 100).toInt()}%",
                        value = profile.typoProbability.toFloat(),
                        valueRange = 0f..0.20f,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnTypoProbabilityChange(it.toDouble())) }
                    )

                    // Word Boundary Delay
                    SliderSettingRow(
                        label = "Word Boundary Delay",
                        badgeValue = "${profile.wordDelayMs} ms",
                        value = profile.wordDelayMs.toFloat(),
                        valueRange = 0f..300f,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnWordDelayChange(it.toLong())) }
                    )

                    // Punctuation Multiplier
                    val formattedMult = (Math.round(profile.punctuationDelayMultiplier * 10.0) / 10.0).toString()
                    SliderSettingRow(
                        label = "Punctuation Delay Multiplier",
                        badgeValue = "${formattedMult}x",
                        value = profile.punctuationDelayMultiplier.toFloat(),
                        valueRange = 1.0f..4.0f,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnPunctuationMultiplierChange(it.toDouble())) }
                    )

                    // Thinking Pauses
                    SliderSettingRow(
                        label = "Thinking Pause Probability",
                        badgeValue = "${(profile.thinkingPauseProbability * 100).toInt()}%",
                        value = profile.thinkingPauseProbability.toFloat(),
                        valueRange = 0f..0.10f,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnThinkingPauseChange(it.toDouble())) }
                    )

                    // Burst Variation
                    SliderSettingRow(
                        label = "Burst Speed Variation",
                        badgeValue = "${(profile.burstVariation * 100).toInt()}%",
                        value = profile.burstVariation.toFloat(),
                        valueRange = 0f..0.40f,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.OnBurstVariationChange(it.toDouble())) }
                    )

                    // Correction Pause Range
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Correction Pause Range",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            PillValueBadge("${profile.correctionPauseRangeMs.first} - ${profile.correctionPauseRangeMs.last} ms")
                        }
                        Spacer(Modifier.height(4.dp))
                        RangeSlider(
                            value = profile.correctionPauseRangeMs.first.toFloat()..profile.correctionPauseRangeMs.last.toFloat(),
                            onValueChange = { range ->
                                viewModel.onEvent(
                                    SettingsUiEvent.OnCorrectionPauseRangeChange(
                                        range.start.toLong(),
                                        range.endInclusive.toLong()
                                    )
                                )
                            },
                            valueRange = 20f..500f
                        )
                    }
                }
            }

            // ── Section 4: System Appearance & Theme Options ──
            SettingsCardSection(
                title = "Appearance & System Options",
                icon = Icons.Default.Palette
            ) {
                Text(
                    "System Theme Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "System" to ThemeMode.SYSTEM,
                        "Light" to ThemeMode.LIGHT,
                        "Dark" to ThemeMode.DARK
                    ).forEach { (label, mode) ->
                        val isSelected = state.settings.themeMode == mode
                        Box(modifier = Modifier.weight(1f)) {
                            ChoiceChipOption(
                                label = label,
                                isSelected = isSelected,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.onEvent(SettingsUiEvent.OnThemeModeChange(mode)) }
                            )
                        }
                    }
                }
            }

            // ── Section 5: Reset All Settings Button ──
            OutlinedButton(
                onClick = { viewModel.onEvent(SettingsUiEvent.OnResetToDefaults) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Reset All Settings to Defaults",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCardSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ChoiceChipOption(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold, // Bold text in system options & presets
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PillValueBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    badgeValue: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            PillValueBadge(badgeValue)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}
