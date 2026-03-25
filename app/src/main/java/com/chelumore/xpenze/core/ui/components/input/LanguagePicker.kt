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
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.ui.components.layout.XpenzeBottomSheet

@Composable
fun LanguagePicker(
    label: String,
    selectedLanguageTag: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true
) {
    var showSheet by remember { mutableStateOf(false) }
    var query by remember(showSheet) { mutableStateOf("") }

    val selectedTag = remember(selectedLanguageTag) {
        AppLanguages.normalizeTag(selectedLanguageTag)
    }
    val selectedLabel = remember(selectedTag) {
        AppLanguages.displayName(selectedTag)
    }
    val resolvedPlaceholder = placeholder ?: stringResource(R.string.language_picker_placeholder)

    Box(modifier = modifier.fillMaxWidth()) {
        ExpenseTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = label,
            placeholder = resolvedPlaceholder,
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
                label = stringResource(R.string.common_search),
                placeholder = stringResource(R.string.language_picker_search_placeholder),
                leadingIcon = Icons.Default.Search,
                singleLine = true
            )

            val filtered = remember(query) {
                val q = query.trim()
                if (q.isBlank()) {
                    AppLanguages.options
                } else {
                    AppLanguages.options.filter { option ->
                        option.label.contains(q, ignoreCase = true) ||
                            option.tag.contains(q, ignoreCase = true)
                    }
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
                    key = { it.tag }
                ) { option ->
                    PickerRow(
                        title = option.label,
                        subtitle = option.tag,
                        leadingColor = MaterialTheme.colorScheme.primary,
                        leadingIcon = Icons.Default.Language,
                        onClick = {
                            onSelected(option.tag)
                            showSheet = false
                        }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.language_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

