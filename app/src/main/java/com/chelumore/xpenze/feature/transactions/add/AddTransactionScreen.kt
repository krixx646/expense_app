package com.chelumore.xpenze.feature.transactions.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.domain.model.CategoryType
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.YellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionType: String = "EXPENSE",
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backText = stringResource(R.string.common_back)
    val categoryLabel = stringResource(R.string.transaction_form_category_label)
    val categoryPlaceholder = stringResource(R.string.transaction_form_category_placeholder)
    val accountLabel = stringResource(R.string.transaction_form_account_label)
    val fromAccountLabel = stringResource(R.string.transaction_form_from_account_label)
    val toAccountLabel = stringResource(R.string.transaction_form_to_account_label)
    val accountPlaceholder = stringResource(R.string.transaction_form_account_placeholder)
    val noItemsAvailableText = stringResource(R.string.common_no_items_available)
    val selectableCategories = remember(uiState.type, uiState.categories) {
        when (uiState.type) {
            TransactionType.EXPENSE -> uiState.categories.filter { it.type == CategoryType.EXPENSE }
            TransactionType.INCOME -> uiState.categories.filter { it.type == CategoryType.INCOME }
            TransactionType.TRANSFER -> emptyList()
        }
    }

    // Handle initial type passed from navigation
    LaunchedEffect(transactionType) {
        val type = try {
            TransactionType.valueOf(transactionType.uppercase())
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
        viewModel.setType(type)
    }

    // Handle save success
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transaction_form_title_add), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
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

            // Amount Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.common_amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::setAmount,
                    placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = YellowPrimary,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium
                )
            }

            // Title Input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::setTitle,
                label = { Text(stringResource(R.string.transaction_form_title_label)) },
                placeholder = { Text(stringResource(R.string.transaction_form_title_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YellowPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = YellowPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            // Category Selection (Hidden for Transfer)
            if (uiState.type != TransactionType.TRANSFER) {
                SelectionDropdown(
                    label = categoryLabel,
                    options = selectableCategories,
                    selectedOption = uiState.selectedCategory,
                    onOptionSelected = { viewModel.setCategory(it) },
                    optionToString = { it?.name ?: categoryPlaceholder },
                    placeholder = categoryPlaceholder,
                    noItemsText = noItemsAvailableText
                )
            }

            // Account Selection (From)
            SelectionDropdown(
                label = if (uiState.type == TransactionType.TRANSFER) {
                    fromAccountLabel
                } else {
                    accountLabel
                },
                options = uiState.accounts,
                selectedOption = uiState.selectedAccount,
                onOptionSelected = { viewModel.setAccount(it) },
                optionToString = { it?.name ?: accountPlaceholder },
                placeholder = accountPlaceholder,
                noItemsText = noItemsAvailableText
            )

            // To Account Selection (Only for Transfer)
            if (uiState.type == TransactionType.TRANSFER) {
                SelectionDropdown(
                    label = toAccountLabel,
                    options = uiState.accounts.filter { it.id != uiState.selectedAccount?.id },
                    selectedOption = uiState.selectedToAccount,
                    onOptionSelected = { viewModel.setToAccount(it) },
                    optionToString = { it?.name ?: accountPlaceholder },
                    placeholder = accountPlaceholder,
                    noItemsText = noItemsAvailableText
                )
            }

            // Notes Input
            OutlinedTextField(
                value = uiState.notes ?: "",
                onValueChange = viewModel::setNotes,
                label = { Text(stringResource(R.string.transaction_form_notes_label)) },
                placeholder = { Text(stringResource(R.string.transaction_form_notes_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YellowPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = YellowPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveTransaction() },
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
                enabled = uiState.canSave && !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BlackBackground
                    )
                } else {
                    Text(
                        text = stringResource(R.string.transaction_form_save_transaction),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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

@Composable
private fun <T> SelectionDropdown(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionToString: (T?) -> String,
    placeholder: String,
    noItemsText: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            OutlinedTextField(
                value = if (selectedOption != null) optionToString(selectedOption) else "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YellowPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                enabled = false
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionToString(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
                if (options.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(noItemsText) },
                        onClick = { expanded = false },
                        enabled = false
                    )
                }
            }
        }
    }
}
