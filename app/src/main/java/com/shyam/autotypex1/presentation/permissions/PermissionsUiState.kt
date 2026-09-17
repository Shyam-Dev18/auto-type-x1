package com.shyam.autotypex1.presentation.permissions

/**
 * Sealed interface representing the UI state of the Permissions screen.
 * Explicit sealed states — no independent booleans.
 */
sealed interface PermissionsUiState {
    /** Initial loading state while evaluating permissions. */
    data object Loading : PermissionsUiState

    /**
     * Permissions have been evaluated and the screen is ready to display.
     *
     * @param bluetoothPermissions State of each Bluetooth-related permission.
     * @param notificationPermission State of the notification permission, null if not required (< Android 13).
     * @param allGranted True if every required permission is granted.
     * @param hasPermanentlyDenied True if any permission is permanently denied (needs Settings redirect).
     */
    data class Ready(
        val bluetoothPermissions: List<PermissionItemState>,
        val notificationPermission: PermissionItemState?,
        val allGranted: Boolean,
        val hasPermanentlyDenied: Boolean
    ) : PermissionsUiState
}

/**
 * Represents the state of a single permission item for display.
 *
 * @param permission The Android permission string (e.g., Manifest.permission.BLUETOOTH_CONNECT).
 * @param displayName Human-readable name shown to the user.
 * @param rationale Why this permission is needed.
 * @param status Current grant/denial status.
 */
data class PermissionItemState(
    val permission: String,
    val displayName: String,
    val rationale: String,
    val status: PermissionStatus
)

/**
 * Grant status of a single permission.
 */
enum class PermissionStatus {
    /** Permission is granted. */
    GRANTED,
    /** Permission was denied but can still be requested again. */
    DENIED,
    /** Permission was denied with "Don't ask again" — requires Settings redirect. */
    PERMANENTLY_DENIED
}
