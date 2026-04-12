package com.chelumore.xpenze.core.premium

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
)
