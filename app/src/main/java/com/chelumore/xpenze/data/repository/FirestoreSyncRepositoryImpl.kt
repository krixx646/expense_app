package com.chelumore.xpenze.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.chelumore.xpenze.core.common.await
import com.chelumore.xpenze.core.database.AppDatabase
import com.chelumore.xpenze.core.database.dao.AccountDao
import com.chelumore.xpenze.core.database.dao.BudgetDao
import com.chelumore.xpenze.core.database.dao.CategoryDao
import com.chelumore.xpenze.core.database.dao.RecurringRuleDao
import com.chelumore.xpenze.core.database.dao.TransactionDao
import com.chelumore.xpenze.core.database.entity.AccountEntity
import com.chelumore.xpenze.core.database.entity.AccountType
import com.chelumore.xpenze.core.database.entity.BudgetEntity
import com.chelumore.xpenze.core.database.entity.BudgetPeriod
import com.chelumore.xpenze.core.database.entity.CategoryEntity
import com.chelumore.xpenze.core.database.entity.CategoryType
import com.chelumore.xpenze.core.database.entity.RecurringFrequency
import com.chelumore.xpenze.core.database.entity.RecurringRuleEntity
import com.chelumore.xpenze.core.database.entity.TransactionEntity
import com.chelumore.xpenze.core.database.entity.TransactionType
import com.chelumore.xpenze.core.datastore.PreferencesDataStore
import com.chelumore.xpenze.core.di.IoDispatcher
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.AuthRepository
import com.chelumore.xpenze.domain.repository.SyncRepository
import com.chelumore.xpenze.domain.repository.SyncState
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class FirestoreSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val preferencesDataStore: PreferencesDataStore,
    private val appDatabase: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val recurringRuleDao: RecurringRuleDao,
    private val transactionDao: TransactionDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SyncRepository {

    private val mutex = Mutex()
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: Flow<SyncState> = _syncState

    private companion object {
        const val DOCUMENT_APPEARANCE_SETTINGS = "appearance"
    }

    override suspend fun syncTransactions() = fullSync()
    override suspend fun syncAccounts() = fullSync()
    override suspend fun syncCategories() = fullSync()
    override suspend fun syncBudgets() = fullSync()

    override suspend fun fullSync() = withContext(ioDispatcher) {
        mutex.withLock {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _syncState.value = SyncState.Idle
                return@withContext
            }

            val firestore = firestoreOrNull()
            if (firestore == null) {
                _syncState.value = SyncState.Error("Firestore is not configured. Add google-services.json and rebuild.")
                return@withContext
            }

            _syncState.value = SyncState.Syncing

            try {
                val uid = user.uid
                val root = firestore.collection("users").document(uid)
                val appearanceSettings = root.collection("settings").document(DOCUMENT_APPEARANCE_SETTINGS)

                // Order matters (FKs): accounts -> categories -> budgets -> recurring -> transactions.
                // We intentionally do a full cloud pull after pushing local deltas because relying on a
                // device-local watermark can miss changes from another device with a different clock.
                rebaseLocalAccountInitialBalances()
                pushAccounts(root.collection("accounts"))
                pullAccounts(root.collection("accounts"))

                // Account card appearance sync is optional until matching Firestore rules are deployed.
                ignoreOptionalPermissionDenied {
                    pushAccountCardStyleSettings(appearanceSettings)
                }
                ignoreOptionalPermissionDenied {
                    pullAccountCardStyleSettings(appearanceSettings)
                }

                pushCategories(root.collection("categories"))
                pullCategories(root.collection("categories"))
                repairInvalidCategoryLinksAndScopes()

                pushBudgets(root.collection("budgets"))
                pullBudgets(root.collection("budgets"))

                pushRecurringRules(root.collection("recurring_rules"))
                pullRecurringRules(root.collection("recurring_rules"))

                pushTransactions(root.collection("transactions"))
                pullTransactions(root.collection("transactions"))

                // Defensive: balances are derived from transactions; recalc prevents drift/double-apply across sync runs.
                recalculateAccountBalancesFromTransactions()

                val completedAt = System.currentTimeMillis()
                preferencesDataStore.updateLastSyncTime(completedAt)
                _syncState.value = SyncState.Success(completedAt)
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Sync failed", e)
                throw e
            }
        }
    }

    private fun firebaseAppOrNull(): FirebaseApp? {
        return runCatching { FirebaseApp.getInstance() }.getOrNull()
            ?: runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
    }

    private fun firestoreOrNull(): FirebaseFirestore? {
        val app = firebaseAppOrNull() ?: return null
        return runCatching { FirebaseFirestore.getInstance(app) }.getOrNull()
    }

    private suspend fun ignoreOptionalPermissionDenied(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw e
            }
        }
    }

    // ---------------------------
    // Push (local -> Firestore)
    // ---------------------------

    private suspend fun pushAccounts(collection: com.google.firebase.firestore.CollectionReference): Long {
        val unsynced = accountDao.getUnsynced()
        val deleted = accountDao.getDeleted()

        var maxUpdatedAt = 0L
        val syncedIds = mutableListOf<String>()

        for (a in unsynced) {
            maxUpdatedAt = maxOf(maxUpdatedAt, a.updatedAt)
            val ok = upsertIfNewer(collection.document(a.id), a.updatedAt, accountToDoc(a))
            if (ok) syncedIds.add(a.id)
        }

        for (a in deleted) {
            maxUpdatedAt = maxOf(maxUpdatedAt, a.updatedAt)
            val ok = upsertIfNewer(
                collection.document(a.id),
                a.updatedAt,
                mapOf("id" to a.id, "deleted" to true, "updatedAt" to a.updatedAt, "createdAt" to a.createdAt)
            )
            if (ok) syncedIds.add(a.id)
        }

        if (syncedIds.isNotEmpty()) accountDao.markSyncedBatch(syncedIds.distinct())
        return maxUpdatedAt
    }

    private suspend fun pushAccountCardStyleSettings(doc: DocumentReference): Long {
        val prefs = preferencesDataStore.userPreferences.first()
        val updatedAt = when {
            prefs.accountCardStyleUpdatedAt > 0L -> prefs.accountCardStyleUpdatedAt
            prefs.accountCardStyle != AccountCardStyle.MATTE || prefs.accountCardStyleOverrides.isNotEmpty() -> {
                System.currentTimeMillis()
            }
            else -> 0L
        }
        if (updatedAt <= 0L) return 0L

        if (prefs.accountCardStyleUpdatedAt <= 0L) {
            preferencesDataStore.applyAccountCardStyleSync(
                style = prefs.accountCardStyle,
                overrides = prefs.accountCardStyleOverrides,
                updatedAt = updatedAt
            )
        }

        val didPush = upsertIfNewer(
            doc = doc,
            localUpdatedAt = updatedAt,
            data = mapOf(
                "accountCardStyle" to prefs.accountCardStyle.name,
                "accountCardStyleOverrides" to prefs.accountCardStyleOverrides.mapValues { (_, style) ->
                    style.name
                },
                "updatedAt" to updatedAt
            )
        )
        return if (didPush) updatedAt else 0L
    }

    private suspend fun pushCategories(collection: com.google.firebase.firestore.CollectionReference): Long {
        val unsynced = categoryDao.getUnsynced()
        val deleted = categoryDao.getDeleted()

        var maxUpdatedAt = 0L
        val syncedIds = mutableListOf<String>()

        for (c in unsynced) {
            maxUpdatedAt = maxOf(maxUpdatedAt, c.updatedAt)
            val ok = upsertIfNewer(collection.document(c.id), c.updatedAt, categoryToDoc(c))
            if (ok) syncedIds.add(c.id)
        }

        for (c in deleted) {
            maxUpdatedAt = maxOf(maxUpdatedAt, c.updatedAt)
            val ok = upsertIfNewer(
                collection.document(c.id),
                c.updatedAt,
                mapOf("id" to c.id, "deleted" to true, "updatedAt" to c.updatedAt, "createdAt" to c.createdAt)
            )
            if (ok) syncedIds.add(c.id)
        }

        if (syncedIds.isNotEmpty()) categoryDao.markSyncedBatch(syncedIds.distinct())
        return maxUpdatedAt
    }

    private suspend fun pushBudgets(collection: com.google.firebase.firestore.CollectionReference): Long {
        val unsynced = budgetDao.getUnsynced()
        val deleted = budgetDao.getDeleted()

        var maxUpdatedAt = 0L
        val syncedIds = mutableListOf<String>()

        for (b in unsynced) {
            maxUpdatedAt = maxOf(maxUpdatedAt, b.updatedAt)
            val ok = upsertIfNewer(collection.document(b.id), b.updatedAt, budgetToDoc(b))
            if (ok) syncedIds.add(b.id)
        }

        for (b in deleted) {
            maxUpdatedAt = maxOf(maxUpdatedAt, b.updatedAt)
            val ok = upsertIfNewer(
                collection.document(b.id),
                b.updatedAt,
                mapOf("id" to b.id, "deleted" to true, "updatedAt" to b.updatedAt, "createdAt" to b.createdAt)
            )
            if (ok) syncedIds.add(b.id)
        }

        if (syncedIds.isNotEmpty()) budgetDao.markSyncedBatch(syncedIds.distinct())
        return maxUpdatedAt
    }

    private suspend fun pushRecurringRules(collection: com.google.firebase.firestore.CollectionReference): Long {
        val unsynced = recurringRuleDao.getUnsynced()
        val deleted = recurringRuleDao.getDeleted()

        var maxUpdatedAt = 0L
        val syncedIds = mutableListOf<String>()

        for (r in unsynced) {
            maxUpdatedAt = maxOf(maxUpdatedAt, r.updatedAt)
            val ok = upsertIfNewer(collection.document(r.id), r.updatedAt, recurringRuleToDoc(r))
            if (ok) syncedIds.add(r.id)
        }

        for (r in deleted) {
            maxUpdatedAt = maxOf(maxUpdatedAt, r.updatedAt)
            val ok = upsertIfNewer(
                collection.document(r.id),
                r.updatedAt,
                mapOf("id" to r.id, "deleted" to true, "updatedAt" to r.updatedAt, "createdAt" to r.createdAt)
            )
            if (ok) syncedIds.add(r.id)
        }

        if (syncedIds.isNotEmpty()) recurringRuleDao.markSyncedBatch(syncedIds.distinct())
        return maxUpdatedAt
    }

    private suspend fun pushTransactions(collection: com.google.firebase.firestore.CollectionReference): Long {
        val unsynced = transactionDao.getUnsynced()
        val deleted = transactionDao.getDeleted()

        var maxUpdatedAt = 0L
        val syncedIds = mutableListOf<String>()

        for (t in unsynced) {
            maxUpdatedAt = maxOf(maxUpdatedAt, t.updatedAt)
            val ok = upsertIfNewer(collection.document(t.id), t.updatedAt, transactionToDoc(t))
            if (ok) syncedIds.add(t.id)
        }

        for (t in deleted) {
            maxUpdatedAt = maxOf(maxUpdatedAt, t.updatedAt)
            val ok = upsertIfNewer(
                collection.document(t.id),
                t.updatedAt,
                mapOf("id" to t.id, "deleted" to true, "updatedAt" to t.updatedAt, "createdAt" to t.createdAt)
            )
            if (ok) syncedIds.add(t.id)
        }

        if (syncedIds.isNotEmpty()) transactionDao.markSyncedBatch(syncedIds.distinct())
        return maxUpdatedAt
    }

    private suspend fun upsertIfNewer(doc: DocumentReference, localUpdatedAt: Long, data: Map<String, Any?>): Boolean {
        val firestore = doc.firestore
        return firestore.runTransaction { txn ->
            val snap = txn.get(doc)
            val remoteUpdatedAt = snap.getLong("updatedAt") ?: 0L
            if (!snap.exists() || remoteUpdatedAt <= localUpdatedAt) {
                txn.set(doc, data, SetOptions.merge())
                true
            } else {
                false
            }
        }.await()
    }

    // ---------------------------
    // Pull (Firestore -> local)
    // ---------------------------

    private suspend fun pullAccounts(
        collection: com.google.firebase.firestore.CollectionReference
    ): Long {
        val docs = queryAllDocuments(collection)
        if (docs.isEmpty()) return 0L

        var maxUpdatedAt = 0L
        val entities = docs.mapNotNull { snap ->
            val updatedAt = snap.getLong("updatedAt") ?: 0L
            maxUpdatedAt = maxOf(maxUpdatedAt, updatedAt)
            snapToAccount(snap)
        }

        appDatabase.withTransaction {
            for (remote in entities) {
                val local = accountDao.getByIdAnyOnce(remote.id)
                if (shouldPreserveUnsyncedLocal(local?.synced ?: true, local?.updatedAt ?: -1L, remote.updatedAt)) {
                    continue
                }

                if (remote.deleted) {
                    accountDao.softDelete(remote.id, remote.updatedAt)
                    accountDao.markSynced(remote.id)
                } else {
                    val merged = when {
                        local == null -> remote.copy(
                            currentBalance = remote.initialBalance,
                            synced = true,
                            deleted = false
                        )
                        local.deleted -> remote.copy(
                            currentBalance = remote.initialBalance,
                            synced = true,
                            deleted = false
                        )
                        else -> remote.copy(
                            initialBalance = local.initialBalance,
                            currentBalance = local.currentBalance,
                            synced = true,
                            deleted = false
                        )
                    }
                    if (local == null) {
                        accountDao.insert(merged)
                    } else {
                        accountDao.update(merged)
                    }
                }
            }
        }

        return maxUpdatedAt
    }

    private suspend fun pullAccountCardStyleSettings(doc: DocumentReference): Long {
        val snap = doc.get().await()
        if (!snap.exists()) return 0L

        val remoteUpdatedAt = snap.getLong("updatedAt") ?: 0L
        if (remoteUpdatedAt <= 0L) return 0L

        val localUpdatedAt = preferencesDataStore.userPreferences.first().accountCardStyleUpdatedAt
        if (localUpdatedAt > remoteUpdatedAt) return remoteUpdatedAt

        val style = runCatching {
            AccountCardStyle.valueOf(
                snap.getString("accountCardStyle").orEmpty().ifBlank { AccountCardStyle.MATTE.name }
            )
        }.getOrDefault(AccountCardStyle.MATTE)
        val overrides = (snap.get("accountCardStyleOverrides") as? Map<*, *>)
            .orEmpty()
            .mapNotNull { (key, value) ->
                val accountId = (key as? String)?.trim().orEmpty()
                val overrideStyle = runCatching {
                    AccountCardStyle.valueOf((value as? String)?.trim().orEmpty())
                }.getOrNull()
                if (accountId.isBlank() || overrideStyle == null) {
                    null
                } else {
                    accountId to overrideStyle
                }
            }
            .toMap()

        preferencesDataStore.applyAccountCardStyleSync(
            style = style,
            overrides = overrides,
            updatedAt = remoteUpdatedAt
        )
        return remoteUpdatedAt
    }

    private suspend fun pullCategories(
        collection: com.google.firebase.firestore.CollectionReference
    ): Long {
        val docs = queryAllDocuments(collection)
        if (docs.isEmpty()) return 0L

        var maxUpdatedAt = 0L
        val entities = docs.mapNotNull { snap ->
            val updatedAt = snap.getLong("updatedAt") ?: 0L
            maxUpdatedAt = maxOf(maxUpdatedAt, updatedAt)
            snapToCategory(snap)
        }

        val deleted = entities.filter { it.deleted }
        val alive = entities.filterNot { it.deleted }

        appDatabase.withTransaction {
            // apply deletions first
            for (remote in deleted) {
                val local = categoryDao.getByIdAnyOnce(remote.id)
                if (shouldPreserveUnsyncedLocal(local?.synced ?: true, local?.updatedAt ?: -1L, remote.updatedAt)) {
                    continue
                }
                categoryDao.softDelete(remote.id, remote.updatedAt)
                categoryDao.markSynced(remote.id)
            }

            // insert/update (parents first when possible)
            val byId = alive.associateBy { it.id }
            val visited = mutableSetOf<String>()
            val ordered = mutableListOf<CategoryEntity>()

            fun visit(id: String) {
                if (!visited.add(id)) return
                val e = byId[id] ?: return
                val parentId = e.parentCategoryId
                if (!parentId.isNullOrBlank() && byId.containsKey(parentId)) {
                    visit(parentId)
                }
                ordered.add(e)
            }

            for (id in byId.keys) visit(id)

            for (remote in ordered) {
                val local = categoryDao.getByIdAnyOnce(remote.id)
                if (shouldPreserveUnsyncedLocal(local?.synced ?: true, local?.updatedAt ?: -1L, remote.updatedAt)) {
                    continue
                }
                val merged = remote.copy(synced = true, deleted = false)
                if (local == null) {
                    categoryDao.insert(merged)
                } else {
                    categoryDao.update(merged)
                }
            }
        }

        return maxUpdatedAt
    }

    private suspend fun pullBudgets(
        collection: com.google.firebase.firestore.CollectionReference
    ): Long {
        val docs = queryAllDocuments(collection)
        if (docs.isEmpty()) return 0L

        var maxUpdatedAt = 0L
        val entities = docs.mapNotNull { snap ->
            val updatedAt = snap.getLong("updatedAt") ?: 0L
            maxUpdatedAt = maxOf(maxUpdatedAt, updatedAt)
            snapToBudget(snap)
        }

        appDatabase.withTransaction {
            for (remote in entities) {
                val local = budgetDao.getByIdAnyOnce(remote.id)
                if (shouldPreserveUnsyncedLocal(local?.synced ?: true, local?.updatedAt ?: -1L, remote.updatedAt)) {
                    continue
                }

                if (remote.deleted) {
                    budgetDao.softDelete(remote.id, remote.updatedAt)
                    budgetDao.markSynced(remote.id)
                } else {
                    budgetDao.insert(remote.copy(synced = true, deleted = false))
                }
            }
        }

        return maxUpdatedAt
    }

    private suspend fun pullRecurringRules(
        collection: com.google.firebase.firestore.CollectionReference
    ): Long {
        val docs = queryAllDocuments(collection)
        if (docs.isEmpty()) return 0L

        var maxUpdatedAt = 0L
        val entities = docs.mapNotNull { snap ->
            val updatedAt = snap.getLong("updatedAt") ?: 0L
            maxUpdatedAt = maxOf(maxUpdatedAt, updatedAt)
            snapToRecurringRule(snap)
        }

        appDatabase.withTransaction {
            for (remote in entities) {
                val local = recurringRuleDao.getByIdAnyOnce(remote.id)
                if (shouldPreserveUnsyncedLocal(local?.synced ?: true, local?.updatedAt ?: -1L, remote.updatedAt)) {
                    continue
                }

                if (remote.deleted) {
                    recurringRuleDao.softDelete(remote.id, remote.updatedAt)
                    recurringRuleDao.markSynced(remote.id)
                } else {
                    recurringRuleDao.insert(remote.copy(synced = true, deleted = false))
                }
            }
        }

        return maxUpdatedAt
    }

    private suspend fun pullTransactions(
        collection: com.google.firebase.firestore.CollectionReference
    ): Long {
        val docs = queryAllDocuments(collection)
        if (docs.isEmpty()) return 0L

        var maxUpdatedAt = 0L
        val entities = docs.mapNotNull { snap ->
            val updatedAt = snap.getLong("updatedAt") ?: 0L
            maxUpdatedAt = maxOf(maxUpdatedAt, updatedAt)
            snapToTransaction(snap)
        }

        appDatabase.withTransaction {
            for (remote in entities) {
                val local = transactionDao.getByIdAnyOnce(remote.id)
                if (shouldPreserveUnsyncedLocal(local?.synced ?: true, local?.updatedAt ?: -1L, remote.updatedAt)) {
                    continue
                }

                if (remote.deleted) {
                    // Reverse deltas if we had an active tx locally
                    if (local != null && !local.deleted) {
                        val reverse = deltasFor(local).mapValues { (_, v) -> -v }
                        applyAccountDeltas(reverse)
                    }
                    transactionDao.softDelete(remote.id, remote.updatedAt)
                    transactionDao.markSynced(remote.id)
                } else {
                    val oldDeltas = local?.takeIf { !it.deleted }?.let { deltasFor(it) }.orEmpty()
                    val newDeltas = deltasFor(remote)
                    val diff = diffDeltas(oldDeltas, newDeltas)
                    transactionDao.insert(remote.copy(synced = true, deleted = false))
                    applyAccountDeltas(diff)
                }
            }
        }

        return maxUpdatedAt
    }

    private suspend fun queryAllDocuments(
        collection: com.google.firebase.firestore.CollectionReference
    ): List<DocumentSnapshot> {
        val pageSize = 500L
        var query: Query = collection
            .orderBy("updatedAt", Query.Direction.ASCENDING)
            .limit(pageSize)

        val out = mutableListOf<DocumentSnapshot>()
        while (true) {
            val snap = query.get().await()
            val docs = snap.documents
            if (docs.isEmpty()) break
            out.addAll(docs)
            if (docs.size < pageSize) break
            query = query.startAfter(docs.last())
        }
        return out
    }

    private fun shouldPreserveUnsyncedLocal(
        localSynced: Boolean,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long
    ): Boolean {
        // Once a local row is already synced, the cloud copy becomes the source of truth.
        // Only block the incoming remote row when there is a pending unsynced local edit
        // that is newer than what the server currently has.
        return !localSynced && localUpdatedAt > remoteUpdatedAt
    }

    // ---------------------------
    // Local account delta helpers
    // ---------------------------

    private fun deltasFor(tx: TransactionEntity): Map<String, Long> {
        val amount = tx.amount
        val deltas = mutableMapOf<String, Long>()
        fun add(accountId: String?, delta: Long) {
            if (accountId.isNullOrBlank()) return
            deltas[accountId] = (deltas[accountId] ?: 0L) + delta
        }

        when (tx.type) {
            TransactionType.INCOME -> add(tx.accountId, amount)
            TransactionType.EXPENSE -> add(tx.accountId, -amount)
            TransactionType.TRANSFER -> {
                add(tx.accountId, -amount)
                add(tx.toAccountId, amount)
            }
        }
        return deltas
    }

    private fun diffDeltas(old: Map<String, Long>, new: Map<String, Long>): Map<String, Long> {
        if (old.isEmpty()) return new
        if (new.isEmpty()) return old.mapValues { (_, v) -> -v }

        val keys = old.keys + new.keys
        val out = mutableMapOf<String, Long>()
        for (k in keys) {
            val delta = (new[k] ?: 0L) - (old[k] ?: 0L)
            if (delta != 0L) out[k] = delta
        }
        return out
    }

    private suspend fun applyAccountDeltas(deltas: Map<String, Long>) {
        if (deltas.isEmpty()) return
        for ((accountId, delta) in deltas) {
            if (delta == 0L) continue
            val account = accountDao.getByIdAnyOnce(accountId) ?: continue
            if (account.deleted) continue
            val newBalance = account.currentBalance + delta
            accountDao.updateBalance(accountId, newBalance)
        }
    }

    private suspend fun repairInvalidCategoryLinksAndScopes() {
        appDatabase.withTransaction {
            val categories = categoryDao.getAllAliveOnce()
            if (categories.isEmpty()) return@withTransaction

            val categoriesById = categories.associateBy { it.id }
            val expenseCategoriesByName = categories
                .filter { it.type == CategoryType.EXPENSE }
                .groupBy { normalizeLabel(it.name) }
            val incomeCategoriesByName = categories
                .filter { it.type == CategoryType.INCOME }
                .groupBy { normalizeLabel(it.name) }

            val now = System.currentTimeMillis()
            transactionDao.getAllAliveOnce().forEach { tx ->
                val repairedCategoryId = resolveTransactionCategoryId(
                    tx = tx,
                    categoriesById = categoriesById,
                    expenseCategoriesByName = expenseCategoriesByName,
                    incomeCategoriesByName = incomeCategoriesByName
                )
                if (tx.categoryId != repairedCategoryId) {
                    transactionDao.update(
                        tx.copy(
                            categoryId = repairedCategoryId,
                            synced = false,
                            updatedAt = maxOf(now, tx.updatedAt + 1L)
                        )
                    )
                }
            }

            budgetDao.getAllAliveOnce().forEach { budget ->
                val repairedCategoryId = resolveScopedExpenseCategoryId(
                    currentCategoryId = budget.categoryId,
                    categoriesById = categoriesById,
                    expenseCategoriesByName = expenseCategoriesByName
                )
                if (budget.categoryId != repairedCategoryId) {
                    budgetDao.update(
                        budget.copy(
                            categoryId = repairedCategoryId,
                            synced = false,
                            updatedAt = maxOf(now, budget.updatedAt + 1L)
                        )
                    )
                }
            }

            recurringRuleDao.getAllAliveOnce().forEach { rule ->
                val repairedCategoryId = when (rule.type) {
                    TransactionType.EXPENSE -> resolveScopedExpenseCategoryId(
                        currentCategoryId = rule.categoryId,
                        categoriesById = categoriesById,
                        expenseCategoriesByName = expenseCategoriesByName
                    )
                    TransactionType.INCOME -> resolveScopedIncomeCategoryId(
                        currentCategoryId = rule.categoryId,
                        categoriesById = categoriesById,
                        incomeCategoriesByName = incomeCategoriesByName
                    )
                    TransactionType.TRANSFER -> null
                }
                if (rule.categoryId != repairedCategoryId) {
                    recurringRuleDao.update(
                        rule.copy(
                            categoryId = repairedCategoryId,
                            synced = false,
                            updatedAt = maxOf(now, rule.updatedAt + 1L)
                        )
                    )
                }
            }
        }
    }

    private fun resolveTransactionCategoryId(
        tx: TransactionEntity,
        categoriesById: Map<String, CategoryEntity>,
        expenseCategoriesByName: Map<String, List<CategoryEntity>>,
        incomeCategoriesByName: Map<String, List<CategoryEntity>>
    ): String? {
        return when (tx.type) {
            TransactionType.EXPENSE -> {
                val currentCategory = tx.categoryId?.let(categoriesById::get)
                when {
                    currentCategory?.type == CategoryType.EXPENSE -> currentCategory.id
                    else -> expenseCategoriesByName[normalizeLabel(tx.title)].orEmpty().singleOrNull()?.id
                }
            }
            TransactionType.INCOME -> {
                val currentCategory = tx.categoryId?.let(categoriesById::get)
                when {
                    currentCategory?.type == CategoryType.INCOME -> currentCategory.id
                    else -> incomeCategoriesByName[normalizeLabel(tx.title)].orEmpty().singleOrNull()?.id
                }
            }
            TransactionType.TRANSFER -> null
        }
    }

    private fun resolveScopedExpenseCategoryId(
        currentCategoryId: String?,
        categoriesById: Map<String, CategoryEntity>,
        expenseCategoriesByName: Map<String, List<CategoryEntity>>
    ): String? {
        val currentCategory = currentCategoryId?.let(categoriesById::get) ?: return null
        if (currentCategory.type == CategoryType.EXPENSE) return currentCategory.id
        return expenseCategoriesByName[normalizeLabel(currentCategory.name)].orEmpty().singleOrNull()?.id
    }

    private fun resolveScopedIncomeCategoryId(
        currentCategoryId: String?,
        categoriesById: Map<String, CategoryEntity>,
        incomeCategoriesByName: Map<String, List<CategoryEntity>>
    ): String? {
        val currentCategory = currentCategoryId?.let(categoriesById::get) ?: return null
        if (currentCategory.type == CategoryType.INCOME) return currentCategory.id
        return incomeCategoriesByName[normalizeLabel(currentCategory.name)].orEmpty().singleOrNull()?.id
    }

    private suspend fun rebaseLocalAccountInitialBalances() {
        appDatabase.withTransaction {
            val accounts = accountDao.getAllAliveOnce()
            if (accounts.isEmpty()) return@withTransaction

            val deltas = mutableMapOf<String, Long>()
            fun add(accountId: String?, delta: Long) {
                if (accountId.isNullOrBlank() || delta == 0L) return
                deltas[accountId] = (deltas[accountId] ?: 0L) + delta
            }

            transactionDao.getAllAliveOnce().forEach { tx ->
                when (tx.type) {
                    TransactionType.INCOME -> add(tx.accountId, tx.amount)
                    TransactionType.EXPENSE -> add(tx.accountId, -tx.amount)
                    TransactionType.TRANSFER -> {
                        add(tx.accountId, -tx.amount)
                        add(tx.toAccountId, tx.amount)
                    }
                }
            }

            val now = System.currentTimeMillis()
            accounts.forEach { account ->
                val effectiveInitialBalance = account.currentBalance - (deltas[account.id] ?: 0L)
                if (effectiveInitialBalance != account.initialBalance) {
                    accountDao.rebaseBalanceState(
                        id = account.id,
                        initialBalance = effectiveInitialBalance,
                        currentBalance = account.currentBalance,
                        updatedAt = maxOf(now, account.updatedAt + 1L)
                    )
                }
            }
        }
    }

    private suspend fun recalculateAccountBalancesFromTransactions() {
        appDatabase.withTransaction {
            val accounts = accountDao.getAllAliveOnce()
            if (accounts.isEmpty()) return@withTransaction

            val deltas = mutableMapOf<String, Long>()
            fun add(accountId: String?, delta: Long) {
                if (accountId.isNullOrBlank() || delta == 0L) return
                deltas[accountId] = (deltas[accountId] ?: 0L) + delta
            }

            val transactions = transactionDao.getAllAliveOnce()
            for (tx in transactions) {
                when (tx.type) {
                    TransactionType.INCOME -> add(tx.accountId, tx.amount)
                    TransactionType.EXPENSE -> add(tx.accountId, -tx.amount)
                    TransactionType.TRANSFER -> {
                        add(tx.accountId, -tx.amount)
                        add(tx.toAccountId, tx.amount)
                    }
                }
            }

            for (account in accounts) {
                val newBalance = account.initialBalance + (deltas[account.id] ?: 0L)
                accountDao.updateBalance(account.id, newBalance)
            }
        }
    }

    // ---------------------------
    // Firestore doc mapping
    // ---------------------------

    private fun accountToDoc(a: AccountEntity): Map<String, Any?> = mapOf(
        "id" to a.id,
        "name" to a.name,
        "type" to a.type.name,
        "currency" to a.currency,
        "initialBalance" to a.initialBalance,
        "colorHex" to a.colorHex,
        "iconName" to a.iconName,
        "isDefault" to a.isDefault,
        "isArchived" to a.isArchived,
        "sortOrder" to a.sortOrder,
        "deleted" to a.deleted,
        "createdAt" to a.createdAt,
        "updatedAt" to a.updatedAt
    )

    private fun categoryToDoc(c: CategoryEntity): Map<String, Any?> = mapOf(
        "id" to c.id,
        "name" to c.name,
        "type" to c.type.name,
        "parentCategoryId" to c.parentCategoryId,
        "colorHex" to c.colorHex,
        "iconName" to c.iconName,
        "isDefault" to c.isDefault,
        "sortOrder" to c.sortOrder,
        "deleted" to c.deleted,
        "createdAt" to c.createdAt,
        "updatedAt" to c.updatedAt
    )

    private fun budgetToDoc(b: BudgetEntity): Map<String, Any?> = mapOf(
        "id" to b.id,
        "name" to b.name,
        "categoryId" to b.categoryId,
        "accountId" to b.accountId,
        "amountLimitCents" to b.amountLimitCents,
        "period" to b.period.name,
        "startDate" to b.startDate,
        "endDate" to b.endDate,
        "alertThresholdPercent" to b.alertThresholdPercent,
        "isActive" to b.isActive,
        "isRecurring" to b.isRecurring,
        "seriesId" to b.seriesId,
        "deleted" to b.deleted,
        "createdAt" to b.createdAt,
        "updatedAt" to b.updatedAt
    )

    private fun recurringRuleToDoc(r: RecurringRuleEntity): Map<String, Any?> = mapOf(
        "id" to r.id,
        "accountId" to r.accountId,
        "toAccountId" to r.toAccountId,
        "categoryId" to r.categoryId,
        "amount" to r.amount,
        "currency" to r.currency,
        "type" to r.type.name,
        "title" to r.title,
        "notes" to r.notes,
        "frequency" to r.frequency.name,
        "interval" to r.interval,
        "dayOfWeek" to r.dayOfWeek,
        "dayOfMonth" to r.dayOfMonth,
        "monthOfYear" to r.monthOfYear,
        "startDate" to r.startDate,
        "endDate" to r.endDate,
        "nextOccurrence" to r.nextOccurrence,
        "lastProcessed" to r.lastProcessed,
        "isActive" to r.isActive,
        "deleted" to r.deleted,
        "createdAt" to r.createdAt,
        "updatedAt" to r.updatedAt
    )

    private fun transactionToDoc(t: TransactionEntity): Map<String, Any?> = mapOf(
        "id" to t.id,
        "accountId" to t.accountId,
        "toAccountId" to t.toAccountId,
        "categoryId" to t.categoryId,
        "amount" to t.amount,
        "currency" to t.currency,
        "type" to t.type.name,
        "title" to t.title,
        "notes" to t.notes,
        "dateTime" to t.dateTime,
        "receiptUri" to t.receiptUri,
        "isRecurring" to t.isRecurring,
        "recurringRuleId" to t.recurringRuleId,
        "tags" to t.tags,
        "deleted" to t.deleted,
        "createdAt" to t.createdAt,
        "updatedAt" to t.updatedAt
    )

    private fun snapToAccount(s: DocumentSnapshot): AccountEntity? {
        val id = s.getString("id") ?: s.id
        val type = enumOrNull<AccountType>(s.getString("type")) ?: AccountType.CASH
        val updatedAt = s.getLong("updatedAt") ?: 0L
        val createdAt = s.getLong("createdAt") ?: updatedAt
        return AccountEntity(
            id = id,
            name = s.getString("name").orEmpty(),
            type = type,
            currency = s.getString("currency").orEmpty().ifBlank { "USD" },
            initialBalance = s.getLong("initialBalance") ?: 0L,
            currentBalance = 0L, // will be preserved/derived locally
            colorHex = s.getString("colorHex").orEmpty().ifBlank { "#81C784" },
            iconName = s.getString("iconName").orEmpty().ifBlank { "wallet" },
            isDefault = s.getBoolean("isDefault") ?: false,
            isArchived = s.getBoolean("isArchived") ?: false,
            sortOrder = (s.getLong("sortOrder") ?: 0L).toInt(),
            synced = true,
            deleted = s.getBoolean("deleted") ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun snapToCategory(s: DocumentSnapshot): CategoryEntity? {
        val id = s.getString("id") ?: s.id
        val type = enumOrNull<CategoryType>(s.getString("type")) ?: CategoryType.EXPENSE
        val updatedAt = s.getLong("updatedAt") ?: 0L
        val createdAt = s.getLong("createdAt") ?: updatedAt
        return CategoryEntity(
            id = id,
            name = s.getString("name").orEmpty(),
            type = type,
            parentCategoryId = s.getString("parentCategoryId"),
            colorHex = s.getString("colorHex").orEmpty().ifBlank { "#F9D648" },
            iconName = s.getString("iconName").orEmpty().ifBlank { "category" },
            isDefault = s.getBoolean("isDefault") ?: false,
            sortOrder = (s.getLong("sortOrder") ?: 0L).toInt(),
            synced = true,
            deleted = s.getBoolean("deleted") ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun snapToBudget(s: DocumentSnapshot): BudgetEntity? {
        val id = s.getString("id") ?: s.id
        val period = enumOrNull<BudgetPeriod>(s.getString("period")) ?: BudgetPeriod.MONTHLY
        val updatedAt = s.getLong("updatedAt") ?: 0L
        val createdAt = s.getLong("createdAt") ?: updatedAt
        return BudgetEntity(
            id = id,
            name = s.getString("name").orEmpty(),
            categoryId = s.getString("categoryId"),
            accountId = s.getString("accountId"),
            amountLimitCents = s.getLong("amountLimitCents") ?: 0L,
            period = period,
            startDate = s.getLong("startDate") ?: 0L,
            endDate = s.getLong("endDate"),
            alertThresholdPercent = (s.getLong("alertThresholdPercent") ?: 80L).toInt(),
            isActive = s.getBoolean("isActive") ?: true,
            synced = true,
            deleted = s.getBoolean("deleted") ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isRecurring = s.getBoolean("isRecurring") ?: false,
            seriesId = s.getString("seriesId")
        )
    }

    private fun snapToRecurringRule(s: DocumentSnapshot): RecurringRuleEntity? {
        val id = s.getString("id") ?: s.id
        val type = enumOrNull<TransactionType>(s.getString("type")) ?: TransactionType.EXPENSE
        val freq = enumOrNull<RecurringFrequency>(s.getString("frequency")) ?: RecurringFrequency.MONTHLY
        val updatedAt = s.getLong("updatedAt") ?: 0L
        val createdAt = s.getLong("createdAt") ?: updatedAt
        return RecurringRuleEntity(
            id = id,
            accountId = s.getString("accountId").orEmpty(),
            toAccountId = s.getString("toAccountId"),
            categoryId = s.getString("categoryId"),
            amount = s.getLong("amount") ?: 0L,
            currency = s.getString("currency").orEmpty().ifBlank { "USD" },
            type = type,
            title = s.getString("title").orEmpty(),
            notes = s.getString("notes"),
            frequency = freq,
            interval = (s.getLong("interval") ?: 1L).toInt().coerceAtLeast(1),
            dayOfWeek = s.getLong("dayOfWeek")?.toInt(),
            dayOfMonth = s.getLong("dayOfMonth")?.toInt(),
            monthOfYear = s.getLong("monthOfYear")?.toInt(),
            startDate = s.getLong("startDate") ?: 0L,
            endDate = s.getLong("endDate"),
            nextOccurrence = s.getLong("nextOccurrence") ?: 0L,
            lastProcessed = s.getLong("lastProcessed"),
            isActive = s.getBoolean("isActive") ?: true,
            synced = true,
            deleted = s.getBoolean("deleted") ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun snapToTransaction(s: DocumentSnapshot): TransactionEntity? {
        val id = s.getString("id") ?: s.id
        val type = enumOrNull<TransactionType>(s.getString("type")) ?: TransactionType.EXPENSE
        val updatedAt = s.getLong("updatedAt") ?: 0L
        val createdAt = s.getLong("createdAt") ?: updatedAt
        val tags = (s.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty()
        return TransactionEntity(
            id = id,
            accountId = s.getString("accountId").orEmpty(),
            toAccountId = s.getString("toAccountId"),
            categoryId = s.getString("categoryId"),
            amount = s.getLong("amount") ?: 0L,
            currency = s.getString("currency").orEmpty().ifBlank { "USD" },
            type = type,
            title = s.getString("title").orEmpty(),
            notes = s.getString("notes"),
            dateTime = s.getLong("dateTime") ?: 0L,
            receiptUri = s.getString("receiptUri"),
            isRecurring = s.getBoolean("isRecurring") ?: false,
            recurringRuleId = s.getString("recurringRuleId"),
            tags = tags,
            synced = true,
            deleted = s.getBoolean("deleted") ?: false,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private inline fun <reified T : Enum<T>> enumOrNull(value: String?): T? {
        if (value.isNullOrBlank()) return null
        return runCatching { enumValueOf<T>(value) }.getOrNull()
    }

    private fun normalizeLabel(value: String): String {
        return value.trim().lowercase().replace(Regex("""\s+"""), " ")
    }
}


