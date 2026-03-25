package com.chelumore.xpenze.domain.model

enum class CategoryType {
    INCOME,
    EXPENSE
}

data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val parentCategoryId: String? = null,
    val colorHex: String,
    val iconName: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val synced: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)


