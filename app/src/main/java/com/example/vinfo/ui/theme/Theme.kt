package com.example.vinfo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VinfoPrimary,
    primaryContainer = VinfoPrimaryContainer,
    onPrimary = Color.White,
    onPrimaryContainer = Color.White,
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF252A35),
    onSurfaceVariant = Color(0xFFC7CBD6),
    outlineVariant = Color(0xFF464C5C),
    error = Color(0xFFFFB4AB),
    background = Color(0xFF101318)
)

private val LightColorScheme = lightColorScheme(
    primary = VinfoPrimary,
    onPrimary = Color.White,
    primaryContainer = VinfoPrimaryContainer,
    onPrimaryContainer = Color.White,
    surface = VinfoSurface,
    onSurface = VinfoOnSurface,
    surfaceVariant = VinfoSurfaceVariant,
    onSurfaceVariant = VinfoOnSurfaceVariant,
    outlineVariant = VinfoOutlineVariant,
    error = VinfoError,
    background = VinfoSurface
)

@Composable
fun VinfoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to strictly follow the Vinfo Design System
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
