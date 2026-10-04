package com.chelumore.xpenze.core.localization

import java.util.Locale

data class AppLanguageOption(
    val tag: String,
    val label: String
)

object AppLanguages {
    const val FALLBACK_LANGUAGE_TAG = "en"

    // Expanded selector list. For languages without dedicated translations yet,
    // Android falls back to base strings while still applying locale formatting.
    val options: List<AppLanguageOption> = listOf(
        AppLanguageOption(tag = "en", label = "English"),
        AppLanguageOption(tag = "es", label = "Espanol"),
        AppLanguageOption(tag = "fr", label = "Francais"),
        AppLanguageOption(tag = "de", label = "Deutsch"),
        AppLanguageOption(tag = "lv", label = "Latviešu"),
        AppLanguageOption(tag = "pt", label = "Portugues"),
        AppLanguageOption(tag = "ar", label = "العربية"),
        AppLanguageOption(tag = "sw", label = "Kiswahili"),
        AppLanguageOption(tag = "hi", label = "हिन्दी"),
        AppLanguageOption(tag = "zh-CN", label = "简体中文"),
        AppLanguageOption(tag = "ja", label = "日本語")
    )

    fun normalizeTag(rawTag: String?): String {
        val tag = rawTag?.trim().orEmpty()
        if (tag.isBlank()) return FALLBACK_LANGUAGE_TAG

        val exact = options.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
        if (exact != null) return exact.tag

        val language = Locale.forLanguageTag(tag).language
        val byLanguage = options.firstOrNull {
            Locale.forLanguageTag(it.tag).language.equals(language, ignoreCase = true)
        }
        return byLanguage?.tag ?: FALLBACK_LANGUAGE_TAG
    }

    fun displayName(rawTag: String?): String {
        val normalized = normalizeTag(rawTag)
        return options.firstOrNull { it.tag == normalized }?.label ?: "English"
    }
}
