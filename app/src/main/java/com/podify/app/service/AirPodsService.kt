package com.podify.app.service

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.podify.app.MainActivity
import com.podify.app.R
import com.podify.app.bluetooth.AirPodsScanner
import com.podify.app.util.AirPodsStatus
import com.podify.app.util.MediaControlHelper
import com.podify.app.util.PermissionHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Foreground service for persistent AirPods monitoring.
 * Handles:
 * - Scanning
 * - Notifications
 * - Media Control (Auto Play/Pause)
 * - Overlay triggering
 */
class AirPodsService : Service() {
    
    companion object {
        private const val TAG = "AirPodsService"
        const val ACTION_START = "com.podify.app.START_SERVICE"
        const val ACTION_STOP = "com.podify.app.STOP_SERVICE"
        
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "airpods_service"
    }
    
    private var scanner: AirPodsScanner? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var mediaControlHelper: MediaControlHelper? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Overlay Service Binding
    private var overlayService: OverlayService? = null
    private var isOverlayBound = false
    
    private val overlayConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as OverlayService.LocalBinder
            overlayService = binder.getService()
            isOverlayBound = true
            Log.d(TAG, "OverlayService bound")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isOverlayBound = false
            overlayService = null
            Log.d(TAG, "OverlayService disconnected")
        }
    }
    
    // State tracking to trigger popup
    private var lastCaseOpenState = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Create notification channel
        createNotificationChannel()
        
        // Start as foreground
        startForeground(NOTIFICATION_ID, createNotification(null))
        
        // Initialize helpers
        scanner = AirPodsScanner(this)
        mediaControlHelper = MediaControlHelper(this)
        
        // Register Bluetooth receiver
        registerBluetoothReceiver()
        
        // Bind to OverlayService
        bindOverlayService()
        
        // Check if already connected
        checkAlreadyConnected()
        
        // Start scanning if Bluetooth is on
        if (isBluetoothEnabled()) {
            startScanning()
        }
        
        // Observe status changes
        serviceScope.launch {
            scanner?.status?.collectLatest { status ->
                if (status.isValid) {
                    updateNotification(status)
                    mediaControlHelper?.onStateChanged(status)
                    handleOverlayTrigger(status)
                }
            }
        }
    }
    
    private fun bindOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        bindService(intent, overlayConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun handleOverlayTrigger(status: AirPodsStatus) {
        // Trigger popup if:
        // 1. Case just opened (status.caseLidOpen became true)
        // 2. We have overlay permissions
        
        if (PermissionHelper.hasOverlayPermission(this)) {
            if (status.caseLidOpen && !lastCaseOpenState) {
                Log.d(TAG, "Case opened -> Triggering overlay")
                overlayService?.updateStatus(status)
            } else if (isOverlayBound && overlayService != null) {
                // Determine if we should update the existing overlay if it's visible
                // For now, let's just push updates if it's already showing
                overlayService?.updateStatus(status)
            }
        }
        
        lastCaseOpenState = status.caseLidOpen
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        if (isOverlayBound) {
            unbindService(overlayConnection)
            isOverlayBound = false
        }
        
        serviceScope.cancel()
        scanner?.stopScanning()
        unregisterBluetoothReceiver()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AirPods Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows AirPods connection status and battery levels"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(status: AirPodsStatus?): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val title: String
        val text: String
        
        if (status != null && status.isValid) {
            title = status.model.displayName
            text = buildString {
                if (status.leftBattery >= 0) append("L:${status.leftBattery}% ")
                if (status.rightBattery >= 0) append("R:${status.rightBattery}% ")
                if (status.caseBattery >= 0) append("C:${status.caseBattery}%")
            }
        } else {
            title = "Podify"
            text = "Scanning for AirPods..."
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(status: AirPodsStatus) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, createNotification(status))
    }
    
    @SuppressLint("MissingPermission")
    private fun registerBluetoothReceiver() {
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        when (state) {
                            BluetoothAdapter.STATE_ON -> {
                                Log.d(TAG, "Bluetooth ON")
                                startScanning()
                            }
                            BluetoothAdapter.STATE_OFF -> {
                                Log.d(TAG, "Bluetooth OFF")
                                scanner?.stopScanning()
                                scanner?.setDisconnected()
                            }
                        }
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && isAirPodsDevice(device)) {
                            Log.d(TAG, "AirPods connected: ${device.name}")
                            // We can trigger an immediate scan or update here
                            scanner?.startScanning()
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (device != null && isAirPodsDevice(device)) {
                            Log.d(TAG, "AirPods disconnected: ${device.name}")
                            scanner?.setDisconnected()
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, filter)
        }
    }
    
    private fun unregisterBluetoothReceiver() {
        try {
            bluetoothReceiver?.let { unregisterReceiver(it) }
            bluetoothReceiver = null
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun isAirPodsDevice(device: BluetoothDevice): Boolean {
        // Check by name
        if (device.name?.lowercase()?.contains("airpods") == true) {
            return true
        }
        
        // Check by UUID
        val uuids = device.uuids ?: return false
        return uuids.any { uuid -> 
            AirPodsScanner.AIRPODS_UUIDS.contains(uuid) 
        }
    }
    
    private fun isBluetoothEnabled(): Boolean {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }
    
    private fun startScanning() {
        if (PermissionHelper.hasBluetoothPermissions(this)) {
            Log.d(TAG, "Starting scanner")
            scanner?.startScanning()
        } else {
            Log.w(TAG, "Missing Bluetooth permissions")
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun checkAlreadyConnected() {
        try {
            val manager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter ?: return
            
            adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    for (device in proxy.connectedDevices) {
                        if (isAirPodsDevice(device)) {
                            Log.d(TAG, "AirPods already connected: ${device.name}")
                            break
                        }
                    }
                    adapter.closeProfileProxy(profile, proxy)
                }
                
                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.HEADSET)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connected devices", e)
        }
    }
}
