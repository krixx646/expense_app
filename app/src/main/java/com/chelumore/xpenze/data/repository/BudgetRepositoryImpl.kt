package com.chelumore.xpenze.data.repository

import com.chelumore.xpenze.core.common.currentTimeMillis
import com.chelumore.xpenze.core.database.dao.TransactionDao
import com.chelumore.xpenze.core.database.entity.TransactionType as EntityTransactionType
import com.chelumore.xpenze.core.database.dao.BudgetDao
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.data.mapper.toDomain
import com.chelumore.xpenze.data.mapper.toEntity
import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetWithProgress
import com.chelumore.xpenze.domain.model.currentPeriodWindow
import com.chelumore.xpenze.domain.repository.BudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val syncScheduler: SyncScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BudgetRepository {

    override fun getAll(): Flow<List<Budget>> =
        budgetDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getActive(): Flow<List<Budget>> =
        budgetDao.getActive().map { list -> list.map { it.toDomain() } }

    override fun getById(id: String): Flow<Budget?> =
        budgetDao.getById(id).map { it?.toDomain() }

    override fun getByCategory(categoryId: String): Flow<List<Budget>> =
        budgetDao.getByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getCurrentBudgets(): Flow<List<Budget>> =
        budgetDao.getCurrentBudgets(currentTimeMillis()).map { list -> list.map { it.toDomain() } }

    override fun getAllWithProgress(): Flow<List<BudgetWithProgress>> =
        budgetsWithProgressFlow(budgetDao.getAll().map { list -> list.map { it.toDomain() } })

    override fun getActiveWithProgress(): Flow<List<BudgetWithProgress>> =
        budgetsWithProgressFlow(budgetDao.getActive().map { list -> list.map { it.toDomain() } })

    override suspend fun insert(budget: Budget) = withContext(ioDispatcher) {
        budgetDao.insert(budget.toEntity())
        syncScheduler.scheduleNow()
    }

    override suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        budgetDao.update(budget.copy(updatedAt = currentTimeMillis(), synced = false).toEntity())
        syncScheduler.scheduleNow()
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        budgetDao.softDelete(id, currentTimeMillis())
        syncScheduler.scheduleNow()
    }

    override suspend fun setActive(id: String, isActive: Boolean) = withContext(ioDispatcher) {
        budgetDao.setActive(id, isActive, currentTimeMillis())
        syncScheduler.scheduleNow()
    }

    override suspend fun getUnsynced(): List<Budget> = withContext(ioDispatcher) {
        budgetDao.getUnsynced().map { it.toDomain() }
    }

    override suspend fun markSynced(id: String) = withContext(ioDispatcher) {
        budgetDao.markSynced(id)
    }

    private fun budgetsWithProgressFlow(budgetsFlow: Flow<List<Budget>>): Flow<List<BudgetWithProgress>> {
        return combine(
            budgetsFlow,
            transactionDao.getAll()
        ) { budgets, transactions ->
            val now = currentTimeMillis()
            val expenseTx = transactions.filter { it.type == EntityTransactionType.EXPENSE }

            budgets.map { budget ->
                val window = budget.currentPeriodWindow(now)
                val spent = if (window == null) {
                    0L
                } else {
                    expenseTx.asSequence()
                        .filter { it.dateTime in window.spentRange.startMillis..window.spentRange.endMillis }
                        .filter { budget.categoryId == null || it.categoryId == budget.categoryId }
                        .filter { budget.accountId == null || it.accountId == budget.accountId }
                        .sumOf { it.amount }
                }

                val progress = if (budget.amountLimitCents > 0) {
                    spent.toFloat() / budget.amountLimitCents.toFloat()
                } else 0f

                BudgetWithProgress(
                    budget = budget,
                    spentCents = spent,
                    progress = progress
                )
            }
        }
    }
}

