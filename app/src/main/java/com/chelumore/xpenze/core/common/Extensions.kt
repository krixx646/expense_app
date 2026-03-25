package com.chelumore.xpenze.core.common

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import java.util.UUID

// ============================================
// String Extensions
// ============================================

fun String.Companion.randomUUID(): String = UUID.randomUUID().toString()

fun String?.orEmpty(): String = this ?: ""

fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

// ============================================
// Long (Money) Extensions - Stored as cents
// ============================================

/**
 * Converts cents to a formatted currency string.
 * Example: 12345L.toCurrencyString("USD") -> "$123.45"
 */
fun Long.toCurrencyString(currencyCode: String = "USD", locale: Locale = Locale.getDefault()): String {
    val amount = this / 100.0
    val currency = Currency.getInstance(currencyCode)
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = currency
    }

    // Ensure USD uses "$" (not locale-specific variants like "US$").
    if (format is DecimalFormat) {
        val symbols = format.decimalFormatSymbols
        val desiredSymbol = when (currencyCode.uppercase()) {
            "USD" -> "$"
            else -> currency.getSymbol(locale)
        }
        if (symbols.currencySymbol != desiredSymbol) {
            symbols.currencySymbol = desiredSymbol
            format.decimalFormatSymbols = symbols
        }
    }
    return format.format(amount)
}

/**
 * Converts cents to a decimal amount.
 * Example: 12345L.toDecimalAmount() -> 123.45
 */
fun Long.toDecimalAmount(): Double = this / 100.0

/**
 * Converts a decimal amount to cents.
 * Example: 123.45.toCents() -> 12345L
 */
fun Double.toCents(): Long = (this * 100).toLong()

/**
 * Formats the amount with sign (+/-) for display.
 */
fun Long.toSignedCurrencyString(currencyCode: String = "USD", locale: Locale = Locale.getDefault()): String {
    val sign = if (this >= 0) "+" else ""
    return sign + this.toCurrencyString(currencyCode, locale)
}

// ============================================
// Date/Time Extensions - Stored as epoch millis
// ============================================

/**
 * Converts epoch millis to LocalDateTime.
 */
fun Long.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zoneId)

/**
 * Converts epoch millis to LocalDate.
 */
fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

/**
 * Converts LocalDateTime to epoch millis.
 */
fun LocalDateTime.toEpochMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long =
    this.atZone(zoneId).toInstant().toEpochMilli()

/**
 * Converts LocalDate to epoch millis (start of day).
 */
fun LocalDate.toEpochMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long =
    this.atStartOfDay(zoneId).toInstant().toEpochMilli()

/**
 * Formats epoch millis to a readable date string.
 */
fun Long.toFormattedDate(pattern: String = Constants.DATE_FORMAT_DISPLAY): String {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return this.toLocalDateTime().format(formatter)
}

/**
 * Formats epoch millis to a readable date-time string.
 */
fun Long.toFormattedDateTime(pattern: String = Constants.DATETIME_FORMAT_DISPLAY): String {
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return this.toLocalDateTime().format(formatter)
}

/**
 * Returns the current time in epoch millis.
 */
fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Returns today's date at start of day in epoch millis.
 */
fun todayStartMillis(): Long = LocalDate.now().toEpochMillis()

/**
 * Returns the start of the month for a given epoch millis.
 */
fun Long.startOfMonth(): Long {
    val date = this.toLocalDate()
    return date.withDayOfMonth(1).toEpochMillis()
}

/**
 * Returns the end of the month for a given epoch millis.
 */
fun Long.endOfMonth(): Long {
    val date = this.toLocalDate()
    return date.withDayOfMonth(date.lengthOfMonth())
        .atTime(23, 59, 59)
        .toEpochMillis()
}

// ============================================
// List Extensions
// ============================================

fun <T> List<T>.safeGet(index: Int): T? = if (index in indices) this[index] else null

inline fun <T, R> List<T>.mapAsync(crossinline transform: suspend (T) -> R): List<suspend () -> R> =
    map { { transform(it) } }

// ============================================
// Color Extensions
// ============================================

/**
 * Converts a hex color string to an Android color int.
 * Supports formats: "#RRGGBB" or "#AARRGGBB"
 */
fun String.toColorInt(): Int {
    val hex = if (this.startsWith("#")) this.substring(1) else this
    return when (hex.length) {
        6 -> android.graphics.Color.parseColor("#FF$hex")
        8 -> android.graphics.Color.parseColor("#$hex")
        else -> android.graphics.Color.GRAY
    }
}

