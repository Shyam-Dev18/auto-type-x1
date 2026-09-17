package com.shyam.autotypex1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shyam.autotypex1.domain.model.ThemeMode
import com.shyam.autotypex1.domain.repository.SettingsRepository
import com.shyam.autotypex1.presentation.navigation.NavGraph
import com.shyam.autotypex1.presentation.theme.AutoTypeX1Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity Compose host for AutoType X1.
 *
 * All screens are rendered as Compose destinations within [NavGraph].
 * Injects [SettingsRepository] to dynamically reflect the selected [ThemeMode].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.observeSettings()
                .collectAsStateWithLifecycle(initialValue = null)

            AutoTypeX1Theme(themeMode = settings?.themeMode ?: ThemeMode.SYSTEM) {
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }
}