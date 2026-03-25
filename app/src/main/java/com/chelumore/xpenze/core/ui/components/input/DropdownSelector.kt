package com.chelumore.xpenze.core.ui.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chelumore.xpenze.R

data class DropdownItem(
    val id: String,
    val label: String
)

@Composable
fun DropdownSelector(
    label: String,
    items: List<DropdownItem>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    allowClearSelection: Boolean = true,
    clearLabel: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = remember(selectedId, items) {
        items.firstOrNull { it.id == selectedId }?.label
    }
    val placeholderText = placeholder ?: stringResource(R.string.common_select)
    val clearSelectionText = clearLabel ?: stringResource(R.string.common_none)

    Box(modifier = modifier) {
        ExpenseTextField(
            value = selectedLabel ?: "",
            onValueChange = {},
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
            },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (allowClearSelection) {
                DropdownMenuItem(
                    text = { Text(clearSelectionText, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
            }
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onSelected(item.id)
                        expanded = false
                    }
                )
            }
            if (items.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.common_no_items_available),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { expanded = false },
                    enabled = false
                )
            }
        }
    }
}


