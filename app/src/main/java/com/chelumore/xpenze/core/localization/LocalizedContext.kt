package com.chelumore.xpenze.core.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

fun Context.withAppLanguage(languageTag: String?): Context {
    val locale = Locale.forLanguageTag(AppLanguages.normalizeTag(languageTag))
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocales(LocaleList(locale))
    }
    return createConfigurationContext(configuration)
}
