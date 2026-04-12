package com.chelumore.xpenze.feature.budgets.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.core.ui.util.parseHexColorOrFallback
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToAddBudget: (String) -> Unit,
    viewModel: BudgetDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(budgetId) {
        viewModel.load(budgetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_budget), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(budgetId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = ErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = YellowPrimary)
            }
            return@Scaffold
        }

        val budget = uiState.budget
        if (budget == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error ?: stringResource(R.string.error_budget_not_found))
            }
            return@Scaffold
        }

        val threshold = (budget.alertThresholdPercent / 100f).coerceIn(0f, 1f)
        val progressColor = when {
            uiState.progress >= 1f -> ErrorRed
            uiState.progress >= threshold -> WarningOrange
            else -> SuccessGreen
        }
        val historyOutcomeText = if (uiState.remainingCents >= 0L) {
            stringResource(
                R.string.budget_detail_history_result_under,
                uiState.remainingCents.toCurrencyString()
            )
        } else {
            stringResource(
                R.string.budget_detail_history_result_over,
                (-uiState.remainingCents).toCurrencyString()
            )
        }
        val historyOutcomeColor = if (uiState.remainingCents >= 0L) SuccessGreen else ErrorRed

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        budget.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${uiState.categoryName} - ${uiState.accountName}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.timeframeLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = uiState.statusLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    uiState.nextTimeframeLabel?.takeIf { budget.isRecurring }?.let { nextTimeframe ->
                        Text(
                            text = stringResource(R.string.budget_detail_next_cycle, nextTimeframe),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            BudgetSnapshotSection(
                isPast = uiState.isPast,
                spentCents = uiState.spentCents,
                limitCents = budget.amountLimitCents,
                remainingCents = uiState.remainingCents,
                transactionCount = uiState.transactionCount,
                averageSpendCents = uiState.averageSpendCents,
                largestExpenseTitle = uiState.largestExpenseTitle,
                largestExpenseCents = uiState.largestExpenseCents,
                progress = uiState.progress,
                progressColor = progressColor,
                historyOutcomeText = historyOutcomeText,
                historyOutcomeColor = historyOutcomeColor
            )

            if (uiState.breakdown.isNotEmpty()) {
                BudgetBreakdownSection(uiState.breakdown)
            }

            if (uiState.canCreateNextBudget) {
                Button(
                    onClick = { onNavigateToAddBudget(budget.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.budget_detail_create_next), fontWeight = FontWeight.Bold)
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (uiState.isPast) {
                            stringResource(R.string.budget_detail_transactions_period)
                        } else {
                            stringResource(R.string.common_transactions)
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.transactionCount > 0) {
                        Text(
                            text = stringResource(R.string.budget_detail_transaction_count, uiState.transactionCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (uiState.transactions.isEmpty()) {
                        Text(
                            if (uiState.isPast) {
                                stringResource(R.string.budget_detail_no_transactions_period)
                            } else {
                                stringResource(R.string.budget_detail_no_transactions)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.transactions.forEach { tx ->
                            BudgetTransactionItem(tx)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.budget_editor_delete_title)) },
            text = { Text(stringResource(R.string.budget_editor_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete(onDeleted = onNavigateBack)
                    }
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = ErrorRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun BudgetSnapshotSection(
    isPast: Boolean,
    spentCents: Long,
    limitCents: Long,
    remainingCents: Long,
    transactionCount: Int,
    averageSpendCents: Long,
    largestExpenseTitle: String?,
    largestExpenseCents: Long,
    progress: Float,
    progressColor: Color,
    historyOutcomeText: String,
    historyOutcomeColor: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.budget_detail_snapshot_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isPast) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BudgetSnapshotCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.common_spent),
                    value = spentCents.toCurrencyString(),
                    accentColor = ErrorRed,
                    backgroundColor = Color(0xFF1E1010)
                )
                BudgetSnapshotCard(
                    modifier = Modifier.weight(1f),
                    label = if (isPast) {
                        stringResource(R.string.common_limit)
                    } else {
                        stringResource(R.string.common_remaining)
                    },
                    value = if (isPast) {
                        limitCents.toCurrencyString()
                    } else {
                        remainingCents.toCurrencyString()
                    },
                    accentColor = if (isPast) YellowPrimary else progressColor,
                    backgroundColor = Color(0xFF1E1C10)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BudgetSnapshotCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.budget_detail_average_spend),
                    value = averageSpendCents.toCurrencyString(),
                    accentColor = WarningOrange,
                    backgroundColor = Color(0xFF1E1500)
                )
                BudgetSnapshotCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.common_transactions),
                    value = transactionCount.toString(),
                    accentColor = SuccessGreen,
                    backgroundColor = Color(0xFF0F1E15)
                )
            }
            if (isPast) {
                Text(
                    text = historyOutcomeText,
                    color = historyOutcomeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            largestExpenseTitle?.takeIf { largestExpenseCents > 0L }?.let { title ->
                BudgetInfoRow(
                    label = stringResource(R.string.budget_detail_largest_expense),
                    value = "$title - ${largestExpenseCents.toCurrencyString()}"
                )
            }
        }
    }
}

@Composable
private fun BudgetSnapshotCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    backgroundColor: Color
) {
    val palette = rememberBudgetSnapshotPalette(
        accentColor = accentColor,
        darkContainerColor = backgroundColor
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.containerColor,
            contentColor = palette.valueColor
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.labelColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = palette.valueColor
            )
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 4.dp)
                    .background(accentColor, RoundedCornerShape(100))
            )
        }
    }
}

