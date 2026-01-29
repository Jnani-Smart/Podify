package com.podify.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.podify.app.ui.theme.AppleBlue
import com.podify.app.ui.theme.AppleLightBlue
import com.podify.app.ui.theme.DarkSurfaceElevated
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimalist AirPods Pro illustration using Canvas.
 * Creates a clean, scalable vector representation that matches Apple's aesthetic.
 */
@Composable
fun AirPodsIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    isConnected: Boolean = false,
    showCase: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "airpods_anim")
    
    // Subtle floating animation when connected
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    // Glow pulse when connected
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val baseColor = MaterialTheme.colorScheme.onSurface
    val accentColor = AppleBlue
    val glowColor = AppleLightBlue
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isConnected) floatOffset.dp else 0.dp)
        ) {
            val width = this.size.width
            val height = this.size.height
            val centerX = width / 2
            val centerY = height / 2
            
            // Draw connection glow when connected
            if (isConnected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = width * 0.6f
                    ),
                    radius = width * 0.6f,
                    center = Offset(centerX, centerY)
                )
            }
            
            // Draw case if requested
            if (showCase) {
                drawAirPodsCase(
                    centerX = centerX,
                    centerY = centerY + height * 0.15f,
                    caseWidth = width * 0.55f,
                    caseHeight = height * 0.4f,
                    color = baseColor
                )
            }
            
            // Left AirPod
            drawAirPod(
                centerX = centerX - width * 0.18f,
                centerY = if (showCase) centerY - height * 0.1f else centerY,
                podWidth = width * 0.14f,
                podHeight = height * 0.35f,
                color = baseColor,
                accentColor = if (isConnected) accentColor else baseColor.copy(alpha = 0.5f),
                isLeft = true
            )
            
            // Right AirPod
            drawAirPod(
                centerX = centerX + width * 0.18f,
                centerY = if (showCase) centerY - height * 0.1f else centerY,
                podWidth = width * 0.14f,
                podHeight = height * 0.35f,
                color = baseColor,
                accentColor = if (isConnected) accentColor else baseColor.copy(alpha = 0.5f),
                isLeft = false
            )
        }
    }
}

private fun DrawScope.drawAirPod(
    centerX: Float,
    centerY: Float,
    podWidth: Float,
    podHeight: Float,
    color: Color,
    accentColor: Color,
    isLeft: Boolean
) {
    val stemWidth = podWidth * 0.3f
    val stemHeight = podHeight * 0.5f
    val headRadius = podWidth * 0.7f
    
    // Stem
    val stemPath = Path().apply {
        val offsetX = if (isLeft) -stemWidth * 0.3f else stemWidth * 0.3f
        moveTo(centerX + offsetX, centerY)
        lineTo(centerX + offsetX + stemWidth / 2, centerY)
        lineTo(centerX + offsetX + stemWidth / 2, centerY + stemHeight)
        lineTo(centerX + offsetX - stemWidth / 2, centerY + stemHeight)
        lineTo(centerX + offsetX - stemWidth / 2, centerY)
        close()
    }
    
    drawPath(
        path = stemPath,
        color = color,
        style = Stroke(width = 3f, cap = StrokeCap.Round)
    )
    
    // Head (ear piece)
    drawCircle(
        color = color,
        radius = headRadius,
        center = Offset(centerX, centerY - headRadius * 0.3f),
        style = Stroke(width = 3f)
    )
    
    // Inner detail
    drawCircle(
        color = accentColor,
        radius = headRadius * 0.4f,
        center = Offset(centerX, centerY - headRadius * 0.3f)
    )
    
    // Sensor dot
    drawCircle(
        color = color.copy(alpha = 0.5f),
        radius = stemWidth * 0.25f,
        center = Offset(
            centerX + if (isLeft) -stemWidth * 0.15f else stemWidth * 0.15f,
            centerY + stemHeight * 0.3f
        )
    )
}

private fun DrawScope.drawAirPodsCase(
    centerX: Float,
    centerY: Float,
    caseWidth: Float,
    caseHeight: Float,
    color: Color
) {
    val cornerRadius = caseHeight * 0.3f
    
    // Case outline
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX - caseWidth / 2, centerY - caseHeight / 2),
        size = androidx.compose.ui.geometry.Size(caseWidth, caseHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = 3f)
    )
    
    // Lid line
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(centerX - caseWidth * 0.35f, centerY - caseHeight * 0.2f),
        end = Offset(centerX + caseWidth * 0.35f, centerY - caseHeight * 0.2f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )
    
    // Status LED indicator
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = caseWidth * 0.03f,
        center = Offset(centerX, centerY + caseHeight * 0.25f)
    )
}

/**
 * Animated connecting dots
 */
@Composable
fun ConnectingAnimation(
    modifier: Modifier = Modifier,
    dotCount: Int = 3,
    color: Color = AppleBlue
) {
    val infiniteTransition = rememberInfiniteTransition(label = "connecting")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(dotCount) { index ->
            val delay = index * 150
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            
            Canvas(modifier = Modifier.size(10.dp)) {
                drawCircle(
                    color = color,
                    radius = size.minDimension / 2 * scale
                )
            }
        }
    }
}

/**
 * Ripple effect animation for connection status
 */
@Composable
fun ConnectionRipple(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    color: Color = AppleBlue
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    
    val ripples = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, delayMillis = index * 600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ripple_$index"
        )
    }
    
    if (isActive) {
        Canvas(modifier = modifier.size(120.dp)) {
            val maxRadius = size.minDimension / 2
            
            ripples.forEach { animatedValue ->
                val progress = animatedValue.value
                val radius = maxRadius * progress
                val alpha = (1f - progress) * 0.4f
                
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}
