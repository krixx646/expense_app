package com.chelumore.xpenze.feature.recurring

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.ui.components.input.AccountPicker
import com.chelumore.xpenze.core.ui.components.input.AmountInputField
import com.chelumore.xpenze.core.ui.components.input.CategoryPicker
import com.chelumore.xpenze.core.ui.components.input.DatePickerField
import com.chelumore.xpenze.core.ui.components.input.DropdownItem
import com.chelumore.xpenze.core.ui.components.input.DropdownSelector
import com.chelumore.xpenze.core.ui.components.input.ExpenseTextField
import com.chelumore.xpenze.core.ui.components.input.TimePickerField
import com.chelumore.xpenze.core.ui.components.layout.ConfirmationDialog
import com.chelumore.xpenze.core.ui.components.layout.XpenzeBottomSheet
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.domain.model.RecurringFrequency
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.YellowPrimary
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurringRulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultTransferTitle = stringResource(R.string.transaction_default_title_transfer)
    val defaultIncomeTitle = stringResource(R.string.transaction_default_title_income)
    val defaultExpenseTitle = stringResource(R.string.transaction_default_title_expense)

    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var showAdvancedEditorOptions by remember { mutableStateOf(false) }

    // Editor fields (local UI state)
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf<String?>(null) }
    var toAccountId by remember { mutableStateOf<String?>(null) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var frequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var intervalText by remember { mutableStateOf("1") }
    var startDateMillis by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var startTime by remember { mutableStateOf(LocalTime.now().withSecond(0).withNano(0)) }
    var endDateMillis by remember { mutableStateOf<Long?>(null) }
    var isActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RecurringRulesEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    fun openNew() {
        editingId = null
        type = TransactionType.EXPENSE
        title = ""
        notes = ""
        amountText = ""
        accountId = uiState.accounts.firstOrNull()?.id
        toAccountId = null
        categoryId = null
        frequency = RecurringFrequency.MONTHLY
        intervalText = "1"
        startDateMillis = System.currentTimeMillis()
        startTime = LocalTime.now().withSecond(0).withNano(0)
        endDateMillis = null
        isActive = true
        showAdvancedEditorOptions = false
        showEditor = true
    }

    fun openEdit(id: String) {
        val rule = uiState.rules.firstOrNull { it.rule.id == id }?.rule ?: return
        editingId = id
        type = rule.type
        title = rule.title
        notes = rule.notes.orEmpty()
        amountText = (rule.money.amountCents / 100.0).toString()
        accountId = rule.accountId
        toAccountId = rule.toAccountId
        categoryId = rule.categoryId
        frequency = rule.frequency
        intervalText = rule.interval.toString()
        startDateMillis = rule.startDate
        startTime = Instant.ofEpochMilli(rule.startDate).atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0)
        endDateMillis = rule.endDate
        isActive = rule.isActive
        showAdvancedEditorOptions = true
        showEditor = true
    }

    fun save() {
        val fromAccount = accountId ?: run {
            snackbarHostState.currentSnackbarData?.dismiss()
            return
        }

        val cents = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (cents <= 0L) return
        val categoryName = uiState.categories.firstOrNull { it.id == categoryId }?.name
        val name = title.trim().ifBlank {
            when (type) {
                TransactionType.TRANSFER -> defaultTransferTitle
                TransactionType.INCOME -> categoryName ?: defaultIncomeTitle
                TransactionType.EXPENSE -> categoryName ?: defaultExpenseTitle
            }
        }

        if (type == TransactionType.TRANSFER) {
            if (toAccountId.isNullOrBlank() || toAccountId == fromAccount) return
        }

        val zone = ZoneId.systemDefault()
        val date = (startDateMillis ?: System.currentTimeMillis())
        val localDate = Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
        val startDateTimeMillis = localDate.atTime(startTime).atZone(zone).toInstant().toEpochMilli()

        val resolvedEnd = endDateMillis?.let {
            // End of day (inclusive)
            val endLocal = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
            endLocal.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        }

        val interval = intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1

        // Frequency-specific hints (derive from start date)
        val startZdt = Instant.ofEpochMilli(startDateTimeMillis).atZone(zone)
        val dow = startZdt.dayOfWeek.value
        val dom = startZdt.dayOfMonth
        val moy = startZdt.monthValue

        val currency = uiState.accounts.firstOrNull { it.id == fromAccount }?.currency ?: "USD"

        viewModel.upsertRule(
            existingId = editingId,
            accountId = fromAccount,
            toAccountId = toAccountId,
            categoryId = categoryId,
            amountCents = cents,
            currency = currency,
            type = type,
            title = name,
            notes = notes.trim().takeIf { it.isNotBlank() },
            frequency = frequency,
            interval = interval,
            dayOfWeek = if (frequency == RecurringFrequency.WEEKLY || frequency == RecurringFrequency.BIWEEKLY) dow else null,
            dayOfMonth = if (frequency == RecurringFrequency.MONTHLY) dom else null,
            monthOfYear = if (frequency == RecurringFrequency.YEARLY) moy else null,
            startDate = startDateTimeMillis,
            endDate = resolvedEnd,
            isActive = isActive
        )
        showEditor = false
    }

    Scaffold(
        topBar = {
            XpenzeTopAppBar(
                title = stringResource(R.string.recurring_screen_title),
                onNavigateBack = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openNew() },
                containerColor = YellowPrimary,
                contentColor = BlackBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.common_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = YellowPrimary)
            }
            return@Scaffold
        }

        if (uiState.rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Repeat, contentDescription = null, tint = YellowPrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.recurring_empty_title),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    stringResource(R.string.recurring_empty_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { openNew() },
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = BlackBackground)
                ) {
                    Text(stringResource(R.string.recurring_create), fontWeight = FontWeight.Bold)
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.recurring_info_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = viewModel::runDueNow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.recurring_check_due_now))
                        }
                    }
                }
            }
            items(uiState.rules, key = { it.rule.id }) { item ->
                val rule = item.rule
                val ruleFrequencyLabel = frequencyLabel(rule.frequency)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openEdit(rule.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    stringResource(
                                        R.string.recurring_rule_next_occurrence,
                                        ruleFrequencyLabel,
                                        rule.nextOccurrence.toFormattedDateTime()
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = rule.isActive,
                                onCheckedChange = { viewModel.setActive(rule.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = BlackBackground,
                                    checkedTrackColor = YellowPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.accountName}${item.toAccountName?.let { " → $it" }.orEmpty()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = rule.money.amountCents.toCurrencyString(rule.money.currency),
                                fontWeight = FontWeight.Bold,
                                color = YellowPrimary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (rule.type == TransactionType.TRANSFER) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.transaction_form_type_transfer), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(item.categoryName ?: stringResource(R.string.common_all_categories), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { confirmDeleteId = rule.id }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete), tint = ErrorRed)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showEditor) {
        XpenzeBottomSheet(
            title = if (editingId == null) {
                stringResource(R.string.recurring_editor_title_new)
            } else {
                stringResource(R.string.recurring_editor_title_edit)
            },
            onDismissRequest = { showEditor = false }
        ) {
            val freqItems = RecurringFrequency.entries.map { DropdownItem(it.name, frequencyLabel(it)) }
            val typeItems = TransactionType.entries.map {
                DropdownItem(
                    it.name,
                    when (it) {
                        TransactionType.EXPENSE -> stringResource(R.string.transaction_form_type_expense)
                        TransactionType.INCOME -> stringResource(R.string.transaction_form_type_income)
                        TransactionType.TRANSFER -> stringResource(R.string.transaction_form_type_transfer)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val previewStartMillis = remember(startDateMillis, startTime) {
                    val zone = ZoneId.systemDefault()
                    val date = startDateMillis ?: System.currentTimeMillis()
                    val localDate = Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
                    localDate.atTime(startTime).atZone(zone).toInstant().toEpochMilli()
                }

                DropdownSelector(
                    label = stringResource(R.string.common_type),
                    items = typeItems,
                    selectedId = type.name,
                    onSelected = { id -> id?.let { type = TransactionType.valueOf(it) } },
                    allowClearSelection = false
                )

                AmountInputField(
                    amountText = amountText,
                    onAmountTextChange = { amountText = it },
                    label = stringResource(R.string.common_amount),
                    currencyCode = uiState.accounts.firstOrNull { it.id == accountId }?.currency ?: "USD"
                )

                AccountPicker(
                    label = stringResource(R.string.transaction_form_from_account_label),
                    accounts = uiState.accounts,
                    selectedAccountId = accountId,
                    onSelected = { accountId = it },
                    allowClearSelection = false
                )

                if (type == TransactionType.TRANSFER) {
                    AccountPicker(
                        label = stringResource(R.string.transaction_form_to_account_label),
                        accounts = uiState.accounts,
                        selectedAccountId = toAccountId,
                        onSelected = { toAccountId = it },
                        allowClearSelection = false,
                        excludeAccountIds = setOfNotNull(accountId)
                    )
                } else {
                    CategoryPicker(
                        label = stringResource(
                            R.string.common_optional_format,
                            stringResource(R.string.transaction_form_category_label)
                        ),
                        categories = uiState.categories,
                        selectedCategoryId = categoryId,
                        onSelected = { categoryId = it },
                        allowClearSelection = true,
                        clearLabel = stringResource(R.string.transaction_form_uncategorized)
                    )
                }

                DropdownSelector(
                    label = stringResource(R.string.transaction_form_frequency_label),
                    items = freqItems,
                    selectedId = frequency.name,
                    onSelected = { id -> id?.let { frequency = RecurringFrequency.valueOf(it) } },
                    allowClearSelection = false
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DatePickerField(
                        label = stringResource(R.string.common_start_date),
                        dateMillis = startDateMillis,
                        onDateMillisChange = { startDateMillis = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = stringResource(R.string.transaction_form_time_label),
                        time = startTime,
                        onTimeChange = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val frequencyLabel = frequencyLabel(frequency)
                        Text(
                            text = stringResource(
                                R.string.recurring_preview,
                                frequencyLabel,
                                previewStartMillis.toFormattedDateTime()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { showAdvancedEditorOptions = !showAdvancedEditorOptions },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (showAdvancedEditorOptions) {
                                    stringResource(R.string.common_hide_extra_options)
                                } else {
                                    stringResource(R.string.common_show_extra_options)
                                }
                            )
                        }
                    }
                }

                if (showAdvancedEditorOptions) {
                    ExpenseTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = stringResource(
                            R.string.common_optional_format,
                            stringResource(R.string.transaction_form_title_label)
                        ),
                        placeholder = stringResource(R.string.recurring_title_placeholder_auto)
                    )

                    ExpenseTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = stringResource(R.string.transaction_form_notes_label),
                        placeholder = stringResource(R.string.recurring_notes_placeholder),
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )

                    ExpenseTextField(
                        value = intervalText,
                        onValueChange = { intervalText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = stringResource(R.string.transaction_form_interval_label),
                        placeholder = "1"
                    )

                    DatePickerField(
                        label = stringResource(
                            R.string.common_optional_format,
                            stringResource(R.string.common_end_date)
                        ),
                        dateMillis = endDateMillis,
                        onDateMillisChange = { endDateMillis = it }
                    )
                    if (endDateMillis != null) {
                        Text(
                            text = stringResource(R.string.common_clear_end_date),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { endDateMillis = null }
                                .padding(top = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.common_active),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.recurring_inactive_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BlackBackground,
                                checkedTrackColor = YellowPrimary
                            )
                        )
                    }
                }

                Button(
                    onClick = { save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = BlackBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.recurring_save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmDeleteId != null) {
        val id = confirmDeleteId!!
        ConfirmationDialog(
            title = stringResource(R.string.recurring_delete_title),
            message = stringResource(R.string.recurring_delete_message),
            confirmText = stringResource(R.string.common_delete),
            dismissText = stringResource(R.string.common_cancel),
            confirmIsDestructive = true,
            onConfirm = {
                viewModel.delete(id)
                confirmDeleteId = null
            },
            onDismiss = { confirmDeleteId = null }
        )
    }
}

@Composable
private fun frequencyLabel(frequency: RecurringFrequency): String = when (frequency) {
    RecurringFrequency.DAILY -> stringResource(R.string.transaction_form_frequency_daily)
    RecurringFrequency.WEEKLY -> stringResource(R.string.transaction_form_frequency_weekly)
    RecurringFrequency.BIWEEKLY -> stringResource(R.string.transaction_form_frequency_biweekly)
    RecurringFrequency.MONTHLY -> stringResource(R.string.transaction_form_frequency_monthly)
    RecurringFrequency.YEARLY -> stringResource(R.string.transaction_form_frequency_yearly)
}


