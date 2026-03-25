package com.chelumore.xpenze.core.ui.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.components.layout.XpenzeBottomSheet
import java.util.Currency
import java.util.Locale

@Composable
fun CurrencyPicker(
    label: String,
    selectedCurrencyCode: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    currencies: List<String>? = null,
    locale: Locale = Locale.getDefault()
) {
    var showSheet by remember { mutableStateOf(false) }
    var query by remember(showSheet) { mutableStateOf("") }
    val placeholderText = placeholder ?: stringResource(R.string.common_select)
    val searchText = stringResource(R.string.common_search)
    val noItemsText = stringResource(R.string.common_no_items_available)

    val display = selectedCurrencyCode ?: ""

    data class CurrencyItem(val code: String, val displayName: String)

    val currencyItems = remember(currencies, locale) {
        val codes = currencies ?: Currency.getAvailableCurrencies()
            .map { it.currencyCode }
            .distinct()
            .sorted()

        codes.map { code ->
            val currency = runCatching { Currency.getInstance(code) }.getOrNull()
            CurrencyItem(code = code, displayName = currency?.getDisplayName(locale).orEmpty())
        }
    }

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
            ExpenseTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = searchText,
                placeholder = searchText,
                leadingIcon = Icons.Default.Search,
                singleLine = true
            )

            val filtered = remember(currencyItems, query) {
                val q = query.trim()
                if (q.isBlank()) currencyItems
                else currencyItems.filter { item ->
                    item.code.contains(q, ignoreCase = true) ||
                        item.displayName.contains(q, ignoreCase = true)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.code }
                ) { item ->
                    PickerRow(
                        title = item.code,
                        subtitle = item.displayName.ifBlank { null },
                        leadingColor = MaterialTheme.colorScheme.primary,
                        leadingIcon = Icons.Default.AttachMoney,
                        onClick = {
                            onSelected(item.code)
                            showSheet = false
                        }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = noItemsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


