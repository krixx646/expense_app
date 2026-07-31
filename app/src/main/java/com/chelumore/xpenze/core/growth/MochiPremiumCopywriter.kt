package com.chelumore.xpenze.core.growth

import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.common.toCurrencyString

data class MochiPremiumCopy(
    val badge: String,
    val headline: String,
    val body: String,
    val note: String
)

object MochiPremiumCopywriter {
    fun paywall(
        variant: PremiumEmotionVariant,
        potentialMonthlySavingsCents: Long,
        currencyCode: String,
        monthlyPriceText: String? = null
    ): MochiPremiumCopy {
        val savingsText = if (potentialMonthlySavingsCents > 0L) {
            potentialMonthlySavingsCents.toCurrencyString(currencyCode)
        } else {
            null
        }
        val priceText = monthlyPriceText?.trim()?.takeIf { it.isNotBlank() }
            ?: Constants.DEFAULT_MONTHLY_PRICE_CENTS.toCurrencyString(currencyCode)

        return when (variant) {
            PremiumEmotionVariant.PLAYFUL -> MochiPremiumCopy(
                badge = "Mochi mode: playful",
                headline = "Mochi wants your budget to purr, not panic.",
                body = if (savingsText != null) {
                    "I spotted around $savingsText in possible monthly savings. Want me to walk you through it?"
                } else {
                    "Unlock Premium and I will hunt hidden spending leaks for you."
                },
                note = "7-day trial first, then $priceText/month. Cancel anytime."
            )
            PremiumEmotionVariant.ADVISOR -> MochiPremiumCopy(
                badge = "Mochi mode: advisor",
                headline = "Your data is already signaling upgrade value.",
                body = if (savingsText != null) {
                    "Based on your recent spending pattern, you may be able to trim around $savingsText per month."
                } else {
                    "Enable Premium to get personalized savings opportunities from your real spending behavior."
                },
                note = "Try risk-free for 7 days, then $priceText/month if it helps."
            )
            PremiumEmotionVariant.URGENT_SOFT -> MochiPremiumCopy(
                badge = "Mochi mode: gentle urgency",
                headline = "Small leaks become expensive habits over time.",
                body = if (savingsText != null) {
                    "Even recovering around $savingsText each month can compound into meaningful yearly progress."
                } else {
                    "Premium can alert you earlier before overspending patterns become hard to reverse."
                },
                note = "Start with 7 days free. Keep it only if it saves you more than it costs."
            )
        }
    }

    fun lockedFeature(
        variant: PremiumEmotionVariant,
        featureName: String
    ): MochiPremiumCopy {
        return when (variant) {
            PremiumEmotionVariant.PLAYFUL -> MochiPremiumCopy(
                badge = "Mochi mode: playful",
                headline = "$featureName is sleeping behind Premium.",
                body = "Wake me up and I will guide your money decisions with less stress and more confidence.",
                note = "Unlock with Premium or start your 7-day trial."
            )
            PremiumEmotionVariant.ADVISOR -> MochiPremiumCopy(
                badge = "Mochi mode: advisor",
                headline = "$featureName is part of your premium toolkit.",
                body = "This feature is designed to reduce decision fatigue and improve month-end outcomes.",
                note = "Upgrade to activate data-backed guidance."
            )
            PremiumEmotionVariant.URGENT_SOFT -> MochiPremiumCopy(
                badge = "Mochi mode: gentle urgency",
                headline = "$featureName can prevent costly blind spots.",
                body = "A few guided decisions now can reduce bigger money mistakes later.",
                note = "Activate Premium to unlock it instantly."
            )
        }
    }
}

