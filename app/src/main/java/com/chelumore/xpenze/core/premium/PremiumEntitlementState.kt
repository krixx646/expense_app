package com.chelumore.xpenze.core.premium

import com.chelumore.xpenze.domain.repository.TRIAL_ENDING_SOON_WINDOW_MS

data class PremiumEntitlementState(
    val isPremium: Boolean = false,
    val isTrialActive: Boolean = false,
    val hasPremiumAccess: Boolean = false,
    val trialConsumed: Boolean = false,
    val trialStartedAt: Long = 0L,
    val trialExpiresAt: Long = 0L,
    val hasFounderReward: Boolean = false,
    val founderTrialActivated: Boolean = false,
    val millisUntilTrialExpiry: Long = 0L
) {
    val isTrialEndingSoon: Boolean
        get() = isTrialActive &&
            millisUntilTrialExpiry in 1L..TRIAL_ENDING_SOON_WINDOW_MS
}

/**
 * Whether the persistent "Go Premium" / "Keep Premium" CTA should be visible.
 * Hidden while the user already has access (paid or trial), except in the final days of a trial.
 */
fun shouldShowPremiumCta(hasPremiumAccess: Boolean, isTrialEndingSoon: Boolean): Boolean =
    !hasPremiumAccess || isTrialEndingSoon
