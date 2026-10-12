package com.chelumore.xpenze.domain.usecase.budget

import com.chelumore.xpenze.domain.model.Budget
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.domain.model.BudgetWithProgress
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.BudgetRepository
import com.chelumore.xpenze.domain.repository.ThemeMode
import com.chelumore.xpenze.domain.repository.UserPreferences
import com.chelumore.xpenze.domain.repository.UserRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessRecurringBudgetsUseCaseTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun `premium user gets next recurring budget period after expiry`() = runTest {
        val original = sampleBudget(
            id = "monthly-food",
            startDate = millis(2026, 3, 1),
            endDate = millis(2026, 3, 31, 23, 59, 59, 999),
            isRecurring = true
        )
        val budgets = FakeBudgetRepository(listOf(original))
        val useCase = ProcessRecurringBudgetsUseCase(
            budgetRepository = budgets,
            userRepository = FakeUserRepository(UserPreferences(isPremium = true))
        )

        useCase(nowMillis = millis(2026, 4, 2))

        val stored = budgets.getAll().first().sortedBy { it.startDate }
        assertEquals(2, stored.size)
        assertEquals(original.id, stored.first().seriesId)
        assertEquals(millis(2026, 4, 1), stored.last().startDate)
        assertEquals(millis(2026, 4, 30, 23, 59, 59, 999), stored.last().endDate)
        assertEquals(original.id, stored.last().seriesId)
        assertTrue(stored.last().isRecurring)
        assertFalse(stored.last().synced)
    }

    @Test
    fun `recurring rollover does not create duplicate next periods`() = runTest {
        val original = sampleBudget(
            id = "monthly-food",
            startDate = millis(2026, 3, 1),
            endDate = millis(2026, 3, 31, 23, 59, 59, 999),
            isRecurring = true,
            seriesId = "monthly-food"
        )
        val next = sampleBudget(
            id = "monthly-food-next",
            startDate = millis(2026, 4, 1),
            endDate = millis(2026, 4, 30, 23, 59, 59, 999),
            isRecurring = true,
            seriesId = "monthly-food"
        )
        val budgets = FakeBudgetRepository(listOf(original, next))
        val useCase = ProcessRecurringBudgetsUseCase(
            budgetRepository = budgets,
            userRepository = FakeUserRepository(UserPreferences(isPremium = true))
        )

        useCase(nowMillis = millis(2026, 4, 2))

        val stored = budgets.getAll().first()
        assertEquals(2, stored.size)
        assertEquals(1, stored.count { it.startDate == millis(2026, 4, 1) })
    }

    @Test
    fun `free user does not get recurring budget rollover`() = runTest {
        val original = sampleBudget(
            id = "monthly-food",
            startDate = millis(2026, 3, 1),
            endDate = millis(2026, 3, 31, 23, 59, 59, 999),
            isRecurring = true
        )
        val budgets = FakeBudgetRepository(listOf(original))
        val useCase = ProcessRecurringBudgetsUseCase(
            budgetRepository = budgets,
            userRepository = FakeUserRepository(UserPreferences(isPremium = false))
        )

        useCase(nowMillis = millis(2026, 4, 2))

        assertEquals(1, budgets.getAll().first().size)
    }

    private fun sampleBudget(
        id: String,
        startDate: Long,
        endDate: Long?,
        isRecurring: Boolean,
        seriesId: String? = null
    ): Budget = Budget(
        id = id,
        name = "Food",
        categoryId = null,
        accountId = null,
        amountLimitCents = 100_00L,
        period = BudgetPeriod.MONTHLY,
        startDate = startDate,
        endDate = endDate,
        alertThresholdPercent = 80,
        isActive = true,
        synced = true,
        deleted = false,
        createdAt = startDate,
        updatedAt = startDate,
        isRecurring = isRecurring,
        seriesId = seriesId
    )

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millis: Int = 0
    ): Long = ZonedDateTime.of(
        year,
        month,
        day,
        hour,
        minute,
        second,
        millis * 1_000_000,
        zone
    ).toInstant().toEpochMilli()
}

