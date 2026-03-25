package com.chelumore.xpenze.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType {
    CASH,
    BANK,
    CREDIT_CARD,
    SAVINGS,
    INVESTMENT,
    OTHER
}

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isDefault"]),
        Index(value = ["isArchived"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: AccountType,
    val currency: String,
    val initialBalance: Long, // In cents
    val currentBalance: Long, // Cached balance in cents
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

