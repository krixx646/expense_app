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
 * For DAILY/WEEKLY/MONTHLY/YEARLY, the window follows calendar periods.
 * For CUSTOM, it uses the explicit start/end dates.
 *
 * Returns null when the budget is currently inactive by time bounds.
 */
fun Budget.currentPeriodWindow(
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): BudgetPeriodWindow? {
    if (deleted || !isActive) return null

    val normalizedStart = startDate
    val normalizedEnd = endDate?.coerceAtLeast(normalizedStart)

    if (nowMillis < normalizedStart) return null
    if (normalizedEnd != null && nowMillis > normalizedEnd) return null

    val (rawPeriodStart, rawPeriodEnd) = when (period) {
        BudgetPeriod.CUSTOM -> {
            val customEnd = normalizedEnd ?: nowMillis
            normalizedStart to customEnd
        }
        else -> currentCalendarPeriodBounds(period, nowMillis, zoneId)
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

private fun currentCalendarPeriodBounds(
    period: BudgetPeriod,
    nowMillis: Long,
    zoneId: ZoneId
): Pair<Long, Long> {
    val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val start = when (period) {
        BudgetPeriod.DAILY -> nowDate.atStartOfDay(zoneId)
        BudgetPeriod.WEEKLY -> nowDate
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zoneId)
        BudgetPeriod.MONTHLY -> nowDate.withDayOfMonth(1).atStartOfDay(zoneId)
        BudgetPeriod.YEARLY -> nowDate.withDayOfYear(1).atStartOfDay(zoneId)
        BudgetPeriod.CUSTOM -> nowDate.atStartOfDay(zoneId)
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
