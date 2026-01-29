package com.podify.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.podify.app.ui.theme.*

private enum class BatteryCategory {
    UNKNOWN, CRITICAL, LOW, MEDIUM, HIGH
}

/**
 * Circular battery indicator with animated fill and gradient colors.
 * Displays battery percentage in the center with appropriate color coding.
 */
@Composable
fun CircularBatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    label: String? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (batteryLevel >= 0) batteryLevel / 100f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "battery_progress"
    )
    
    val category = when {
        batteryLevel < 0 -> BatteryCategory.UNKNOWN
        batteryLevel <= 10 -> BatteryCategory.CRITICAL
        batteryLevel <= 20 -> BatteryCategory.LOW
        batteryLevel <= 50 -> BatteryCategory.MEDIUM
        else -> BatteryCategory.HIGH
    }
    
    val batteryColor by animateColorAsState(
        targetValue = when (category) {
            BatteryCategory.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            BatteryCategory.CRITICAL -> BatteryCritical
            BatteryCategory.LOW -> BatteryLow
            BatteryCategory.MEDIUM -> BatteryMedium
            BatteryCategory.HIGH -> BatteryHigh
        },
        animationSpec = tween(500),
        label = "battery_color"
    )
    
    // Charging animation
    val infiniteTransition = rememberInfiniteTransition(label = "charging")
    val chargingPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "charging_pulse"
    )
    
    val chargingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "charging_rotation"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = strokeWidth.toPx()
                val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
                val topLeft = Offset(strokePx / 2, strokePx / 2)
                
                // Background track
                drawArc(
                    color = batteryColor.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
                
                // Progress arc with gradient
                if (animatedProgress > 0) {
                    if (isCharging) {
                        rotate(chargingRotation) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        batteryColor,
                                        batteryColor.copy(alpha = 0.3f),
                                        batteryColor
                                    )
                                ),
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = strokePx * (if (isCharging) chargingPulse else 1f),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    } else {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    batteryColor.copy(alpha = 0.6f),
                                    batteryColor,
                                    batteryColor
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = animatedProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
            
            // Center text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (batteryLevel >= 0) {
                    Text(
                        text = "$batteryLevel",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (size.value * 0.28f).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "--",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isCharging) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡",
                    style = MaterialTheme.typography.labelSmall
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "Charging",
                    style = MaterialTheme.typography.labelSmall,
                    color = BatteryHigh
                )
            }
        }
    }
}

/**
 * Horizontal battery bar indicator
 */
@Composable
fun BatteryBar(
    batteryLevel: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 24.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (batteryLevel >= 0) batteryLevel / 100f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "battery_bar_progress"
    )
    
    val batteryColor = when {
        batteryLevel < 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        batteryLevel <= 10 -> BatteryCritical
        batteryLevel <= 20 -> BatteryLow
        batteryLevel <= 50 -> BatteryMedium
        else -> BatteryHigh
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(batteryColor.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                batteryColor.copy(alpha = 0.7f),
                                batteryColor
                            )
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = if (batteryLevel >= 0) "$batteryLevel%" else "--",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = batteryColor
        )
        
        if (isCharging) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "⚡",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
