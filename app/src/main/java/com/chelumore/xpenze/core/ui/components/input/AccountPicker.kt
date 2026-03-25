package com.chelumore.xpenze.core.ui.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.components.layout.XpenzeBottomSheet
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.core.ui.util.parseHexColorOrFallback
import com.chelumore.xpenze.domain.model.Account

@Composable
fun AccountPicker(
    label: String,
    accounts: List<Account>,
    selectedAccountId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    allowClearSelection: Boolean = false,
    clearLabel: String? = null,
    excludeAccountIds: Set<String> = emptySet()
) {
    var showSheet by remember { mutableStateOf(false) }
    val placeholderText = placeholder ?: stringResource(R.string.transaction_form_account_placeholder)
    val clearSelectionText = clearLabel ?: stringResource(R.string.common_none)
    val noItemsText = stringResource(R.string.common_no_items_available)

    val filtered = remember(accounts, excludeAccountIds) {
        if (excludeAccountIds.isEmpty()) accounts else accounts.filterNot { excludeAccountIds.contains(it.id) }
    }

    val selected = remember(selectedAccountId, accounts) {
        accounts.firstOrNull { it.id == selectedAccountId }
    }
    val display = selected?.name ?: ""

    // Overlay click target (OutlinedTextField frequently consumes clicks for focus)
    Box(modifier = modifier.fillMaxWidth()) {
        ExpenseTextField(
            value = display,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = label,
            placeholder = placeholderText,
            readOnly = true,
            enabled = enabled,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { showSheet = true }
        )
    }

    if (showSheet) {
        XpenzeBottomSheet(
            title = label,
            onDismissRequest = { showSheet = false }
        ) {
            if (allowClearSelection) {
                PickerRow(
                    title = clearSelectionText,
                    subtitle = null,
                    leadingColor = MaterialTheme.colorScheme.surfaceVariant,
                    leadingIcon = iconForName("wallet"),
                    onClick = {
                        onSelected(null)
                        showSheet = false
                    }
                )
            }

            filtered.forEach { account ->
                val color = parseHexColorOrFallback(account.colorHex, MaterialTheme.colorScheme.primary)
                PickerRow(
                    title = account.name,
                    subtitle = account.currency,
                    leadingColor = color,
                    leadingIcon = iconForName(account.iconName),
                    onClick = {
                        onSelected(account.id)
                        showSheet = false
                    }
                )
            }

            if (filtered.isEmpty()) {
                androidx.compose.material3.Text(
                    text = noItemsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}




