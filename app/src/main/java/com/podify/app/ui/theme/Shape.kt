package com.podify.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra small - for chips, small buttons
    extraSmall = RoundedCornerShape(8.dp),
    
    // Small - for text fields, small cards
    small = RoundedCornerShape(12.dp),
    
    // Medium - for cards, dialogs
    medium = RoundedCornerShape(16.dp),
    
    // Large - for bottom sheets, large cards
    large = RoundedCornerShape(24.dp),
    
    // Extra large - for full-screen modals
    extraLarge = RoundedCornerShape(32.dp)
)

// iOS-style corner radii
object PodifyCorners {
    val None = 0.dp
    val ExtraSmall = 6.dp
    val Small = 10.dp
    val Medium = 14.dp
    val Large = 20.dp
    val ExtraLarge = 28.dp
    val Full = 100.dp
}

// Standard spacing values
object PodifySpacing {
    val XXSmall = 2.dp
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Default = 16.dp
    val Large = 20.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp
    val XXXLarge = 48.dp
}
