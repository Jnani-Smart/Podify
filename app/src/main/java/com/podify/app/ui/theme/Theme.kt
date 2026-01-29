package com.podify.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = AppleBlue.copy(alpha = 0.2f),
    onPrimaryContainer = AppleLightBlue,
    secondary = AppleTeal,
    onSecondary = Color.White,
    secondaryContainer = AppleTeal.copy(alpha = 0.2f),
    onSecondaryContainer = AppleTeal,
    tertiary = ApplePurple,
    onTertiary = Color.White,
    tertiaryContainer = ApplePurple.copy(alpha = 0.2f),
    onTertiaryContainer = ApplePurple,
    error = AppleRed,
    onError = Color.White,
    errorContainer = AppleRed.copy(alpha = 0.2f),
    onErrorContainer = AppleRed,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkDivider,
    outlineVariant = DarkDivider.copy(alpha = 0.5f),
    scrim = Color.Black.copy(alpha = 0.5f),
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    inversePrimary = AppleBlue
)

private val LightColorScheme = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = AppleBlue.copy(alpha = 0.1f),
    onPrimaryContainer = AppleBlue,
    secondary = AppleTeal,
    onSecondary = Color.White,
    secondaryContainer = AppleTeal.copy(alpha = 0.1f),
    onSecondaryContainer = AppleTeal,
    tertiary = ApplePurple,
    onTertiary = Color.White,
    tertiaryContainer = ApplePurple.copy(alpha = 0.1f),
    onTertiaryContainer = ApplePurple,
    error = AppleRed,
    onError = Color.White,
    errorContainer = AppleRed.copy(alpha = 0.1f),
    onErrorContainer = AppleRed,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightDivider,
    outlineVariant = LightDivider.copy(alpha = 0.5f),
    scrim = Color.Black.copy(alpha = 0.3f),
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    inversePrimary = AppleLightBlue
)

@Composable
fun PodifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
