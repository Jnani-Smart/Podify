package com.podify.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.podify.app.ui.components.AirPodsIllustration
import com.podify.app.ui.components.CircularBatteryIndicator
import com.podify.app.ui.components.GlassCard
import com.podify.app.ui.theme.PodifyTheme
import com.podify.app.util.AirPodsStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : Service(), SavedStateRegistryOwner {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.podify.app.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.podify.app.HIDE_OVERLAY"
        const val EXTRA_STATUS = "extra_status"
        
        private const val TAG = "OverlayService"
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    
    // Lifecycle components required for Compose in Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val viewModelStore = ViewModelStore()
    
    // State management
    private val _overlayState = mutableStateOf<AirPodsStatus?>(null)
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    
    // Auto-dismiss handler
    private var dismissJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService created")
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Initialize lifecycle
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                // We can't pass complex objects easily via intent extras often, 
                // but for now let's assume the service status is managed centrally 
                // or passed as serialized fields. 
                // Actually, a better way is to have the AirPodsService trigger this 
                // and maybe bind or use a shared singleton/flow. 
                // Simulating popup for now.
                Log.d(TAG, "Show overlay requested")
                showOverlay()
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
            }
        }
        return START_NOT_STICKY
    }
    
    // Exposed function to update state from AirPodsService
    fun updateStatus(status: AirPodsStatus) {
        if (_overlayState.value == null) {
            showOverlay()
        }
        _overlayState.value = status
        
        // Reset dismiss timer
        dismissJob?.cancel()
        dismissJob = serviceScope.launch {
            delay(5000) // Hide after 5 seconds of inactivity/stability
            hideOverlay()
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        
        Log.d(TAG, "Creating overlay view")
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 100 // Padding from bottom
        }

        overlayView = ComposeView(this).apply {
            // Set lifecycle owners
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore = this@OverlayService.viewModelStore
            })
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            
            setContent {
                PodifyTheme(darkTheme = true) {
                    val status by _overlayState
                    
                    if (status != null) {
                        OverlayContent(status!!)
                    }
                }
            }
        }
        
        try {
            windowManager.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view", e)
        }
    }

    private fun hideOverlay() {
        if (overlayView == null) return
        
        try {
            windowManager.removeView(overlayView)
            overlayView = null
            _overlayState.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view", e)
        }
    }

    override fun onDestroy() {
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return LocalBinder()
    }

    inner class LocalBinder : android.os.Binder() {
        fun getService(): OverlayService = this@OverlayService
    }
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}

@Composable
fun OverlayContent(status: AirPodsStatus) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Top Indicator
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                AirPodsIllustration(
                    size = 120.dp,
                    isConnected = true,
                    showCase = status.caseBattery >= 0
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = status.model.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CircularBatteryIndicator(
                        batteryLevel = status.leftBattery,
                        isCharging = status.leftCharging,
                        label = "Left",
                        size = 60.dp,
                        strokeWidth = 6.dp
                    )
                    
                    CircularBatteryIndicator(
                        batteryLevel = status.rightBattery,
                        isCharging = status.rightCharging,
                        label = "Right",
                        size = 60.dp,
                        strokeWidth = 6.dp
                    )
                    
                    if (status.caseBattery >= 0) {
                        CircularBatteryIndicator(
                            batteryLevel = status.caseBattery,
                            isCharging = status.caseCharging,
                            label = "Case",
                            size = 60.dp,
                            strokeWidth = 6.dp
                        )
                    }
                }
            }
        }
    }
}
