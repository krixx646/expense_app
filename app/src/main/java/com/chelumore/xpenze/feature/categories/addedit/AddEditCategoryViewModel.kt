package com.chelumore.xpenze.feature.categories.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.usecase.category.AddCategoryUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoryByIdUseCase
import com.chelumore.xpenze.domain.usecase.category.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditCategoryViewModel @Inject constructor(
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoryByIdUseCase: GetCategoryByIdUseCase
) : ViewModel() {

    private val categoryId = MutableStateFlow<String?>(null)
    private val initialType = MutableStateFlow(CategoryType.EXPENSE)

    private val form = MutableStateFlow(AddEditCategoryUiState())
    val uiState: StateFlow<AddEditCategoryUiState> = combine(
        form,
        initialType.flatMapLatest { type -> getCategoriesUseCase(type) }
    ) { state, categories ->
        state.copy(allCategoriesOfType = categories)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditCategoryUiState()
    )

    fun init(categoryId: String?, type: CategoryType) {
        this.initialType.value = type
        this.categoryId.value = categoryId

        if (categoryId.isNullOrBlank()) {
            form.update {
                AddEditCategoryUiState(
                    isLoading = false,
                    isEdit = false,
                    type = type,
                    colorHex = defaultColorForType(type),
                    iconName = defaultIconForType(type)
                )
            }
            return
        }

        viewModelScope.launch {
            form.update { it.copy(isLoading = true, error = null, isSaved = false) }
            try {
                val category = getCategoryByIdUseCase(categoryId).first()
                    ?: throw IllegalStateException("Category not found")
                initialType.value = category.type
                form.update {
                    it.copy(
                        isLoading = false,
                        isEdit = true,
                        categoryId = category.id,
                        type = category.type,
                        name = category.name,
                        parentCategoryId = category.parentCategoryId,
                        colorHex = category.colorHex,
                        iconName = category.iconName,
                        isDefault = category.isDefault
                    )
                }
            } catch (e: Exception) {
                form.update { it.copy(isLoading = false, error = e.message ?: "Failed to load category") }
            }
        }
    }

    fun setName(value: String) = form.update { it.copy(name = value) }
    fun setType(value: CategoryType) {
        initialType.value = value
        form.update { it.copy(type = value, parentCategoryId = null) }
    }

    fun setParentCategoryId(value: String?) = form.update { it.copy(parentCategoryId = value) }
    fun setColorHex(value: String) = form.update { it.copy(colorHex = value) }
    fun setIconName(value: String) = form.update { it.copy(iconName = value) }
    fun setDefault(value: Boolean) = form.update { it.copy(isDefault = value) }

    fun clearError() = form.update { it.copy(error = null) }

    fun save() {
        val state = uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            try {
                form.update { it.copy(isSaving = true, error = null) }
                val now = System.currentTimeMillis()

                if (!state.isEdit) {
                    val sortOrder = (state.allCategoriesOfType.maxOfOrNull { it.sortOrder } ?: 0) + 1
                    addCategoryUseCase(
                        name = state.name.trim(),
                        type = state.type,
                        parentCategoryId = state.parentCategoryId,
                        colorHex = state.colorHex,
                        iconName = state.iconName,
                        sortOrder = sortOrder,
                        isDefault = state.isDefault
                    )
                } else {
                    val existing = getCategoryByIdUseCase(state.categoryId!!).first()
                        ?: throw IllegalStateException("Category not found")
                    updateCategoryUseCase(
                        existing.copy(
                            name = state.name.trim(),
                            type = state.type,
                            parentCategoryId = state.parentCategoryId,
                            colorHex = state.colorHex,
                            iconName = state.iconName,
                            isDefault = state.isDefault,
                            updatedAt = now
                        )
                    )
                }

                form.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                form.update { it.copy(isSaving = false, error = e.message ?: "Failed to save category") }
            }
        }
    }
}

private fun defaultColorForType(type: CategoryType): String {
    return when (type) {
        CategoryType.EXPENSE -> "#FFB74D"
        CategoryType.INCOME -> "#81C784"
    }
}

private fun defaultIconForType(type: CategoryType): String {
    return when (type) {
        CategoryType.EXPENSE -> "shopping_bag"
        CategoryType.INCOME -> "payments"
    }
}


