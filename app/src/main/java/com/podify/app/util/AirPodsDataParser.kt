package com.podify.app.util

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult

/**
 * Parser for AirPods BLE advertising data.
 * 
 * Based on research from:
 * - OpenPods project
 * - hexway/apple_bleee research
 * - CAPods project
 * 
 * ## AirPods Proximity Pairing Message structure (27 bytes):
 * 
 * Byte 0: 0x07 (Proximity Pairing message type)
 * Byte 1: 0x19 (25 = remaining length)
 * Byte 2: 0x01 (fixed)
 * Byte 3-4: Device model (e.g., 0x0220 = AirPods)
 * Byte 5: UTP (Status flags - in ear, in case, etc.)
 * Byte 6: Battery1 (Left battery in upper nibble, Right battery in lower nibble)
 * Byte 7: Battery2 (Case battery in upper nibble, charging flags in lower nibble)
 * Byte 8: Lid open counter
 * Byte 9: Device color
 * Byte 10: 0x00 (fixed)
 * Byte 11-26: Encrypted payload
 */
object AirPodsDataParser {
    
    private const val TAG = "AirPodsDataParser"
    
    const val APPLE_MANUFACTURER_ID = 76 // 0x004C
    const val AIRPODS_DATA_LENGTH = 27
    const val MIN_RSSI = -60
    
    // AirPods state mapping from hexway/apple_bleee research
    // UTP byte (byte 5) indicates the current state
    private val AIRPODS_STATES = mapOf(
        0x00 to AirPodsWearingState.CASE_CLOSED,
        0x01 to AirPodsWearingState.ALL_OUT,
        0x02 to AirPodsWearingState.LEFT_OUT,
        0x03 to AirPodsWearingState.LEFT_OUT,
        0x05 to AirPodsWearingState.RIGHT_OUT,
        0x09 to AirPodsWearingState.RIGHT_OUT,
        0x0B to AirPodsWearingState.BOTH_IN_EAR,
        0x11 to AirPodsWearingState.RIGHT_OUT,
        0x13 to AirPodsWearingState.RIGHT_IN_EAR,
        0x15 to AirPodsWearingState.RIGHT_IN_CASE,
        0x20 to AirPodsWearingState.LEFT_OUT,
        0x21 to AirPodsWearingState.ALL_OUT,
        0x22 to AirPodsWearingState.LEFT_OUT_CASE_OPEN,
        0x23 to AirPodsWearingState.RIGHT_OUT,
        0x29 to AirPodsWearingState.LEFT_OUT,
        0x2B to AirPodsWearingState.BOTH_IN_EAR,
        0x31 to AirPodsWearingState.LEFT_OUT_CASE_OPEN,
        0x33 to AirPodsWearingState.LEFT_OUT_CASE_OPEN,
        0x50 to AirPodsWearingState.CASE_OPEN,
        0x51 to AirPodsWearingState.LEFT_OUT,
        0x53 to AirPodsWearingState.LEFT_IN_EAR,
        0x55 to AirPodsWearingState.CASE_OPEN,
        0x70 to AirPodsWearingState.CASE_OPEN,
        0x71 to AirPodsWearingState.RIGHT_OUT_CASE_OPEN,
        0x73 to AirPodsWearingState.RIGHT_OUT_CASE_OPEN,
        0x75 to AirPodsWearingState.CASE_OPEN
    )
    
    // Device models from proximity pairing
    private val DEVICE_MODELS = mapOf(
        0x0220 to AirPodsModel.AIRPODS_1,
        0x0F20 to AirPodsModel.AIRPODS_2,
        0x1320 to AirPodsModel.AIRPODS_3,
        0x0E20 to AirPodsModel.AIRPODS_PRO,
        0x1420 to AirPodsModel.AIRPODS_PRO_2,
        0x0A20 to AirPodsModel.AIRPODS_MAX,
        0x0320 to AirPodsModel.POWERBEATS_3,
        0x0520 to AirPodsModel.BEATS_X,
        0x0620 to AirPodsModel.BEATS_SOLO_3
    )
    
    // Device colors
    private val DEVICE_COLORS = mapOf(
        0x00 to "White",
        0x01 to "Black",
        0x02 to "Red",
        0x03 to "Blue",
        0x04 to "Pink",
        0x05 to "Gray",
        0x06 to "Silver",
        0x07 to "Gold",
        0x08 to "Rose Gold",
        0x09 to "Space Gray",
        0x0A to "Dark Blue",
        0x0B to "Light Blue",
        0x0C to "Yellow"
    )
    
