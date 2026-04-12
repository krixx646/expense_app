package com.chelumore.xpenze.domain.usecase.analytics

import com.chelumore.xpenze.core.localization.localizedUncategorized
import com.chelumore.xpenze.core.localization.localizedUnknown
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.DateRange
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.resolvedCategoryId
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSpendingByCategoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(range: DateRange): Flow<List<CategorySpending>> {
        return combine(
            transactionRepository.getByDateRange(range.startMillis, range.endMillis),
            categoryRepository.getAll()
        ) { transactions, categories ->
            val expenseCategories = categories.filter { it.type == CategoryType.EXPENSE }
            val totalsByCategory = transactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.resolvedCategoryId(expenseCategories) }
                .mapValues { (_, list) -> list.sumOf { it.money.amountCents } }

            val totalExpense = totalsByCategory.values.sum().coerceAtLeast(0L)

            totalsByCategory.entries
                .sortedByDescending { it.value }
                .map { (categoryId, amount) ->
                    val resolvedCategory = categoryId?.let { id ->
                        expenseCategories.firstOrNull { it.id == id }
                    }
                    val name = when {
                        categoryId == null -> localizedUncategorized()
                        resolvedCategory != null -> resolvedCategory.name
                        else -> localizedUnknown()
                    }
                    val percent = if (totalExpense > 0) (amount.toFloat() / totalExpense.toFloat()) else 0f
                    CategorySpending(
                        categoryId = categoryId,
                        categoryName = name,
                        amountCents = amount,
                        percent = percent
                    )
                }
        }
    }
}


