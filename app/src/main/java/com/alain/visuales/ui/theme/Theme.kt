package com.alain.visuales.ui.theme

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
import android.app.Activity
import android.os.Build

// Brand palette (kept from v1)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate300 = Color(0xFFCBD5E1)
private val Sky400 = Color(0xFF38BDF8)
private val Sky500 = Color(0xFF0EA5E9)
private val Sky600 = Color(0xFF0284C7)

// Dark scheme (default — the app is dark-first)
private val DarkColors = darkColorScheme(
    primary = Sky400,
    onPrimary = Slate900,
    primaryContainer = Sky600,
    onPrimaryContainer = Color.White,
    secondary = Sky500,
    onSecondary = Slate900,
    tertiary = Color(0xFFA78BFA),
    background = Slate900,
    onBackground = Color(0xFFE2E8F0),
    surface = Slate800,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300,
    error = Color(0xFFEF4444),
    onError = Color.White,
    outline = Slate700
)

// Light scheme (used if user forces light mode)
private val LightColors = lightColorScheme(
    primary = Sky600,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF075985),
    secondary = Sky500,
    onSecondary = Color.White,
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626),
    onError = Color.White,
    outline = Color(0xFFCBD5E1)
)

/**
 * App theme. Uses Material You dynamic color on Android 12+ unless [forceBrand] is true.
 */
@Composable
fun VisualesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    forceBrand: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        !forceBrand && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
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
        typography = VisualesTypography,
        shapes = VisualesShapes,
        content = content
    )
}
