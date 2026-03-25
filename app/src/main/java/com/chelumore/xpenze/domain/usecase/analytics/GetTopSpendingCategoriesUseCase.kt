package com.chelumore.xpenze.domain.usecase.analytics

import com.chelumore.xpenze.domain.model.DateRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTopSpendingCategoriesUseCase @Inject constructor(
    private val getSpendingByCategoryUseCase: GetSpendingByCategoryUseCase
) {
    operator fun invoke(range: DateRange, limit: Int = 5): Flow<List<CategorySpending>> {
        require(limit > 0) { "limit must be > 0" }
        return getSpendingByCategoryUseCase(range).map { it.take(limit) }
    }
}


