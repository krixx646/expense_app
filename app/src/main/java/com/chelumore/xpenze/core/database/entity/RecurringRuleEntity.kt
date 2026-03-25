package com.chelumore.xpenze.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    YEARLY
}

@Entity(
    tableName = "recurring_rules",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["nextOccurrence"])
    ]
)
data class RecurringRuleEntity(
    @PrimaryKey
    val id: String,
    // Template transaction data
    val accountId: String,
    val toAccountId: String? = null, // For transfers
    val categoryId: String?,
    val amount: Long,
    val currency: String,
    val type: TransactionType,
    val title: String,
    val notes: String? = null,
    // Recurrence settings
    val frequency: RecurringFrequency,
    val interval: Int = 1, // Every N periods
    val dayOfWeek: Int? = null, // 1-7 for weekly
    val dayOfMonth: Int? = null, // 1-31 for monthly
    val monthOfYear: Int? = null, // 1-12 for yearly
    // Scheduling
    val startDate: Long,
    val endDate: Long? = null, // null = no end
    val nextOccurrence: Long,
    val lastProcessed: Long? = null,
    val isActive: Boolean = true,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

