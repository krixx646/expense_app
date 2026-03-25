package com.chelumore.xpenze.core.ui.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
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
import com.chelumore.xpenze.core.ui.util.iconForName
import com.chelumore.xpenze.core.ui.util.parseHexColorOrFallback
import com.chelumore.xpenze.domain.model.Category

@Composable
fun CategoryPicker(
    label: String,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    allowClearSelection: Boolean = true,
    clearLabel: String? = null
) {
    var showSheet by remember { mutableStateOf(false) }
    var query by remember(showSheet) { mutableStateOf("") }
    val placeholderText = placeholder ?: stringResource(R.string.transaction_form_category_placeholder)
    val clearSelectionText = clearLabel ?: stringResource(R.string.transaction_form_uncategorized)
    val searchText = stringResource(R.string.common_search)
    val noItemsText = stringResource(R.string.common_no_items_available)

    val selected = remember(selectedCategoryId, categories) {
        categories.firstOrNull { it.id == selectedCategoryId }
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
            ExpenseTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = searchText,
                placeholder = searchText,
                leadingIcon = Icons.Default.Search,
                singleLine = true
            )

            if (allowClearSelection) {
                PickerRow(
                    title = clearSelectionText,
                    subtitle = null,
                    leadingColor = MaterialTheme.colorScheme.surfaceVariant,
                    leadingIcon = iconForName("category"),
                    onClick = {
                        onSelected(null)
                        showSheet = false
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val filtered = remember(categories, query) {
                // Hide duplicates with same (type + name) to improve UX when older data/sync produced duplicates.
                val unique = categories
                    .groupBy { it.type to it.name.trim().lowercase() }
                    .mapNotNull { (_, list) -> list.minByOrNull { it.sortOrder } }
                    .sortedWith(compareBy<Category>({ it.type.ordinal }, { it.sortOrder }, { it.name.lowercase() }))

                val q = query.trim()
                if (q.isBlank()) unique
                else unique.filter { it.name.contains(q, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id }
                ) { category ->
                    val color = parseHexColorOrFallback(category.colorHex, MaterialTheme.colorScheme.primary)
                    PickerRow(
                        title = category.name,
                        subtitle = null,
                        leadingColor = color,
                        leadingIcon = iconForName(category.iconName),
                        onClick = {
                            onSelected(category.id)
                            showSheet = false
                        }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = noItemsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

