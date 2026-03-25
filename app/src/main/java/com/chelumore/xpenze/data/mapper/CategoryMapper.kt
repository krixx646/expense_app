package com.chelumore.xpenze.data.mapper

import com.chelumore.xpenze.core.database.entity.CategoryEntity
import com.chelumore.xpenze.core.database.entity.CategoryType as EntityCategoryType
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType as DomainCategoryType

fun CategoryEntity.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        type = DomainCategoryType.valueOf(type.name),
        parentCategoryId = parentCategoryId,
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault,
        sortOrder = sortOrder,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        type = EntityCategoryType.valueOf(type.name),
        parentCategoryId = parentCategoryId,
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault,
        sortOrder = sortOrder,
        synced = synced,
        deleted = deleted,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}


