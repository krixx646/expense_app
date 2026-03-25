package com.chelumore.xpenze.core.ui.components.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toFormattedDate
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    dateMillis: Long?,
    onDateMillisChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    val systemZone = remember { ZoneId.systemDefault() }

    val minLocalDate = remember(minDateMillis) {
        minDateMillis?.let { Instant.ofEpochMilli(it).atZone(systemZone).toLocalDate() }
    }
    val maxLocalDate = remember(maxDateMillis) {
        maxDateMillis?.let { Instant.ofEpochMilli(it).atZone(systemZone).toLocalDate() }
    }

    val selectableDates = remember(minDateMillis, maxDateMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selected = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                if (minLocalDate != null && selected.isBefore(minLocalDate)) return false
                if (maxLocalDate != null && selected.isAfter(maxLocalDate)) return false
                return true
            }
        }
    }

    val initialSelectedUtcMillis = remember(dateMillis) {
        dateMillis?.let {
            val localDate = Instant.ofEpochMilli(it).atZone(systemZone).toLocalDate()
            localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedUtcMillis,
        selectableDates = selectableDates
    )

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            // DatePicker uses UTC midnight; convert to local start-of-day millis.
                            val localDate = Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate()
                            val localMillis = localDate.atStartOfDay(systemZone).toInstant().toEpochMilli()
                            onDateMillisChange(localMillis)
                        }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val display = dateMillis?.toFormattedDate() ?: ""
    val placeholderText = placeholder ?: stringResource(R.string.common_select)

    // NOTE: Don't rely on `Modifier.clickable` directly on the TextField — OutlinedTextField
    // often consumes the click for focus, preventing our dialog from opening.
    Box(modifier = modifier) {
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
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled) { showDialog = true }
        )
    }
}


