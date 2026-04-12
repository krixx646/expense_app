package com.chelumore.xpenze.feature.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.ui.components.ads.AdPlacement
import com.chelumore.xpenze.core.ui.components.ads.FreeTierAdSlot
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.core.ui.util.parseHexColorOrFallback
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = hiltViewModel(),
    onNavigateToAddBudget: (String?) -> Unit = {},
    onNavigateToBudgetDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val budgetsTitle = stringResource(R.string.main_nav_budgets)
    val sortText = stringResource(R.string.common_sort)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(budgetsTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = sortText)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.budgets_sort_usage_desc)) },
                                onClick = {
                                    viewModel.setSort(BudgetSortOption.USAGE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.budgets_sort_remaining_asc)) },
                                onClick = {
                                    viewModel.setSort(BudgetSortOption.REMAINING_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.budgets_sort_name)) },
                                onClick = {
                                    viewModel.setSort(BudgetSortOption.NAME)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddBudget(null) },
                containerColor = YellowPrimary,
                contentColor = BlackBackground,
                shape = RoundedCornerShape(100)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.budgets_add_budget))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
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
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 220.dp)
            ) {
                item { BudgetOverviewCard(uiState) }

                item {
                    BudgetFilters(
                        state = uiState,
                        onStatusSelected = viewModel::setFilter,
                        onPeriodSelected = viewModel::setPeriodFilter,
                        onAccountSelected = viewModel::setAccountFilter,
                        onCategorySelected = viewModel::setCategoryFilter
                    )
                }

                item {
                    FreeTierAdSlot(
                        hasPremiumAccess = uiState.hasPremiumAccess,
                        placement = AdPlacement.BUDGETS
                    )
                }

                item {
                    BudgetSectionHeader(
                        title = stringResource(R.string.budgets_current_section_title, uiState.currentBudgets.size),
                        subtitle = stringResource(R.string.budgets_current_section_body)
                    )
                }

                if (uiState.expiredPrompt != null) {
                    item {
                        ExpiredBudgetPromptCard(
                            prompt = uiState.expiredPrompt,
                            onCreateNextBudget = { onNavigateToAddBudget(uiState.expiredPrompt?.budgetId) }
                        )
                    }
                }

                if (uiState.currentBudgets.isEmpty()) {
                    item {
                        BudgetEmptyState(stringResource(R.string.budgets_empty_current))
                    }
                } else {
                    items(uiState.currentBudgets, key = { it.id }) { budget ->
                        CurrentBudgetCard(
                            budget = budget,
                            onClick = { onNavigateToBudgetDetail(budget.id) }
                        )
                    }
                }

                item {
                    BudgetSectionHeader(
                        title = stringResource(R.string.budgets_past_section_title, uiState.pastBudgets.size),
                        subtitle = stringResource(R.string.budgets_past_section_body)
                    )
                }

                if (uiState.pastBudgets.isEmpty()) {
                    item {
                        BudgetEmptyState(stringResource(R.string.budgets_empty_past))
                    }
                } else {
                    items(uiState.pastBudgets, key = { it.id }) { budget ->
                        PastBudgetCard(
                            budget = budget,
                            onClick = { onNavigateToBudgetDetail(budget.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetEmptyState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpiredBudgetPromptCard(
    prompt: ExpiredBudgetPromptUi?,
    onCreateNextBudget: () -> Unit
) {
    if (prompt == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = YellowPrimary.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.budgets_prompt_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.budgets_prompt_body,
                    prompt.budgetName,
                    prompt.timeframeLabel
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${prompt.spentAmount.toCurrencyString()} / ${prompt.limitAmount.toCurrencyString()}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onCreateNextBudget,
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = BlackBackground
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.budgets_prompt_action), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BudgetOverviewCard(state: BudgetsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.budgets_overview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.budgets_overview_body,
                        state.currentCount,
                        state.pastCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BudgetOverviewMetric(
                    modifier = Modifier.weight(1f),
                    value = state.activeCount.toString(),
                    label = stringResource(R.string.common_on_track),
                    accentColor = SuccessGreen,
                    backgroundColor = Color(0xFF0F1E15)
                )
                BudgetOverviewMetric(
                    modifier = Modifier.weight(1f),
                    value = state.warningCount.toString(),
                    label = stringResource(R.string.common_warning),
                    accentColor = WarningOrange,
                    backgroundColor = Color(0xFF1E1500)
                )
                BudgetOverviewMetric(
                    modifier = Modifier.weight(1f),
                    value = state.exceededCount.toString(),
                    label = stringResource(R.string.common_exceeded),
                    accentColor = ErrorRed,
                    backgroundColor = Color(0xFF1E1010)
                )
            }
        }
    }
}

@Composable
private fun BudgetOverviewMetric(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accentColor: Color,
    backgroundColor: Color
) {
    val palette = rememberBudgetOverviewMetricPalette(
        accentColor = accentColor,
        darkContainerColor = backgroundColor
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.containerColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BudgetFilters(
    state: BudgetsUiState,
    onStatusSelected: (BudgetFilter) -> Unit,
    onPeriodSelected: (BudgetPeriod?) -> Unit,
    onAccountSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit
) {
    val allText = stringResource(R.string.common_all)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item { FilterChipItem(allText, state.selectedFilter == BudgetFilter.ALL) { onStatusSelected(BudgetFilter.ALL) } }
            item { FilterChipItem(stringResource(R.string.common_on_track), state.selectedFilter == BudgetFilter.ON_TRACK) { onStatusSelected(BudgetFilter.ON_TRACK) } }
            item { FilterChipItem(stringResource(R.string.common_warning), state.selectedFilter == BudgetFilter.WARNING) { onStatusSelected(BudgetFilter.WARNING) } }
            item { FilterChipItem(stringResource(R.string.common_exceeded), state.selectedFilter == BudgetFilter.EXCEEDED) { onStatusSelected(BudgetFilter.EXCEEDED) } }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val selected = state.selectedPeriod?.let { localizedBudgetPeriodLabel(it) } ?: allText
                val options = buildList {
                    add("" to allText)
                    addAll(BudgetPeriod.entries.map { it.name to localizedBudgetPeriodLabel(it) })
                }
                DropdownFilterButton(
                    label = stringResource(R.string.common_period),
                    value = selected,
                    options = options,
                    selectedId = state.selectedPeriod?.name ?: "",
                    onSelected = { id -> onPeriodSelected(id.takeIf { it.isNotBlank() }?.let { BudgetPeriod.valueOf(it) }) }
                )
            }
            item {
                val selectedAccount = state.selectedAccountId?.let { id -> state.accounts.firstOrNull { it.id == id }?.name } ?: allText
                val options = buildList {
                    add("" to allText)
                    addAll(state.accounts.map { it.id to it.name })
                }
                DropdownFilterButton(
                    label = stringResource(R.string.common_account),
                    value = selectedAccount,
                    options = options,
                    selectedId = state.selectedAccountId ?: "",
                    onSelected = { id -> onAccountSelected(id.takeIf { it.isNotBlank() }) }
                )
            }
            item {
                val selectedCategory = state.selectedCategoryId?.let { id -> state.categories.firstOrNull { it.id == id }?.name } ?: allText
                val options = buildList {
                    add("" to allText)
                    addAll(state.categories.map { it.id to it.name })
                }
                DropdownFilterButton(
                    label = stringResource(R.string.common_category),
                    value = selectedCategory,
                    options = options,
                    selectedId = state.selectedCategoryId ?: "",
                    onSelected = { id -> onCategorySelected(id.takeIf { it.isNotBlank() }) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = YellowPrimary,
            selectedLabelColor = BlackBackground,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun DropdownFilterButton(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 8.dp)
        ) {
            options.forEach { (id, title) ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title)
                            if (id == selectedId) {
                                Text("?", color = YellowPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CurrentBudgetCard(budget: BudgetUiModel, onClick: () -> Unit) {
    val scopeTags = remember(
        budget.categoryId,
        budget.accountId,
        budget.categoryName,
        budget.accountName,
        budget.isRecurring
    ) {
        buildBudgetScopeTags(budget)
    }
    val accentColor = parseHexColorOrFallback(budget.categoryColorHex, YellowPrimary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                BudgetLeadingVisual(
                    iconName = budget.categoryIconName,
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = budget.timeframeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = budget.metaLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (scopeTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            scopeTags.forEach { label ->
                                TagChip(label = label)
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.common_spent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(budget.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (budget.status) {
                            BudgetStatus.ON_TRACK -> SuccessGreen
                            BudgetStatus.WARNING -> WarningOrange
                            BudgetStatus.EXCEEDED -> ErrorRed
                        }
                    )
                }

                LinearProgressIndicator(
                    progress = { budget.progress.coerceAtMost(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (budget.status) {
                        BudgetStatus.ON_TRACK -> SuccessGreen
                        BudgetStatus.WARNING -> WarningOrange
                        BudgetStatus.EXCEEDED -> ErrorRed
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${budget.spentAmount.toCurrencyString()} of ${budget.limitAmount.toCurrencyString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.budgets_past_transactions_count, budget.transactionCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val remainingLabel = if (budget.remainingAmount >= 0L) {
                        stringResource(R.string.budgets_remaining_left, budget.remainingAmount.toCurrencyString())
                    } else {
                        stringResource(R.string.budgets_remaining_over, (-budget.remainingAmount).toCurrencyString())
                    }
                    Text(
                        text = if (budget.daysLeft > 0) {
                            stringResource(R.string.budgets_remaining_with_days, remainingLabel, budget.daysLeft)
                        } else {
                            remainingLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PastBudgetCard(budget: BudgetUiModel, onClick: () -> Unit) {
    val scopeTags = remember(
        budget.categoryId,
        budget.accountId,
        budget.categoryName,
        budget.accountName,
        budget.isRecurring
    ) {
        buildBudgetScopeTags(budget)
    }
    val historyResultText = if (budget.remainingAmount >= 0L) {
        stringResource(R.string.budgets_past_under_by, budget.remainingAmount.toCurrencyString())
    } else {
        stringResource(R.string.budgets_past_over_by, (-budget.remainingAmount).toCurrencyString())
    }
    val historyResultColor = if (budget.remainingAmount >= 0L) SuccessGreen else ErrorRed
    val accentColor = parseHexColorOrFallback(budget.categoryColorHex, YellowPrimary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                BudgetLeadingVisual(
                    iconName = budget.categoryIconName,
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = budget.timeframeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = budget.metaLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (scopeTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            scopeTags.forEach { label ->
                                TagChip(label = label)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(historyResultColor.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (budget.remainingAmount >= 0L) {
                                stringResource(R.string.common_on_track)
                            } else {
                                stringResource(R.string.common_exceeded)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = historyResultColor
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BudgetHistoryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.common_spent),
                    value = budget.spentAmount.toCurrencyString(),
                    accentColor = ErrorRed,
                    backgroundColor = Color(0xFF1E1010)
                )
                BudgetHistoryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.common_limit),
                    value = budget.limitAmount.toCurrencyString(),
                    accentColor = YellowPrimary,
                    backgroundColor = Color(0xFF1E1C10)
                )
                BudgetHistoryStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.common_transactions),
                    value = budget.transactionCount.toString(),
                    accentColor = accentColor,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Text(
                text = historyResultText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = historyResultColor
            )
            Text(
                text = stringResource(R.string.budgets_past_view_history),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BudgetHistoryStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    backgroundColor: Color
) {
    val palette = rememberBudgetStatPalette(
        accentColor = accentColor,
        darkContainerColor = backgroundColor
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = palette.containerColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 3.dp)
                    .background(accentColor, RoundedCornerShape(100))
            )
        }
    }
}

@Composable
private fun BudgetLeadingVisual(
    iconName: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconForName(iconName),
            contentDescription = null,
            tint = accentColor
        )
    }
}

private fun buildBudgetScopeTags(budget: BudgetUiModel): List<String> {
    return buildList {
        if (budget.isRecurring) add("Recurring")
        if (budget.categoryId != null) add(budget.categoryName)
        if (budget.accountId != null) add(budget.accountName)
    }
}

@Composable
private fun localizedBudgetPeriodLabel(period: BudgetPeriod): String {
    return when (period) {
        BudgetPeriod.DAILY -> stringResource(R.string.budget_period_daily)
        BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
        BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
        BudgetPeriod.YEARLY -> stringResource(R.string.budget_period_yearly)
        BudgetPeriod.CUSTOM -> stringResource(R.string.budget_period_custom)
    }
}

@Composable
private fun TagChip(label: String) {
    Box(
        modifier = Modifier
            .widthIn(max = 120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun rememberBudgetOverviewMetricPalette(
    accentColor: Color,
    darkContainerColor: Color
): BudgetMetricPalette {
    val surface = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val useDarkPalette = surface.luminance() < 0.5f
    return remember(accentColor, darkContainerColor, onSurfaceVariant, useDarkPalette) {
        if (useDarkPalette) {
            BudgetMetricPalette(
                containerColor = darkContainerColor,
                valueColor = accentColor,
                labelColor = Color.White.copy(alpha = 0.72f)
            )
        } else {
            BudgetMetricPalette(
                containerColor = accentColor.copy(alpha = 0.14f),
                valueColor = accentColor,
                labelColor = onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberBudgetStatPalette(
    accentColor: Color,
    darkContainerColor: Color
): BudgetMetricPalette {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val useDarkPalette = surface.luminance() < 0.5f
    return remember(accentColor, darkContainerColor, onSurface, onSurfaceVariant, useDarkPalette) {
        if (useDarkPalette) {
            BudgetMetricPalette(
                containerColor = darkContainerColor,
                valueColor = Color.White,
                labelColor = Color.White.copy(alpha = 0.72f)
            )
        } else {
            BudgetMetricPalette(
                containerColor = accentColor.copy(alpha = 0.12f),
                valueColor = onSurface,
                labelColor = onSurfaceVariant
            )
        }
    }
}

private data class BudgetMetricPalette(
    val containerColor: Color,
    val valueColor: Color,
    val labelColor: Color
)
