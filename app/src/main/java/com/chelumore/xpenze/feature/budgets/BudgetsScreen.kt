package com.chelumore.xpenze.feature.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.ui.components.ads.AdPlacement
import com.chelumore.xpenze.core.ui.components.ads.FreeTierAdSlot
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = hiltViewModel(),
    onNavigateToAddBudget: () -> Unit = {},
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
                    // Removed redundant Add button here as per mockup and to avoid duplication with FAB
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
                onClick = onNavigateToAddBudget,
                containerColor = YellowPrimary,
                contentColor = BlackBackground,
                shape = RoundedCornerShape(100) // Pill shape
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
                item { BudgetSummaryChips(uiState) }

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

                if (uiState.budgets.isEmpty()) {
                    item {
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
                                    text = stringResource(R.string.budgets_empty_filtered),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.budgets, key = { it.id }) { budget ->
                        BudgetCard(
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
private fun BudgetSummaryChips(state: BudgetsUiState) {
    // Use LazyRow to allow horizontal scrolling if needed
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SummaryChip(
                label = stringResource(R.string.budgets_summary_on_track, state.activeCount),
                color = SuccessGreen,
                backgroundColor = Color(0xFF0F1E15) // Dark Green tint
            )
        }
        item {
            SummaryChip(
                label = stringResource(R.string.budgets_summary_warning, state.warningCount),
                color = WarningOrange,
                backgroundColor = Color(0xFF1E1500) // Dark Orange tint
            )
        }
        item {
            SummaryChip(
                label = stringResource(R.string.budgets_summary_exceeded, state.exceededCount),
                color = ErrorRed,
                backgroundColor = Color(0xFF1E1010) // Dark Red tint
            )
        }
    }
}

@Composable
private fun SummaryChip(label: String, color: Color, backgroundColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100)) // Fully rounded
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Replaced generic add icon with a small dot or status indicator
             Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
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
        // Status Chips - Scrollable
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item { FilterChipItem(allText, state.selectedFilter == BudgetFilter.ALL) { onStatusSelected(BudgetFilter.ALL) } }
            item { FilterChipItem(stringResource(R.string.common_on_track), state.selectedFilter == BudgetFilter.ON_TRACK) { onStatusSelected(BudgetFilter.ON_TRACK) } }
            item { FilterChipItem(stringResource(R.string.common_warning), state.selectedFilter == BudgetFilter.WARNING) { onStatusSelected(BudgetFilter.WARNING) } }
            item { FilterChipItem(stringResource(R.string.common_exceeded), state.selectedFilter == BudgetFilter.EXCEEDED) { onStatusSelected(BudgetFilter.EXCEEDED) } }
        }
        
        // Dropdowns - Scrollable
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                                Text("✓", color = YellowPrimary, fontWeight = FontWeight.Bold)
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
private fun BudgetCard(budget: BudgetUiModel, onClick: () -> Unit) {
    val scopeTags = remember(
        budget.categoryId,
        budget.accountId,
        budget.categoryName,
        budget.accountName
    ) {
        buildList {
            if (budget.categoryId != null) add(budget.categoryName)
            if (budget.accountId != null) add(budget.accountName)
        }
    }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = budget.currentPeriodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (scopeTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        scopeTags.forEach { label ->
                            TagChip(label = label)
                        }
                    }
                }
            }

            // Progress Section
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
                        color = when(budget.status) {
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
                    color = when(budget.status) {
                        BudgetStatus.ON_TRACK -> SuccessGreen
                        BudgetStatus.WARNING -> WarningOrange
                        BudgetStatus.EXCEEDED -> ErrorRed
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${budget.spentAmount.toCurrencyString()} of ${budget.limitAmount.toCurrencyString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
