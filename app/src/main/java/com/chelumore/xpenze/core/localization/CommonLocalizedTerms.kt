package com.chelumore.xpenze.core.localization

import java.util.Locale

internal fun localizedUncategorized(languageTag: String? = null): String = pickCommonLocalizedTerm(
    languageTag = languageTag,
    en = "Uncategorized",
    es = "Sin categoria",
    fr = "Sans categorie",
    de = "Ohne Kategorie",
    pt = "Sem categoria",
    ar = "غير مصنف",
    hi = "बिना श्रेणी",
    ja = "未分類",
    sw = "Bila kategoria",
    zh = "未分类"
)

internal fun localizedOther(languageTag: String? = null): String = pickCommonLocalizedTerm(
    languageTag = languageTag,
    en = "Other",
    es = "Otros",
    fr = "Autre",
    de = "Andere",
    pt = "Outros",
    ar = "أخرى",
    hi = "अन्य",
    ja = "その他",
    sw = "Nyingine",
    zh = "其他"
)

internal fun localizedUnknown(languageTag: String? = null): String = pickCommonLocalizedTerm(
    languageTag = languageTag,
    en = "Unknown",
    es = "Desconocido",
    fr = "Inconnu",
    de = "Unbekannt",
    pt = "Desconhecido",
    ar = "غير معروف",
    hi = "अज्ञात",
    ja = "不明",
    sw = "Haijulikani",
    zh = "未知"
)

internal fun localizedUnknownAccount(languageTag: String? = null): String = pickCommonLocalizedTerm(
    languageTag = languageTag,
    en = "Unknown account",
    es = "Cuenta desconocida",
    fr = "Compte inconnu",
    de = "Unbekanntes Konto",
    pt = "Conta desconhecida",
    ar = "حساب غير معروف",
    hi = "अज्ञात खाता",
    ja = "不明な口座",
    sw = "Akaunti isiyojulikana",
    zh = "未知账户"
)

internal fun localizedTransfer(languageTag: String? = null): String = pickCommonLocalizedTerm(
    languageTag = languageTag,
    en = "Transfer",
    es = "Transferencia",
    fr = "Transfert",
    de = "Uberweisung",
    pt = "Transferencia",
    ar = "تحويل",
    hi = "ट्रांसफर",
    ja = "振替",
    sw = "Hamisho",
    zh = "转账"
)

internal fun localizedRecurring(languageTag: String? = null): String = pickCommonLocalizedTerm(
    languageTag = languageTag,
    en = "Recurring",
    es = "Recurrente",
    fr = "Recurrent",
    de = "Wiederkehrend",
    pt = "Recorrente",
    ar = "متكرر",
    hi = "आवर्ती",
    ja = "定期",
    sw = "Inayojirudia",
    zh = "定期"
)

private fun pickCommonLocalizedTerm(
    languageTag: String?,
    en: String,
    es: String,
    fr: String,
    de: String,
    pt: String,
    ar: String,
    hi: String,
    ja: String,
    sw: String,
    zh: String
): String {
    return when (AppLanguages.normalizeTag(languageTag ?: Locale.getDefault().toLanguageTag())) {
        "es" -> es
        "fr" -> fr
        "de" -> de
        "pt" -> pt
        "ar" -> ar
        "hi" -> hi
        "ja" -> ja
        "sw" -> sw
        "zh" -> zh
        else -> en
    }
}
