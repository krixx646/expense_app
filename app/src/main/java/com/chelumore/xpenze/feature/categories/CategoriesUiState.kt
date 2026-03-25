package com.chelumore.xpenze.feature.categories

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedType: CategoryType = CategoryType.EXPENSE,
    val categories: List<Category> = emptyList()
)


