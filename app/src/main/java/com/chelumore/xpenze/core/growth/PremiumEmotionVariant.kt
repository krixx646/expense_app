package com.chelumore.xpenze.core.growth

enum class PremiumEmotionVariant {
    PLAYFUL,
    ADVISOR,
    URGENT_SOFT;

    companion object {
        fun fromSeed(seed: String): PremiumEmotionVariant {
            val normalized = seed.trim().ifBlank { "guest" }
            val bucket = kotlin.math.abs(normalized.hashCode()) % entries.size
            return entries[bucket]
        }

        fun forTimestamp(timestampMillis: Long): PremiumEmotionVariant {
            val dayBucket = (timestampMillis / (24L * 60L * 60L * 1000L)).toString()
            return fromSeed(dayBucket)
        }
    }
}

