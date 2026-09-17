package com.shyam.autotypex1.presentation.permissions

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.lifecycle.ViewModel
import com.shyam.autotypex1.core.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Permissions screen.
 *
 * Manages permission state evaluation and tracks whether permissions have been
 * requested at least once (to correctly detect permanent denial vs first-time request).
 *
 * No auto-relaunching of permission requests after denial — the user must
 * explicitly tap "Try Again" or "Open Settings".
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<PermissionsUiState>(PermissionsUiState.Loading)
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    /**
     * Tracks which permissions have been requested at least once.
     * Used to differentiate "never requested" (shouldShowRationale = false)
     * from "permanently denied" (shouldShowRationale = false after a denial).
     */
    private val requestedPermissions = mutableSetOf<String>()

    /**
     * Refresh the permission state. Should be called:
     * - On screen entry
     * - When lifecycle resumes (returning from Settings)
     * - After a permission request result
     *
     * @param activity Required for checking shouldShowRequestPermissionRationale
     */
    fun refreshPermissionState(activity: Activity) {
        val bluetoothPerms = permissionManager.getRequiredBluetoothPermissions().map { perm ->
            PermissionItemState(
                permission = perm,
                displayName = getDisplayName(perm),
                rationale = getRationale(perm),
                status = getPermissionStatus(activity, perm)
            )
        }

        val notificationPerm = permissionManager.getRequiredNotificationPermission()?.let { perm ->
            PermissionItemState(
                permission = perm,
                displayName = getDisplayName(perm),
                rationale = getRationale(perm),
                status = getPermissionStatus(activity, perm)
            )
        }

        val allItems = bluetoothPerms + listOfNotNull(notificationPerm)
        val allGranted = allItems.all { it.status == PermissionStatus.GRANTED }
        val hasPermanentlyDenied = allItems.any { it.status == PermissionStatus.PERMANENTLY_DENIED }

        _uiState.value = PermissionsUiState.Ready(
            bluetoothPermissions = bluetoothPerms,
            notificationPermission = notificationPerm,
            allGranted = allGranted,
            hasPermanentlyDenied = hasPermanentlyDenied
        )
    }

    /**
     * Called after a permission request completes. Marks requested permissions
     * as having been requested (for permanent-denial detection) and refreshes state.
     *
     * @param permissions Map of permission → granted result from the system callback.
     * @param activity Required for status re-evaluation.
     */
    fun onPermissionResult(permissions: Map<String, Boolean>, activity: Activity) {
        requestedPermissions.addAll(permissions.keys)
        refreshPermissionState(activity)
    }

    /**
     * Returns the list of permissions that still need to be requested.
     */
    fun getPermissionsToRequest(): Array<String> {
        return permissionManager.getMissingPermissions().toTypedArray()
    }

    private fun getPermissionStatus(activity: Activity, permission: String): PermissionStatus {
        val granted = permissionManager.getGrantedPermissions().contains(permission)
        if (granted) return PermissionStatus.GRANTED

        // Only consider "permanently denied" if we've actually requested it before.
        // Before the first request, shouldShowRationale is also false, which would
        // be a false positive for permanent denial.
        if (permission in requestedPermissions &&
            permissionManager.isPermissionPermanentlyDenied(activity, permission)
        ) {
            return PermissionStatus.PERMANENTLY_DENIED
        }

        return PermissionStatus.DENIED
    }

    private fun getDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH -> "Bluetooth"
            Manifest.permission.BLUETOOTH_ADMIN -> "Bluetooth Admin"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location"
            Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scan"
            Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connect"
            Manifest.permission.BLUETOOTH_ADVERTISE -> "Bluetooth Advertise"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications"
            else -> permission.substringAfterLast('.')
        }
    }

    private fun getRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN ->
                "Required for Bluetooth Classic HID keyboard connection with your PC."

            Manifest.permission.ACCESS_FINE_LOCATION ->
                "Required on Android 11 and below to discover nearby Bluetooth devices for pairing."

            Manifest.permission.BLUETOOTH_SCAN ->
                "Required to discover nearby Bluetooth devices for pairing."

            Manifest.permission.BLUETOOTH_CONNECT ->
                "Required to connect to paired devices and establish the HID keyboard connection."

            Manifest.permission.BLUETOOTH_ADVERTISE ->
                "Required to make this device visible as a Bluetooth HID keyboard."

            Manifest.permission.POST_NOTIFICATIONS ->
                "Required to show a notification while the typing service is running in the foreground."

            else -> "Required for app functionality."
        }
    }
}
