package com.chelumore.xpenze.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.growth.buildMochiPromptForOpportunity
import com.chelumore.xpenze.core.growth.formatPremiumOutcomeSummary
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.premiumOpportunityActionLabel
import com.chelumore.xpenze.core.growth.premiumOpportunityDashboardMessage
import com.chelumore.xpenze.core.growth.premiumOpportunityPrimaryCta
import com.chelumore.xpenze.core.growth.premiumOpportunitySectionTitle
import com.chelumore.xpenze.core.localization.localizedAutoCaptureHomeCopy
import com.chelumore.xpenze.core.ui.components.growth.MochiValueTrackerCard
import com.chelumore.xpenze.core.ui.components.ads.AdPlacement
import com.chelumore.xpenze.core.ui.components.ads.FreeTierAdSlot
import com.chelumore.xpenze.core.ui.components.display.AccountCard
import com.chelumore.xpenze.core.ui.components.growth.PremiumOpportunityCard
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.core.ui.util.parseHexColorOrFallback
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary
import com.chelumore.xpenze.ui.theme.YellowSecondary
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val HiddenAmountPlaceholder = "••••••"

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToAddTransaction: (String) -> Unit = {},
    onNavigateToAllTransactions: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToDraftInbox: () -> Unit = {},
    onNavigateToAutoCaptureSetup: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToAiChat: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YellowPrimary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { HeaderSection(uiState, onSelectMonth = viewModel::setMonth) }
                    item {
                        BalanceSection(
                            state = uiState,
                            onToggleBalanceVisibility = viewModel::toggleBalanceVisibility
                        )
                    }
                    item {
                        FreeTierAdSlot(
                            hasPremiumAccess = uiState.hasPremiumAccess,
                            placement = AdPlacement.DASHBOARD
                        )
                    }
                    item {
                        ActionButtonsSection(
                            hasTransactions = uiState.recentTransactions.isNotEmpty(),
                            onNavigateToAddTransaction = onNavigateToAddTransaction
                        )
                    }
                    item {
                        AutoCaptureHomeSection(
                            state = uiState,
                            onOpenAutoCapture = {
                                if (uiState.hasPremiumAccess && uiState.autoCaptureEnabled) {
                                    onNavigateToDraftInbox()
                                } else {
                                    onNavigateToAutoCaptureSetup()
                                }
                            },
                            onUpgrade = onNavigateToSubscription
                        )
                    }
                    if (uiState.savingsOpportunities.isNotEmpty()) {
                        item {
                            SavingsOpportunitiesSection(
                                state = uiState,
                                onPrimaryAction = {
                                    if (uiState.hasPremiumAccess) {
                                        onNavigateToAiChat(null)
                                    } else {
                                        onNavigateToSubscription()
                                    }
                                },
                                onOpenOpportunity = { prompt -> onNavigateToAiChat(prompt) },
                                onUpgrade = onNavigateToSubscription
                            )
                        }
                    }
                    item { SpendingSection(uiState) }
                    item {
                        RecentTransactionsSection(
                            state = uiState,
                            onNavigateToAllTransactions = onNavigateToAllTransactions
                        )
                    }
                    item { BudgetAlertsSection(uiState) }
                    item { AccountsSection(uiState, onNavigateToAccounts) }
                }
            }
        }
    }
}

