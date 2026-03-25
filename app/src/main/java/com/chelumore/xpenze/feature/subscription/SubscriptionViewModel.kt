package com.chelumore.xpenze.feature.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.billing.BillingCatalogResult
import com.chelumore.xpenze.core.billing.BillingService
import com.chelumore.xpenze.core.billing.BillingSubscriptionPlan
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.core.growth.PremiumOpportunityType
import com.chelumore.xpenze.core.growth.buildPremiumOpportunities
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.model.UserSubscription
import com.chelumore.xpenze.domain.repository.PurchaseSubscriptionResult
import com.chelumore.xpenze.domain.repository.RestorePurchasesResult
import com.chelumore.xpenze.domain.repository.SubscriptionRepository
import com.chelumore.xpenze.domain.repository.TrialActivationResult
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.isTrialActive
import com.chelumore.xpenze.domain.usecase.budget.GetBudgetsUseCase
import com.chelumore.xpenze.domain.usecase.category.GetCategoriesUseCase
import com.chelumore.xpenze.domain.usecase.transaction.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.YearMonth
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

data class SubscriptionPlanUi(
    val productId: String,
    val title: String,
    val priceText: String,
    val billingDetail: String,
    val secondaryDetail: String? = null,
    val badgeText: String? = null
)

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    // Paid subscription status
    val isPremium: Boolean = false,
    // Paid subscription OR active trial.
    val hasPremiumAccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialConsumed: Boolean = false,
    val trialExpiresAt: Long = 0L,
    val activeSubscription: UserSubscription? = null,
    val currencyCode: String = "USD",
    val appLanguageTag: String = "en",
    val paywallTopCategoryName: String? = null,
    val paywallTopCategorySpendCents: Long = 0L,
    val paywallPotentialMonthlySavingsCents: Long = 0L,
    val paywallPotentialAnnualSavingsCents: Long = 0L,
    val paywallOpportunities: List<PremiumOpportunity> = emptyList(),
    val paywallSavingsVsPriceRatio: Float = 0f,
    val availablePlans: List<SubscriptionPlanUi> = emptyList(),
    val selectedPlanId: String? = null,
    val isPricingLoading: Boolean = true,
    val monthlyReferencePriceText: String = "$9.99",
    val nudgeVariant: PremiumEmotionVariant = PremiumEmotionVariant.PLAYFUL,
    val message: String? = null,
    val error: String? = null
)

