package com.chelumore.xpenze.feature.transactions.addedit

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.common.toLocalDateTime
import com.chelumore.xpenze.core.ui.components.input.AccountPicker
import com.chelumore.xpenze.core.ui.components.input.AmountInputField
import com.chelumore.xpenze.core.ui.components.input.CategoryPicker
import com.chelumore.xpenze.core.ui.components.input.DatePickerField
import com.chelumore.xpenze.core.ui.components.input.DropdownItem
import com.chelumore.xpenze.core.ui.components.input.DropdownSelector
import com.chelumore.xpenze.core.ui.components.input.ExpenseTextField
import com.chelumore.xpenze.core.ui.components.input.TimePickerField
import com.chelumore.xpenze.core.ui.components.layout.ConfirmationDialog
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.domain.model.RecurringFrequency
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.YellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: String?,
    draftId: String? = null,
    initialType: String = "EXPENSE",
    onNavigateBack: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAdvancedFields by rememberSaveable(transactionId, draftId) {
        mutableStateOf(transactionId != null || draftId != null)
    }

    val pickReceipt = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.setReceiptUri(uri.toString())
    }

    LaunchedEffect(transactionId, draftId, initialType) {
        val parsed = runCatching { TransactionType.valueOf(initialType.uppercase()) }.getOrDefault(TransactionType.EXPENSE)
        viewModel.init(transactionId = transactionId, draftId = draftId, initialType = parsed)
    }

    LaunchedEffect(uiState.error) {
        val msg = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            XpenzeTopAppBar(
                title = if (uiState.isEdit) {
                    stringResource(R.string.transaction_form_title_edit)
                } else if (draftId != null) {
                    stringResource(R.string.auto_capture_review_draft_title)
                } else {
                    stringResource(R.string.transaction_form_title_add)
                },
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.isEdit) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = ErrorRed
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (draftId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.auto_capture_review_draft_banner_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.auto_capture_review_draft_banner_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Type Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionTypeChip(
                    label = stringResource(R.string.transaction_form_type_expense),
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.setType(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f)
                )
                TransactionTypeChip(
                    label = stringResource(R.string.transaction_form_type_income),
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.setType(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f)
                )
                TransactionTypeChip(
                    label = stringResource(R.string.transaction_form_type_transfer),
                    selected = uiState.type == TransactionType.TRANSFER,
                    onClick = { viewModel.setType(TransactionType.TRANSFER) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (uiState.type == TransactionType.TRANSFER) {
                Text(
                    text = stringResource(R.string.transaction_form_transfer_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount Input
            AmountInputField(
                amountText = uiState.amountText,
                onAmountTextChange = viewModel::setAmount,
                currencyCode = uiState.currency,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection (Hidden for Transfer)
            if (uiState.type != TransactionType.TRANSFER) {
                CategoryPicker(
                    label = stringResource(
                        R.string.common_optional_format,
                        stringResource(R.string.transaction_form_category_label)
                    ),
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelected = viewModel::setCategoryId,
                    placeholder = stringResource(R.string.transaction_form_category_placeholder),
                    allowClearSelection = true,
                    clearLabel = stringResource(R.string.transaction_form_uncategorized)
                )
            }

            // Account Selection (From)
            AccountPicker(
                label = if (uiState.type == TransactionType.TRANSFER) {
                    stringResource(R.string.transaction_form_from_account_label)
                } else {
                    stringResource(R.string.transaction_form_account_label)
                },
                accounts = uiState.accounts,
                selectedAccountId = uiState.selectedAccountId,
                onSelected = viewModel::setAccountId,
                placeholder = stringResource(R.string.transaction_form_account_placeholder)
            )

            // To Account Selection (Only for Transfer)
            if (uiState.type == TransactionType.TRANSFER) {
                val fromId = uiState.selectedAccountId
                AccountPicker(
                    label = stringResource(R.string.transaction_form_to_account_label),
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedToAccountId,
                    onSelected = viewModel::setToAccountId,
                    placeholder = stringResource(R.string.transaction_form_account_placeholder),
                    excludeAccountIds = setOfNotNull(fromId)
                )
            }
            val advancedSummary = remember(
                uiState.dateTime,
                uiState.isRecurring,
                uiState.recurringFrequency,
                uiState.receiptUri,
                uiState.tagsText,
                uiState.notes,
                context
            ) {
                val recurringLabel = when (uiState.recurringFrequency) {
                    RecurringFrequency.DAILY -> context.getString(R.string.transaction_form_frequency_daily)
                    RecurringFrequency.WEEKLY -> context.getString(R.string.transaction_form_frequency_weekly)
                    RecurringFrequency.BIWEEKLY -> context.getString(R.string.transaction_form_frequency_biweekly)
                    RecurringFrequency.MONTHLY -> context.getString(R.string.transaction_form_frequency_monthly)
                    RecurringFrequency.YEARLY -> context.getString(R.string.transaction_form_frequency_yearly)
                }
                buildList {
                    add(context.getString(R.string.transaction_form_summary_date, uiState.dateTime.toFormattedDateTime()))
                    if (uiState.isRecurring) {
                        add(context.getString(R.string.transaction_form_summary_repeats, recurringLabel))
                    }
                    if (!uiState.receiptUri.isNullOrBlank()) add(context.getString(R.string.transaction_form_summary_receipt_attached))
                    if (!uiState.notes.isNullOrBlank()) add(context.getString(R.string.transaction_form_summary_notes_added))
                    if (uiState.tagsText.isNotBlank()) add(context.getString(R.string.transaction_form_summary_tags_added))
                }.joinToString(" • ")
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
                    Text(
                        text = if (showAdvancedFields) {
                            stringResource(R.string.transaction_form_extra_details_title)
                        } else {
                            stringResource(R.string.transaction_form_keep_simple_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = advancedSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showAdvancedFields = !showAdvancedFields },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        Text(
                            if (showAdvancedFields) {
                                stringResource(R.string.common_hide_extra_options)
                            } else {
                                stringResource(R.string.common_show_extra_options)
                            }
                        )
                    }
                }
            }

            if (showAdvancedFields) {
                ExpenseTextField(
                    value = uiState.title,
                    onValueChange = viewModel::setTitle,
                    label = stringResource(
                        R.string.common_optional_format,
                        stringResource(R.string.transaction_form_title_label)
                    ),
                    placeholder = stringResource(R.string.transaction_form_title_placeholder_auto),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                uiState.merchantSuggestion?.let { suggestion ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.auto_capture_merchant_memory_card_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(
                                    R.string.auto_capture_merchant_memory_card_body,
                                    suggestion.matchedTitle
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = viewModel::applyMerchantSuggestion,
                                    enabled = !uiState.isSaving
                                ) {
                                    Text(stringResource(R.string.auto_capture_apply_memory_button))
                                }
                                if (suggestion.recurringSuggestion != null) {
                                    OutlinedButton(
                                        onClick = viewModel::applyRecurringSuggestion,
                                        enabled = !uiState.isSaving
                                    ) {
                                        Text(stringResource(R.string.auto_capture_apply_recurring_button))
                                    }
                                }
                            }
                        }
                    }
                }

                val localTime = remember(uiState.dateTime) { uiState.dateTime.toLocalDateTime().toLocalTime() }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePickerField(
                        label = stringResource(R.string.transaction_form_date_label),
                        dateMillis = uiState.dateTime,
                        onDateMillisChange = viewModel::setDate,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = stringResource(R.string.transaction_form_time_label),
                        time = localTime,
                        onTimeChange = viewModel::setTime,
                        modifier = Modifier.weight(1f)
                    )
                }

                ExpenseTextField(
                    value = uiState.notes ?: "",
                    onValueChange = viewModel::setNotes,
                    label = stringResource(R.string.transaction_form_notes_label),
                    placeholder = stringResource(R.string.transaction_form_notes_placeholder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    singleLine = false,
                    maxLines = 4
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.transaction_form_receipt_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (!uiState.receiptUri.isNullOrBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = uiState.receiptUri,
                                contentDescription = stringResource(R.string.transaction_form_receipt_preview_cd),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { pickReceipt.launch(arrayOf("image/*")) },
                                enabled = !uiState.isSaving
                            ) { Text(stringResource(R.string.transaction_form_receipt_change)) }

                            OutlinedButton(
                                onClick = viewModel::clearReceipt,
                                enabled = !uiState.isSaving
                            ) { Text(stringResource(R.string.transaction_form_receipt_remove)) }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { pickReceipt.launch(arrayOf("image/*")) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.transaction_form_receipt_attach)) }

                        Text(
                            text = stringResource(R.string.transaction_form_receipt_tip),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ExpenseTextField(
                    value = uiState.tagsText,
                    onValueChange = viewModel::setTagsText,
                    label = stringResource(R.string.transaction_form_tags_label),
                    placeholder = stringResource(R.string.transaction_form_tags_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val freqItems = remember {
                    RecurringFrequency.entries.map {
                        val label = when (it) {
                            RecurringFrequency.DAILY -> context.getString(R.string.transaction_form_frequency_daily)
                            RecurringFrequency.WEEKLY -> context.getString(R.string.transaction_form_frequency_weekly)
                            RecurringFrequency.BIWEEKLY -> context.getString(R.string.transaction_form_frequency_biweekly)
                            RecurringFrequency.MONTHLY -> context.getString(R.string.transaction_form_frequency_monthly)
                            RecurringFrequency.YEARLY -> context.getString(R.string.transaction_form_frequency_yearly)
                        }
                        DropdownItem(it.name, label)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.transaction_form_repeat_toggle_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.transaction_form_repeat_toggle_body),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isRecurring,
                        onCheckedChange = viewModel::setRecurringEnabled,
                        enabled = !uiState.isSaving,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BlackBackground,
                            checkedTrackColor = YellowPrimary
                        )
                    )
                }

                if (uiState.isRecurring) {
                    DropdownSelector(
                        label = stringResource(R.string.transaction_form_frequency_label),
                        items = freqItems,
                        selectedId = uiState.recurringFrequency.name,
                        onSelected = { id -> id?.let { viewModel.setRecurringFrequency(RecurringFrequency.valueOf(it)) } },
                        allowClearSelection = false,
                        enabled = !uiState.isSaving
                    )

                    ExpenseTextField(
                        value = uiState.recurringIntervalText,
                        onValueChange = viewModel::setRecurringIntervalText,
                        label = stringResource(R.string.transaction_form_interval_label),
                        placeholder = stringResource(R.string.transaction_form_interval_placeholder),
                        enabled = !uiState.isSaving
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = BlackBackground,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.canSave
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BlackBackground
                    )
                } else {
                    Text(
                        text = if (uiState.isEdit) {
                            stringResource(R.string.transaction_form_save_changes)
                        } else {
                            stringResource(R.string.transaction_form_save_transaction)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.transaction_form_delete_title),
            message = stringResource(R.string.transaction_form_delete_message),
            confirmText = stringResource(R.string.common_delete),
            dismissText = stringResource(R.string.common_cancel),
            confirmIsDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.delete()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

@Composable
private fun TransactionTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = YellowPrimary,
            selectedLabelColor = BlackBackground,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        border = null
    )
}
