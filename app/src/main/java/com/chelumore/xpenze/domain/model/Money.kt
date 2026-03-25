package com.chelumore.xpenze.domain.model

/**
 * Represents a monetary value in minor units (cents).
 *
 * This app stores money as cents (Long) for accuracy.
 */
data class Money(
    val amountCents: Long,
    val currency: String
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return copy(amountCents = amountCents + other.amountCents)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return copy(amountCents = amountCents - other.amountCents)
    }

    fun isZero(): Boolean = amountCents == 0L
    fun isPositive(): Boolean = amountCents > 0L
    fun isNegative(): Boolean = amountCents < 0L
}


