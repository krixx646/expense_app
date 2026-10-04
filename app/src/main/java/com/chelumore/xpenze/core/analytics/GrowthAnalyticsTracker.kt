package com.chelumore.xpenze.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuthException
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

    fun logScreenView(route: String?) {
        val screenName = screenNameFromRoute(route) ?: return
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        runCatching { firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params) }
        Log.d(TAG, "screen_view=$screenName")
    }

    fun setAccountType(isGuest: Boolean, isSignedIn: Boolean) {
        val accountType = when {
            isGuest -> "guest"
            isSignedIn -> "account"
            else -> "none"
        }
        runCatching { firebaseAnalytics?.setUserProperty(USER_PROP_ACCOUNT_TYPE, accountType) }
    }

    fun logWelcomeChoice(choice: String) {
        logFunnelEvent(EVENT_WELCOME_CHOICE) { putString(PARAM_CHOICE, choice) }
    }

    fun logAuthResult(flow: String, method: String, success: Boolean, error: Throwable? = null) {
        logFunnelEvent(if (success) EVENT_AUTH_SUCCESS else EVENT_AUTH_FAILED) {
            putString(PARAM_FLOW, flow)
            putString(PARAM_METHOD, method)
            if (error != null) putString(PARAM_REASON, authErrorReason(error))
        }
    }

    fun logOnboardingStep(step: String) {
        logFunnelEvent(EVENT_ONBOARDING_STEP) { putString(PARAM_STEP, step.lowercase()) }
    }

    fun logOnboardingCompleted(addedBudget: Boolean) {
        logFunnelEvent(EVENT_ONBOARDING_COMPLETE) { putLong(PARAM_ADDED_BUDGET, if (addedBudget) 1L else 0L) }
    }

    fun logOnboardingFailed(reason: String) {
        logFunnelEvent(EVENT_ONBOARDING_FAILED) { putString(PARAM_REASON, reason.take(80)) }
    }

    fun logTransactionCreated(type: String, lifetimeActions: Int) {
        logFunnelEvent(EVENT_TRANSACTION_CREATED) {
            putString(PARAM_TYPE, type.lowercase())
            putLong(PARAM_LIFETIME_ACTIONS, lifetimeActions.toLong())
        }
    }

    fun logBudgetCreated(lifetimeActions: Int) {
        logFunnelEvent(EVENT_BUDGET_CREATED) { putLong(PARAM_LIFETIME_ACTIONS, lifetimeActions.toLong()) }
    }

    private inline fun logFunnelEvent(eventName: String, paramsBuilder: Bundle.() -> Unit) {
        val params = Bundle().apply(paramsBuilder)
        runCatching { firebaseAnalytics?.logEvent(eventName, params) }
        Log.d(TAG, "event=$eventName params=$params")
    }

    private fun authErrorReason(error: Throwable): String {
        val code = (error as? FirebaseAuthException)?.errorCode
        return (code ?: error::class.simpleName ?: "unknown").take(80)
    }

    private fun screenNameFromRoute(route: String?): String? {
        if (route.isNullOrBlank()) return null
        return route
            .substringBefore('/')
            .substringBefore('?')
            .substringAfterLast('.')
            .take(100)
            .ifBlank { null }
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

        private const val EVENT_WELCOME_CHOICE = "welcome_choice"
        private const val EVENT_AUTH_SUCCESS = "auth_success"
        private const val EVENT_AUTH_FAILED = "auth_failed"
        private const val EVENT_ONBOARDING_STEP = "onboarding_step"
        private const val EVENT_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val EVENT_ONBOARDING_FAILED = "onboarding_failed"
        private const val EVENT_TRANSACTION_CREATED = "transaction_created"
        private const val EVENT_BUDGET_CREATED = "budget_created"

        private const val USER_PROP_ACCOUNT_TYPE = "account_type"

        private const val PARAM_CHOICE = "choice"
        private const val PARAM_FLOW = "flow"
        private const val PARAM_METHOD = "method"
        private const val PARAM_STEP = "step"
        private const val PARAM_ADDED_BUDGET = "added_budget"
        private const val PARAM_TYPE = "type"
        private const val PARAM_LIFETIME_ACTIONS = "lifetime_actions"

        const val WELCOME_CHOICE_SIGN_UP = "sign_up"
        const val WELCOME_CHOICE_LOG_IN = "log_in"
        const val WELCOME_CHOICE_GUEST = "guest"
        const val AUTH_FLOW_SIGN_UP = "sign_up"
        const val AUTH_FLOW_LOG_IN = "log_in"
        const val AUTH_METHOD_EMAIL = "email"
        const val AUTH_METHOD_GOOGLE = "google"

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
