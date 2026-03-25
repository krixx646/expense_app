package com.chelumore.xpenze.core.ui.util

import androidx.compose.ui.graphics.Color

fun parseHexColorOrFallback(hex: String?, fallback: Color): Color {
    return try {
        if (hex.isNullOrBlank()) fallback else Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}