@Composable
private fun BudgetInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BudgetBreakdownSection(items: List<BudgetBreakdownItemUi>) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.budget_detail_spend_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            items.forEach { item ->
                BudgetBreakdownItem(item)
            }
        }
    }
}

@Composable
private fun BudgetBreakdownItem(item: BudgetBreakdownItemUi) {
    val accentColor = parseHexColorOrFallback(item.colorHex, YellowPrimary)
    val sharePercentText = stringResource(
        R.string.budget_detail_spend_share,
        (item.sharePercent * 100f).roundToInt()
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForName(item.iconName),
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.subtitle} - ${stringResource(R.string.budget_detail_breakdown_transactions, item.transactionCount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = item.totalAmountCents.toCurrencyString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            LinearProgressIndicator(
                progress = { item.sharePercent.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.background
            )
            Text(
                text = sharePercentText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetTransactionItem(tx: BudgetDetailTransactionUi) {
    val accentColor = parseHexColorOrFallback(tx.categoryColorHex, YellowPrimary)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accentColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForName(tx.categoryIconName),
                    contentDescription = null,
                    tint = accentColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tx.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = tx.dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.isRecurring || tx.hasReceipt) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tx.isRecurring) {
                            BudgetHistoryBadge(
                                label = stringResource(R.string.transactions_badge_recurring),
                                icon = Icons.Outlined.Repeat,
                                backgroundColor = WarningOrange.copy(alpha = 0.18f),
                                contentColor = WarningOrange
                            )
                        }
                        if (tx.hasReceipt) {
                            BudgetHistoryBadge(
                                label = stringResource(R.string.transactions_badge_receipt),
                                icon = Icons.Outlined.Receipt,
                                backgroundColor = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "-${tx.amountCents.toCurrencyString(tx.currency)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
        }
    }
}

@Composable
private fun BudgetHistoryBadge(
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

@Composable
private fun rememberBudgetSnapshotPalette(
    accentColor: Color,
    darkContainerColor: Color
): BudgetSnapshotPalette {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val useDarkPalette = surface.luminance() < 0.5f
    return remember(accentColor, darkContainerColor, onSurface, onSurfaceVariant, useDarkPalette) {
        if (useDarkPalette) {
            BudgetSnapshotPalette(
                containerColor = darkContainerColor,
                valueColor = Color.White,
                labelColor = Color.White.copy(alpha = 0.72f)
            )
        } else {
            BudgetSnapshotPalette(
                containerColor = accentColor.copy(alpha = 0.12f),
                valueColor = onSurface,
                labelColor = onSurfaceVariant
            )
        }
    }
}

private data class BudgetSnapshotPalette(
    val containerColor: Color,
    val valueColor: Color,
    val labelColor: Color
)
