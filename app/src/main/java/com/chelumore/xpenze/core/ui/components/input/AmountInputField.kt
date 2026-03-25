package com.chelumore.xpenze.core.ui.components.input

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.R

@Composable
fun AmountInputField(
    amountText: String,
    onAmountTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    currencyCode: String = "USD",
    placeholder: String = "0.00",
    supportingText: String? = null,
    isError: Boolean = false,
    errorText: String? = null
) {
    val resolvedLabel = label ?: stringResource(R.string.common_amount)
    ExpenseTextField(
        value = amountText,
        onValueChange = { raw -> onAmountTextChange(sanitizeAmountInput(raw)) },
        modifier = modifier,
        label = resolvedLabel,
        placeholder = placeholder,
        supportingText = supportingText,
        isError = isError,
        errorText = errorText,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next
        ),
        textStyle = MaterialTheme.typography.headlineMedium,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currencyCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    )
}

private fun sanitizeAmountInput(raw: String): String {
    if (raw.isBlank()) return ""
    val normalized = raw.replace(',', '.')
    val out = StringBuilder()
    var dotSeen = false
    var decimals = 0

    for (c in normalized) {
        when {
            c.isDigit() -> {
                if (!dotSeen) {
                    out.append(c)
                } else if (decimals < 2) {
                    out.append(c)
                    decimals++
                }
            }
            c == '.' && !dotSeen -> {
                dotSeen = true
                if (out.isEmpty()) out.append('0')
                out.append('.')
            }
        }
    }

    return out.toString()
}


