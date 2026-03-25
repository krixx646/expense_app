package com.chelumore.xpenze.core.ui.components.ads

import com.chelumore.xpenze.BuildConfig

object AdMobConfig {
    const val TEST_APP_ID: String = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID: String = "ca-app-pub-3940256099942544/6300978111"

    fun appId(): String {
        if (BuildConfig.DEBUG) return TEST_APP_ID
        return BuildConfig.ADMOB_APP_ID.ifBlank { TEST_APP_ID }
    }

    fun bannerUnitId(placement: AdPlacement): String {
        if (BuildConfig.DEBUG) return TEST_BANNER_ID
        val releaseId = when (placement) {
            AdPlacement.DASHBOARD -> BuildConfig.ADMOB_BANNER_DASHBOARD
            AdPlacement.TRANSACTIONS -> BuildConfig.ADMOB_BANNER_TRANSACTIONS
            AdPlacement.BUDGETS -> BuildConfig.ADMOB_BANNER_BUDGETS
            AdPlacement.REPORTS -> BuildConfig.ADMOB_BANNER_REPORTS
        }
        return releaseId.ifBlank { TEST_BANNER_ID }
    }
}

