package com.shyam.autotypex1.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized, version-aware permission manager for AutoType X1.
 *
 * Calculates required permissions based on the running Android version:
 * - Android 11 and below: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION
 * - Android 12+: BLUETOOTH_SCAN (neverForLocation), BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE
 * - Android 13+: POST_NOTIFICATIONS (tracked separately from Bluetooth permissions)
 *
 * No location permission is requested on Android 12+ because BLUETOOTH_SCAN uses neverForLocation.
 * No INTERNET permission is used anywhere.
 */
@Singleton
class PermissionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    // --- Required permission queries ---

    /**
     * Returns Bluetooth-related permissions required for the current Android version.
     */
    fun getRequiredBluetoothPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): new Bluetooth permissions, no location needed
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            // Android 11 and below: legacy Bluetooth + location for discovery
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Returns the notification permission if required on this Android version (13+),
     * or null if notifications don't need a runtime permission.
     */
    fun getRequiredNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }

    /**
     * Returns all runtime permissions required for this Android version.
     * Bluetooth permissions + notification permission (if applicable).
     */
    fun getAllRequiredPermissions(): List<String> {
        return buildList {
            addAll(getRequiredBluetoothPermissions())
            getRequiredNotificationPermission()?.let { add(it) }
        }
    }

    // --- Grant status checks ---

    /**
     * Returns all currently granted permissions from the required set.
     */
    fun getGrantedPermissions(): List<String> {
        return getAllRequiredPermissions().filter { isPermissionGranted(it) }
    }

    /**
     * Returns all currently missing (not granted) permissions from the required set.
     */
    fun getMissingPermissions(): List<String> {
        return getAllRequiredPermissions().filter { !isPermissionGranted(it) }
    }

    /**
     * Returns missing Bluetooth-specific permissions.
     */
    fun getMissingBluetoothPermissions(): List<String> {
        return getRequiredBluetoothPermissions().filter { !isPermissionGranted(it) }
    }

    /**
     * Returns the notification permission if it is required and not yet granted,
     * or null if not required or already granted.
     */
    fun getMissingNotificationPermission(): String? {
        val perm = getRequiredNotificationPermission() ?: return null
        return if (!isPermissionGranted(perm)) perm else null
    }

    /**
     * Whether all required permissions (Bluetooth + notifications) are granted.
     */
    fun areAllPermissionsGranted(): Boolean {
        return getAllRequiredPermissions().all { isPermissionGranted(it) }
    }

    /**
     * Whether all Bluetooth-specific permissions are granted.
     */
    fun areBluetoothPermissionsGranted(): Boolean {
        return getRequiredBluetoothPermissions().all { isPermissionGranted(it) }
    }

    /**
     * Whether the notification permission is granted (always true on < Android 13).
     */
    fun isNotificationPermissionGranted(): Boolean {
        val perm = getRequiredNotificationPermission() ?: return true
        return isPermissionGranted(perm)
    }

    // --- Permanent denial detection ---

    /**
     * Checks if a permission is "permanently denied": the permission was denied previously
     * and the system will no longer show the rationale dialog (shouldShowRequestPermission-
     * Rationale returns false after a prior denial).
     *
     * This requires an Activity reference because shouldShowRequestPermissionRationale
     * is an Activity method.
     *
     * Returns true only if: the permission is not granted AND rationale is NOT shown
     * (meaning the user selected "Don't ask again" or the system won't prompt again).
     *
     * Note: Before the first request, shouldShowRequestPermissionRationale also returns
     * false, so this should only be called after the user has denied at least once.
     */
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
        if (isPermissionGranted(permission)) return false
        return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    // --- Internal ---

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}