sealed interface SubscriptionEvent {
    data class Message(val text: String) : SubscriptionEvent
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val billingService: BillingService,
    private val userRepository: UserRepository,
    private val premiumEntitlementTracker: PremiumEntitlementTracker,
    private val growthAnalyticsTracker: GrowthAnalyticsTracker,
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getBudgetsUseCase: GetBudgetsUseCase
) : ViewModel() {
    private val eventsChannel = Channel<SubscriptionEvent>(capacity = Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()
    private var hasLoggedPaywallView = false

    private data class PaywallValueData(
        val currencyCode: String = "USD",
        val appLanguageTag: String = "en",
        val topCategoryName: String? = null,
        val topCategorySpendCents: Long = 0L,
        val potentialMonthlySavingsCents: Long = 0L,
        val potentialAnnualSavingsCents: Long = 0L,
        val opportunities: List<PremiumOpportunity> = emptyList(),
        val nudgeVariant: PremiumEmotionVariant = PremiumEmotionVariant.PLAYFUL
    )

    private data class PricingState(
        val isLoading: Boolean = true,
        val plans: List<SubscriptionPlanUi> = emptyList(),
        val selectedProductId: String? = null,
        val monthlyReferencePriceText: String? = null,
        val monthlyReferencePriceMicros: Long? = null,
        val monthlyReferenceCurrencyCode: String? = null
    )

    private val pricingState = MutableStateFlow(PricingState())

    private val paywallValueData = combine(
        getTransactionsUseCase(),
        getCategoriesUseCase(),
        getBudgetsUseCase(),
        userRepository.userPreferences
    ) { transactions, categories, budgets, prefs ->
        val currency = prefs.defaultCurrency
        val appLanguageTag = AppLanguages.normalizeTag(prefs.appLanguageTag)
        val variantSeed = prefs.userId ?: prefs.userEmail ?: prefs.defaultCurrency
        val variant = PremiumEmotionVariant.fromSeed(variantSeed)
        val opportunities = buildPremiumOpportunities(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            currencyCode = currency
        )
        val topCutback = opportunities.firstOrNull { it.type == PremiumOpportunityType.CATEGORY_CUTBACK }
        val strongestImpact = opportunities.maxOfOrNull { it.impactCents } ?: 0L

        PaywallValueData(
            currencyCode = currency,
            appLanguageTag = appLanguageTag,
            topCategoryName = topCutback?.label,
            topCategorySpendCents = topCutback?.amountCents ?: 0L,
            potentialMonthlySavingsCents = strongestImpact,
            potentialAnnualSavingsCents = strongestImpact * 12L,
            opportunities = opportunities,
            nudgeVariant = variant
        )
    }

    val uiState: StateFlow<SubscriptionUiState> = combine(
        subscriptionRepository.getActiveSubscription(),
        subscriptionRepository.isPremium(),
        premiumEntitlementTracker.entitlement,
        paywallValueData,
        pricingState
    ) { active, isPremium, entitlement, paywall, pricing ->
        val savingsVsPriceRatio = calculateSavingsVsPriceRatio(
            monthlySavingsCents = paywall.potentialMonthlySavingsCents,
            paywallCurrencyCode = paywall.currencyCode,
            pricingState = pricing
        )
        SubscriptionUiState(
            isLoading = false,
            isPremium = isPremium,
            hasPremiumAccess = entitlement.hasPremiumAccess,
            isTrialActive = entitlement.isTrialActive,
            trialConsumed = entitlement.trialConsumed,
            trialExpiresAt = entitlement.trialExpiresAt,
            activeSubscription = if (isPremium) active else null,
            currencyCode = paywall.currencyCode,
            appLanguageTag = paywall.appLanguageTag,
            paywallTopCategoryName = paywall.topCategoryName,
            paywallTopCategorySpendCents = paywall.topCategorySpendCents,
            paywallPotentialMonthlySavingsCents = paywall.potentialMonthlySavingsCents,
            paywallPotentialAnnualSavingsCents = paywall.potentialAnnualSavingsCents,
            paywallOpportunities = paywall.opportunities,
            paywallSavingsVsPriceRatio = savingsVsPriceRatio,
            availablePlans = pricing.plans,
            selectedPlanId = pricing.selectedProductId,
            isPricingLoading = pricing.isLoading,
            monthlyReferencePriceText = pricing.monthlyReferencePriceText
                ?: DEFAULT_MONTHLY_PRICE_CENTS.toCurrencyString(paywall.currencyCode),
            nudgeVariant = paywall.nudgeVariant
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionUiState(isLoading = true)
    )

    init {
        // Keep DataStore "isPremium" in sync with local subscription state so other screens can gate features.
        viewModelScope.launch {
            subscriptionRepository.isPremium()
                .distinctUntilChanged()
                .collect { premium ->
                    userRepository.setPremium(premium)
                }
        }

        // Always use backend/billing sources for entitlement state.
        viewModelScope.launch {
            runCatching { subscriptionRepository.syncTrialStateFromBackend() }
            runCatching { subscriptionRepository.syncPremiumStateFromBackend() }
            runCatching { subscriptionRepository.restorePurchases() }
        }

        viewModelScope.launch {
            loadPricing()
        }
    }

    fun selectPlan(productId: String) {
        pricingState.update { current ->
            if (current.plans.none { it.productId == productId }) {
                current
            } else {
                current.copy(selectedProductId = productId)
            }
        }
    }

    fun startTrial(days: Int = 7) {
        viewModelScope.launch {
            try {
                growthAnalyticsTracker.logSubscriptionTrialClick()
                val prefs = userRepository.userPreferences.first()
                if (prefs.userId.isNullOrBlank()) {
                    growthAnalyticsTracker.logSubscriptionTrialBlocked("not_signed_in")
                    eventsChannel.send(SubscriptionEvent.Message("Sign in to start your trial."))
                    return@launch
                }
                if (prefs.isPremium) {
                    growthAnalyticsTracker.logSubscriptionTrialBlocked("already_premium")
                    eventsChannel.send(SubscriptionEvent.Message("You already have an active premium plan."))
                    return@launch
                }
                if (prefs.isTrialActive()) {
                    growthAnalyticsTracker.logSubscriptionTrialBlocked("trial_already_active")
                    eventsChannel.send(SubscriptionEvent.Message("Your trial is already active."))
                    return@launch
                }

                when (val result = subscriptionRepository.claimTrial(days = days)) {
                    is TrialActivationResult.Started -> {
                        growthAnalyticsTracker.logSubscriptionTrialStarted()
                        eventsChannel.send(
                            SubscriptionEvent.Message("Trial started. Enjoy 7 days of premium access.")
                        )
                    }
                    is TrialActivationResult.AlreadyConsumed -> {
                        growthAnalyticsTracker.logSubscriptionTrialBlocked(
                            if (result.expiresAt > System.currentTimeMillis()) {
                                "already_consumed_active"
                            } else {
                                "already_consumed_expired"
                            }
                        )
                        val message = if (result.expiresAt > System.currentTimeMillis()) {
                            "Your trial is already active on this account."
                        } else {
                            "Your 7-day trial has already been used."
                        }
                        eventsChannel.send(SubscriptionEvent.Message(message))
                    }
                    TrialActivationResult.NotSignedIn -> {
                        growthAnalyticsTracker.logSubscriptionTrialBlocked("not_signed_in")
                        eventsChannel.send(SubscriptionEvent.Message("Sign in to start your trial."))
                    }
                    TrialActivationResult.BackendUnavailable -> {
                        growthAnalyticsTracker.logSubscriptionTrialFailed("backend_unavailable")
                        eventsChannel.send(
                            SubscriptionEvent.Message(
                                "Unable to verify trial eligibility. Please check your connection and try again."
                            )
                        )
                    }
                    is TrialActivationResult.Failure -> {
                        growthAnalyticsTracker.logSubscriptionTrialFailed("backend_failure")
                        eventsChannel.send(
                            SubscriptionEvent.Message(
                                result.message.ifBlank { "Unable to start trial right now." }
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                growthAnalyticsTracker.logSubscriptionTrialFailed("exception")
                eventsChannel.send(
                    SubscriptionEvent.Message(e.message ?: "Unable to start trial right now.")
                )
            }
        }
    }

    fun onPaywallViewed() {
        if (hasLoggedPaywallView) return
        hasLoggedPaywallView = true
        growthAnalyticsTracker.logSubscriptionPaywallViewed()
    }

    fun onMochiNudgeImpression(surface: String, variant: String) {
        growthAnalyticsTracker.logPremiumNudgeImpression(surface = surface, variant = variant)
    }

    fun onMochiNudgeClick(surface: String, variant: String, action: String) {
        growthAnalyticsTracker.logPremiumNudgeClick(
            surface = surface,
            variant = variant,
            action = action
        )
    }

    fun onPurchaseClick() {
        // Kept for compatibility with existing call sites.
        eventsChannel.trySend(SubscriptionEvent.Message("Unable to open Google Play checkout from this screen."))
    }

    fun onPurchaseClick(activity: Activity) {
        viewModelScope.launch {
            if (pricingState.value.isLoading && pricingState.value.plans.isEmpty()) {
                eventsChannel.send(SubscriptionEvent.Message("Loading live Google Play pricing. Please try again in a moment."))
                return@launch
            }
            val selectedProductId = pricingState.value.selectedProductId
                ?: pricingState.value.plans.firstOrNull()?.productId
            if (selectedProductId.isNullOrBlank()) {
                eventsChannel.send(SubscriptionEvent.Message("Premium plans are temporarily unavailable. Please try again shortly."))
                return@launch
            }

            growthAnalyticsTracker.logSubscriptionPurchaseClick()
            when (
                val result = subscriptionRepository.purchaseSubscription(
                    activity = activity,
                    productId = selectedProductId
                )
            ) {
                is PurchaseSubscriptionResult.Success -> {
                    growthAnalyticsTracker.logSubscriptionPurchaseSucceeded(selectedProductId)
                    eventsChannel.send(
                        SubscriptionEvent.Message("Premium unlocked. Purchase verified successfully.")
                    )
                }
                PurchaseSubscriptionResult.NotSignedIn -> {
                    growthAnalyticsTracker.logSubscriptionPurchaseFailed("not_signed_in")
                    eventsChannel.send(
                        SubscriptionEvent.Message("Sign in before purchasing Premium.")
                    )
                }
                PurchaseSubscriptionResult.UserCanceled -> {
                    growthAnalyticsTracker.logSubscriptionPurchaseFailed("user_canceled")
                    eventsChannel.send(
                        SubscriptionEvent.Message("Checkout canceled.")
                    )
                }
                PurchaseSubscriptionResult.Pending -> {
                    growthAnalyticsTracker.logSubscriptionPurchasePending(selectedProductId)
                    eventsChannel.send(
                        SubscriptionEvent.Message("Purchase is pending. Premium unlocks after Google confirms payment.")
                    )
                }
                PurchaseSubscriptionResult.ProductUnavailable -> {
                    growthAnalyticsTracker.logSubscriptionPurchaseFailed("product_unavailable")
                    eventsChannel.send(
                        SubscriptionEvent.Message("Premium plan is not available in Play Console yet.")
                    )
                }
                is PurchaseSubscriptionResult.Failure -> {
                    growthAnalyticsTracker.logSubscriptionPurchaseFailed("purchase_failed")
                    eventsChannel.send(
                        SubscriptionEvent.Message(
                            result.message.ifBlank { "Unable to complete purchase right now." }
                        )
                    )
                }
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            when (val result = subscriptionRepository.restorePurchases()) {
                is RestorePurchasesResult.Restored -> {
                    growthAnalyticsTracker.logSubscriptionRestoreSucceeded(result.activeCount)
                    eventsChannel.send(
                        SubscriptionEvent.Message("Purchases restored. Premium is active.")
                    )
                }
                RestorePurchasesResult.NoActivePurchases -> {
                    growthAnalyticsTracker.logSubscriptionRestoreFailed("none_found")
                    eventsChannel.send(
                        SubscriptionEvent.Message("No active subscription found to restore.")
                    )
                }
                is RestorePurchasesResult.Failure -> {
                    growthAnalyticsTracker.logSubscriptionRestoreFailed("restore_error")
                    eventsChannel.send(
                        SubscriptionEvent.Message(
                            result.message.ifBlank { "Unable to restore purchases right now." }
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadPricing() {
        pricingState.update { it.copy(isLoading = true) }
        when (
            val result = billingService.querySubscriptionPlans(
                productIds = listOf(
                    Constants.SUBSCRIPTION_YEARLY,
                    Constants.SUBSCRIPTION_MONTHLY
                )
            )
        ) {
            is BillingCatalogResult.Failure -> {
                pricingState.update {
                    it.copy(
                        isLoading = false,
                        plans = emptyList(),
                        selectedProductId = null,
                        monthlyReferencePriceText = null,
                        monthlyReferencePriceMicros = null,
                        monthlyReferenceCurrencyCode = null
                    )
                }
            }
            is BillingCatalogResult.Success -> {
                val pricing = buildPricingState(result.plans)
                pricingState.value = pricing
            }
        }
    }

    private fun buildPricingState(
        plans: List<BillingSubscriptionPlan>
    ): PricingState {
        val monthlyPlan = plans.firstOrNull { it.productId == Constants.SUBSCRIPTION_MONTHLY }
        val yearlyPlan = plans.firstOrNull { it.productId == Constants.SUBSCRIPTION_YEARLY }
        val orderedPlans = buildList {
            if (yearlyPlan != null) add(yearlyPlan)
            if (monthlyPlan != null) add(monthlyPlan)
            plans.forEach { plan ->
                if (plan.productId != Constants.SUBSCRIPTION_YEARLY && plan.productId != Constants.SUBSCRIPTION_MONTHLY) {
                    add(plan)
                }
            }
        }
        val uiPlans = orderedPlans.map { plan ->
            plan.toUiModel(monthlyPlan = monthlyPlan)
        }

        val monthlyReferenceText = monthlyPlan?.formattedPrice
            ?: yearlyPlan?.let { formatMicrosAsCurrency(it.priceAmountMicros / 12L, it.priceCurrencyCode) }
        val monthlyReferenceMicros = monthlyPlan?.priceAmountMicros
            ?: yearlyPlan?.priceAmountMicros?.div(12L)
        val monthlyReferenceCurrencyCode = monthlyPlan?.priceCurrencyCode ?: yearlyPlan?.priceCurrencyCode
        val defaultSelectedProductId = when {
            uiPlans.any { it.productId == Constants.SUBSCRIPTION_YEARLY } -> Constants.SUBSCRIPTION_YEARLY
            uiPlans.any { it.productId == Constants.SUBSCRIPTION_MONTHLY } -> Constants.SUBSCRIPTION_MONTHLY
            else -> uiPlans.firstOrNull()?.productId
        }

        return PricingState(
            isLoading = false,
            plans = uiPlans,
            selectedProductId = defaultSelectedProductId,
            monthlyReferencePriceText = monthlyReferenceText,
            monthlyReferencePriceMicros = monthlyReferenceMicros,
            monthlyReferenceCurrencyCode = monthlyReferenceCurrencyCode
        )
    }

    private fun BillingSubscriptionPlan.toUiModel(
        monthlyPlan: BillingSubscriptionPlan?
    ): SubscriptionPlanUi {
        return when (productId) {
            Constants.SUBSCRIPTION_YEARLY -> {
                val monthlyEquivalentText = formatMicrosAsCurrency(
                    micros = priceAmountMicros / 12L,
                    currencyCode = priceCurrencyCode
                )
                val yearlySavingsPercent = calculateYearlySavingsPercent(
                    yearlyPlan = this,
                    monthlyPlan = monthlyPlan
                )
                SubscriptionPlanUi(
                    productId = productId,
                    title = "Yearly Premium",
                    priceText = formattedPrice,
                    billingDetail = "Billed yearly",
                    secondaryDetail = monthlyEquivalentText?.let { "About $it per month" },
                    badgeText = if (yearlySavingsPercent != null && yearlySavingsPercent > 0) {
                        "Best value"
                    } else {
                        null
                    }
                )
            }
            Constants.SUBSCRIPTION_MONTHLY -> {
                SubscriptionPlanUi(
                    productId = productId,
                    title = "Monthly Premium",
                    priceText = formattedPrice,
                    billingDetail = "Billed monthly",
                    secondaryDetail = "Flexible monthly billing"
                )
            }
            else -> {
                SubscriptionPlanUi(
                    productId = productId,
                    title = productId,
                    priceText = formattedPrice,
                    billingDetail = humanizeBillingPeriod(billingPeriod)
                )
            }
        }
    }

    private fun calculateSavingsVsPriceRatio(
        monthlySavingsCents: Long,
        paywallCurrencyCode: String,
        pricingState: PricingState
    ): Float {
        val priceMicros = pricingState.monthlyReferencePriceMicros ?: return 0f
        val priceCurrencyCode = pricingState.monthlyReferenceCurrencyCode ?: return 0f
        if (!priceCurrencyCode.equals(paywallCurrencyCode, ignoreCase = true) || priceMicros <= 0L || monthlySavingsCents <= 0L) {
            return 0f
        }
        return ((monthlySavingsCents.toDouble() * 10_000.0) / priceMicros.toDouble()).toFloat()
    }

    private fun calculateYearlySavingsPercent(
        yearlyPlan: BillingSubscriptionPlan,
        monthlyPlan: BillingSubscriptionPlan?
    ): Int? {
        if (monthlyPlan == null) return null
        if (!yearlyPlan.priceCurrencyCode.equals(monthlyPlan.priceCurrencyCode, ignoreCase = true)) {
            return null
        }
        val annualMonthlyMicros = monthlyPlan.priceAmountMicros * 12L
        if (annualMonthlyMicros <= yearlyPlan.priceAmountMicros) return null
        val savingsRatio = (annualMonthlyMicros - yearlyPlan.priceAmountMicros).toDouble() / annualMonthlyMicros.toDouble()
        return (savingsRatio * 100.0).toInt().takeIf { it > 0 }
    }

    private fun formatMicrosAsCurrency(
        micros: Long,
        currencyCode: String
    ): String? {
        if (micros <= 0L) return null
        return runCatching {
            val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                currency = Currency.getInstance(currencyCode)
            }
            format.format(micros / 1_000_000.0)
        }.getOrNull()
    }

    private fun humanizeBillingPeriod(billingPeriod: String): String {
        return when {
            billingPeriod.contains("Y", ignoreCase = true) -> "Billed yearly"
            billingPeriod.contains("M", ignoreCase = true) -> "Billed monthly"
            billingPeriod.contains("W", ignoreCase = true) -> "Billed weekly"
            else -> "Recurring subscription"
        }
    }

    private companion object {
        const val DEFAULT_MONTHLY_PRICE_CENTS = 999L

        fun monthBounds(month: YearMonth): Pair<Long, Long> {
            val zone = ZoneId.systemDefault()
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return start to (endExclusive - 1L)
        }
    }
}


