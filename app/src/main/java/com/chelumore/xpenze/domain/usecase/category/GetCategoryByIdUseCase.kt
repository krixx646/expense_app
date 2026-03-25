package com.chelumore.xpenze.domain.usecase.category

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoryByIdUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(id: String): Flow<Category?> {
        require(id.isNotBlank()) { "id is required" }
        return categoryRepository.getById(id)
    }
}


