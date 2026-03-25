package com.chelumore.xpenze.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrowthAnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics? = runCatching {
        FirebaseAnalytics.getInstance(context)
    }.getOrNull()

    fun logPremiumCtaImpression(screenRoute: String?) {
        logEvent(
            eventName = EVENT_PREMIUM_CTA_IMPRESSION,
            screenRoute = screenRoute,
            surface = SURFACE_GLOBAL_OVERLAY
        ) {}
    }

    fun logPremiumCtaClick(screenRoute: String?) {
        logEvent(
            eventName = EVENT_PREMIUM_CTA_CLICK,
            screenRoute = screenRoute,
            surface = SURFACE_GLOBAL_OVERLAY
        ) {}
    }

    fun logAiCtaImpression(screenRoute: String?, isNewBadgeVisible: Boolean) {
        logEvent(
            eventName = EVENT_AI_CTA_IMPRESSION,
            screenRoute = screenRoute,
            surface = SURFACE_GLOBAL_OVERLAY
        ) {
            putLong(PARAM_IS_NEW_BADGE_VISIBLE, if (isNewBadgeVisible) 1L else 0L)
        }
    }

    fun logAiCtaClick(screenRoute: String?, isNewBadgeVisible: Boolean) {
        logEvent(
            eventName = EVENT_AI_CTA_CLICK,
            screenRoute = screenRoute,
            surface = SURFACE_GLOBAL_OVERLAY
        ) {
            putLong(PARAM_IS_NEW_BADGE_VISIBLE, if (isNewBadgeVisible) 1L else 0L)
        }
    }

    fun logSubscriptionPaywallViewed() {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_PAYWALL_VIEW,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {}
    }

    fun logSubscriptionTrialClick() {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_TRIAL_CLICK,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {}
    }

    fun logSubscriptionTrialStarted() {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_TRIAL_STARTED,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {}
    }

    fun logSubscriptionTrialBlocked(reason: String) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_TRIAL_BLOCKED,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putString(PARAM_REASON, reason.take(40))
        }
    }

    fun logSubscriptionTrialFailed(reason: String) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_TRIAL_FAILED,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putString(PARAM_REASON, reason.take(40))
        }
    }

    fun logSubscriptionPurchaseClick() {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_PURCHASE_CLICK,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {}
    }

    fun logSubscriptionPurchaseSucceeded(productId: String) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_PURCHASE_SUCCESS,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putString(PARAM_PRODUCT_ID, productId.take(60))
        }
    }

    fun logSubscriptionPurchasePending(productId: String) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_PURCHASE_PENDING,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putString(PARAM_PRODUCT_ID, productId.take(60))
        }
    }

    fun logSubscriptionPurchaseFailed(reason: String) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_PURCHASE_FAILED,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putString(PARAM_REASON, reason.take(40))
        }
    }

    fun logSubscriptionRestoreSucceeded(activeCount: Int) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_RESTORE_SUCCESS,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putLong(PARAM_ACTIVE_COUNT, activeCount.toLong().coerceAtLeast(0L))
        }
    }

    fun logSubscriptionRestoreFailed(reason: String) {
        logEvent(
            eventName = EVENT_SUBSCRIPTION_RESTORE_FAILED,
            screenRoute = "SubscriptionScreen",
            surface = SURFACE_SUBSCRIPTION_SCREEN
        ) {
            putString(PARAM_REASON, reason.take(40))
        }
    }

    fun logPremiumNudgeImpression(surface: String, variant: String) {
        logEvent(
            eventName = EVENT_PREMIUM_NUDGE_IMPRESSION,
            screenRoute = "SubscriptionScreen",
            surface = surface
        ) {
            putString(PARAM_VARIANT, variant.take(24))
        }
    }

    fun logPremiumNudgeClick(surface: String, variant: String, action: String) {
        logEvent(
            eventName = EVENT_PREMIUM_NUDGE_CLICK,
            screenRoute = "SubscriptionScreen",
            surface = surface
        ) {
            putString(PARAM_VARIANT, variant.take(24))
            putString(PARAM_ACTION, action.take(24))
        }
    }

    private inline fun logEvent(
        eventName: String,
        screenRoute: String?,
        surface: String,
        paramsBuilder: Bundle.() -> Unit
    ) {
        val params = Bundle().apply {
            putString(PARAM_SURFACE, surface)
            putString(PARAM_SCREEN_ROUTE, normalizeRoute(screenRoute))
            paramsBuilder()
        }

        runCatching { firebaseAnalytics?.logEvent(eventName, params) }
        Log.d(TAG, "event=$eventName route=${normalizeRoute(screenRoute)}")
    }

    private fun normalizeRoute(route: String?): String {
        return route
            ?.takeIf { it.isNotBlank() }
            ?.take(100)
            ?: "unknown"
    }

    companion object {
        private const val TAG = "GrowthAnalytics"

        private const val EVENT_PREMIUM_CTA_IMPRESSION = "premium_cta_impression"
        private const val EVENT_PREMIUM_CTA_CLICK = "premium_cta_click"
        private const val EVENT_AI_CTA_IMPRESSION = "ai_cta_impression"
        private const val EVENT_AI_CTA_CLICK = "ai_cta_click"
        private const val EVENT_SUBSCRIPTION_PAYWALL_VIEW = "subscription_paywall_view"
        private const val EVENT_SUBSCRIPTION_TRIAL_CLICK = "subscription_trial_click"
        private const val EVENT_SUBSCRIPTION_TRIAL_STARTED = "subscription_trial_started"
        private const val EVENT_SUBSCRIPTION_TRIAL_BLOCKED = "subscription_trial_blocked"
        private const val EVENT_SUBSCRIPTION_TRIAL_FAILED = "subscription_trial_failed"
        private const val EVENT_SUBSCRIPTION_PURCHASE_CLICK = "subscription_purchase_click"
        private const val EVENT_SUBSCRIPTION_PURCHASE_SUCCESS = "subscription_purchase_success"
        private const val EVENT_SUBSCRIPTION_PURCHASE_PENDING = "subscription_purchase_pending"
        private const val EVENT_SUBSCRIPTION_PURCHASE_FAILED = "subscription_purchase_failed"
        private const val EVENT_SUBSCRIPTION_RESTORE_SUCCESS = "subscription_restore_success"
        private const val EVENT_SUBSCRIPTION_RESTORE_FAILED = "subscription_restore_failed"
        private const val EVENT_PREMIUM_NUDGE_IMPRESSION = "premium_nudge_impression"
        private const val EVENT_PREMIUM_NUDGE_CLICK = "premium_nudge_click"

        private const val SURFACE_GLOBAL_OVERLAY = "global_overlay"
        private const val SURFACE_SUBSCRIPTION_SCREEN = "subscription_screen"

        private const val PARAM_SCREEN_ROUTE = "screen_route"
        private const val PARAM_SURFACE = "cta_surface"
        private const val PARAM_IS_NEW_BADGE_VISIBLE = "is_new_badge"
        private const val PARAM_REASON = "reason"
        private const val PARAM_PRODUCT_ID = "product_id"
        private const val PARAM_ACTIVE_COUNT = "active_count"
        private const val PARAM_VARIANT = "variant"
        private const val PARAM_ACTION = "action"
    }
}
