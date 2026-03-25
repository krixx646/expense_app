package com.chelumore.xpenze.domain.usecase.category

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        val trimmedName = category.name.trim()
        require(trimmedName.isNotBlank()) { "name is required" }

        val duplicateExists = categoryRepository.getByType(category.type).first().any { existing ->
            existing.id != category.id &&
                normalizeCategoryName(existing.name) == normalizeCategoryName(trimmedName) &&
                existing.parentCategoryId == category.parentCategoryId
        }
        require(!duplicateExists) { "A category with this name already exists." }

        categoryRepository.update(category.copy(name = trimmedName))
    }
}

private fun normalizeCategoryName(name: String): String {
    return name.trim().lowercase().replace(Regex("""\s+"""), " ")
}


