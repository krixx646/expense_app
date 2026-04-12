package com.chelumore.xpenze.core.ui.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

fun Context.hasNotificationsPermissionEnabled(): Boolean {
    val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
    if (!notificationsEnabled) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

fun Context.openAppNotificationSettings() {
    val appIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(appIntent) }
        .onFailure { runCatching { startActivity(detailsIntent) } }
}

fun Context.hasNotificationListenerAccess(): Boolean {
    val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
    if (flat.isNullOrBlank()) return false
    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(flat) }
    for (entry in splitter) {
        if (entry.contains(packageName, ignoreCase = true)) {
            return true
        }
    }
    return false
}

fun Context.openNotificationListenerSettings() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
        .onFailure { openAppNotificationSettings() }
}

fun Context.isXiaomiFamilyDevice(): Boolean {
    val brand = Build.BRAND.orEmpty().lowercase()
    val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
    return manufacturer.contains("xiaomi") ||
        brand.contains("xiaomi") ||
        brand.contains("redmi") ||
        brand.contains("poco")
}

fun Context.openXiaomiAutoStartSettings() {
    val miuiIntent = Intent().apply {
        component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(miuiIntent) }
        .onFailure { runCatching { startActivity(detailsIntent) } }
}

