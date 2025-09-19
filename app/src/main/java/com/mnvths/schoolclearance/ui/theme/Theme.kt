// file: ui/theme/Theme.kt
package com.mnvths.schoolclearance.ui.theme

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

// 1. Your new custom colors inspired by the MNVTHS school logo
val MnvthsBlue = Color(0xFF0D47A1)
val MnvthsGold = Color(0xFFFFC107)
val MnvthsRed = Color(0xFFB71C1C)

// Optional: Lighter shades for dark theme
val MnvthsBlueDark = Color(0xFF5891F4)
val MnvthsGoldDark = Color(0xFFFFD54F)

// 2. Updated DarkColorScheme with school colors
private val DarkColorScheme = darkColorScheme(
    primary = MnvthsBlueDark,
    secondary = MnvthsGoldDark,
    tertiary = MnvthsRed
)

// 3. Updated LightColorScheme with school colors
private val LightColorScheme = lightColorScheme(
    primary = MnvthsBlue,
    secondary = MnvthsGold,
    tertiary = MnvthsRed,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun SchoolClearanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false if you ALWAYS want to use school colors
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assumes you have a Typography.kt file
        content = content
    )
}