package com.chelumore.xpenze.domain.usecase.category

import com.chelumore.xpenze.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String) {
        require(id.isNotBlank()) { "id is required" }
        categoryRepository.delete(id)
    }
}


