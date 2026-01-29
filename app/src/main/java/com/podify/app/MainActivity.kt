package com.podify.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.podify.app.bluetooth.AirPodsScanner
import com.podify.app.service.AirPodsService
import com.podify.app.ui.screens.HomeScreen
import com.podify.app.ui.screens.SettingsScreen
import com.podify.app.ui.theme.PodifyTheme
import com.podify.app.util.AirPodsStatus
import com.podify.app.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Scanner instance
    private lateinit var scanner: AirPodsScanner
    
    // State
    private val _airPodsStatus = mutableStateOf(AirPodsStatus.DISCONNECTED)
    private val _isScanning = mutableStateOf(false)
    private val _permissionsGranted = mutableStateOf(false)
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        Log.d(TAG, "Permissions result: $permissions, all granted: $allGranted")
        
        _permissionsGranted.value = allGranted
        
        if (allGranted) {
            startScanningIfReady()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Initialize scanner
        scanner = AirPodsScanner(this)
        
        // Observe scanner status
        lifecycleScope.launch {
            scanner.status.collectLatest { status ->
                _airPodsStatus.value = status
            }
        }
        
        lifecycleScope.launch {
            scanner.isScanning.collectLatest { scanning ->
                _isScanning.value = scanning
            }
        }
        
        setContent {
            val status by remember { _airPodsStatus }
            val isScanning by remember { _isScanning }
            val permissionsGranted by remember { _permissionsGranted }
            
            PodifyTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PodifyApp(
                        status = status,
                        isScanning = isScanning,
                        permissionsGranted = permissionsGranted,
                        onRequestPermissions = { requestPermissions() },
                        onStartScanning = { startScanningIfReady() },
                        isBluetoothEnabled = { scanner.isBluetoothEnabled() }
                    )
                }
            }
        }
        
        // Check permissions on start
        checkAndRequestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        // Recheck permissions when returning to app
        if (PermissionHelper.hasAllPermissions(this)) {
            _permissionsGranted.value = true
            startScanningIfReady()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Keep scanning in background if service is enabled
        // Otherwise scanner will be handled by service
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scanner.stopScanning()
    }
    
    private fun checkAndRequestPermissions() {
        if (PermissionHelper.hasAllPermissions(this)) {
            _permissionsGranted.value = true
            startScanningIfReady()
        } else {
            requestPermissions()
        }
    }
    
    private fun requestPermissions() {
        val permissions = PermissionHelper.getRequiredPermissions()
        Log.d(TAG, "Requesting permissions: ${permissions.toList()}")
        permissionLauncher.launch(permissions)
    }
    
    private fun startScanningIfReady() {
        if (PermissionHelper.hasBluetoothPermissions(this) && 
            PermissionHelper.hasLocationPermission(this)) {
            
            Log.d(TAG, "Starting scanner")
            scanner.startScanning()
            scanner.checkAlreadyConnected()
            
            // Start background service if permissions are granted
            startBackgroundServiceIfEnabled()
        } else {
            Log.w(TAG, "Cannot start scanning - missing permissions")
        }
    }
    
    private fun startBackgroundServiceIfEnabled() {
        try {
            val intent = Intent(this, AirPodsService::class.java).apply {
                action = AirPodsService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service", e)
        }
    }
}

@Composable
fun PodifyApp(
    status: AirPodsStatus,
    isScanning: Boolean,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onStartScanning: () -> Unit,
    isBluetoothEnabled: () -> Boolean
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home",
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("home") {
            HomeScreen(
                status = status,
                isScanning = isScanning,
                permissionsGranted = permissionsGranted,
                onRequestPermissions = onRequestPermissions,
                onNavigateToSettings = { navController.navigate("settings") },
                isBluetoothEnabled = isBluetoothEnabled
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
