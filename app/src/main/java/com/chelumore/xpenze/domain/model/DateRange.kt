package com.chelumore.xpenze.domain.model

/**
 * Simple time range represented with epoch millis (inclusive bounds).
 */
data class DateRange(
    val startMillis: Long,
    val endMillis: Long
) {
    init {
        require(startMillis <= endMillis) { "startMillis must be <= endMillis" }
    }

    fun contains(epochMillis: Long): Boolean = epochMillis in startMillis..endMillis
}