private class FakeBudgetRepository(
    initial: List<Budget>
) : BudgetRepository {
    private val budgets = MutableStateFlow(initial)

    override fun getAll(): Flow<List<Budget>> = budgets

    override fun getActive(): Flow<List<Budget>> =
        budgets.map { list -> list.filter { it.isActive && !it.deleted } }

    override fun getById(id: String): Flow<Budget?> =
        budgets.map { list -> list.firstOrNull { it.id == id && !it.deleted } }

    override fun getByCategory(categoryId: String): Flow<List<Budget>> =
        budgets.map { list -> list.filter { it.categoryId == categoryId && !it.deleted } }

    override fun getCurrentBudgets(): Flow<List<Budget>> =
        budgets.map { list -> list.filter { it.isActive && !it.deleted } }

    override fun getPastBudgets(): Flow<List<Budget>> =
        budgets.map { emptyList<Budget>() }

    override fun getAllWithProgress(): Flow<List<BudgetWithProgress>> =
        budgets.map { emptyList<BudgetWithProgress>() }

    override fun getActiveWithProgress(): Flow<List<BudgetWithProgress>> =
        budgets.map { emptyList<BudgetWithProgress>() }

    override suspend fun insert(budget: Budget) {
        budgets.value = budgets.value.filterNot { it.id == budget.id } + budget
    }

    override suspend fun update(budget: Budget) {
        insert(budget)
    }

    override suspend fun delete(id: String) {
        budgets.value = budgets.value.map { if (it.id == id) it.copy(deleted = true) else it }
    }

    override suspend fun setActive(id: String, isActive: Boolean) {
        budgets.value = budgets.value.map { if (it.id == id) it.copy(isActive = isActive) else it }
    }

    override suspend fun getUnsynced(): List<Budget> =
        budgets.value.filter { !it.synced && !it.deleted }

    override suspend fun markSynced(id: String) {
        budgets.value = budgets.value.map { if (it.id == id) it.copy(synced = true) else it }
    }
}

