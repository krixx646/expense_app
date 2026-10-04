package com.chelumore.xpenze.feature.budgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import kotlin.math.roundToInt

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = hiltViewModel(),
    onNavigateToAddBudget: (String?) -> Unit = {},
    onNavigateToBudgetDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BudgetsContent(
        uiState = uiState,
        onSortSelected = viewModel::setSort,
        onStatusSelected = viewModel::setFilter,
        onPeriodSelected = viewModel::setPeriodFilter,
        onAccountSelected = viewModel::setAccountFilter,
        onCategorySelected = viewModel::setCategoryFilter,
        onNavigateToAddBudget = onNavigateToAddBudget,
        onNavigateToBudgetDetail = onNavigateToBudgetDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetsContent(
    uiState: BudgetsUiState,
    onSortSelected: (BudgetSortOption) -> Unit,
    onStatusSelected: (BudgetFilter) -> Unit,
    onPeriodSelected: (BudgetPeriod?) -> Unit,
    onAccountSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onNavigateToAddBudget: (String?) -> Unit,
    onNavigateToBudgetDetail: (String) -> Unit
) {
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
                                    onSortSelected(BudgetSortOption.USAGE_DESC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.budgets_sort_remaining_asc)) },
                                onClick = {
                                    onSortSelected(BudgetSortOption.REMAINING_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.budgets_sort_name)) },
                                onClick = {
                                    onSortSelected(BudgetSortOption.NAME)
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
                contentPadding = PaddingValues(top = 12.dp, bottom = 220.dp)
            ) {
                item {
                    BudgetHeroCard(
                        state = uiState,
                        onStatusSelected = onStatusSelected
                    )
                }

                item {
                    BudgetFilters(
                        state = uiState,
                        onStatusSelected = onStatusSelected,
                        onPeriodSelected = onPeriodSelected,
                        onAccountSelected = onAccountSelected,
                        onCategorySelected = onCategorySelected
                    )
                }

                item {
                    FreeTierAdSlot(
                        hasPremiumAccess = uiState.hasPremiumAccess,
                        placement = AdPlacement.BUDGETS
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

                item {
                    BudgetSectionHeader(
                        title = stringResource(R.string.budgets_current_section_title, uiState.currentBudgets.size),
                        subtitle = stringResource(R.string.budgets_current_section_body)
                    )
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
private fun BudgetHeroCard(
    state: BudgetsUiState,
    onStatusSelected: (BudgetFilter) -> Unit
) {
    val accent = statusColorFor(state.totalProgress)
    val heroBrush = Brush.verticalGradient(
        listOf(
            YellowPrimary.copy(alpha = if (isDarkSurface()) 0.14f else 0.22f),
            MaterialTheme.colorScheme.surface
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(heroBrush)
            .border(1.dp, hairlineColor(), RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.budgets_overview_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.currentCount == 0) {
                    Text(
                        text = stringResource(R.string.budgets_empty_current),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = state.totalSpentAmount.toCurrencyString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.budgets_overview_of_limit,
                            state.totalLimitAmount.toCurrencyString()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            BudgetUsageRing(
                progress = state.totalProgress,
                accentColor = accent
            )
        }

        if (state.currentCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusSummaryPill(
                    modifier = Modifier.weight(1f),
                    count = state.activeCount,
                    label = stringResource(R.string.common_on_track),
                    color = SuccessGreen,
                    selected = state.selectedFilter == BudgetFilter.ON_TRACK,
                    onClick = { onStatusSelected(toggledFilter(state.selectedFilter, BudgetFilter.ON_TRACK)) }
                )
                StatusSummaryPill(
                    modifier = Modifier.weight(1f),
                    count = state.warningCount,
                    label = stringResource(R.string.common_warning),
                    color = WarningOrange,
                    selected = state.selectedFilter == BudgetFilter.WARNING,
                    onClick = { onStatusSelected(toggledFilter(state.selectedFilter, BudgetFilter.WARNING)) }
                )
                StatusSummaryPill(
                    modifier = Modifier.weight(1f),
                    count = state.exceededCount,
                    label = stringResource(R.string.common_exceeded),
                    color = ErrorRed,
                    selected = state.selectedFilter == BudgetFilter.EXCEEDED,
                    onClick = { onStatusSelected(toggledFilter(state.selectedFilter, BudgetFilter.EXCEEDED)) }
                )
            }
        }
    }
}

private fun toggledFilter(current: BudgetFilter, tapped: BudgetFilter): BudgetFilter {
    return if (current == tapped) BudgetFilter.ALL else tapped
}

@Composable
private fun BudgetUsageRing(
    progress: Float,
    accentColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "budgetUsageRing"
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.budgets_overview_used),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusSummaryPill(
    modifier: Modifier = Modifier,
    count: Int,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = if (selected) 0.24f else 0.10f))
            .border(1.dp, if (selected) color else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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
private fun BudgetFilters(
    state: BudgetsUiState,
    onStatusSelected: (BudgetFilter) -> Unit,
    onPeriodSelected: (BudgetPeriod?) -> Unit,
    onAccountSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit
) {
    val allText = stringResource(R.string.common_all)
    val hasActiveFilter = state.selectedFilter != BudgetFilter.ALL ||
        state.selectedPeriod != null ||
        state.selectedAccountId != null ||
        state.selectedCategoryId != null

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            val options = buildList {
                add("" to allText)
                addAll(BudgetPeriod.entries.map { it.name to localizedBudgetPeriodLabel(it) })
            }
            DropdownFilterButton(
                label = stringResource(R.string.common_period),
                value = state.selectedPeriod?.let { localizedBudgetPeriodLabel(it) },
                options = options,
                selectedId = state.selectedPeriod?.name ?: "",
                onSelected = { id -> onPeriodSelected(id.takeIf { it.isNotBlank() }?.let { BudgetPeriod.valueOf(it) }) }
            )
        }
        item {
            val options = buildList {
                add("" to allText)
                addAll(state.accounts.map { it.id to it.name })
            }
            DropdownFilterButton(
                label = stringResource(R.string.common_account),
                value = state.selectedAccountId?.let { id -> state.accounts.firstOrNull { it.id == id }?.name },
                options = options,
                selectedId = state.selectedAccountId ?: "",
                onSelected = { id -> onAccountSelected(id.takeIf { it.isNotBlank() }) }
            )
        }
        item {
            val options = buildList {
                add("" to allText)
                addAll(state.categories.map { it.id to it.name })
            }
            DropdownFilterButton(
                label = stringResource(R.string.common_category),
                value = state.selectedCategoryId?.let { id -> state.categories.firstOrNull { it.id == id }?.name },
                options = options,
                selectedId = state.selectedCategoryId ?: "",
                onSelected = { id -> onCategorySelected(id.takeIf { it.isNotBlank() }) }
            )
        }
        if (hasActiveFilter) {
            item {
                ClearFiltersChip(
                    onClick = {
                        onStatusSelected(BudgetFilter.ALL)
                        onPeriodSelected(null)
                        onAccountSelected(null)
                        onCategorySelected(null)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClearFiltersChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.common_clear),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DropdownFilterButton(
    label: String,
    value: String?,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = value != null
    val shape = RoundedCornerShape(20.dp)
    val containerColor = if (isActive) {
        YellowPrimary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(containerColor)
                .border(1.dp, if (isActive) YellowPrimary.copy(alpha = 0.6f) else Color.Transparent, shape)
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value ?: label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
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
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = YellowPrimary
                                )
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
private fun BudgetSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 16.dp)
                    .clip(RoundedCornerShape(100))
                    .background(YellowPrimary)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 11.dp)
        )
    }
}

@Composable
private fun BudgetEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, hairlineColor(), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpiredBudgetPromptCard(
    prompt: ExpiredBudgetPromptUi?,
    onCreateNextBudget: () -> Unit
) {
    if (prompt == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(YellowPrimary.copy(alpha = 0.12f))
            .border(1.dp, YellowPrimary.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.budgets_prompt_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(
                R.string.budgets_prompt_body,
                prompt.budgetName,
                prompt.timeframeLabel
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(
                R.string.budgets_spent_of_limit,
                prompt.spentAmount.toCurrencyString(),
                prompt.limitAmount.toCurrencyString()
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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

@Composable
private fun CurrentBudgetCard(budget: BudgetUiModel, onClick: () -> Unit) {
    val accentColor = parseHexColorOrFallback(budget.categoryColorHex, YellowPrimary)
    val statusColor = statusColorFor(budget.status)
    val remainingLabel = if (budget.remainingAmount >= 0L) {
        stringResource(R.string.budgets_remaining_left, budget.remainingAmount.toCurrencyString())
    } else {
        stringResource(R.string.budgets_remaining_over, (-budget.remainingAmount).toCurrencyString())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, hairlineColor())
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = budget.metaLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    label = statusLabel(budget.status),
                    color = statusColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = remainingLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.budgets_spent_of_limit,
                            budget.spentAmount.toCurrencyString(),
                            budget.limitAmount.toCurrencyString()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(budget.progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            BudgetProgressBar(
                progress = budget.progress,
                color = statusColor
            )

            BudgetMetaChips(budget = budget)
        }
    }
}

@Composable
private fun BudgetProgressBar(progress: Float, color: Color, height: Dp = 10.dp) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550),
        label = "budgetProgressBar"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(100))
                .background(
                    Brush.horizontalGradient(
                        listOf(color.copy(alpha = 0.65f), color)
                    )
                )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetMetaChips(budget: BudgetUiModel) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (budget.daysLeft > 0) {
            TagChip(
                label = stringResource(R.string.budgets_days_left_short, budget.daysLeft),
                highlighted = true
            )
        }
        TagChip(label = budget.timeframeLabel)
        if (budget.isRecurring) {
            TagChip(label = stringResource(R.string.common_recurring))
        }
        if (budget.categoryId != null) {
            TagChip(label = budget.categoryName)
        }
        if (budget.accountId != null) {
            TagChip(label = budget.accountName)
        }
        TagChip(label = stringResource(R.string.budgets_past_transactions_count, budget.transactionCount))
    }
}

@Composable
private fun PastBudgetCard(budget: BudgetUiModel, onClick: () -> Unit) {
    val finishedUnderBudget = budget.remainingAmount >= 0L
    val historyResultText = if (finishedUnderBudget) {
        stringResource(R.string.budgets_past_under_by, budget.remainingAmount.toCurrencyString())
    } else {
        stringResource(R.string.budgets_past_over_by, (-budget.remainingAmount).toCurrencyString())
    }
    val historyResultColor = if (finishedUnderBudget) SuccessGreen else ErrorRed
    val accentColor = parseHexColorOrFallback(budget.categoryColorHex, YellowPrimary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, hairlineColor())
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.budgets_past_meta,
                            budget.timeframeLabel,
                            budget.transactionCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.budgets_past_view_history),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        R.string.budgets_spent_of_limit,
                        budget.spentAmount.toCurrencyString(),
                        budget.limitAmount.toCurrencyString()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BudgetProgressBar(
                    progress = budget.progress,
                    color = historyResultColor,
                    height = 6.dp
                )
                Text(
                    text = historyResultText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = historyResultColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
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
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconForName(iconName),
            contentDescription = null,
            tint = accentColor
        )
    }
}

@Composable
private fun TagChip(label: String, highlighted: Boolean = false) {
    Box(
        modifier = Modifier
            .widthIn(max = 140.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (highlighted) {
                    YellowPrimary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun statusLabel(status: BudgetStatus): String {
    return when (status) {
        BudgetStatus.ON_TRACK -> stringResource(R.string.common_on_track)
        BudgetStatus.WARNING -> stringResource(R.string.common_warning)
        BudgetStatus.EXCEEDED -> stringResource(R.string.common_exceeded)
    }
}

private fun statusColorFor(status: BudgetStatus): Color {
    return when (status) {
        BudgetStatus.ON_TRACK -> SuccessGreen
        BudgetStatus.WARNING -> WarningOrange
        BudgetStatus.EXCEEDED -> ErrorRed
    }
}

private fun statusColorFor(progress: Float): Color {
    return when {
        progress >= 1f -> ErrorRed
        progress >= 0.8f -> WarningOrange
        else -> SuccessGreen
    }
}

@Composable
private fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

@Composable
private fun hairlineColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

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
