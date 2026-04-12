package com.chelumore.xpenze.feature.transactions.list

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.ui.components.ads.AdPlacement
import com.chelumore.xpenze.core.ui.components.ads.FreeTierAdSlot
import com.chelumore.xpenze.core.ui.util.LocalWindowWidthSizeClass
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.feature.transactions.detail.TransactionDetailPane
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel = hiltViewModel(),
    onNavigateToAddTransaction: (String) -> Unit = {},
    onNavigateToEditTransaction: (String) -> Unit = {},
    onNavigateToTransactionDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val shareTransactionChooser = stringResource(R.string.transactions_share_chooser)
    val addTransactionText = stringResource(R.string.transaction_form_title_add)
    val widthSizeClass = LocalWindowWidthSizeClass.current
    val isTwoPane = widthSizeClass != WindowWidthSizeClass.Compact
    var selectedTransactionId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionsViewModel.TransactionsEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is TransactionsViewModel.TransactionsEvent.ShareText -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(intent, shareTransactionChooser).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(chooser) }
                }
            }
        }
    }

    // Keep selection valid on tablets (auto-select most recent when needed).
    LaunchedEffect(isTwoPane, uiState.transactionsByDate) {
        if (!isTwoPane) return@LaunchedEffect
        val all = uiState.transactionsByDate
            .keys
            .sortedDescending()
            .asSequence()
            .flatMap { date -> uiState.transactionsByDate.getValue(date).asSequence() }
            .toList()

        val ids = all.map { it.id }.toSet()
        val first = all.firstOrNull()?.id

        selectedTransactionId = when {
            first == null -> null
            selectedTransactionId == null -> first
            selectedTransactionId !in ids -> first
            else -> selectedTransactionId
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddTransaction("EXPENSE") },
                containerColor = YellowPrimary,
                contentColor = BlackBackground,
                shape = RoundedCornerShape(100)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(addTransactionText)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!isTwoPane) {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 220.dp)
            ) {
                item {
                    HeaderSection(
                        currentSort = uiState.selectedSort,
                        onSortSelected = viewModel::setSort,
                        onClearFilters = viewModel::clearAllFilters
                    )
                }
                item { SummaryCardsSection(uiState) }
                item {
                    FreeTierAdSlot(
                        hasPremiumAccess = uiState.hasPremiumAccess,
                        placement = AdPlacement.TRANSACTIONS
                    )
                }
                item {
                    FiltersSection(
                        state = uiState,
                        onTypeSelected = viewModel::setFilterType,
                        onDateRangeSelected = viewModel::setDateRange,
                        onAccountSelected = viewModel::setAccountFilter,
                        onCategorySelected = viewModel::setCategoryFilter
                    )
                }
                item {
                    SearchBarSection(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::setSearchQuery
                    )
                }
                transactionFeedItems(
                    uiState = uiState,
                    onDelete = viewModel::deleteTransaction,
                    onEdit = onNavigateToEditTransaction,
                    onOpenDetails = onNavigateToTransactionDetail,
                    onDuplicate = viewModel::duplicateTransaction,
                    onShare = viewModel::shareTransaction
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HeaderSection(
                        currentSort = uiState.selectedSort,
                        onSortSelected = viewModel::setSort,
                        onClearFilters = viewModel::clearAllFilters
                    )
                    SummaryCardsSection(uiState)
                    FreeTierAdSlot(
                        hasPremiumAccess = uiState.hasPremiumAccess,
                        placement = AdPlacement.TRANSACTIONS
                    )
                    FiltersSection(
                        state = uiState,
                        onTypeSelected = viewModel::setFilterType,
                        onDateRangeSelected = viewModel::setDateRange,
                        onAccountSelected = viewModel::setAccountFilter,
                        onCategorySelected = viewModel::setCategoryFilter
                    )
                    SearchBarSection(uiState.searchQuery, viewModel::setSearchQuery)
                    TransactionListSection(
                        uiState = uiState,
                        onDelete = viewModel::deleteTransaction,
                        onEdit = onNavigateToEditTransaction,
                        onOpenDetails = { id -> selectedTransactionId = id },
                        onDuplicate = viewModel::duplicateTransaction,
                        onShare = viewModel::shareTransaction
                    )
                }

                Card(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val id = selectedTransactionId
                    if (id.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.transactions_select_transaction),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        TransactionDetailPane(
                            transactionId = id,
                            onNavigateToEdit = onNavigateToEditTransaction,
                            modifier = Modifier.fillMaxSize(),
                            onDeleted = { selectedTransactionId = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    currentSort: TransactionSortOption,
    onSortSelected: (TransactionSortOption) -> Unit,
    onClearFilters: () -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    val transactionsTitle = stringResource(R.string.main_nav_transactions)
    val sortText = stringResource(R.string.common_sort)
    val filterText = stringResource(R.string.common_filter)
    val clearFiltersText = stringResource(R.string.transactions_clear_all_filters)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = transactionsTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = { sortMenuExpanded = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz, // Sort icon replacement
                    contentDescription = sortText,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false }
            ) {
                TransactionSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Text(localizedTransactionSortLabel(option))
                                if (option == currentSort) {
                                    Text("✓", color = YellowPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        onClick = {
                            onSortSelected(option)
                            sortMenuExpanded = false
                        }
                    )
                }
            }

            IconButton(
                onClick = { filterMenuExpanded = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = filterText,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = filterMenuExpanded,
                onDismissRequest = { filterMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(clearFiltersText) },
                    onClick = {
                        onClearFilters()
                        filterMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryCardsSection(state: TransactionsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.dashboard_income),
            amount = state.periodIncome,
            trend = state.incomeTrendPercent,
            color = SuccessGreen,
            backgroundColor = Color(0xFF0F1E15) // Dark Green tint
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.dashboard_expense),
            amount = state.periodExpense,
            trend = state.expenseTrendPercent,
            color = ErrorRed,
            backgroundColor = Color(0xFF1E1010) // Dark Red tint
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.reports_net_short),
            amount = state.periodNet,
            trend = state.netTrendPercent,
            color = YellowPrimary,
            backgroundColor = Color(0xFF1E1C10) // Dark Yellow tint
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    amount: Long,
    trend: Double,
    color: Color,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = Color.White // Force light content on dark backgrounds
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f) // Visible label
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount.toCurrencyString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White // Visible amount
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (trend >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${if (trend >= 0) "↑" else "↓"} ${Math.abs(trend)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun FiltersSection(
    state: TransactionsUiState,
    onTypeSelected: (TransactionFilterType) -> Unit,
    onDateRangeSelected: (TransactionDateRange) -> Unit,
    onAccountSelected: (String?) -> Unit,
    onCategorySelected: (String?) -> Unit
) {
    val allText = stringResource(R.string.common_all)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TypeFilterChip(allText, state.selectedType == TransactionFilterType.ALL) {
            onTypeSelected(TransactionFilterType.ALL)
        }
        TypeFilterChip(stringResource(R.string.transaction_form_type_income), state.selectedType == TransactionFilterType.INCOME) {
            onTypeSelected(TransactionFilterType.INCOME)
        }
        TypeFilterChip(stringResource(R.string.transaction_form_type_expense), state.selectedType == TransactionFilterType.EXPENSE) {
            onTypeSelected(TransactionFilterType.EXPENSE)
        }
        TypeFilterChip(stringResource(R.string.transaction_form_type_transfer), state.selectedType == TransactionFilterType.TRANSFER) {
            onTypeSelected(TransactionFilterType.TRANSFER)
        }
    }
    // Dropdowns row (simplified as buttons for now)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterDropdownButton(
            label = stringResource(R.string.common_date),
            value = localizedTransactionDateRangeLabel(state.selectedDateRange),
            options = TransactionDateRange.entries.map { it.name to localizedTransactionDateRangeLabel(it) },
            selectedId = state.selectedDateRange.name,
            onSelected = { id -> onDateRangeSelected(TransactionDateRange.valueOf(id)) }
        )

        val accountName = state.selectedAccountId?.let { id -> state.accounts.firstOrNull { it.id == id }?.name } ?: allText
        FilterDropdownButton(
            label = stringResource(R.string.transaction_form_account_label),
            value = accountName,
            options = buildList {
                add("" to allText)
                addAll(state.accounts.map { it.id to it.name })
            },
            selectedId = state.selectedAccountId ?: "",
            onSelected = { id -> onAccountSelected(id.takeIf { it.isNotBlank() }) }
        )

        val categoryName = state.selectedCategoryId?.let { id -> state.categories.firstOrNull { it.id == id }?.name } ?: allText
        FilterDropdownButton(
            label = stringResource(R.string.transaction_form_category_label),
            value = categoryName,
            options = buildList {
                add("" to allText)
                addAll(state.categories.map { it.id to it.name })
            },
            selectedId = state.selectedCategoryId ?: "",
            onSelected = { id -> onCategorySelected(id.takeIf { it.isNotBlank() }) }
        )
    }
}

@Composable
private fun TypeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = YellowPrimary,
            selectedLabelColor = Color.Black,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun FilterDropdownButton(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(true) {
                    detectTapGestures(onTap = { expanded = true })
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
                        Text(
                text = "$label: $value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
private fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.transactions_search_placeholder)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_clear))
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

private fun LazyListScope.transactionFeedItems(
    uiState: TransactionsUiState,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onDuplicate: (TransactionItemUi) -> Unit,
    onShare: (TransactionItemUi) -> Unit
) {
    if (uiState.transactionsByDate.isEmpty()) {
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
                        text = stringResource(R.string.transactions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    uiState.transactionsByDate.keys.sortedDescending().forEach { date ->
        val transactions = uiState.transactionsByDate.getValue(date)
        item(key = "header_$date") {
            val dateLabel = when (date) {
                LocalDate.now() -> stringResource(R.string.common_today)
                LocalDate.now().minusDays(1) -> stringResource(R.string.common_yesterday)
                else -> date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()))
            }
            val timeLabel = if (date == LocalDate.now()) {
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
            } else null
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(transactions, key = { it.id }) { transaction ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        onDelete(transaction.id)
                        true
                    } else {
                        false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                            .background(ErrorRed)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                    tint = Color.White
                )
            }
                },
                content = {
                    TransactionItem(
                        transaction = transaction,
                        onEdit = onEdit,
                        onOpenDetails = onOpenDetails,
                        onDuplicate = onDuplicate,
                        onShare = onShare
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListSection(
    uiState: TransactionsUiState,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onDuplicate: (TransactionItemUi) -> Unit,
    onShare: (TransactionItemUi) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 180.dp)
    ) {
        transactionFeedItems(
            uiState = uiState,
            onDelete = onDelete,
            onEdit = onEdit,
            onOpenDetails = onOpenDetails,
            onDuplicate = onDuplicate,
            onShare = onShare
        )
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionItemUi,
    onEdit: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onDuplicate: (TransactionItemUi) -> Unit,
    onShare: (TransactionItemUi) -> Unit
) {
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
            }
            .pointerInput(true) {
                detectTapGestures(
                    onLongPress = {
                        pressOffset = DpOffset(it.x.toDp(), it.y.toDp())
                        contextMenuExpanded = true
                    },
                    onTap = {
                        onOpenDetails(transaction.id)
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                // Icon - Use category color for background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            when (transaction.type) {
                                TransactionType.INCOME -> SuccessGreen.copy(alpha = 0.2f)
                                TransactionType.EXPENSE -> ErrorRed.copy(alpha = 0.2f)
                                TransactionType.TRANSFER -> InfoBlue.copy(alpha = 0.2f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (transaction.type) {
                            TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                            else -> iconForName(transaction.categoryIcon)
                        },
                        contentDescription = null,
                        tint = when (transaction.type) {
                            TransactionType.INCOME -> SuccessGreen
                            TransactionType.EXPENSE -> ErrorRed
                            TransactionType.TRANSFER -> InfoBlue
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Subtitle: Category • Account (or From -> To)
                    val subtitle = if (transaction.type == TransactionType.TRANSFER) {
                        "${transaction.accountName} \u2192 ${transaction.toAccountName}"
                    } else {
                        "${transaction.categoryName} • ${transaction.accountName}"
                    }
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                
                    // Badges Row
                    if (transaction.isRecurring || transaction.hasReceipt) {
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (transaction.isRecurring) {
                                Badge(kind = TransactionBadgeKind.RECURRING, icon = Icons.Outlined.Repeat)
                    }
                    if (transaction.hasReceipt) {
                                Badge(kind = TransactionBadgeKind.RECEIPT, icon = Icons.Outlined.Receipt)
                            }
                        }
                }
            }
            
            Text(
                    text = "${when (transaction.type) {
                        TransactionType.INCOME -> "+"
                        TransactionType.EXPENSE -> "-"
                        TransactionType.TRANSFER -> ""
                    }}${transaction.amount.toCurrencyString(transaction.currency)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        DropdownMenu(
            expanded = contextMenuExpanded,
            onDismissRequest = { contextMenuExpanded = false },
            offset = pressOffset
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_edit)) },
                onClick = { 
                    contextMenuExpanded = false
                    onEdit(transaction.id) 
                },
                leadingIcon = { Icon(Icons.Outlined.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_duplicate)) },
                onClick = { 
                    contextMenuExpanded = false
                onDuplicate(transaction) 
                },
                leadingIcon = { Icon(Icons.Outlined.FileCopy, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_share)) },
                onClick = { 
                    contextMenuExpanded = false
                onShare(transaction) 
                },
                leadingIcon = { Icon(Icons.Outlined.Share, null) }
            )
        }
    }
}

