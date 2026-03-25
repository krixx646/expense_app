package com.chelumore.xpenze.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = YellowPrimary,
    onPrimary = BlackBackground,
    primaryContainer = YellowSecondary,
    onPrimaryContainer = BlackBackground,
    secondary = YellowSecondary,
    onSecondary = BlackBackground,
    tertiary = InfoBlue,
    background = BlackBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    outline = DividerColor
)

// We focus on Dark Theme as requested, but providing a Light fallback mapping
private val LightColorScheme = lightColorScheme(
    primary = YellowPrimary,
    onPrimary = BlackBackground,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF111418),
    surface = Color.White,
    onSurface = Color(0xFF111418)
)

@Composable
@Suppress("DEPRECATION")
fun XpenzeTheme(
    darkTheme: Boolean = true, // Force dark theme by default for now as requested
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce Brand Theme
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
            // Keep system bars consistently brand-black to avoid light strips
            // when per-screen themes differ (e.g. auth/onboarding vs app light mode).
            window.statusBarColor = BlackBackground.toArgb()
            window.navigationBarColor = BlackBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes
    ) {
        CompositionLocalProvider(
            LocalDimensions provides Dimensions()
        ) {
            content()
        }
    }
}
