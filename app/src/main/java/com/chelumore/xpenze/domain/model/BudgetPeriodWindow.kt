package com.chelumore.xpenze.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Resolved window for the budget in the *current* period.
 *
 * [periodStartMillis] and [periodEndMillis] represent the active budget window for now.
 * [spentRange] is the transaction range used for progress (clamped to "now").
 */
data class BudgetPeriodWindow(
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val spentRange: DateRange
)

/**
 * Computes the budget window that should be used *right now* for progress/alerts.
 *
 * Each budget row represents a single budget time frame. When a non-custom budget has no
 * explicit [Budget.endDate], we resolve it to the end of the calendar period that contains
 * [Budget.startDate]. That prevents old monthly budgets from silently rolling into new months.
 */
fun Budget.currentPeriodWindow(
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): BudgetPeriodWindow? {
    if (deleted || !isActive) return null

    val normalizedStart = startDate
    val normalizedEnd = resolveBudgetEndDate(
        period = period,
        startDate = normalizedStart,
        explicitEndDate = endDate,
        zoneId = zoneId
    )

    if (nowMillis < normalizedStart) return null
    if (normalizedEnd != null && nowMillis > normalizedEnd) return null

    val (rawPeriodStart, rawPeriodEnd) = when (period) {
        BudgetPeriod.CUSTOM -> {
            val customEnd = normalizedEnd ?: nowMillis
            normalizedStart to customEnd
        }
        else -> calendarPeriodBounds(period, nowMillis, zoneId)
    }

    val periodStart = maxOf(rawPeriodStart, normalizedStart)
    val periodEnd = minOf(rawPeriodEnd, normalizedEnd ?: rawPeriodEnd)
    if (periodStart > periodEnd) return null

    val spentEnd = minOf(nowMillis, periodEnd)
    if (spentEnd < periodStart) return null

    return BudgetPeriodWindow(
        periodStartMillis = periodStart,
        periodEndMillis = periodEnd,
        spentRange = DateRange(periodStart, spentEnd)
    )
}

fun Budget.resolvedEndDate(
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? = resolveBudgetEndDate(
    period = period,
    startDate = startDate,
    explicitEndDate = endDate,
    zoneId = zoneId
)

fun Budget.isExpired(
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Boolean {
    val resolvedEnd = resolvedEndDate(zoneId) ?: return false
    return nowMillis > resolvedEnd
}

fun Budget.timeframeRange(
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): DateRange? {
    if (deleted || !isActive) return null
    if (nowMillis < startDate) return null

    val resolvedEnd = resolvedEndDate(zoneId)
    val effectiveEnd = minOf(resolvedEnd ?: nowMillis, nowMillis)
    if (effectiveEnd < startDate) return null

    return DateRange(startDate, effectiveEnd)
}

fun Budget.fullTimeframeRange(
    zoneId: ZoneId = ZoneId.systemDefault()
): DateRange? {
    val resolvedEnd = resolvedEndDate(zoneId) ?: return null
    if (resolvedEnd < startDate) return null
    return DateRange(startDate, resolvedEnd)
}

fun Budget.nextTimeframeRange(
    zoneId: ZoneId = ZoneId.systemDefault()
): DateRange? {
    val currentEnd = resolvedEndDate(zoneId) ?: return null
    val nextStart = currentEnd + 1
    if (nextStart <= startDate) return null

    val nextEnd = when (period) {
        BudgetPeriod.CUSTOM -> {
            val currentDuration = currentEnd - startDate
            nextStart + currentDuration
        }
        else -> resolveBudgetEndDate(
            period = period,
            startDate = nextStart,
            explicitEndDate = null,
            zoneId = zoneId
        ) ?: return null
    }

    return DateRange(nextStart, nextEnd)
}

fun resolveBudgetEndDate(
    period: BudgetPeriod,
    startDate: Long,
    explicitEndDate: Long?,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? {
    val normalizedExplicitEnd = explicitEndDate?.coerceAtLeast(startDate)
    if (normalizedExplicitEnd != null) return normalizedExplicitEnd
    if (period == BudgetPeriod.CUSTOM) return null
    return calendarPeriodBounds(period, startDate, zoneId).second
}

private fun calendarPeriodBounds(
    period: BudgetPeriod,
    referenceMillis: Long,
    zoneId: ZoneId
): Pair<Long, Long> {
    val referenceDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
    val start = when (period) {
        BudgetPeriod.DAILY -> referenceDate.atStartOfDay(zoneId)
        BudgetPeriod.WEEKLY -> referenceDate
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zoneId)
        BudgetPeriod.MONTHLY -> referenceDate.withDayOfMonth(1).atStartOfDay(zoneId)
        BudgetPeriod.YEARLY -> referenceDate.withDayOfYear(1).atStartOfDay(zoneId)
        BudgetPeriod.CUSTOM -> referenceDate.atStartOfDay(zoneId)
    }

    val end = when (period) {
        BudgetPeriod.DAILY -> start.plusDays(1)
        BudgetPeriod.WEEKLY -> start.plusWeeks(1)
        BudgetPeriod.MONTHLY -> start.plusMonths(1)
        BudgetPeriod.YEARLY -> start.plusYears(1)
        BudgetPeriod.CUSTOM -> start.plusDays(1)
    }.minusNanos(1)

    return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
}
