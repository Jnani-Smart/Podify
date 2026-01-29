package com.podify.app.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receiver that starts the AirPods service when the device boots.
 * This enables auto-connection functionality without requiring the user
 * to manually open the app.
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking permissions")
            
            // Check if we have the required Bluetooth permissions
            val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Pre-Android 12 doesn't need runtime permission
            }
            
            if (!hasBluetoothPermission) {
                Log.w(TAG, "Cannot start service - Bluetooth permission not granted")
                return
            }
            
            try {
                val serviceIntent = Intent(context, AirPodsService::class.java).apply {
                    action = AirPodsService.ACTION_START
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "Service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service on boot", e)
            }
        }
    }
}
