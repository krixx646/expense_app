package com.chelumore.xpenze.core.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Safely unwrap a [Context] into an [Activity] when possible.
 *
 * Some devices/OS builds can provide a themed/wrapped context to Compose, so a direct cast
 * (`LocalContext.current as Activity`) can crash.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


