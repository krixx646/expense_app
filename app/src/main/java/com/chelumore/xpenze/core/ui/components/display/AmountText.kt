package com.chelumore.xpenze.core.ui.components.display

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.common.toSignedCurrencyString

@Composable
fun AmountText(
    amountCents: Long,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showSign: Boolean = false
) {
    val text = if (showSign) {
        amountCents.toSignedCurrencyString(currencyCode)
    } else {
        amountCents.toCurrencyString(currencyCode)
    }
    Text(text = text, modifier = modifier, style = style, color = color)
}