private class FakeUserRepository(
    initial: UserPreferences
) : UserRepository {
    private val prefs = MutableStateFlow(initial)
    override val userPreferences: Flow<UserPreferences> = prefs
    override val isLoggedIn: Flow<Boolean> = prefs.map { !it.userId.isNullOrBlank() }
    override val isPremium: Flow<Boolean> = prefs.map { it.isPremium }
    override val aiChatHistoryJson: Flow<String?> = MutableStateFlow(null)

    override suspend fun setOnboardingComplete(complete: Boolean) {
        prefs.value = prefs.value.copy(isOnboardingComplete = complete)
    }

    override suspend fun setAppLanguageTag(languageTag: String) {
        prefs.value = prefs.value.copy(appLanguageTag = languageTag)
    }

    override suspend fun setAccountCardStyle(style: AccountCardStyle) {
        prefs.value = prefs.value.copy(accountCardStyle = style)
    }

    override suspend fun setAccountCardStyleOverride(accountId: String, style: AccountCardStyle?) {
        val overrides = prefs.value.accountCardStyleOverrides.toMutableMap()
        if (style == null) overrides.remove(accountId) else overrides[accountId] = style
        prefs.value = prefs.value.copy(accountCardStyleOverrides = overrides)
    }

    override suspend fun setBalancesHidden(hidden: Boolean) {
        prefs.value = prefs.value.copy(areBalancesHidden = hidden)
    }

    override suspend fun setDefaultCurrency(currency: String) {
        prefs.value = prefs.value.copy(defaultCurrency = currency)
    }

    override suspend fun setDefaultAccountId(accountId: String?) {
        prefs.value = prefs.value.copy(defaultAccountId = accountId)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.value = prefs.value.copy(themeMode = mode)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        prefs.value = prefs.value.copy(notificationsEnabled = enabled)
    }

    override suspend fun setBudgetAlertEnabled(enabled: Boolean) {
        prefs.value = prefs.value.copy(budgetAlertEnabled = enabled)
    }

    override suspend fun setNotificationPermissionPromptLastShownAt(timestampMillis: Long) {
        prefs.value = prefs.value.copy(notificationPermissionPromptLastShownAt = timestampMillis)
    }

    override suspend fun incrementAppOpenCount() {
        prefs.value = prefs.value.copy(appOpenCount = prefs.value.appOpenCount + 1)
    }

    override suspend fun incrementRateEligibleActionCount() {
        prefs.value = prefs.value.copy(
            rateEligibleActionCount = prefs.value.rateEligibleActionCount + 1
        )
    }

    override suspend fun setRateAppPromptLastShownAt(timestampMillis: Long) {
        prefs.value = prefs.value.copy(rateAppPromptLastShownAt = timestampMillis)
    }

    override suspend fun setHasRatedOnPlayStore(rated: Boolean) {
        prefs.value = prefs.value.copy(hasRatedOnPlayStore = rated)
    }

    override suspend fun updateLastSyncTime(time: Long) {
        prefs.value = prefs.value.copy(lastSyncTime = time)
    }

    override suspend fun setGuestMode(enabled: Boolean) {
        prefs.value = prefs.value.copy(isGuestMode = enabled)
    }

    override suspend fun enterGuestMode() {
        prefs.value = prefs.value.copy(
            isGuestMode = true,
            isPremium = false,
            trialStartedAt = 0L,
            trialExpiresAt = 0L,
            trialConsumed = false
        )
    }

    override suspend fun promoteGuestToUser(
        userId: String,
        email: String?,
        name: String?,
        photoUrl: String?
    ) {
        prefs.value = prefs.value.copy(
            isGuestMode = false,
            userId = userId,
            userEmail = email,
            userName = name,
            userPhotoUrl = photoUrl
        )
    }

    override suspend fun setUserInfo(userId: String?, email: String?, name: String?, photoUrl: String?) {
        prefs.value = prefs.value.copy(
            userId = userId,
            userEmail = email,
            userName = name,
            userPhotoUrl = photoUrl
        )
    }

    override suspend fun setAutoCaptureTutorialSeen(seen: Boolean) = Unit

    override suspend fun setAiCoachCtaSeen(seen: Boolean) {
        prefs.value = prefs.value.copy(hasSeenAiCoachCta = seen)
    }

    override suspend fun setPremium(isPremium: Boolean) {
        prefs.value = prefs.value.copy(isPremium = isPremium)
    }

    override suspend fun setAiChatHistoryJson(historyJson: String?) = Unit

    override suspend fun setTrialState(startedAt: Long, expiresAt: Long, consumed: Boolean) {
        prefs.value = prefs.value.copy(
            trialStartedAt = startedAt,
            trialExpiresAt = expiresAt,
            trialConsumed = consumed
        )
    }

    override suspend fun clearTrialState() {
        prefs.value = prefs.value.copy(
            trialStartedAt = 0L,
            trialExpiresAt = 0L,
            trialConsumed = false
        )
    }

    override suspend fun setFounderRewardState(
        hasFounderReward: Boolean,
        founderTrialActivated: Boolean,
        founderRewardProductId: String?
    ) {
        prefs.value = prefs.value.copy(
            hasFounderReward = hasFounderReward,
            founderTrialActivated = founderTrialActivated,
            founderRewardProductId = founderRewardProductId
        )
    }

    override suspend fun clearFounderRewardState() {
        prefs.value = prefs.value.copy(
            hasFounderReward = false,
            founderTrialActivated = false,
            founderRewardProductId = null
        )
    }

    override suspend fun clearUserData() {
        prefs.value = UserPreferences()
    }
}
