package com.chelumore.xpenze.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.usecase.category.DeleteCategoryUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val selectedType = MutableStateFlow(CategoryType.EXPENSE)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = selectedType
        .flatMapLatest { type ->
            getCategoriesUseCase(type)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        .let { categoriesFlow ->
            kotlinx.coroutines.flow.combine(
                selectedType,
                categoriesFlow,
                error
            ) { type, categories, errorMsg ->
                CategoriesUiState(
                    isLoading = false,
                    error = errorMsg,
                    selectedType = type,
                    categories = categories.sortedWith(compareBy({ it.parentCategoryId != null }, { it.sortOrder }, { it.name }))
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = CategoriesUiState()
            )
        }

    fun setType(type: CategoryType) {
        selectedType.value = type
        error.value = null
    }

    fun clearError() {
        error.value = null
    }

    fun delete(categoryId: String) {
        viewModelScope.launch {
            try {
                deleteCategoryUseCase(categoryId)
            } catch (e: Exception) {
                error.update { e.message ?: "Failed to delete category" }
            }
        }
    }
}


