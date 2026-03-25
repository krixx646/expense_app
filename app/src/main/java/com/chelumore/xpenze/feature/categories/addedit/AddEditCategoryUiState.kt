package com.chelumore.xpenze.feature.categories.addedit

import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType

data class AddEditCategoryUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isSaved: Boolean = false,

    val isEdit: Boolean = false,
    val categoryId: String? = null,
    val type: CategoryType = CategoryType.EXPENSE,
    val name: String = "",
    val parentCategoryId: String? = null,
    val colorHex: String = "#F9D648",
    val iconName: String = "category",
    val isDefault: Boolean = false,

    val allCategoriesOfType: List<Category> = emptyList()
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving

    val parentOptions: List<Category>
        get() = allCategoriesOfType.filter { it.parentCategoryId == null && it.id != categoryId }
}


