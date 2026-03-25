package com.chelumore.xpenze.domain.usecase.category

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.repository.CategoryRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        type: CategoryType,
        parentCategoryId: String? = null,
        colorHex: String,
        iconName: String,
        sortOrder: Int = 0,
        isDefault: Boolean = false
    ): String {
        require(name.isNotBlank()) { "name is required" }
        require(colorHex.isNotBlank()) { "colorHex is required" }
        require(iconName.isNotBlank()) { "iconName is required" }

        val trimmedName = name.trim()
        val duplicateExists = categoryRepository.getByType(type).first().any { existing ->
            normalizeCategoryName(existing.name) == normalizeCategoryName(trimmedName) &&
                existing.parentCategoryId == parentCategoryId
        }
        require(!duplicateExists) { "A category with this name already exists." }

        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val category = Category(
            id = id,
            name = trimmedName,
            type = type,
            parentCategoryId = parentCategoryId,
            colorHex = colorHex,
            iconName = iconName,
            isDefault = isDefault,
            sortOrder = sortOrder,
            synced = false,
            deleted = false,
            createdAt = now,
            updatedAt = now
        )

        categoryRepository.insert(category)
        return id
    }
}

private fun normalizeCategoryName(name: String): String {
    return name.trim().lowercase().replace(Regex("""\s+"""), " ")
}