    /**
     * Creates scan filters to detect AirPods BLE advertisements.
     * Filters for Apple manufacturer ID (76) with proximity pairing prefix (0x07, 0x19)
     */
    fun getScanFilters(): List<ScanFilter> {
        val manufacturerData = ByteArray(AIRPODS_DATA_LENGTH)
        val manufacturerDataMask = ByteArray(AIRPODS_DATA_LENGTH)
        
        // Byte 0: 0x07 = Proximity Pairing
        manufacturerData[0] = 0x07
        // Byte 1: 0x19 = 25 (length indicator)
        manufacturerData[1] = 0x19
        
        // Mask: only match first two bytes
        manufacturerDataMask[0] = 0xFF.toByte()
        manufacturerDataMask[1] = 0xFF.toByte()
        
        val filter = ScanFilter.Builder()
            .setManufacturerData(APPLE_MANUFACTURER_ID, manufacturerData, manufacturerDataMask)
            .build()
        
        return listOf(filter)
    }
    
    /**
     * Check if scan result is from AirPods
     */
    fun isAirPodsResult(result: ScanResult): Boolean {
        val data = result.scanRecord?.getManufacturerSpecificData(APPLE_MANUFACTURER_ID)
        return data != null && data.size == AIRPODS_DATA_LENGTH
    }
    
    /**
     * Parse AirPods status from BLE scan result
     */
    fun parse(result: ScanResult): AirPodsStatus? {
        val data = result.scanRecord?.getManufacturerSpecificData(APPLE_MANUFACTURER_ID)
            ?: return null
        
        if (data.size != AIRPODS_DATA_LENGTH) return null
        
        return try {
            parseManufacturerData(data)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error parsing AirPods data", e)
            null
        }
    }
    
    /**
     * Parse the 27-byte manufacturer data
     * 
     * Based on hexway/apple_bleee parse_airpods function:
     * 
     * Indexes (after 0x07 0x19 prefix):
     * - [2] = 0x01 (fixed)
     * - [3-4] = Device model (big endian)
     * - [5] = UTP (status/wearing state)
     * - [6] = Battery1: upper nibble = left, lower nibble = right
     * - [7] = Battery2: upper nibble = case, lower nibble = charging flags
     * - [8] = Lid open counter
     * - [9] = Device color
     * - [10] = 0x00 (fixed)
     * - [11-26] = Encrypted payload
     */
    private fun parseManufacturerData(data: ByteArray): AirPodsStatus {
        // Convert to hex string for debugging
        val hexString = data.joinToString("") { "%02X".format(it) }
        android.util.Log.d(TAG, "Raw data: $hexString")
        
        // Device model from bytes 3-4 (after prefix bytes 0-1)
        // Note: data[0] and data[1] are 0x07 0x19
        val modelCode = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = DEVICE_MODELS[modelCode] ?: AirPodsModel.UNKNOWN
        
        // UTP byte - wearing/charging state (byte 5)
        val utp = data[5].toInt() and 0xFF
        val wearingState = AIRPODS_STATES[utp] ?: AirPodsWearingState.UNKNOWN
        
        // Battery1 byte (byte 6)
        // Upper 4 bits = Left battery (0-10 scale, multiply by 10 for percentage)
        // Lower 4 bits = Right battery
        val battery1 = data[6].toInt() and 0xFF
        val leftRaw = (battery1 shr 4) and 0x0F
        val rightRaw = battery1 and 0x0F
        
        // Battery2 byte (byte 7)
        // Upper 4 bits = Case battery
        // Lower 4 bits = Charging flags
        val battery2 = data[7].toInt() and 0xFF
        val caseRaw = (battery2 shr 4) and 0x0F
        val chargingFlags = battery2 and 0x0F
        
        // Convert 0-10 scale to 0-100% (value 15 = unknown/unavailable)
        val leftBattery = if (leftRaw in 0..10) leftRaw * 10 else -1
        val rightBattery = if (rightRaw in 0..10) rightRaw * 10 else -1
        val caseBattery = if (caseRaw in 0..10) caseRaw * 10 else -1
        
        // Charging status from lower nibble of battery2
        // Bit 0: Left charging
        // Bit 1: Right charging  
        // Bit 2: Case charging
        val leftCharging = (chargingFlags and 0x01) != 0
        val rightCharging = (chargingFlags and 0x02) != 0
        val caseCharging = (chargingFlags and 0x04) != 0
        
        // Lid open counter (byte 8)
        val lidOpenCount = data[8].toInt() and 0xFF
        
        // Device color (byte 9)
        val colorCode = data[9].toInt() and 0xFF
        val color = DEVICE_COLORS[colorCode] ?: "Unknown"
        
        // Special case: if battery1 == 0x09, case is closed (from hexway research)
        val caseLidOpen = battery1 != 0x09 && (lidOpenCount > 0 || wearingState.isCaseOpen)
        
        // Determine in-ear status from wearing state
        val leftInEar = wearingState.isLeftInEar
        val rightInEar = wearingState.isRightInEar
        
        android.util.Log.d(TAG, "Parsed: model=$model, state=$wearingState, L=$leftBattery%, R=$rightBattery%, C=$caseBattery%, charging=L:$leftCharging R:$rightCharging C:$caseCharging")
        
        return AirPodsStatus(
            model = model,
            leftBattery = leftBattery,
            rightBattery = rightBattery,
            caseBattery = caseBattery,
            leftCharging = leftCharging,
            rightCharging = rightCharging,
            caseCharging = caseCharging,
            leftInEar = leftInEar,
            rightInEar = rightInEar,
            caseLidOpen = caseLidOpen,
            wearingState = wearingState,
            color = color,
            rawHex = hexString
        )
    }
    