@Composable
private fun SavingsOpportunitiesSection(
    state: DashboardUiState,
    onPrimaryAction: () -> Unit,
    onOpenOpportunity: (String) -> Unit,
    onUpgrade: () -> Unit
) {
    val visibleOpportunities = remember(state.savingsOpportunities, state.hasPremiumAccess) {
        state.savingsOpportunities.take(if (state.hasPremiumAccess) 2 else 1)
    }
    val summary = remember(state.savingsOpportunities, state.currencyCode, state.appLanguageTag, state.hasPremiumAccess) {
        formatPremiumOutcomeSummary(
            opportunities = state.savingsOpportunities,
            currencyCode = state.currencyCode,
            languageTag = state.appLanguageTag,
            hasPremiumAccess = state.hasPremiumAccess
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = premiumOpportunitySectionTitle(state.appLanguageTag),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            MochiValueTrackerCard(
                opportunities = state.savingsOpportunities,
                currencyCode = state.currencyCode,
                appLanguageTag = state.appLanguageTag,
                hasPremiumAccess = state.hasPremiumAccess
            )
            if (summary != null) {
                Text(
                    text = summary.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = summary.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = premiumOpportunityDashboardMessage(
                        languageTag = state.appLanguageTag,
                        hasPremiumAccess = state.hasPremiumAccess
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            visibleOpportunities.forEach { opportunity ->
                PremiumOpportunityCard(
                    opportunity = opportunity,
                    currencyCode = state.currencyCode,
                    appLanguageTag = state.appLanguageTag,
                    actionText = premiumOpportunityActionLabel(
                        languageTag = state.appLanguageTag,
                        hasPremiumAccess = state.hasPremiumAccess
                    ),
                    onAction = {
                        if (state.hasPremiumAccess) {
                            onOpenOpportunity(
                                buildMochiPromptForOpportunity(
                                    opportunity = opportunity,
                                    currencyCode = state.currencyCode,
                                    languageTag = state.appLanguageTag
                                )
                            )
                        } else {
                            onUpgrade()
                        }
                    }
                )
            }

            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = premiumOpportunityPrimaryCta(
                        languageTag = state.appLanguageTag,
                        hasPremiumAccess = state.hasPremiumAccess
                    ),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    state: DashboardUiState,
    onSelectMonth: (YearMonth) -> Unit
) {
    val hour = java.time.LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> stringResource(R.string.dashboard_greeting_morning)
        in 12..16 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }
    val appLocale = LocalContext.current.resources.configuration.locales[0]

    val monthFormatter = remember(appLocale) {
        DateTimeFormatter.ofPattern("MMMM yyyy", appLocale)
    }
    val monthLabel = remember(state.currentMonth) { state.currentMonth.format(monthFormatter) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    val monthOptions = remember {
        val now = YearMonth.now()
        (0..23).map { now.minusMonths(it.toLong()) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.hasFounderReward) {
                Spacer(modifier = Modifier.height(8.dp))
                DashboardFounderBadge()
            }
        }
        
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Month Selector Pill
            Box {
                Box(
                modifier = Modifier
                    .widthIn(min = 110.dp, max = 170.dp)
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { monthMenuExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.dashboard_cd_select_month),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false }
                ) {
                    monthOptions.forEach { month ->
                        val label = month.format(monthFormatter)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label)
                                    if (month == state.currentMonth) {
                                        Text("✓", color = YellowPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = {
                                onSelectMonth(month)
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Profile Icon placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!state.userPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = state.userPhotoUrl,
                        contentDescription = stringResource(R.string.dashboard_cd_profile_photo),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = state.userName
                        .trim()
                        .firstOrNull()
                        ?.uppercase()
                        ?: stringResource(R.string.dashboard_profile_initial_fallback)
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        color = YellowPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardFounderBadge() {
    Surface(
        color = YellowPrimary.copy(alpha = 0.16f),
        contentColor = YellowPrimary,
        shape = RoundedCornerShape(100)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.settings_founder_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BalanceSection(
    state: DashboardUiState,
    onToggleBalanceVisibility: () -> Unit
) {
    val change = state.balanceChangePercent
    val changeUp = change >= 0
    val changeColor = if (changeUp) SuccessGreen else ErrorRed
    val changeText = remember(change) {
        val absVal = kotlin.math.abs(change)
        String.format(Locale.getDefault(), "%s%.1f%%", if (changeUp) "+" else "-", absVal)
    }
    val incomeText = if (state.areBalancesHidden) {
        HiddenAmountPlaceholder
    } else {
        state.monthlyIncome.toCurrencyString(state.currencyCode)
    }
    val expenseText = if (state.areBalancesHidden) {
        HiddenAmountPlaceholder
    } else {
        state.monthlyExpense.toCurrencyString(state.currencyCode)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_total_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                IconButton(onClick = onToggleBalanceVisibility) {
                    Icon(
                        imageVector = if (state.areBalancesHidden) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (state.areBalancesHidden) {
                            stringResource(R.string.dashboard_cd_show_balance)
                        } else {
                            stringResource(R.string.dashboard_cd_hide_balance)
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AdaptiveBalanceAmount(
                text = if (state.areBalancesHidden) {
                    HiddenAmountPlaceholder
                } else {
                    state.totalBalance.toCurrencyString(state.currencyCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            )

            Column {
                Text(
                    text = stringResource(R.string.dashboard_total_balance_change_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(changeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (changeUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = changeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = changeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Income / Expense Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income
                IncomeExpenseCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.dashboard_income),
                    amount = incomeText,
                    icon = Icons.Default.ArrowUpward,
                    iconColor = SuccessGreen
                )
                // Expense
                IncomeExpenseCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.dashboard_expense),
                    amount = expenseText,
                    icon = Icons.Default.ArrowDownward,
                    iconColor = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun IncomeExpenseCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    icon: ImageVector,
    iconColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AdaptiveBalanceAmount(
    text: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 260.dp
        val length = text.length
        val fontSize = when {
            length >= 24 -> if (compact) 18.sp else 22.sp
            length >= 21 -> if (compact) 20.sp else 24.sp
            length >= 18 -> if (compact) 24.sp else 28.sp
            length >= 15 -> if (compact) 28.sp else 34.sp
            length >= 12 -> if (compact) 32.sp else 40.sp
            else -> if (compact) 36.sp else 48.sp
        }

        Text(
            text = text,
            fontSize = fontSize,
            lineHeight = (fontSize.value * 1.05f).sp,
            fontWeight = FontWeight.Bold,
            color = YellowPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButtonsSection(
    hasTransactions: Boolean,
    onNavigateToAddTransaction: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasTransactions) {
                    stringResource(R.string.dashboard_quick_actions_title)
                } else {
                    stringResource(R.string.dashboard_start_here_title)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasTransactions) {
                    stringResource(R.string.dashboard_quick_actions_body)
                } else {
                    stringResource(R.string.dashboard_start_here_body)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onNavigateToAddTransaction("EXPENSE") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_add_expense),
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onNavigateToAddTransaction("INCOME") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.dashboard_add_income))
                }

                OutlinedButton(
                    onClick = { onNavigateToAddTransaction("TRANSFER") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.dashboard_transfer))
                }
            }
        }
    }
}

@Composable
private fun AutoCaptureHomeSection(
    state: DashboardUiState,
    onOpenAutoCapture: () -> Unit,
    onUpgrade: () -> Unit
) {
    val copy = remember(
        state.appLanguageTag,
        state.hasPremiumAccess,
        state.autoCaptureEnabled,
        state.pendingAutoCaptureDraftCount
    ) {
        localizedAutoCaptureHomeCopy(
            languageTag = state.appLanguageTag,
            hasPremiumAccess = state.hasPremiumAccess,
            autoCaptureEnabled = state.autoCaptureEnabled,
            pendingDraftCount = state.pendingAutoCaptureDraftCount
        )
    }
    val cardColor = when {
        !state.hasPremiumAccess -> YellowPrimary.copy(alpha = 0.12f)
        state.autoCaptureEnabled && state.pendingAutoCaptureDraftCount > 0 -> InfoBlue.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(YellowPrimary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = YellowPrimary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = copy.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.autoCaptureEnabled && state.pendingAutoCaptureDraftCount > 0) {
                        Text(
                            text = stringResource(
                                R.string.auto_capture_pending_count,
                                state.pendingAutoCaptureDraftCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = InfoBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = copy.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenAutoCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = copy.primaryCta,
                    fontWeight = FontWeight.Bold
                )
            }

            if (!state.hasPremiumAccess) {
                OutlinedButton(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.auto_capture_upgrade_button),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingSection(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.dashboard_spending_by_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (state.categorySpending.isEmpty() || state.monthlyExpense <= 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val appLocale = LocalContext.current.resources.configuration.locales[0]
                    Text(
                        text = stringResource(
                            R.string.dashboard_no_spending_for_period,
                            state.currentMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", appLocale))
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            val donutData = remember(state.categorySpending) { state.categorySpending.map { it.amount.toFloat() } }
            val donutColors = remember(state.categorySpending) {
                state.categorySpending.map { parseHexColorOrFallback(it.colorHex, YellowPrimary) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(data = donutData, colors = donutColors)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.dashboard_total_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.monthlyExpense.toCurrencyString(state.currencyCode),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.categorySpending.forEach { item ->
                        val color = parseHexColorOrFallback(item.colorHex, YellowPrimary)
                        val percentText = "${(item.percent * 100).toInt()}%"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(110.dp),
                                maxLines = 1
                            )
                            Text(
                                text = percentText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    data: List<Float>,
    colors: List<Color>,
    thickness: Dp = 16.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        val total = data.sum()
        
        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors.getOrElse(index) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    state: DashboardUiState,
    onNavigateToAllTransactions: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dashboard_recent_transactions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (state.recentTransactions.isNotEmpty()) {
                TextButton(onClick = onNavigateToAllTransactions) {
                    Text(
                        text = stringResource(R.string.dashboard_see_all),
                        color = YellowPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        if (state.recentTransactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_no_transactions_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.recentTransactions.forEach { tx ->
                    TransactionItem(tx)
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: TransactionUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val iconBg = when (transaction.type) {
                com.chelumore.xpenze.domain.model.TransactionType.TRANSFER -> InfoBlue.copy(alpha = 0.18f)
                else -> parseHexColorOrFallback(transaction.categoryColorHex, YellowPrimary).copy(alpha = 0.18f)
            }
            val iconTint = when (transaction.type) {
                com.chelumore.xpenze.domain.model.TransactionType.INCOME -> SuccessGreen
                com.chelumore.xpenze.domain.model.TransactionType.EXPENSE -> ErrorRed
                com.chelumore.xpenze.domain.model.TransactionType.TRANSFER -> InfoBlue
            }
            val icon = when (transaction.type) {
                com.chelumore.xpenze.domain.model.TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                else -> iconForName(transaction.categoryIcon)
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                val subtitle = if (transaction.type == com.chelumore.xpenze.domain.model.TransactionType.TRANSFER) {
                    val fromName = transaction.accountName.ifBlank { stringResource(R.string.common_from) }
                    val toName = transaction.toAccountName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.common_to)
                    "$fromName \u2192 $toName"
                } else {
                    val categoryLabel = transaction.categoryName.ifBlank {
                        if (transaction.type == com.chelumore.xpenze.domain.model.TransactionType.TRANSFER) {
                            stringResource(R.string.transaction_form_type_transfer)
                        } else {
                            stringResource(R.string.transaction_form_uncategorized)
                        }
                    }
                    val accountLabel = transaction.accountName.ifBlank { stringResource(R.string.common_account) }
                    "$categoryLabel • $accountLabel"
                }
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            val sign = when (transaction.type) {
                com.chelumore.xpenze.domain.model.TransactionType.INCOME -> "+"
                com.chelumore.xpenze.domain.model.TransactionType.EXPENSE -> "-"
                com.chelumore.xpenze.domain.model.TransactionType.TRANSFER -> ""
            }
            Text(
                text = "$sign${transaction.amount.toCurrencyString(transaction.currency)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = iconTint
            )
        }
    }
}

@Composable
private fun BudgetAlertsSection(state: DashboardUiState) {
    Column {
        Text(
            text = stringResource(R.string.dashboard_budget_alerts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (state.budgetAlerts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.dashboard_no_budget_alerts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.budgetAlerts.forEach { alert ->
                BudgetAlertCard(
                    alert = alert,
                    currencyCode = state.currencyCode
                )
            }
        }
    }
}

@Composable
private fun BudgetAlertCard(alert: BudgetAlertUi, currencyCode: String) {
    val isExceeded = alert.overspentAmount > 0L
    val color = if (isExceeded) ErrorRed else WarningOrange

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.budgetName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (isExceeded) {
                        stringResource(
                            R.string.dashboard_budget_exceeded_by,
                            alert.overspentAmount.toCurrencyString(currencyCode)
                        )
                    } else {
                        stringResource(
                            R.string.dashboard_budget_threshold_reached_with_percent,
                            alert.alertThresholdPercent
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${alert.percentUsed}%",
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun AccountsSection(state: DashboardUiState, onManageClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dashboard_accounts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onManageClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(stringResource(R.string.dashboard_manage_accounts), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (state.accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_no_accounts),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.dashboard_create_account_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onManageClick,
                        colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.dashboard_manage_accounts), fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.accounts
                    .sortedByDescending { it.currentBalanceCents }
                    .take(3)
                    .forEach { account ->
                        AccountCard(
                            account = account,
                            style = state.accountCardStyleOverrides[account.id] ?: state.accountCardStyle,
                            maskAmounts = state.areBalancesHidden,
                            onClick = onManageClick
                        )
                    }

                if (state.accounts.size > 3) {
                    TextButton(onClick = onManageClick) {
                        Text(
                            stringResource(R.string.dashboard_view_all_accounts),
                            color = YellowPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

