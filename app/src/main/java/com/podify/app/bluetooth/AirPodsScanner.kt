package com.podify.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import com.podify.app.util.AirPodsDataParser
import com.podify.app.util.AirPodsStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BLE Scanner for AirPods detection.
 * 
 * Based on OpenPods project approach:
 * - Uses proper scan filters for AirPods
 * - Tracks recent beacons and selects strongest signal
 * - Filters by RSSI to avoid detecting other people's AirPods
 * - Handles MAC address randomization
 */
@SuppressLint("MissingPermission")
class AirPodsScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "AirPodsScanner"
        
        // Keep beacons from last 10 seconds
        private const val RECENT_BEACONS_MAX_NS = 10_000_000_000L
        
        // AirPods UUIDs for identifying paired devices
        val AIRPODS_UUIDS = listOf(
            ParcelUuid.fromString("74ec2172-0bad-4d01-8f77-997b2be0722a"),
            ParcelUuid.fromString("2a72e02b-7b99-778f-014d-ad0b7221ec74")
        )
    }
    
    private var bluetoothScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    
    private val recentBeacons = mutableListOf<ScanResult>()
    
    private val _status = MutableStateFlow(AirPodsStatus.DISCONNECTED)
    val status: StateFlow<AirPodsStatus> = _status.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val bluetoothAdapter: BluetoothAdapter?
        get() {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            return manager?.adapter
        }
    
    /**
     * Start scanning for AirPods
     */
    fun startScanning() {
        try {
            Log.d(TAG, "Starting AirPods scanner")
            
            val adapter = bluetoothAdapter
            if (adapter == null) {
                Log.e(TAG, "Bluetooth adapter is null")
                return
            }
            
            if (!adapter.isEnabled) {
                Log.w(TAG, "Bluetooth is disabled")
                return
            }
            
            // Stop any existing scan
            stopScanning()
            
            bluetoothScanner = adapter.bluetoothLeScanner
            if (bluetoothScanner == null) {
                Log.e(TAG, "BLE Scanner is null")
                return
            }
            
            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(1) // Don't use 0, causes issues on some devices
                .build()
            
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    handleScanResult(result)
                }
                
                override fun onBatchScanResults(results: List<ScanResult>) {
                    results.forEach { handleScanResult(it) }
                }
                
                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "Scan failed with error: $errorCode")
                    _isScanning.value = false
                }
            }
            
            bluetoothScanner?.startScan(
                AirPodsDataParser.getScanFilters(),
                scanSettings,
                scanCallback
            )
            
            _isScanning.value = true
            Log.d(TAG, "Scan started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan", e)
            _isScanning.value = false
        }
    }
    
    /**
     * Stop scanning
     */
    fun stopScanning() {
        try {
            scanCallback?.let { callback ->
                bluetoothScanner?.stopScan(callback)
            }
            scanCallback = null
            _isScanning.value = false
            Log.d(TAG, "Scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        }
    }
    
    /**
     * Handle incoming scan result
     */
    private fun handleScanResult(result: ScanResult) {
        try {
            if (!AirPodsDataParser.isAirPodsResult(result)) {
                return
            }
            
            Log.d(TAG, "AirPods beacon: ${result.rssi}dB from ${result.device.address}")
            
            // Get the best (strongest) result from recent beacons
            val bestResult = getBestResult(result)
            if (bestResult == null || bestResult.rssi < AirPodsDataParser.MIN_RSSI) {
                Log.d(TAG, "Signal too weak: ${bestResult?.rssi}dB < ${AirPodsDataParser.MIN_RSSI}dB")
                return
            }
            
            // Parse the status
            val status = AirPodsDataParser.parse(bestResult)
            if (status != null && status.isValid) {
                _status.value = status
                _isConnected.value = true
                Log.d(TAG, "Status updated: L=${status.leftBattery}% R=${status.rightBattery}% C=${status.caseBattery}%")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling scan result", e)
        }
    }
    
    /**
     * Get the strongest beacon from recent results.
     * This handles MAC address randomization by picking the strongest signal.
     */
    private fun getBestResult(result: ScanResult): ScanResult? {
        synchronized(recentBeacons) {
            recentBeacons.add(result)
            
            var strongestBeacon: ScanResult? = null
            val now = SystemClock.elapsedRealtimeNanos()
            
            val iterator = recentBeacons.iterator()
            while (iterator.hasNext()) {
                val beacon = iterator.next()
                
                // Remove old beacons
                if (now - beacon.timestampNanos > RECENT_BEACONS_MAX_NS) {
                    iterator.remove()
                    continue
                }
                
                // Track strongest beacon
                if (strongestBeacon == null || strongestBeacon.rssi < beacon.rssi) {
                    strongestBeacon = beacon
                }
            }
            
            // If the strongest beacon has the same address as current, use current
            if (strongestBeacon != null && 
                strongestBeacon.device.address == result.device.address) {
                return result
            }
            
            return strongestBeacon
        }
    }
    
    /**
     * Check if a BluetoothDevice is an AirPods device
     */
    fun isAirPodsDevice(device: BluetoothDevice): Boolean {
        val uuids = device.uuids ?: return false
        return uuids.any { uuid -> AIRPODS_UUIDS.contains(uuid) }
    }
    
    /**
     * Check if AirPods are already connected via classic Bluetooth
     */
    fun checkAlreadyConnected() {
        try {
            val adapter = bluetoothAdapter ?: return
            
            adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val devices = proxy.connectedDevices
                    for (device in devices) {
                        if (isAirPodsDevice(device)) {
                            Log.d(TAG, "AirPods already connected: ${device.name}")
                            _isConnected.value = true
                            break
                        }
                    }
                    adapter.closeProfileProxy(profile, proxy)
                }
                
                override fun onServiceDisconnected(profile: Int) {
                    // Ignored
                }
            }, BluetoothProfile.HEADSET)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connected devices", e)
        }
    }
    
    /**
     * Get paired AirPods devices
     */
    fun getPairedAirPods(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices
                ?.filter { device ->
                    device.name?.lowercase()?.contains("airpods") == true ||
                    isAirPodsDevice(device)
                }
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired devices", e)
            emptyList()
        }
    }
    
    /**
     * Reset status to disconnected
     */
    fun setDisconnected() {
        _status.value = AirPodsStatus.DISCONNECTED
        _isConnected.value = false
    }
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
}
