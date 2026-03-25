package com.chelumore.xpenze.domain.model

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetPeriodWindowTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    @Test
    fun `monthly budget resolves to current calendar month`() {
        val budget = sampleBudget(
            period = BudgetPeriod.MONTHLY,
            startDate = utcMillis(2026, 1, 1)
        )
        val now = utcMillis(2026, 3, 15, 10, 30, 0, 0)

        val window = budget.currentPeriodWindow(nowMillis = now, zoneId = utc)

        requireNotNull(window)
        assertEquals(utcMillis(2026, 3, 1), window.periodStartMillis)
        assertEquals(utcMillis(2026, 3, 31, 23, 59, 59, 999), window.periodEndMillis)
        assertEquals(utcMillis(2026, 3, 1), window.spentRange.startMillis)
        assertEquals(now, window.spentRange.endMillis)
    }

    @Test
    fun `monthly budget clamps start to configured start date when created mid-month`() {
        val budget = sampleBudget(
            period = BudgetPeriod.MONTHLY,
            startDate = utcMillis(2026, 3, 10)
        )
        val now = utcMillis(2026, 3, 15, 9, 0, 0, 0)

        val window = budget.currentPeriodWindow(nowMillis = now, zoneId = utc)

        requireNotNull(window)
        assertEquals(utcMillis(2026, 3, 10), window.periodStartMillis)
        assertEquals(now, window.spentRange.endMillis)
    }

    @Test
    fun `future budget returns null window`() {
        val budget = sampleBudget(
            period = BudgetPeriod.MONTHLY,
            startDate = utcMillis(2026, 5, 1)
        )
        val now = utcMillis(2026, 4, 20, 12, 0, 0, 0)

        val window = budget.currentPeriodWindow(nowMillis = now, zoneId = utc)

        assertNull(window)
    }

    @Test
    fun `custom budget respects explicit end date`() {
        val budget = sampleBudget(
            period = BudgetPeriod.CUSTOM,
            startDate = utcMillis(2026, 4, 1),
            endDate = utcMillis(2026, 4, 20, 23, 59, 59, 999)
        )
        val now = utcMillis(2026, 4, 15, 8, 0, 0, 0)

        val window = budget.currentPeriodWindow(nowMillis = now, zoneId = utc)

        requireNotNull(window)
        assertEquals(utcMillis(2026, 4, 1), window.periodStartMillis)
        assertEquals(utcMillis(2026, 4, 20, 23, 59, 59, 999), window.periodEndMillis)
        assertEquals(now, window.spentRange.endMillis)
    }

    @Test
    fun `budget after end date returns null window`() {
        val budget = sampleBudget(
            period = BudgetPeriod.WEEKLY,
            startDate = utcMillis(2026, 1, 1),
            endDate = utcMillis(2026, 1, 31, 23, 59, 59, 999)
        )
        val now = utcMillis(2026, 2, 5, 9, 0, 0, 0)

        val window = budget.currentPeriodWindow(nowMillis = now, zoneId = utc)

        assertNull(window)
    }

    private fun sampleBudget(
        period: BudgetPeriod,
        startDate: Long,
        endDate: Long? = null
    ): Budget = Budget(
        id = "b1",
        name = "Budget",
        categoryId = null,
        accountId = null,
        amountLimitCents = 100_00L,
        period = period,
        startDate = startDate,
        endDate = endDate,
        alertThresholdPercent = 80,
        isActive = true,
        synced = false,
        deleted = false,
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millis: Int = 0
    ): Long = ZonedDateTime.of(
        year,
        month,
        day,
        hour,
        minute,
        second,
        millis * 1_000_000,
        ZoneOffset.UTC
    ).toInstant().toEpochMilli()
}
