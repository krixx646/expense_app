package com.chelumore.xpenze.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.MochiPremiumCopywriter
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.ui.components.growth.MochiPremiumNudgeCard
import com.chelumore.xpenze.core.ui.components.input.DatePickerField
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun AdvancedReportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: AdvancedReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { XpenzeTopAppBar(title = stringResource(R.string.reports_recurring_reports_title), onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (!uiState.hasPremiumAccess) {
                LockedCard(
                    variant = uiState.nudgeVariant,
                    onUpgrade = onNavigateToSubscription
                )
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            if (uiState.isTrialActive) {
                Text(
                    text = stringResource(R.string.reports_trial_recurring_reports),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.reports_date_range),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = stringResource(R.string.export_data_start),
                    dateMillis = uiState.range.startMillis,
                    onDateMillisChange = viewModel::setStartDate,
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = stringResource(R.string.export_data_end),
                    dateMillis = uiState.range.endMillis,
                    onDateMillisChange = viewModel::setEndDate,
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.reports_recurring_activity), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    StatRow(stringResource(R.string.reports_recurring_transactions), uiState.recurringCount.toString())
                    StatRow(stringResource(R.string.reports_recurring_income), uiState.recurringIncomeCents.toCurrencyString(uiState.currencyCode), SuccessGreen)
                    StatRow(stringResource(R.string.reports_recurring_expense), uiState.recurringExpenseCents.toCurrencyString(uiState.currencyCode), ErrorRed)
                }
            }

            Text(
                text = stringResource(R.string.reports_recurring_focus_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LockedCard(
    variant: PremiumEmotionVariant,
    onUpgrade: () -> Unit
) {
    val copy = MochiPremiumCopywriter.lockedFeature(
        variant = variant,
        featureName = stringResource(R.string.reports_feature_recurring_reports)
    )
    MochiPremiumNudgeCard(
        copy = copy,
        primaryCtaText = stringResource(R.string.ai_chat_go_premium),
        onPrimaryCta = onUpgrade
    )
}

@Composable
private fun StatRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
    }
}


