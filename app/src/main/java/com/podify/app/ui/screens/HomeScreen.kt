package com.podify.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.podify.app.ui.components.*
import com.podify.app.ui.theme.*
import com.podify.app.util.AirPodsStatus

/**
 * Main home screen displaying AirPods status
 */
@Composable
fun HomeScreen(
    status: AirPodsStatus,
    isScanning: Boolean,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isBluetoothEnabled: () -> Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            !permissionsGranted -> {
                PermissionRequestScreen(onRequestPermissions = onRequestPermissions)
            }
            !isBluetoothEnabled() -> {
                BluetoothDisabledScreen()
            }
            else -> {
                HomeContent(
                    status = status,
                    isScanning = isScanning,
                    onNavigateToSettings = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    status: AirPodsStatus,
    isScanning: Boolean,
    onNavigateToSettings: () -> Unit
) {
    val isConnected = status.isValid
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PodifySpacing.Default),
        contentPadding = PaddingValues(
            top = PodifySpacing.XXXLarge,
            bottom = PodifySpacing.XXLarge
        ),
        verticalArrangement = Arrangement.spacedBy(PodifySpacing.Default)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Podify",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isConnected) "Connected" else if (isScanning) "Searching..." else "Not Connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConnected) AppleGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        }
        
        // Main AirPods Card
        item {
            AirPodsStatusCard(
                status = status,
                isScanning = isScanning
            )
        }
        
        // Battery Details
        if (isConnected) {
            item {
                BatteryDetailsCard(status = status)
            }
        }
        
        // Features Card
        item {
            FeaturesCard()
        }
        
        // Debug Info (remove in production)
        if (status.rawHex.isNotEmpty()) {
            item {
                DebugCard(status = status)
            }
        }
    }
}

@Composable
private fun AirPodsStatusCard(
    status: AirPodsStatus,
    isScanning: Boolean
) {
    val isConnected = status.isValid
    
    GradientCard(
        gradientColors = if (isConnected) {
            listOf(AirPodsGradientStart, AirPodsGradientMiddle, AirPodsGradientEnd)
        } else {
            listOf(DarkSurfaceElevated, DarkSurfaceVariant)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(PodifySpacing.Large)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isConnected) {
                    ConnectionRipple(isActive = true, color = Color.White.copy(alpha = 0.3f))
                }
                
                AirPodsIllustration(
                    size = 160.dp,
                    isConnected = isConnected,
                    showCase = status.caseBattery >= 0
                )
            }
            
            Spacer(modifier = Modifier.height(PodifySpacing.Large))
            
            Text(
                text = status.model.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(PodifySpacing.Small))
            
            AnimatedContent(
                targetState = isConnected,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "status"
            ) { connected ->
                if (connected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(PodifySpacing.Default),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BatteryPill("L", status.leftBattery, status.leftCharging)
                        BatteryPill("R", status.rightBattery, status.rightCharging)
                        if (status.caseBattery >= 0) {
                            BatteryPill("Case", status.caseBattery, status.caseCharging)
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isScanning) {
                            ConnectingAnimation(color = Color.White)
                            Spacer(modifier = Modifier.width(PodifySpacing.Small))
                        }
                        Text(
                            text = if (isScanning) "Searching for AirPods..." else "Open AirPods case nearby",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryPill(
    label: String,
    level: Int,
    isCharging: Boolean
) {
    val color = when {
        level < 0 -> Color.White.copy(alpha = 0.4f)
        level <= 10 -> BatteryCritical
        level <= 20 -> BatteryLow
        else -> Color.White
    }
    
    Surface(
        shape = RoundedCornerShape(PodifyCorners.Full),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (level >= 0) "$level%" else "--",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (isCharging) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "⚡", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun BatteryDetailsCard(status: AirPodsStatus) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Battery Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.Default))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CircularBatteryIndicator(
                batteryLevel = status.leftBattery,
                isCharging = status.leftCharging,
                label = "Left",
                size = 80.dp
            )
            
            CircularBatteryIndicator(
                batteryLevel = status.rightBattery,
                isCharging = status.rightCharging,
                label = "Right",
                size = 80.dp
            )
            
            CircularBatteryIndicator(
                batteryLevel = status.caseBattery,
                isCharging = status.caseCharging,
                label = "Case",
                size = 80.dp
            )
        }
        
        Spacer(modifier = Modifier.height(PodifySpacing.Default))
        
        // Status indicator based on wearing state
        val (statusText, statusColor, statusIcon) = when {
            status.bothInEar -> Triple(
                "Both AirPods in ear",
                AppleGreen,
                Icons.Outlined.Headphones
            )
            status.leftInEar && !status.rightInEar -> Triple(
                "Left AirPod in ear",
                AppleBlue,
                Icons.Outlined.Headphones
            )
            !status.leftInEar && status.rightInEar -> Triple(
                "Right AirPod in ear",
                AppleBlue,
                Icons.Outlined.Headphones
            )
            status.caseLidOpen -> Triple(
                "Case open",
                AppleOrange,
                Icons.Outlined.Inventory2
            )
            status.leftCharging || status.rightCharging || status.caseCharging -> Triple(
                "Charging",
                AppleGreen,
                Icons.Outlined.BatteryChargingFull
            )
            else -> Triple(
                status.wearingState.displayName,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Outlined.Bluetooth
            )
        }
        
        Surface(
            shape = RoundedCornerShape(PodifyCorners.Small),
            color = statusColor.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(PodifySpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(PodifySpacing.Small))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun FeaturesCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.Medium))
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FeatureRow(
                icon = Icons.Outlined.BatteryFull,
                title = "Battery Monitoring",
                description = "View left, right, and case levels",
                isActive = true
            )
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            
            FeatureRow(
                icon = Icons.Outlined.Sensors,
                title = "In-Ear Detection",
                description = "Auto play/pause (handled by Podify)",
                isActive = true
            )
            
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            
            FeatureRow(
                icon = Icons.Outlined.BluetoothConnected,
                title = "Background Scanning",
                description = "Detects AirPods when case opens",
                isActive = true
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PodifySpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(PodifyCorners.Small))
                .background(
                    if (isActive) AppleBlue.copy(alpha = 0.1f) 
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) AppleBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(PodifySpacing.Medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = AppleGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DebugCard(status: AirPodsStatus) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Debug Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.Small))
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "State: ${status.wearingState.name}",
                style = MaterialTheme.typography.bodySmall,
                color = AppleBlue
            )
            Text(
                text = "In Ear: L=${status.leftInEar} R=${status.rightInEar}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Charging: L=${status.leftCharging} R=${status.rightCharging} C=${status.caseCharging}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Case Lid: ${if (status.caseLidOpen) "Open" else "Closed"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Color: ${status.color}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Raw: ${status.rawHex}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PodifySpacing.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = AppleBlue
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.XLarge))
        
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.Default))
        
        Text(
            text = "Podify needs the following permissions to detect your AirPods:\n\n" +
                   "• Nearby Devices - To scan for AirPods\n" +
                   "• Location - Required for Bluetooth scanning\n" +
                   "• Notifications - For battery alerts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.XXLarge))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(PodifyCorners.Medium)
        ) {
            Text(
                text = "Grant Permissions",
                modifier = Modifier.padding(vertical = PodifySpacing.Small)
            )
        }
    }
}

@Composable
private fun BluetoothDisabledScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PodifySpacing.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.BluetoothDisabled,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.XLarge))
        
        Text(
            text = "Bluetooth Disabled",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(PodifySpacing.Default))
        
        Text(
            text = "Please enable Bluetooth in your device settings to use Podify.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
