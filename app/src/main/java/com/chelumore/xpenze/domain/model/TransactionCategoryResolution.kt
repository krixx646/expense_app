package com.chelumore.xpenze.domain.model

fun Transaction.resolvedCategoryId(categories: List<Category>): String? {
    val expectedType = when (type) {
        TransactionType.EXPENSE -> CategoryType.EXPENSE
        TransactionType.INCOME -> CategoryType.INCOME
        TransactionType.TRANSFER -> return null
    }
    val categoriesById = categories
        .asSequence()
        .filter { !it.deleted }
        .associateBy { it.id }

    val existingCategory = categoryId?.let(categoriesById::get)
    if (existingCategory?.type == expectedType) return existingCategory.id

    val normalizedTitle = normalizeCategoryResolutionLabel(title)
    if (normalizedTitle.isBlank()) return null

    val matches = categoriesById.values.filter { category ->
            category.type == expectedType &&
            normalizeCategoryResolutionLabel(category.name) == normalizedTitle
    }

    return matches.singleOrNull()?.id
}

fun Transaction.resolvedCategory(categories: List<Category>): Category? {
    val resolvedId = resolvedCategoryId(categories) ?: return null
    return categories.firstOrNull { it.id == resolvedId }
}

fun resolveCategorySelectionForTransactionType(
    categoryId: String?,
    transactionType: TransactionType,
    categories: List<Category>
): String? {
    if (categoryId.isNullOrBlank() || transactionType == TransactionType.TRANSFER) return null

    val expectedType = when (transactionType) {
        TransactionType.EXPENSE -> CategoryType.EXPENSE
        TransactionType.INCOME -> CategoryType.INCOME
        TransactionType.TRANSFER -> return null
    }
    val categoriesById = categories
        .asSequence()
        .filter { !it.deleted }
        .associateBy { it.id }
    val selectedCategory = categoriesById[categoryId] ?: return null
    if (selectedCategory.type == expectedType) return selectedCategory.id

    val normalizedName = normalizeCategoryResolutionLabel(selectedCategory.name)
    if (normalizedName.isBlank()) return null

    return categoriesById.values
        .filter { category ->
            category.type == expectedType &&
                normalizeCategoryResolutionLabel(category.name) == normalizedName
        }
        .singleOrNull()
        ?.id
}

fun Transaction.matchesBudgetScope(budget: Budget, categories: List<Category>): Boolean {
    if (type != TransactionType.EXPENSE) return false

    val scopedBudgetCategoryId = budget.resolvedExpenseCategoryId(categories)
    val categoryOk = scopedBudgetCategoryId?.let { budgetCategoryId ->
        resolvedCategoryId(categories) == budgetCategoryId
    } ?: true
    val accountOk = budget.accountId?.let { budgetAccountId ->
        accountId == budgetAccountId
    } ?: true

    return categoryOk && accountOk
}

fun Budget.resolvedExpenseCategoryId(categories: List<Category>): String? {
    val rawCategoryId = categoryId ?: return null
    val category = categories.firstOrNull { it.id == rawCategoryId && !it.deleted }
    return category?.takeIf { it.type == CategoryType.EXPENSE }?.id
}

private fun normalizeCategoryResolutionLabel(value: String): String {
    return value.trim().lowercase().replace(Regex("""\s+"""), " ")
}
