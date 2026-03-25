package com.chelumore.xpenze.testutil

import com.chelumore.xpenze.domain.model.Account
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.Category
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AccountRepository
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import com.chelumore.xpenze.domain.repository.CategoryRepository
import com.chelumore.xpenze.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryAccountRepository(
    initial: List<Account> = emptyList()
) : AccountRepository {
    private val accounts = MutableStateFlow(initial)

    override fun getAll(): Flow<List<Account>> = accounts

    override fun getActive(): Flow<List<Account>> =
        accounts.map { list -> list.filter { !it.deleted && !it.isArchived } }

    override fun getById(id: String): Flow<Account?> =
        accounts.map { list -> list.firstOrNull { it.id == id && !it.deleted } }

    override fun getDefault(): Flow<Account?> =
        accounts.map { list -> list.firstOrNull { it.isDefault && !it.deleted } }

    override fun getTotalBalance(): Flow<Long> =
        accounts.map { list -> list.filter { !it.deleted }.sumOf { it.currentBalanceCents } }

    override suspend fun insert(account: Account) {
        accounts.value = accounts.value
            .filterNot { it.id == account.id }
            .plus(account)
    }

    override suspend fun update(account: Account) {
        insert(account)
    }

    override suspend fun delete(id: String) {
        accounts.value = accounts.value.map { a ->
            if (a.id == id) a.copy(deleted = true) else a
        }
    }

    override suspend fun setDefault(id: String) {
        accounts.value = accounts.value.map { a ->
            when {
                a.id == id -> a.copy(isDefault = true)
                a.isDefault -> a.copy(isDefault = false)
                else -> a
            }
        }
    }

    override suspend fun updateBalance(id: String, newBalance: Long) {
        accounts.value = accounts.value.map { a ->
            if (a.id == id) a.copy(currentBalanceCents = newBalance) else a
        }
    }

    override suspend fun getUnsynced(): List<Account> =
        accounts.value.filter { !it.synced && !it.deleted }

    override suspend fun markSynced(id: String) {
        accounts.value = accounts.value.map { a ->
            if (a.id == id) a.copy(synced = true) else a
        }
    }
}

class InMemoryCategoryRepository(
    initial: List<Category> = emptyList()
) : CategoryRepository {
    private val categories = MutableStateFlow(initial)

    override fun getAll(): Flow<List<Category>> = categories

    override fun getByType(type: CategoryType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type && !it.deleted } }

    override fun getById(id: String): Flow<Category?> =
        categories.map { list -> list.firstOrNull { it.id == id && !it.deleted } }

    override fun getSubcategories(parentId: String): Flow<List<Category>> =
        categories.map { list -> list.filter { it.parentCategoryId == parentId && !it.deleted } }

    override suspend fun insert(category: Category) {
        categories.value = categories.value
            .filterNot { it.id == category.id }
            .plus(category)
    }

    override suspend fun update(category: Category) {
        insert(category)
    }

    override suspend fun delete(id: String) {
        categories.value = categories.value.map { c ->
            if (c.id == id) c.copy(deleted = true) else c
        }
    }

    override suspend fun seedDefaultCategories() {
        // No-op for tests
    }

    override suspend fun getUnsynced(): List<Category> =
        categories.value.filter { !it.synced && !it.deleted }

    override suspend fun markSynced(id: String) {
        categories.value = categories.value.map { c ->
            if (c.id == id) c.copy(synced = true) else c
        }
    }
}

class InMemoryTransactionRepository(
    initial: List<Transaction> = emptyList()
) : TransactionRepository {
    private val transactions = MutableStateFlow(initial)

    override fun getAll(): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { !it.deleted } }

    override fun getById(id: String): Flow<Transaction?> =
        transactions.map { list -> list.firstOrNull { it.id == id && !it.deleted } }

    override fun getByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactions.map { list ->
            list.filter { !it.deleted && it.dateTime in startTime..endTime }
        }

    override fun getByAccount(accountId: String): Flow<List<Transaction>> =
        transactions.map { list ->
            list.filter { !it.deleted && (it.accountId == accountId || it.toAccountId == accountId) }
        }

    override fun getByCategory(categoryId: String): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { !it.deleted && it.categoryId == categoryId } }

    override fun getByType(type: TransactionType): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { !it.deleted && it.type == type } }

    override fun getRecent(limit: Int): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { !it.deleted }.sortedByDescending { it.dateTime }.take(limit) }

    override fun search(query: String): Flow<List<Transaction>> =
        transactions.map { list ->
            val q = query.lowercase()
            list.filter { !it.deleted && (it.title.lowercase().contains(q) || (it.notes?.lowercase()?.contains(q) == true)) }
        }

    override fun getExpensesByDateRange(startTime: Long, endTime: Long): Flow<Long> =
        getByDateRange(startTime, endTime).map { list -> list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.money.amountCents } }

    override fun getIncomeByDateRange(startTime: Long, endTime: Long): Flow<Long> =
        getByDateRange(startTime, endTime).map { list -> list.filter { it.type == TransactionType.INCOME }.sumOf { it.money.amountCents } }

    override fun getCategorySpending(categoryId: String, startTime: Long, endTime: Long): Flow<Long> =
        getByDateRange(startTime, endTime).map { list ->
            list.filter { it.type == TransactionType.EXPENSE && it.categoryId == categoryId }.sumOf { it.money.amountCents }
        }

    override suspend fun insert(transaction: Transaction) {
        transactions.value = transactions.value
            .filterNot { it.id == transaction.id }
            .plus(transaction)
    }

    override suspend fun update(transaction: Transaction) {
        insert(transaction)
    }

    override suspend fun delete(id: String) {
        transactions.value = transactions.value.map { t ->
            if (t.id == id) t.copy(deleted = true) else t
        }
    }

    override suspend fun getUnsynced(): List<Transaction> =
        transactions.value.filter { !it.synced && !it.deleted }

    override suspend fun markSynced(id: String) {
        transactions.value = transactions.value.map { t ->
            if (t.id == id) t.copy(synced = true) else t
        }
    }
}

class InMemoryAutoCaptureDraftRepository(
    initial: List<AutoCaptureDraft> = emptyList()
) : AutoCaptureDraftRepository {
    private val drafts = MutableStateFlow(initial)

    override fun getPending(): Flow<List<AutoCaptureDraft>> =
        drafts.map { list -> list.filter { it.status != AutoCaptureDraftStatus.RESOLVED } }

    override fun getPendingCount(): Flow<Int> =
        getPending().map { it.size }

    override fun getById(id: String): Flow<AutoCaptureDraft?> =
        drafts.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getByIdOnce(id: String): AutoCaptureDraft? =
        drafts.value.firstOrNull { it.id == id }

    override suspend fun addOrIgnore(draft: AutoCaptureDraft): Boolean {
        if (drafts.value.any { it.fingerprint == draft.fingerprint }) return false
        drafts.value = drafts.value + draft
        return true
    }

    override suspend fun addAllOrIgnore(drafts: List<AutoCaptureDraft>): Int {
        var added = 0
        drafts.forEach { draft ->
            if (addOrIgnore(draft)) added++
        }
        return added
    }

    override suspend fun markResolved(
        id: String,
        resolution: AutoCaptureResolution,
        createdTransactionId: String?
    ) {
        drafts.value = drafts.value.map { draft ->
            if (draft.id == id) {
                draft.copy(
                    status = AutoCaptureDraftStatus.RESOLVED,
                    resolution = resolution,
                    createdTransactionId = createdTransactionId
                )
            } else {
                draft
            }
        }
    }
}


