package com.chelumore.xpenze.feature.subscription

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import com.chelumore.xpenze.core.localization.localizedUnknown
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.Constants
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.growth.formatPremiumOutcomeSummary
import com.chelumore.xpenze.core.growth.MochiPremiumCopywriter
import com.chelumore.xpenze.core.growth.premiumOpportunitySectionTitle
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumBenefit
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumComparisonBody
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumFeatureLabel
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumFreeValue
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumValue
import com.chelumore.xpenze.core.ui.components.growth.AnimatedMochiCatHero
import com.chelumore.xpenze.core.ui.components.growth.MochiPremiumNudgeCard
import com.chelumore.xpenze.core.ui.components.growth.MochiValueTrackerCard
import com.chelumore.xpenze.core.ui.components.growth.PremiumOpportunityCard
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.YellowPrimary
import java.util.Locale

@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPlan = remember(uiState.availablePlans, uiState.selectedPlanId) {
        uiState.availablePlans.firstOrNull { it.productId == uiState.selectedPlanId }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SubscriptionEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(uiState.hasPremiumAccess) {
        if (!uiState.hasPremiumAccess) {
            viewModel.onPaywallViewed()
        }
    }

    LaunchedEffect(uiState.hasPremiumAccess, uiState.nudgeVariant) {
        if (!uiState.hasPremiumAccess) {
            viewModel.onMochiNudgeImpression(
                surface = "subscription_paywall",
                variant = uiState.nudgeVariant.name.lowercase(Locale.ROOT)
            )
        }
    }

    Scaffold(
        topBar = { XpenzeTopAppBar(title = stringResource(R.string.subscription_title), onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 820.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                StatusCard(
                    isPremium = uiState.isPremium,
                    isTrialActive = uiState.isTrialActive
                )

            if (uiState.hasPremiumAccess) {
                if (uiState.isPremium) {
                    val sub = uiState.activeSubscription
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.subscription_active_plan),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.subscription_expires,
                                    sub?.expiryTime?.toFormattedDateTime()
                                        ?: stringResource(R.string.subscription_unknown)
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(
                                    R.string.subscription_product,
                                    sub?.productId.toDisplayPlanName()
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { openPlaySubscriptionManagement(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ErrorRed.copy(alpha = 0.14f),
                                    contentColor = ErrorRed
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.subscription_cancel_plan), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.subscription_trial_card_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.subscription_trial_ends,
                                    uiState.trialExpiresAt.toFormattedDateTime()
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.subscription_trial_note),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                PaywallValueCard(uiState = uiState)
                PlanSelectorCard(
                    plans = uiState.availablePlans,
                    selectedProductId = uiState.selectedPlanId,
                    isLoading = uiState.isPricingLoading,
                    onSelectPlan = viewModel::selectPlan
                )
                FreeVsPremiumComparisonCard(uiState.appLanguageTag)

                val mochiCopy = MochiPremiumCopywriter.paywall(
                    variant = uiState.nudgeVariant,
                    potentialMonthlySavingsCents = uiState.paywallPotentialMonthlySavingsCents,
                    currencyCode = uiState.currencyCode,
                    monthlyPriceText = uiState.monthlyReferencePriceText
                )
                AnimatedMochiCatHero(
                    headline = mochiCopy.headline
                )
                MochiPremiumNudgeCard(
                    copy = mochiCopy,
                    primaryCtaText = stringResource(R.string.subscription_checkout_cta),
                    onPrimaryCta = {
                        viewModel.onMochiNudgeClick(
                            surface = "subscription_paywall",
                            variant = uiState.nudgeVariant.name.lowercase(Locale.ROOT),
                            action = "checkout"
                        )
                        val hostActivity = activity
                        if (hostActivity != null) {
                            viewModel.onPurchaseClick(hostActivity)
                        } else {
                            viewModel.onPurchaseClick()
                        }
                    },
                    secondaryCtaText = if (uiState.trialConsumed) null else stringResource(R.string.subscription_start_trial),
                    onSecondaryCta = if (uiState.trialConsumed) {
                        null
                    } else {
                        {
                            viewModel.onMochiNudgeClick(
                                surface = "subscription_paywall",
                                variant = uiState.nudgeVariant.name.lowercase(Locale.ROOT),
                                action = "start_trial"
                            )
                            viewModel.startTrial(days = 7)
                        }
                    }
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Benefit(localizedAutoCapturePremiumBenefit(uiState.appLanguageTag))
                        Benefit(stringResource(R.string.subscription_feature_monthly_savings_insights))
                        Benefit(stringResource(R.string.subscription_benefit_ai))
                        Benefit(stringResource(R.string.subscription_benefit_reports))
                        Benefit(stringResource(R.string.subscription_benefit_export))
                        Benefit(stringResource(R.string.subscription_benefit_ads))
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.onMochiNudgeClick(
                            surface = "subscription_paywall",
                            variant = uiState.nudgeVariant.name.lowercase(Locale.ROOT),
                            action = "restore"
                        )
                        viewModel.restorePurchases()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.subscription_restore_purchases),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.trialConsumed) {
                    Text(
                        text = stringResource(R.string.subscription_trial_used_note),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

            }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(isPremium: Boolean, isTrialActive: Boolean) {
    val (label, color, bg) = when {
        isPremium -> Triple(
            stringResource(R.string.subscription_status_premium_active),
            SuccessGreen,
            SuccessGreen.copy(alpha = 0.14f)
        )
        isTrialActive -> Triple(
            stringResource(R.string.subscription_status_trial_active),
            InfoBlue,
            InfoBlue.copy(alpha = 0.14f)
        )
        else -> Triple(
            stringResource(R.string.subscription_status_free_plan),
            YellowPrimary,
            YellowPrimary.copy(alpha = 0.12f)
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(Icons.Default.Star, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = when {
                        isPremium -> stringResource(R.string.subscription_desc_premium)
                        isTrialActive -> stringResource(R.string.subscription_desc_trial)
                        else -> stringResource(R.string.subscription_desc_free)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Text(
        text = "• $text",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun PaywallValueCard(uiState: SubscriptionUiState) {
    val outcomeSummary = remember(
        uiState.paywallOpportunities,
        uiState.currencyCode,
        uiState.appLanguageTag
    ) {
        formatPremiumOutcomeSummary(
            opportunities = uiState.paywallOpportunities,
            currencyCode = uiState.currencyCode,
            languageTag = uiState.appLanguageTag,
            hasPremiumAccess = false
        )
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = YellowPrimary.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MochiValueTrackerCard(
                opportunities = uiState.paywallOpportunities,
                currencyCode = uiState.currencyCode,
                appLanguageTag = uiState.appLanguageTag,
                hasPremiumAccess = false
            )
            if (outcomeSummary != null) {
                Text(
                    text = outcomeSummary.headline,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = outcomeSummary.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.subscription_value_save_more_than,
                        uiState.monthlyReferencePriceText
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            val topCategory = uiState.paywallTopCategoryName
            if (!topCategory.isNullOrBlank() && uiState.paywallTopCategorySpendCents > 0L) {
                Text(
                    text = stringResource(
                        R.string.subscription_value_highest_spend,
                        topCategory,
                        uiState.paywallTopCategorySpendCents.toCurrencyString(uiState.currencyCode)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(
                        R.string.subscription_value_reduction,
                        uiState.paywallPotentialMonthlySavingsCents.toCurrencyString(uiState.currencyCode),
                        uiState.paywallPotentialAnnualSavingsCents.toCurrencyString(uiState.currencyCode)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (uiState.paywallSavingsVsPriceRatio > 0f) {
                    Text(
                        text = stringResource(
                            R.string.subscription_value_ratio,
                            String.format(Locale.getDefault(), "%.1f", uiState.paywallSavingsVsPriceRatio)
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.subscription_value_track_more),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (uiState.paywallOpportunities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = premiumOpportunitySectionTitle(uiState.appLanguageTag),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
                uiState.paywallOpportunities.forEach { opportunity ->
                    PremiumOpportunityCard(
                        opportunity = opportunity,
                        currencyCode = uiState.currencyCode,
                        appLanguageTag = uiState.appLanguageTag
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanSelectorCard(
    plans: List<SubscriptionPlanUi>,
    selectedProductId: String?,
    isLoading: Boolean,
    onSelectPlan: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.subscription_choose_plan),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isLoading && plans.isEmpty()) {
                Text(
                    text = stringResource(R.string.subscription_loading_pricing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (plans.isEmpty()) {
                Text(
                    text = stringResource(R.string.subscription_pricing_unavailable),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                plans.forEach { plan ->
                    val isSelected = plan.productId == selectedProductId
                    val borderColor = if (isSelected) YellowPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    val containerColor = if (isSelected) {
                        YellowPrimary.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectPlan(plan.productId) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = plan.title,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    plan.badgeText?.let { badge ->
                                        Box(
                                            modifier = Modifier
                                                .background(YellowPrimary.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = badge,
                                                color = YellowPrimary,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = plan.billingDetail,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                plan.secondaryDetail?.let { detail ->
                                    Text(
                                        text = detail,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = plan.priceText,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeVsPremiumComparisonCard(appLanguageTag: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.subscription_free_vs_premium),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = localizedAutoCapturePremiumComparisonBody(appLanguageTag),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            ComparisonRow(
                feature = localizedAutoCapturePremiumFeatureLabel(appLanguageTag),
                freeValue = localizedAutoCapturePremiumFreeValue(appLanguageTag),
                premiumValue = localizedAutoCapturePremiumValue(appLanguageTag)
            )
            ComparisonRow(
                feature = stringResource(R.string.subscription_feature_mochi_finance_chat),
                freeValue = stringResource(R.string.subscription_feature_locked),
                premiumValue = stringResource(R.string.subscription_feature_included)
            )
            ComparisonRow(
                feature = stringResource(R.string.subscription_feature_monthly_savings_insights),
                freeValue = stringResource(R.string.subscription_feature_basic),
                premiumValue = stringResource(R.string.subscription_feature_personalized)
            )
            ComparisonRow(
                feature = stringResource(R.string.subscription_feature_advanced_reports),
                freeValue = stringResource(R.string.subscription_feature_locked),
                premiumValue = stringResource(R.string.subscription_feature_included)
            )
            ComparisonRow(
                feature = stringResource(R.string.subscription_benefit_export),
                freeValue = stringResource(R.string.subscription_feature_locked),
                premiumValue = stringResource(R.string.subscription_feature_included)
            )
            ComparisonRow(
                feature = stringResource(R.string.subscription_feature_ads),
                freeValue = stringResource(R.string.subscription_feature_shown),
                premiumValue = stringResource(R.string.subscription_feature_removed)
            )
        }
    }
}

@Composable
private fun ComparisonRow(feature: String, freeValue: String, premiumValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        ComparisonValuePill(
            text = freeValue,
            highlight = false
        )
        ComparisonValuePill(
            text = premiumValue,
            highlight = true
        )
    }
}

@Composable
private fun ComparisonValuePill(text: String, highlight: Boolean) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .background(
                color = if (highlight) YellowPrimary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (highlight) YellowPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun String?.toDisplayPlanName(): String {
    return when (this) {
        Constants.SUBSCRIPTION_MONTHLY -> "Monthly Premium"
        Constants.SUBSCRIPTION_YEARLY -> "Yearly Premium"
        null -> localizedUnknown()
        else -> this
    }
}

private fun openPlaySubscriptionManagement(context: Context) {
    val appPackage = context.packageName
    val deepLink = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/account/subscriptions?package=$appPackage")
    ).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webLink = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/account/subscriptions?package=$appPackage")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(deepLink) }
        .onFailure { runCatching { context.startActivity(webLink) } }
}