private enum class TransactionBadgeKind {
    RECURRING,
    RECEIPT
}

@Composable
private fun Badge(kind: TransactionBadgeKind, icon: ImageVector) {
    val text = when (kind) {
        TransactionBadgeKind.RECURRING -> stringResource(R.string.transactions_badge_recurring)
        TransactionBadgeKind.RECEIPT -> stringResource(R.string.transactions_badge_receipt)
    }
    val badgeColor = when (kind) {
        TransactionBadgeKind.RECURRING -> WarningOrange.copy(alpha = 0.2f)
        TransactionBadgeKind.RECEIPT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val badgeTextColor = when (kind) {
        TransactionBadgeKind.RECURRING -> WarningOrange
        TransactionBadgeKind.RECEIPT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = badgeTextColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = badgeTextColor
            )
        }
    }
}

@Composable
private fun localizedTransactionDateRangeLabel(range: TransactionDateRange): String {
    return when (range) {
        TransactionDateRange.THIS_MONTH -> stringResource(R.string.period_this_month)
        TransactionDateRange.LAST_MONTH -> stringResource(R.string.period_last_month)
        TransactionDateRange.THIS_YEAR -> stringResource(R.string.period_this_year)
        TransactionDateRange.ALL_TIME -> stringResource(R.string.period_all_time)
    }
}

@Composable
private fun localizedTransactionSortLabel(option: TransactionSortOption): String {
    return when (option) {
        TransactionSortOption.DATE_DESC -> stringResource(R.string.transactions_sort_date_newest)
        TransactionSortOption.DATE_ASC -> stringResource(R.string.transactions_sort_date_oldest)
        TransactionSortOption.AMOUNT_DESC -> stringResource(R.string.transactions_sort_amount_high_low)
        TransactionSortOption.AMOUNT_ASC -> stringResource(R.string.transactions_sort_amount_low_high)
    }
}
