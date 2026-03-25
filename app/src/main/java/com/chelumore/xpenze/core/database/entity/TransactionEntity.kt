package com.chelumore.xpenze.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["dateTime"]),
        Index(value = ["type"]),
        Index(value = ["synced"])
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val accountId: String,
    val toAccountId: String? = null, // For transfers
    val categoryId: String?,
    val amount: Long, // Amount in cents
    val currency: String,
    val type: TransactionType,
    val title: String,
    val notes: String? = null,
    val dateTime: Long, // Epoch millis
    val receiptUri: String? = null,
    val isRecurring: Boolean = false,
    val recurringRuleId: String? = null,
    val tags: List<String> = emptyList(),
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