    /**
     * Decode hex for debugging
     */
    fun decodeHex(data: ByteArray): String {
        return data.joinToString("") { "%02X".format(it) }
    }
}

/**
 * Parsed AirPods status
 */
data class AirPodsStatus(
    val model: AirPodsModel = AirPodsModel.UNKNOWN,
    val leftBattery: Int = -1,      // 0-100 or -1 if unknown
    val rightBattery: Int = -1,     // 0-100 or -1 if unknown  
    val caseBattery: Int = -1,      // 0-100 or -1 if unknown
    val leftCharging: Boolean = false,
    val rightCharging: Boolean = false,
    val caseCharging: Boolean = false,
    val leftInEar: Boolean = false,
    val rightInEar: Boolean = false,
    val caseLidOpen: Boolean = false,
    val wearingState: AirPodsWearingState = AirPodsWearingState.UNKNOWN,
    val color: String = "Unknown",
    val rawHex: String = ""
) {
    val isValid: Boolean
        get() = leftBattery >= 0 || rightBattery >= 0
    
    val isWearing: Boolean
        get() = leftInEar || rightInEar
    
    val bothInEar: Boolean
        get() = leftInEar && rightInEar
    
    companion object {
        val DISCONNECTED = AirPodsStatus()
    }
}

/**
 * Wearing state based on UTP byte from hexway/apple_bleee research
 */
enum class AirPodsWearingState(
    val displayName: String,
    val isLeftInEar: Boolean = false,
    val isRightInEar: Boolean = false,
    val isCaseOpen: Boolean = false
) {
    UNKNOWN("Unknown"),
    CASE_CLOSED("Case Closed"),
    CASE_OPEN("Case Open", isCaseOpen = true),
    ALL_OUT("All Out of Case"),
    LEFT_OUT("Left Out"),
    RIGHT_OUT("Right Out"),
    LEFT_IN_EAR("Left In Ear", isLeftInEar = true),
    RIGHT_IN_EAR("Right In Ear", isRightInEar = true),
    BOTH_IN_EAR("Both In Ear", isLeftInEar = true, isRightInEar = true),
    LEFT_IN_CASE("Left In Case"),
    RIGHT_IN_CASE("Right In Case"),
    LEFT_OUT_CASE_OPEN("Left Out, Case Open", isCaseOpen = true),
    RIGHT_OUT_CASE_OPEN("Right Out, Case Open", isCaseOpen = true)
}

enum class AirPodsModel(val displayName: String) {
    AIRPODS_1("AirPods"),
    AIRPODS_2("AirPods 2nd Gen"),
    AIRPODS_3("AirPods 3rd Gen"),
    AIRPODS_PRO("AirPods Pro"),
    AIRPODS_PRO_2("AirPods Pro 2"),
    AIRPODS_MAX("AirPods Max"),
    POWERBEATS_3("Powerbeats3"),
    BEATS_X("BeatsX"),
    BEATS_SOLO_3("Beats Solo3"),
    UNKNOWN("AirPods")
}
