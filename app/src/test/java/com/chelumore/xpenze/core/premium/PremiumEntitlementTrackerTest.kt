package com.chelumore.xpenze.core.premium

import app.cash.turbine.test
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.domain.repository.ThemeMode
import com.chelumore.xpenze.domain.repository.UserPreferences
import com.chelumore.xpenze.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumEntitlementTrackerTest {

    @Test
    fun `trial entitlement auto-expires while app stays open`() = runTest {
        var now = 1_000L
        val repository = FakeUserRepository(
            UserPreferences(
                userId = "user-1",
                isPremium = false,
                trialConsumed = true,
                trialStartedAt = 500L,
                trialExpiresAt = 2_000L
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = PremiumEntitlementTracker(
            userRepository = repository,
            defaultDispatcher = dispatcher,
            nowProvider = { now },
            tickMs = 100L
        )

        try {
            tracker.entitlement.test {
                val first = awaitItem()
                val active = if (first.isTrialActive) first else awaitItem()
                assertTrue(active.isTrialActive)
                assertTrue(active.hasPremiumAccess)

                now = 2_500L
                advanceTimeBy(100L)
                val expired = awaitItem()
                assertFalse(expired.isTrialActive)
                assertFalse(expired.hasPremiumAccess)
            }
        } finally {
            tracker.cancelForTests()
        }
    }

    @Test
    fun `guest leftover trial dates do not grant premium access`() = runTest {
        var now = 1_000L
        val repository = FakeUserRepository(
            UserPreferences(
                userId = null,
                isGuestMode = true,
                isPremium = false,
                trialConsumed = true,
                trialStartedAt = 500L,
                trialExpiresAt = 50_000L
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = PremiumEntitlementTracker(
            userRepository = repository,
            defaultDispatcher = dispatcher,
            nowProvider = { now },
            tickMs = 100L
        )

        try {
            tracker.entitlement.test {
                val first = awaitItem()
                val stable = if (!first.hasPremiumAccess && !first.isTrialActive) first else awaitItem()
                assertFalse(stable.isTrialActive)
                assertFalse(stable.hasPremiumAccess)
                assertFalse(stable.isTrialEndingSoon)
            }
        } finally {
            tracker.cancelForTests()
        }
    }

    @Test
    fun `trial ending soon is true inside final two day window`() = runTest {
        val dayMs = 24L * 60L * 60L * 1000L
        var now = 1_000L
        val repository = FakeUserRepository(
            UserPreferences(
                userId = "user-1",
                isPremium = false,
                trialConsumed = true,
                trialStartedAt = 500L,
                trialExpiresAt = now + dayMs
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = PremiumEntitlementTracker(
            userRepository = repository,
            defaultDispatcher = dispatcher,
            nowProvider = { now },
            tickMs = 100L
        )

        try {
            tracker.entitlement.test {
                val first = awaitItem()
                val active = if (first.isTrialActive) first else awaitItem()
                assertTrue(active.isTrialActive)
                assertTrue(active.isTrialEndingSoon)
            }
        } finally {
            tracker.cancelForTests()
        }
    }

    @Test
    fun `go premium cta follows full seven day trial lifecycle without app restart`() = runTest {
        val dayMs = 24L * 60L * 60L * 1000L
        val trialStart = 1_000_000L
        var now = trialStart
        val repository = FakeUserRepository(
            UserPreferences(
                userId = "user-1",
                isPremium = false,
                trialConsumed = true,
                trialStartedAt = trialStart,
                trialExpiresAt = trialStart + 7 * dayMs
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = PremiumEntitlementTracker(
            userRepository = repository,
            defaultDispatcher = dispatcher,
            nowProvider = { now },
            tickMs = 100L
        )

        try {
            tracker.entitlement.test {
                // Day 1: trial active, CTA hidden.
                var state = awaitItem()
                if (!state.isTrialActive) state = awaitItem()
                assertTrue(state.isTrialActive)
                assertFalse(shouldShowPremiumCta(state.hasPremiumAccess, state.isTrialEndingSoon))

                // Day 4: still mid-trial, CTA hidden. Same state, so no new emission expected.
                now = trialStart + 4 * dayMs
                advanceTimeBy(100L)
                state = tracker.entitlement.value
                assertTrue(state.isTrialActive)
                assertFalse(state.isTrialEndingSoon)
                assertFalse(shouldShowPremiumCta(state.hasPremiumAccess, state.isTrialEndingSoon))
                cancelAndIgnoreRemainingEvents()
            }

            // Day 6: last two days, "Keep Premium" CTA visible.
            now = trialStart + 6 * dayMs
            advanceTimeBy(100L)
            var state = tracker.entitlement.value
            assertTrue(state.isTrialActive)
            assertTrue(state.isTrialEndingSoon)
            assertTrue(shouldShowPremiumCta(state.hasPremiumAccess, state.isTrialEndingSoon))

            // Day 8: trial over, access gone, "Go Premium" CTA back.
            now = trialStart + 8 * dayMs
            advanceTimeBy(100L)
            state = tracker.entitlement.value
            assertFalse(state.isTrialActive)
            assertFalse(state.hasPremiumAccess)
            assertFalse(state.isTrialEndingSoon)
            assertTrue(shouldShowPremiumCta(state.hasPremiumAccess, state.isTrialEndingSoon))
        } finally {
            tracker.cancelForTests()
        }
    }

    @Test
    fun `go premium cta hidden for paid subscribers`() {
        assertFalse(shouldShowPremiumCta(hasPremiumAccess = true, isTrialEndingSoon = false))
    }

    @Test
    fun `go premium cta visible for free users with no trial`() {
        assertTrue(shouldShowPremiumCta(hasPremiumAccess = false, isTrialEndingSoon = false))
    }

    @Test
    fun `paid premium remains active even after trial expiry`() = runTest {
        var now = 5_000L
        val repository = FakeUserRepository(
            UserPreferences(
                userId = "user-1",
                isPremium = true,
                trialConsumed = true,
                trialStartedAt = 1_000L,
                trialExpiresAt = 2_000L
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val tracker = PremiumEntitlementTracker(
            userRepository = repository,
            defaultDispatcher = dispatcher,
            nowProvider = { now },
            tickMs = 100L
        )

        try {
            tracker.entitlement.test {
                val first = awaitItem()
                val stable = if (first.hasPremiumAccess) first else awaitItem()
                assertTrue(stable.hasPremiumAccess)
                assertFalse(stable.isTrialActive)
            }
        } finally {
            tracker.cancelForTests()
        }
    }

    private class FakeUserRepository(
        initial: UserPreferences
    ) : UserRepository {
        private val prefs = MutableStateFlow(initial)
        private val chatHistory = MutableStateFlow<String?>(null)

        override val userPreferences: Flow<UserPreferences> = prefs
        override val isLoggedIn: Flow<Boolean> = prefs.map { !it.userId.isNullOrBlank() }
        override val isPremium: Flow<Boolean> = prefs.map { it.isPremium }
        override val aiChatHistoryJson: Flow<String?> = chatHistory

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
            val current = prefs.value.accountCardStyleOverrides.toMutableMap()
            if (style == null) current.remove(accountId) else current[accountId] = style
            prefs.value = prefs.value.copy(accountCardStyleOverrides = current)
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

        override suspend fun setAiCoachCtaSeen(seen: Boolean) {
            prefs.value = prefs.value.copy(hasSeenAiCoachCta = seen)
        }

        override suspend fun setAutoCaptureTutorialSeen(seen: Boolean) {
            prefs.value = prefs.value.copy(hasSeenAutoCaptureTutorial = seen)
        }

        override suspend fun setPremium(isPremium: Boolean) {
            prefs.value = prefs.value.copy(isPremium = isPremium)
        }

        override suspend fun setAiChatHistoryJson(historyJson: String?) {
            chatHistory.value = historyJson
        }

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
}
