package com.chelumore.xpenze.domain.usecase.autocapture

import com.chelumore.xpenze.core.autocapture.captureFingerprint
import com.chelumore.xpenze.core.autocapture.extractMoneyCandidates
import com.chelumore.xpenze.core.autocapture.normalizeCaptureText
import com.chelumore.xpenze.core.autocapture.selectBestMoneyCandidate
import com.chelumore.xpenze.core.localization.localizedAutoCaptureDefaultTitle
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAmbiguousReason
import com.chelumore.xpenze.domain.model.AutoCaptureConfidence
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureResolution
import com.chelumore.xpenze.domain.model.AutoCaptureSource
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AutoCaptureDraftRepository
import java.util.UUID
import javax.inject.Inject

class CaptureNotificationToDraftUseCase @Inject constructor(
    private val autoCaptureDraftRepository: AutoCaptureDraftRepository
) {

    suspend operator fun invoke(
        packageName: String,
        appLabel: String?,
        notificationTitle: String?,
        notificationText: String?,
        postedAt: Long,
        accountId: String?,
        fallbackCurrency: String
    ): Boolean {
        val title = notificationTitle.orEmpty().trim()
        val body = notificationText.orEmpty().trim()
        val combined = listOf(title, body)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        if (combined.isBlank()) return false

        val normalized = normalizeCaptureText(combined)
        val normalizedPackage = normalizeCaptureText(packageName)
        if (blockedKeywords.any { normalized.contains(it) }) return false

        val moneyCandidates = extractMoneyCandidates(combined, fallbackCurrency)
        val primaryAmount = selectBestMoneyCandidate(combined, moneyCandidates) ?: return false
        val transactionKeywordHits = countMatches(normalized, transactionSignalKeywords)
        val accountContextHits = countMatches(normalized, accountContextKeywords)
        val hasFinancePackageHint = financePackageHints.any { normalizedPackage.contains(it) }
        if (transactionKeywordHits == 0) return false
        if (!hasFinancePackageHint && transactionKeywordHits < 2 && accountContextHits == 0) return false

        val suggestedType = when {
            transferKeywords.any { normalized.contains(it) } -> TransactionType.TRANSFER
            incomeKeywords.any { normalized.contains(it) } -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
        val isAmbiguous = ambiguityKeywords.any { normalized.contains(it) } ||
            suggestedType == TransactionType.TRANSFER
        val needsReviewForConfidence = !hasFinancePackageHint && accountContextHits == 0
        val resolvedTitle = extractBestTitle(title, body, appLabel)
        val now = System.currentTimeMillis()

        val draft = AutoCaptureDraft(
            id = UUID.randomUUID().toString(),
            source = AutoCaptureSource.NOTIFICATION,
            status = if (isAmbiguous || needsReviewForConfidence) {
                AutoCaptureDraftStatus.NEEDS_REVIEW
            } else {
                AutoCaptureDraftStatus.PENDING
            },
            confidence = when {
                hasFinancePackageHint && !isAmbiguous -> AutoCaptureConfidence.HIGH
                transactionKeywordHits >= 2 || accountContextHits > 0 -> AutoCaptureConfidence.MEDIUM
                else -> AutoCaptureConfidence.LOW
            },
            title = resolvedTitle,
            rawText = combined,
            sourcePackage = packageName,
            sourceLabel = appLabel ?: packageName,
            amountCents = primaryAmount.amountCents,
            currency = primaryAmount.currency,
            occurredAt = if (postedAt > 0L) postedAt else now,
            suggestedType = suggestedType,
            accountId = accountId,
            notes = body.takeIf { it.isNotBlank() && it != title },
            needsReviewReason = if (isAmbiguous || needsReviewForConfidence) {
                localizedAutoCaptureNotificationAmbiguousReason()
            } else {
                null
            },
            fingerprint = captureFingerprint(
                AutoCaptureSource.NOTIFICATION.name,
                packageName,
                resolvedTitle,
                primaryAmount.amountCents.toString(),
                postedAt.toString(),
                combined
            ),
            resolution = AutoCaptureResolution.NONE,
            createdAt = now,
            updatedAt = now
        )
        return autoCaptureDraftRepository.addOrIgnore(draft)
    }

    private fun extractBestTitle(
        title: String,
        body: String,
        appLabel: String?
    ): String {
        return sequenceOf(
            title.takeIf { candidate ->
                candidate.isNotBlank() &&
                    candidate.length >= 3 &&
                    genericTitleKeywords.none { keyword ->
                        normalizeCaptureText(candidate).contains(keyword)
                    }
            },
            body.split(Regex("[.!\\n]"))
                .map { it.trim() }
                .firstOrNull { it.length in 4..80 },
            appLabel
        ).firstOrNull { !it.isNullOrBlank() }
            ?.take(80)
            ?: localizedAutoCaptureDefaultTitle()
    }

    private fun countMatches(text: String, keywords: Set<String>): Int {
        return keywords.count { keyword -> text.contains(keyword) }
    }

    private companion object {
        val transactionSignalKeywords = setOf(
            "debited",
            "credited",
            "received",
            "paid",
            "purchase",
            "transfer",
            "withdrawn",
            "deposit",
            "payment",
            "spent",
            "sent",
            "charged",
            "refund",
            "debitado",
            "acreditado",
            "recibido",
            "pago",
            "compra",
            "transferencia",
            "retiro",
            "deposito",
            "gastado",
            "enviado",
            "cobrado",
            "creditado",
            "recebido",
            "pagamento",
            "saque",
            "debite",
            "credite",
            "recu",
            "paiement",
            "achat",
            "virement",
            "retrait",
            "depot",
            "envoye",
            "preleve",
            "belastet",
            "gutgeschrieben",
            "erhalten",
            "zahlung",
            "kauf",
            "uberweisung",
            "abhebung",
            "abgebucht",
            "erstattung",
            "خصم",
            "ايداع",
            "استلام",
            "دفع",
            "شراء",
            "تحويل",
            "سحب",
            "ارسال",
            "जमा",
            "निकासी",
            "प्राप्त",
            "भुगतान",
            "खरीद",
            "ट्रांसफर",
            "भेजा",
            "डेबिट",
            "क्रेडिट",
            "引き落とし",
            "入金",
            "受取",
            "支払",
            "購入",
            "振込",
            "出金",
            "送金",
            "malipo",
            "hamisho",
            "manunuzi",
            "imetumwa",
            "imepokelewa",
            "扣款",
            "收款",
            "收入",
            "入账",
            "支付",
            "消费",
            "转账",
            "提现",
            "存入",
            "退款"
        )
        val blockedKeywords = setOf(
            "otp",
            "verification code",
            "one time password",
            "login",
            "available balance",
            "credit limit",
            "promo",
            "offer",
            "reward points",
            "cashback offer",
            "bill due",
            "statement ready",
            "password",
            "codigo de verificacion",
            "saldo disponible",
            "limite de credito",
            "promocao",
            "saldo disponivel",
            "codigo de verificacao",
            "solde disponible",
            "limite de credit",
            "verifizierungscode",
            "verfugbares guthaben",
            "kreditlimit",
            "رمز التحقق",
            "الرصيد المتاح",
            "حد الائتمان",
            "सत्यापन कोड",
            "उपलब्ध शेष",
            "क्रेडिट सीमा",
            "認証コード",
            "利用可能残高",
            "利用枠",
            "msimbo wa uthibitisho",
            "salio lililopo",
            "验证码",
            "可用余额",
            "信用额度"
        )
        val accountContextKeywords = setOf(
            "account",
            "acct",
            "card",
            "wallet",
            "cuenta",
            "tarjeta",
            "conta",
            "cartao",
            "compte",
            "carte",
            "konto",
            "karte",
            "حساب",
            "بطاقة",
            "खाता",
            "कार्ड",
            "口座",
            "カード",
            "akaunti",
            "kadi",
            "账户",
            "帐户",
            "卡",
            "钱包"
        )
        val financePackageHints = setOf(
            "bank",
            "wallet",
            "pay",
            "money",
            "finance",
            "card",
            "cash",
            "upi",
            "mpesa",
            "momo",
            "paypal",
            "wise",
            "revolut",
            "venmo",
            "gcash",
            "paytm",
            "alipay",
            "wechat"
        )
        val incomeKeywords = setOf(
            "credited",
            "received",
            "deposit",
            "refund",
            "cashback",
            "acreditado",
            "recibido",
            "abonado",
            "creditado",
            "recebido",
            "reembolso",
            "credite",
            "recu",
            "depot",
            "gutgeschrieben",
            "erstattung",
            "ايداع",
            "استلام",
            "जमा",
            "प्राप्त",
            "入金",
            "受取",
            "imepokelewa",
            "收款",
            "入账",
            "退款"
        )
        val transferKeywords = setOf(
            "transfer",
            "sent to",
            "from your account",
            "to your account",
            "transferencia",
            "virement",
            "uberweisung",
            "تحويل",
            "ट्रांसफर",
            "振込",
            "送金",
            "hamisho",
            "转账",
            "汇款"
        )
        val ambiguityKeywords = setOf(
            "refund",
            "reimbursement",
            "cashback",
            "transfer",
            "reembolso",
            "remboursement",
            "erstattung",
            "تعويض",
            "refund",
            "प्रतिपूर्ति",
            "返金",
            "rejesho",
            "退款"
        )
        val genericTitleKeywords = setOf(
            "transaction",
            "payment alert",
            "alert",
            "notification",
            "aviso",
            "notificacion",
            "notificacao",
            "alerte",
            "benachrichtigung",
            "إشعار",
            "सूचना",
            "通知",
            "arifa"
        )
    }
}
