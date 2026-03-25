package com.chelumore.xpenze.core.ui.components.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.core.growth.PremiumOpportunityType
import com.chelumore.xpenze.core.growth.formatPremiumOpportunity
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary
import java.util.Locale

@Composable
fun PremiumOpportunityCard(
    opportunity: PremiumOpportunity,
    currencyCode: String,
    appLanguageTag: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val copy = formatPremiumOpportunity(
        opportunity = opportunity,
        currencyCode = currencyCode,
        languageTag = appLanguageTag
    )
    val accentColor = when (opportunity.type) {
        PremiumOpportunityType.CATEGORY_CUTBACK -> SuccessGreen
        PremiumOpportunityType.CATEGORY_SPIKE -> WarningOrange
        PremiumOpportunityType.BUDGET_PRESSURE -> ErrorRed
        PremiumOpportunityType.RECURRING_REVIEW -> InfoBlue
        PremiumOpportunityType.BUDGET_SUGGESTION -> YellowPrimary
    }
    val locale = runCatching { Locale.forLanguageTag(appLanguageTag) }.getOrDefault(Locale.getDefault())
    val badgeText = when {
        opportunity.impactCents > 0L -> opportunity.impactCents.toCurrencyString(currencyCode, locale)
        opportunity.amountCents > 0L -> opportunity.amountCents.toCurrencyString(currencyCode, locale)
        else -> opportunity.label
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = badgeText,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = copy.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!actionText.isNullOrBlank() && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
