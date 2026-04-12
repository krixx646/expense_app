package com.chelumore.xpenze.core.autocapture

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class ParsedMoneyCandidate(
    val amountCents: Long,
    val currency: String,
    val raw: String,
    val startIndex: Int
)

private val amountRegex = Regex(
    pattern = """(?i)(?:[-+]\s*)?(?:USD|EUR|GBP|NGN|INR|JPY|KES|CNY|RMB|AED|CAD|AUD|ZAR|CHF|MXN|[$€£₦₹¥₩])?\s*\d+(?:[,\s]\d{3})*(?:\.\d{2})?""",
    option = RegexOption.MULTILINE
)

private val currencyBySymbol = mapOf(
    "$" to "USD",
    "€" to "EUR",
    "£" to "GBP",
    "₦" to "NGN",
    "₹" to "INR",
    "¥" to "JPY",
    "₩" to "KRW"
)

private val dateFormats = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US),
    DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US),
    DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.US),
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US),
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US),
    DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US),
    DateTimeFormatter.ofPattern("MMM d yyyy", Locale.US)
)

internal fun extractMoneyCandidates(
    text: String,
    fallbackCurrency: String
): List<ParsedMoneyCandidate> {
    return amountRegex.findAll(text)
        .mapNotNull { match ->
            val raw = match.value.trim()
            val amountCents = raw.toCentsOrNull() ?: return@mapNotNull null
            if (amountCents <= 0L) return@mapNotNull null
            val currency = detectCurrency(raw, fallbackCurrency)
            ParsedMoneyCandidate(
                amountCents = amountCents,
                currency = currency,
                raw = raw,
                startIndex = match.range.first
            )
        }
        .toList()
}

internal fun selectBestMoneyCandidate(
    text: String,
    candidates: List<ParsedMoneyCandidate>
): ParsedMoneyCandidate? {
    return candidates.maxWithOrNull(
        compareBy<ParsedMoneyCandidate> { scoreMoneyCandidate(text, it) }
            .thenBy { it.amountCents }
    )
}

internal fun extractFirstDateMillis(text: String): Long? {
    val zone = ZoneId.systemDefault()
    val trimmed = text.replace(',', ' ')
    val tokens = trimmed.split(Regex("\\s+"))
    val joinedWindows = buildList {
        for (windowSize in 1..4) {
            tokens.windowed(size = windowSize, step = 1, partialWindows = false)
                .mapTo(this) { it.joinToString(" ").trim() }
        }
    }.distinct()

    joinedWindows.forEach { candidate ->
        dateFormats.forEach { formatter ->
            runCatching {
                LocalDate.parse(candidate, formatter)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()?.let { return it }
        }
        runCatching {
            LocalDateTime.parse(candidate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
    }
    return null
}

private fun detectCurrency(raw: String, fallbackCurrency: String): String {
    currencyBySymbol.entries.firstOrNull { (symbol, _) -> raw.contains(symbol) }?.let { (_, code) ->
        return code
    }
    return when {
        raw.contains("EUR", ignoreCase = true) -> "EUR"
        raw.contains("GBP", ignoreCase = true) -> "GBP"
        raw.contains("NGN", ignoreCase = true) -> "NGN"
        raw.contains("INR", ignoreCase = true) -> "INR"
        raw.contains("JPY", ignoreCase = true) -> "JPY"
        raw.contains("KES", ignoreCase = true) -> "KES"
        raw.contains("CNY", ignoreCase = true) || raw.contains("RMB", ignoreCase = true) -> "CNY"
        raw.contains("AED", ignoreCase = true) -> "AED"
        raw.contains("MXN", ignoreCase = true) -> "MXN"
        raw.contains("CAD", ignoreCase = true) -> "CAD"
        raw.contains("AUD", ignoreCase = true) -> "AUD"
        raw.contains("ZAR", ignoreCase = true) -> "ZAR"
        raw.contains("CHF", ignoreCase = true) -> "CHF"
        else -> fallbackCurrency
    }
}

private fun scoreMoneyCandidate(text: String, candidate: ParsedMoneyCandidate): Int {
    val start = (candidate.startIndex - 24).coerceAtLeast(0)
    val end = (candidate.startIndex + candidate.raw.length + 24).coerceAtMost(text.length)
    val context = normalizeCaptureText(text.substring(start, end))
    val leadingContext = normalizeCaptureText(text.substring(start, candidate.startIndex))
    val normalizedRaw = normalizeCaptureText(candidate.raw)
    var score = 0

    if (currencyBySymbol.keys.any { candidate.raw.contains(it) } || currencyCodeRegex.containsMatchIn(candidate.raw)) {
        score += 8
    }
    if (decimalAmountRegex.containsMatchIn(candidate.raw)) {
        score += 5
    }
    if (positiveAmountContextKeywords.any { context.contains(it) }) {
        score += 3
    }
    if (balanceLeadingContextKeywords.any { leadingContext.contains(it) }) {
        score -= 10
    }
    if (accountLikeContextKeywords.any { context.contains(it) }) {
        score -= 6
    }
    if (plainIdentifierRegex.matches(normalizedRaw) && !decimalAmountRegex.containsMatchIn(candidate.raw)) {
        score -= 4
    }
    return score
}

private fun String.toCentsOrNull(): Long? {
    val clean = replace(Regex("(?i)[^0-9,.-]"), "")
        .replace(" ", "")
        .replace(",", "")
        .trim()
    if (clean.isBlank()) return null
    return runCatching {
        BigDecimal(clean)
            .abs()
            .multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()
}

private val decimalAmountRegex = Regex("""\d+[.,]\d{2}\b""")
private val currencyCodeRegex = Regex("""(?i)\b(?:USD|EUR|GBP|NGN|INR|JPY|KES|CNY|RMB|AED|CAD|AUD|ZAR|CHF|MXN)\b""")
private val plainIdentifierRegex = Regex("""\d{4,}""")
private val positiveAmountContextKeywords = setOf(
    "amount",
    "total",
    "paid",
    "debited",
    "credited",
    "received",
    "deposit",
    "withdrawn",
    "purchase",
    "payment",
    "transfer",
    "pago",
    "compra",
    "deposito",
    "transferencia",
    "paiement",
    "achat",
    "virement",
    "zahlung",
    "kauf",
    "uberweisung",
    "入金",
    "支払",
    "振込",
    "支付",
    "转账"
)
private val accountLikeContextKeywords = setOf(
    "acct",
    "account",
    "cuenta",
    "conta",
    "compte",
    "konto",
    "card",
    "tarjeta",
    "cartao",
    "carte",
    "karte",
    "wallet",
    "口座",
    "カード",
    "账户",
    "帐户",
    "card ending",
    "ending"
)
private val balanceLeadingContextKeywords = setOf(
    "available balance",
    "avail balance",
    "avail bal",
    "balance",
    "ledger balance",
    "credit limit",
    "remaining limit"
)
