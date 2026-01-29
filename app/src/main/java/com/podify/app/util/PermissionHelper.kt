package com.podify.app.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper for managing runtime permissions required by the app.
 * 
 * Required permissions:
 * - Android 12+ (API 31+): BLUETOOTH_SCAN, BLUETOOTH_CONNECT
 * - All versions: ACCESS_FINE_LOCATION (for BLE scanning)
 * - Android 13+ (API 33+): POST_NOTIFICATIONS
 */
object PermissionHelper {
    
    /**
     * Get all required permissions based on Android version
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        // Location is required for BLE scanning on all versions
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ needs notification permission
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        return permissions.toTypedArray()
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if Bluetooth permissions are granted
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 12 doesn't need runtime Bluetooth permissions
        }
    }
    
    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-Android 13 doesn't need runtime notification permission
        }
    }
    
    /**
     * Check if we should show rationale for any permission
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return getRequiredPermissions().any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }
    
    /**
     * Get the list of permissions that are not yet granted
     */
    fun getMissingPermissions(context: Context): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Open app settings page for manual permission grant
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Check if overlay permission is granted (for connection popup)
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
    
    /**
     * Open overlay permission settings
     */
    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Get user-friendly description for a permission
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH_SCAN -> "Nearby Devices - Find AirPods via Bluetooth"
            Manifest.permission.BLUETOOTH_CONNECT -> "Nearby Devices - Connect to AirPods"
            Manifest.permission.ACCESS_FINE_LOCATION -> "Location - Required for Bluetooth scanning"
            Manifest.permission.POST_NOTIFICATIONS -> "Notifications - Show battery alerts"
            else -> permission
        }
    }
}
