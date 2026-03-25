package com.chelumore.xpenze.data.repository

import com.chelumore.xpenze.core.common.currentTimeMillis
import com.chelumore.xpenze.core.database.dao.RecurringRuleDao
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.domain.model.RecurringRule
import com.chelumore.xpenze.domain.model.RecurringFrequency
import com.chelumore.xpenze.domain.repository.RecurringRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRuleRepositoryImpl @Inject constructor(
    private val recurringRuleDao: RecurringRuleDao,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : RecurringRuleRepository {

    override fun getAll(): Flow<List<RecurringRule>> =
        recurringRuleDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getActive(): Flow<List<RecurringRule>> =
        recurringRuleDao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getById(id: String): Flow<RecurringRule?> =
        recurringRuleDao.getById(id).map { it?.toDomain() }

    override suspend fun insert(rule: RecurringRule) = withContext(ioDispatcher) {
        val now = currentTimeMillis()
        val from = maxOf(rule.lastProcessed ?: 0L, now)
        val next = computeNextOccurrenceMillis(rule, from)
        recurringRuleDao.insert(
            rule.copy(
                nextOccurrence = next,
                updatedAt = now,
                synced = false
            ).toEntity()
        )
        syncScheduler.scheduleNow()
    }

    override suspend fun update(rule: RecurringRule) = withContext(ioDispatcher) {
        val now = currentTimeMillis()
        val from = maxOf(rule.lastProcessed ?: 0L, now)
        val next = computeNextOccurrenceMillis(rule, from)
        recurringRuleDao.update(
            rule.copy(
                nextOccurrence = next,
                updatedAt = now,
                synced = false
            ).toEntity()
        )
        syncScheduler.scheduleNow()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        recurringRuleDao.softDelete(id, currentTimeMillis())
        syncScheduler.scheduleNow()
    }

    override suspend fun getDueRules(): List<RecurringRule> = withContext(ioDispatcher) {
        recurringRuleDao.getDueRules(currentTimeMillis()).map { it.toDomain() }
    }

    override suspend fun updateNextOccurrence(id: String, nextOccurrence: Long, lastProcessed: Long) =
        withContext(ioDispatcher) {
            val now = currentTimeMillis()
            val resolvedNext = if (nextOccurrence > 0L) {
                nextOccurrence
            } else {
                val rule = recurringRuleDao.getByIdOnce(id)?.toDomain()
                    ?: return@withContext
                computeNextOccurrenceMillis(rule, lastProcessed)
            }

            recurringRuleDao.updateNextOccurrence(id, resolvedNext, lastProcessed, now)

            // Auto-deactivate after endDate
            val rule = recurringRuleDao.getByIdOnce(id)?.toDomain()
            if (rule?.endDate != null && resolvedNext > rule.endDate) {
                recurringRuleDao.setActive(id, false, now)
            }

            syncScheduler.scheduleNow()
        }

    override suspend fun setActive(id: String, isActive: Boolean) = withContext(ioDispatcher) {
        recurringRuleDao.setActive(id, isActive, currentTimeMillis())
        syncScheduler.scheduleNow()
    }

    override suspend fun getUnsynced(): List<RecurringRule> = withContext(ioDispatcher) {
        recurringRuleDao.getUnsynced().map { it.toDomain() }
    }

    override suspend fun markSynced(id: String) = withContext(ioDispatcher) {
        recurringRuleDao.markSynced(id)
    }

    private fun computeNextOccurrenceMillis(rule: RecurringRule, fromMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        val start = Instant.ofEpochMilli(rule.startDate).atZone(zone)
        val from = Instant.ofEpochMilli(fromMillis).atZone(zone)

        val interval = rule.interval.coerceAtLeast(1)

        val base = when (rule.frequency) {
            RecurringFrequency.DAILY -> start
            RecurringFrequency.WEEKLY, RecurringFrequency.BIWEEKLY -> {
                val desiredDow = rule.dayOfWeek?.coerceIn(1, 7)?.let { DayOfWeek.of(it) } ?: start.dayOfWeek
                start.with(TemporalAdjusters.nextOrSame(desiredDow))
            }
            RecurringFrequency.MONTHLY -> firstMonthlyCandidate(start, rule.dayOfMonth)
            RecurringFrequency.YEARLY -> firstYearlyCandidate(start, rule.monthOfYear, rule.dayOfMonth)
        }

        var candidate = base
        while (!candidate.isAfter(from)) {
            candidate = nextCandidate(candidate, rule, interval)
        }

        return candidate.toInstant().toEpochMilli()
    }

    private fun nextCandidate(current: ZonedDateTime, rule: RecurringRule, interval: Int): ZonedDateTime {
        return when (rule.frequency) {
            RecurringFrequency.DAILY -> current.plusDays(interval.toLong())
            RecurringFrequency.WEEKLY -> current.plusWeeks(interval.toLong())
            RecurringFrequency.BIWEEKLY -> current.plusWeeks((interval * 2L))
            RecurringFrequency.MONTHLY -> addMonthsKeepingDay(current, interval.toLong(), rule.dayOfMonth)
            RecurringFrequency.YEARLY -> addYearsKeepingMonthDay(current, interval.toLong(), rule.monthOfYear, rule.dayOfMonth)
        }
    }

    private fun firstMonthlyCandidate(start: ZonedDateTime, desiredDayOfMonth: Int?): ZonedDateTime {
        val zone = start.zone
        val desired = (desiredDayOfMonth ?: start.dayOfMonth).coerceIn(1, 31)
        val time = start.toLocalTime()
        var ym = YearMonth.from(start)

        fun candidateFor(ym: YearMonth): ZonedDateTime {
            val day = desired.coerceAtMost(ym.lengthOfMonth())
            return ZonedDateTime.of(LocalDate.of(ym.year, ym.month, day), time, zone)
        }

        var candidate = candidateFor(ym)
        if (candidate.isBefore(start)) {
            ym = ym.plusMonths(1)
            candidate = candidateFor(ym)
        }
        return candidate
    }

    private fun addMonthsKeepingDay(current: ZonedDateTime, months: Long, desiredDayOfMonth: Int?): ZonedDateTime {
        val zone = current.zone
        val desired = (desiredDayOfMonth ?: current.dayOfMonth).coerceIn(1, 31)
        val time = current.toLocalTime()
        val ym = YearMonth.from(current).plusMonths(months)
        val day = desired.coerceAtMost(ym.lengthOfMonth())
        return ZonedDateTime.of(LocalDate.of(ym.year, ym.month, day), time, zone)
    }

    private fun firstYearlyCandidate(start: ZonedDateTime, desiredMonthOfYear: Int?, desiredDayOfMonth: Int?): ZonedDateTime {
        val zone = start.zone
        val month = (desiredMonthOfYear ?: start.monthValue).coerceIn(1, 12)
        val dayDesired = (desiredDayOfMonth ?: start.dayOfMonth).coerceIn(1, 31)
        val time = start.toLocalTime()

        fun candidateFor(year: Int): ZonedDateTime {
            val ym = YearMonth.of(year, month)
            val day = dayDesired.coerceAtMost(ym.lengthOfMonth())
            return ZonedDateTime.of(LocalDate.of(year, month, day), time, zone)
        }

        var candidate = candidateFor(start.year)
        if (candidate.isBefore(start)) {
            candidate = candidateFor(start.year + 1)
        }
        return candidate
    }

    private fun addYearsKeepingMonthDay(
        current: ZonedDateTime,
        years: Long,
        desiredMonthOfYear: Int?,
        desiredDayOfMonth: Int?
    ): ZonedDateTime {
        val zone = current.zone
        val month = (desiredMonthOfYear ?: current.monthValue).coerceIn(1, 12)
        val dayDesired = (desiredDayOfMonth ?: current.dayOfMonth).coerceIn(1, 31)
        val time = current.toLocalTime()
        val year = (current.year + years).toInt()
        val ym = YearMonth.of(year, month)
        val day = dayDesired.coerceAtMost(ym.lengthOfMonth())
        return ZonedDateTime.of(LocalDate.of(year, month, day), time, zone)
    }
}

