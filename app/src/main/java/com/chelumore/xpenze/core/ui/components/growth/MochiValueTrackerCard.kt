package com.chelumore.xpenze.core.ui.components.growth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.core.growth.formatPremiumValueTracker
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun MochiValueTrackerCard(
    opportunities: List<PremiumOpportunity>,
    currencyCode: String,
    appLanguageTag: String,
    hasPremiumAccess: Boolean,
    modifier: Modifier = Modifier
) {
    val tracker = formatPremiumValueTracker(
        opportunities = opportunities,
        currencyCode = currencyCode,
        languageTag = appLanguageTag,
        hasPremiumAccess = hasPremiumAccess
    ) ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = YellowPrimary.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tracker.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = tracker.amountText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tracker.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
