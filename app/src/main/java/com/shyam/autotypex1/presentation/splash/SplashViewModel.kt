package com.shyam.autotypex1.presentation.splash

import androidx.lifecycle.ViewModel
import com.shyam.autotypex1.core.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Splash screen.
 *
 * On initialization, checks the current permission state via [PermissionManager]
 * and emits the appropriate [SplashUiState]. No Android platform/service logic here —
 * the PermissionManager handles all platform interaction.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Checking)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
    }

    /**
     * Evaluate current permission state and update the UI state.
     * Called on init and can be re-invoked if needed (e.g., on resume).
     */
    fun checkPermissions() {
        _uiState.value = if (permissionManager.areAllPermissionsGranted()) {
            SplashUiState.AllPermissionsGranted
        } else {
            SplashUiState.NeedsPermissions
        }
    }
}
