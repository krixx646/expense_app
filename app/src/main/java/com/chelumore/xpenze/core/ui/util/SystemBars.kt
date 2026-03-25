package com.chelumore.xpenze.core.ui.util

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Sets status + navigation bar colors for the current screen.
 *
 * Useful when a screen uses a fixed background (e.g. auth screens) and you want the system bars
 * to visually blend in (no white/grey "cut off" areas).
 */
@Composable
@Suppress("DEPRECATION")
fun SystemBars(
    color: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    // Capture current values so we can restore when this screen leaves composition.
    // NOTE: Actual bar updates happen in SideEffect (runs after theme SideEffect), so it wins.
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        val previousStatus = window.statusBarColor
        val previousNav = window.navigationBarColor
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNav = controller.isAppearanceLightNavigationBars

        val previousNavContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced else null
        val previousStatusContrast = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isStatusBarContrastEnforced else null

        onDispose {
            window.statusBarColor = previousStatus
            window.navigationBarColor = previousNav
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNav

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                previousNavContrast?.let { window.isNavigationBarContrastEnforced = it }
                previousStatusContrast?.let { window.isStatusBarContrastEnforced = it }
            }
        }
    }

    // Apply every recomposition, after parent/theme SideEffect.
    SideEffect {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)

        window.statusBarColor = color.toArgb()
        window.navigationBarColor = color.toArgb()

        // Avoid OEM/system "contrast enforcement" turning dark bars into light/grey scrims.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
    }
}


