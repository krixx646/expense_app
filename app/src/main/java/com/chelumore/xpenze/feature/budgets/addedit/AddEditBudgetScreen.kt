package com.chelumore.xpenze.feature.budgets.addedit

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.components.input.AccountPicker
import com.chelumore.xpenze.core.ui.components.input.AmountInputField
import com.chelumore.xpenze.core.ui.components.input.CategoryPicker
import com.chelumore.xpenze.core.ui.components.input.DatePickerField
import com.chelumore.xpenze.core.ui.components.input.DropdownItem
import com.chelumore.xpenze.core.ui.components.input.DropdownSelector
import com.chelumore.xpenze.core.ui.components.input.ExpenseTextField
import com.chelumore.xpenze.core.ui.components.layout.ConfirmationDialog
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.YellowPrimary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBudgetScreen(
    budgetId: String?,
    cloneFromBudgetId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: AddEditBudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAdvancedOptions by rememberSaveable(budgetId, cloneFromBudgetId) {
        mutableStateOf(budgetId != null || cloneFromBudgetId != null)
    }

    LaunchedEffect(budgetId, cloneFromBudgetId) {
        viewModel.init(budgetId = budgetId, cloneFromBudgetId = cloneFromBudgetId)
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
                    stringResource(R.string.budget_editor_title_edit)
                } else {
                    stringResource(R.string.budget_editor_title_new)
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

        val periodLabel = localizedPeriodLabel(uiState.period)
        val categoryScope = uiState.selectedCategory?.name ?: stringResource(R.string.common_all_categories)
        val accountScope = uiState.selectedAccount?.name ?: stringResource(R.string.common_all_accounts)
        val scopeLabel = stringResource(R.string.budget_editor_scope_pair, categoryScope, accountScope)
        val autoEndDate = uiState.effectiveEndDate
        val expenseCategories = remember(uiState.categories) {
            uiState.categories.filter { it.type == CategoryType.EXPENSE }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AmountInputField(
                amountText = uiState.amountText,
                onAmountTextChange = viewModel::setAmount,
                label = stringResource(R.string.budget_editor_limit_amount),
                currencyCode = uiState.currencyCode,
                modifier = Modifier.fillMaxWidth()
            )

            ExpenseTextField(
                value = uiState.name,
                onValueChange = viewModel::setName,
                label = stringResource(
                    R.string.common_optional_format,
                    stringResource(R.string.onboarding_budget_name_label)
                ),
                placeholder = stringResource(R.string.budget_editor_name_placeholder),
                modifier = Modifier.fillMaxWidth()
            )

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
                        text = if (showAdvancedOptions) {
                            stringResource(R.string.budget_editor_details_title)
                        } else {
                            stringResource(R.string.budget_editor_simple_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.budget_editor_scope_summary, scopeLabel, periodLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showAdvancedOptions = !showAdvancedOptions },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving
                    ) {
                        Text(
                            if (showAdvancedOptions) {
                                stringResource(R.string.common_hide_extra_options)
                            } else {
                                stringResource(R.string.common_show_extra_options)
                            }
                        )
                    }
                }
            }

            if (showAdvancedOptions) {
                val periodItems = BudgetPeriod.entries.map {
                    DropdownItem(
                        id = it.name,
                        label = localizedPeriodLabel(it)
                    )
                }
                DropdownSelector(
                    label = stringResource(R.string.onboarding_budget_period_label),
                    items = periodItems,
                    selectedId = uiState.period.name,
                    onSelected = { id -> id?.let { value -> viewModel.setPeriod(BudgetPeriod.valueOf(value)) } },
                    placeholder = stringResource(R.string.onboarding_budget_period_label),
                    allowClearSelection = false
                )
                Text(
                    text = stringResource(R.string.onboarding_budget_period_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DatePickerField(
                    label = stringResource(R.string.common_start_date),
                    dateMillis = uiState.startDate,
                    onDateMillisChange = viewModel::setStartDate,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.period == BudgetPeriod.CUSTOM) {
                    DatePickerField(
                        label = stringResource(R.string.common_end_date),
                        dateMillis = uiState.endDate,
                        onDateMillisChange = viewModel::setEndDate,
                        placeholder = stringResource(R.string.common_no_end_date),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.endDate != null) {
                        Text(
                            text = stringResource(R.string.common_clear_end_date),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { viewModel.setEndDate(null) }
                                .padding(top = 4.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.budget_editor_custom_end_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (autoEndDate != null) {
                    Text(
                        text = stringResource(
                            R.string.budget_editor_auto_end_hint,
                            formatBudgetDate(autoEndDate)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RecurringBudgetCard(
                    hasPremiumAccess = uiState.hasPremiumAccess,
                    isRecurring = uiState.isRecurring,
                    periodLabel = periodLabel,
                    onRecurringChange = viewModel::setRecurring,
                    onNavigateToSubscription = onNavigateToSubscription
                )

                CategoryPicker(
                    label = stringResource(R.string.transaction_form_category_label),
                    categories = expenseCategories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onSelected = viewModel::setCategoryId,
                    placeholder = stringResource(R.string.common_all_categories),
                    allowClearSelection = true,
                    clearLabel = stringResource(R.string.common_all_categories)
                )
                Text(
                    text = stringResource(R.string.budget_editor_category_expense_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AccountPicker(
                    label = stringResource(R.string.transaction_form_account_label),
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedAccountId,
                    onSelected = viewModel::setAccountId,
                    placeholder = stringResource(R.string.common_all_accounts),
                    allowClearSelection = true,
                    clearLabel = stringResource(R.string.common_all_accounts)
                )
                Text(
                    text = stringResource(R.string.budget_editor_scope_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.budget_editor_alert_threshold),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.budget_editor_alert_threshold_value,
                            (uiState.alertThreshold * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Slider(
                        value = uiState.alertThreshold,
                        onValueChange = viewModel::setAlertThreshold,
                        valueRange = 0.5f..1.0f,
                        steps = 9
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.common_active),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.budget_editor_inactive_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isActive,
                        onCheckedChange = viewModel::setActive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BlackBackground,
                            checkedTrackColor = YellowPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BlackBackground)
                } else {
                    Text(
                        if (uiState.isEdit) {
                            stringResource(R.string.transaction_form_save_changes)
                        } else {
                            stringResource(R.string.budget_editor_create)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = stringResource(R.string.budget_editor_delete_title),
            message = stringResource(R.string.budget_editor_delete_message),
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
private fun RecurringBudgetCard(
    hasPremiumAccess: Boolean,
    isRecurring: Boolean,
    periodLabel: String,
    onRecurringChange: (Boolean) -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.budget_editor_recurring_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasPremiumAccess) {
                            stringResource(R.string.budget_editor_recurring_body)
                        } else {
                            stringResource(R.string.budget_editor_recurring_premium_body)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasPremiumAccess) {
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = onRecurringChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BlackBackground,
                            checkedTrackColor = YellowPrimary
                        )
                    )
                }
            }
            if (hasPremiumAccess && isRecurring) {
                Text(
                    text = stringResource(R.string.budget_editor_recurring_next_hint, periodLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!hasPremiumAccess) {
                OutlinedButton(
                    onClick = onNavigateToSubscription,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ai_chat_go_premium))
                }
            }
        }
    }
}

@Composable
private fun localizedPeriodLabel(period: BudgetPeriod): String {
    return when (period) {
        BudgetPeriod.DAILY -> stringResource(R.string.budget_period_daily)
        BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
        BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
        BudgetPeriod.YEARLY -> stringResource(R.string.budget_period_yearly)
        BudgetPeriod.CUSTOM -> stringResource(R.string.budget_period_custom)
    }
}

private fun formatBudgetDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}
