package com.chelumore.xpenze.core.premium

import androidx.annotation.VisibleForTesting
import com.chelumore.xpenze.core.di.DefaultDispatcher
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.hasPremiumAccess
import com.chelumore.xpenze.domain.repository.isTrialActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@Singleton
class PremiumEntitlementTracker(
    private val userRepository: UserRepository,
    private val defaultDispatcher: CoroutineDispatcher,
    private val nowProvider: () -> Long,
    private val tickMs: Long
) {
    @Inject
    constructor(
        userRepository: UserRepository,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ) : this(
        userRepository = userRepository,
        defaultDispatcher = defaultDispatcher,
        nowProvider = { System.currentTimeMillis() },
        tickMs = DEFAULT_TICK_MS
    )

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    private val ticker = flow {
        emit(nowProvider())
        while (true) {
            delay(tickMs)
            emit(nowProvider())
        }
    }

    val entitlement: StateFlow<PremiumEntitlementState> = combine(
        userRepository.userPreferences,
        ticker
    ) { prefs, now ->
        val trialActive = prefs.isTrialActive(now)
        val hasAccess = prefs.hasPremiumAccess(now)
        val millisUntilExpiry = if (prefs.trialExpiresAt <= 0L) {
            0L
        } else {
            (prefs.trialExpiresAt - now).coerceAtLeast(0L)
        }

        PremiumEntitlementState(
            isPremium = prefs.isPremium,
            isTrialActive = trialActive,
            hasPremiumAccess = hasAccess,
            trialConsumed = prefs.trialConsumed,
            trialStartedAt = prefs.trialStartedAt,
            trialExpiresAt = prefs.trialExpiresAt,
            millisUntilTrialExpiry = millisUntilExpiry
        )
    }.distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = PremiumEntitlementState()
        )

    @VisibleForTesting
    internal fun cancelForTests() {
        scope.cancel()
    }

    companion object {
        const val DEFAULT_TICK_MS = 30_000L
    }
}
