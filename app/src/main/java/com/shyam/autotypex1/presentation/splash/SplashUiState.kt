package com.shyam.autotypex1.presentation.splash

/**
 * Sealed interface representing the UI state of the Splash screen.
 *
 * The ViewModel transitions through these states:
 * 1. [Checking] — initial state while evaluating permission status
 * 2. [AllPermissionsGranted] — all required permissions are granted, navigate forward
 * 3. [NeedsPermissions] — one or more permissions are missing, navigate to Permissions screen
 */
sealed interface SplashUiState {
    /** Currently evaluating permission state. */
    data object Checking : SplashUiState

    /** All required permissions are granted — proceed to the next screen. */
    data object AllPermissionsGranted : SplashUiState

    /** One or more permissions are missing — navigate to the Permissions screen. */
    data object NeedsPermissions : SplashUiState
}
