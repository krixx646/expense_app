package com.chelumore.xpenze.domain.usecase.category

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(type: CategoryType? = null): Flow<List<Category>> {
        return if (type == null) categoryRepository.getAll() else categoryRepository.getByType(type)
    }
}


