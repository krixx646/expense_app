package com.chelumore.xpenze.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class CategoryType {
    INCOME,
    EXPENSE
}

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentCategoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["type"]),
        Index(value = ["parentCategoryId"]),
        Index(value = ["isDefault"])
    ]
)
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: CategoryType,
    val parentCategoryId: String? = null,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false, // System-provided categories
    val sortOrder: Int = 0,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

