package com.chelumore.xpenze.feature.autocapture

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSourceLabel
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatusLabel
import com.chelumore.xpenze.core.ui.components.input.AccountPicker
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.domain.model.AutoCaptureDraft
import com.chelumore.xpenze.domain.model.AutoCaptureDraftStatus
import com.chelumore.xpenze.domain.model.AutoCaptureSource
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun DraftInboxScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToReviewDraft: (String, String) -> Unit,
    viewModel: DraftInboxViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pickStatement = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }.orEmpty()
        }.onSuccess { content ->
            if (content.isNotBlank()) viewModel.importStatement(content)
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.captureReceipt(uri.toString())
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DraftInboxViewModel.DraftInboxEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            XpenzeTopAppBar(
                title = stringResource(R.string.auto_capture_draft_inbox_title),
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            item {
                SummaryCard(
                    count = uiState.drafts.size,
                    autoCaptureEnabled = uiState.autoCaptureEnabled,
                    hasPremiumAccess = uiState.hasPremiumAccess
                )
            }
            item {
                CaptureActionsCard(
                    uiState = uiState,
                    onImportAccountChanged = viewModel::setImportAccount,
                    onImportStatement = {
                        pickStatement.launch(
                            arrayOf(
                                "text/*",
                                "text/csv",
                                "application/csv",
                                "application/vnd.ms-excel"
                            )
                        )
                    },
                    onPickImage = { pickImage.launch(arrayOf("image/*")) },
                    onUpgrade = onNavigateToSubscription
                )
            }
            if (uiState.drafts.isEmpty()) {
                item {
                    EmptyDraftsCard()
                }
            } else {
                items(uiState.drafts, key = { it.id }) { draft ->
                    DraftCard(
                        draft = draft,
                        onReview = {
                            onNavigateToReviewDraft(
                                draft.id,
                                (draft.suggestedType ?: TransactionType.EXPENSE).name
                            )
                        },
                        onReviewAsTransfer = {
                            onNavigateToReviewDraft(draft.id, TransactionType.TRANSFER.name)
                        },
                        onSaveAsExpense = { viewModel.saveAsExpense(draft.id) },
                        onSaveAsIncome = { viewModel.saveAsIncome(draft.id) },
                        onSaveAsReimbursement = { viewModel.saveAsReimbursement(draft.id) },
                        onMarkPassThrough = { viewModel.markPassThrough(draft.id) },
                        onMarkExcluded = { viewModel.markExcluded(draft.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    count: Int,
    autoCaptureEnabled: Boolean,
    hasPremiumAccess: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_capture_draft_inbox_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    !hasPremiumAccess -> stringResource(R.string.auto_capture_premium_required)
                    !autoCaptureEnabled -> stringResource(R.string.auto_capture_master_toggle_required)
                    count == 0 -> stringResource(R.string.auto_capture_empty_hint)
                    else -> stringResource(R.string.auto_capture_pending_count, count)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CaptureActionsCard(
    uiState: DraftInboxUiState,
    onImportAccountChanged: (String?) -> Unit,
    onImportStatement: () -> Unit,
    onPickImage: () -> Unit,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_capture_capture_tools_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.auto_capture_capture_tools_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AccountPicker(
                label = stringResource(R.string.auto_capture_import_account_label),
                accounts = uiState.accounts,
                selectedAccountId = uiState.selectedImportAccountId,
                onSelected = onImportAccountChanged,
                placeholder = stringResource(R.string.settings_select_account)
            )
            if (!uiState.hasPremiumAccess) {
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = BlackBackground
                    )
                ) {
                    Text(stringResource(R.string.auto_capture_upgrade_button))
                }
            } else {
                OutlinedButton(
                    onClick = onImportStatement,
                    enabled = uiState.autoCaptureEnabled && uiState.statementImportEnabled && !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.auto_capture_import_statement_button))
                }
                OutlinedButton(
                    onClick = onPickImage,
                    enabled = uiState.autoCaptureEnabled && uiState.ocrEnabled && !uiState.isWorking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.auto_capture_scan_receipt_button))
                }
                Text(
                    text = buildCaptureActionsFootnote(context = LocalContext.current, uiState = uiState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyDraftsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(R.string.auto_capture_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.auto_capture_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DraftCard(
    draft: AutoCaptureDraft,
    onReview: () -> Unit,
    onReviewAsTransfer: () -> Unit,
    onSaveAsExpense: () -> Unit,
    onSaveAsIncome: () -> Unit,
    onSaveAsReimbursement: () -> Unit,
    onMarkPassThrough: () -> Unit,
    onMarkExcluded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    draft.amountCents?.let { amount ->
                        Text(
                            text = amount.toCurrencyString(draft.currency),
                            style = MaterialTheme.typography.titleSmall,
                            color = when (draft.suggestedType) {
                                TransactionType.INCOME -> SuccessGreen
                                TransactionType.TRANSFER -> InfoBlue
                                else -> WarningOrange
                            }
                        )
                    }
                    Text(
                        text = draft.occurredAt.toFormattedDateTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DraftPill(label = draft.source.toDisplayLabel(), color = sourceColor(draft.source))
                    DraftPill(label = draft.status.toDisplayLabel(), color = statusColor(draft.status))
                }
            }

            if (!draft.notes.isNullOrBlank()) {
                Text(
                    text = draft.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!draft.rawText.isNullOrBlank()) {
                Text(
                    text = draft.rawText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onReview,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = BlackBackground
                    )
                ) {
                    Text(stringResource(R.string.auto_capture_review_button))
                }
                OutlinedButton(
                    onClick = onSaveAsExpense,
                    modifier = Modifier.weight(1f),
                    enabled = draft.amountCents != null
                ) {
                    Text(stringResource(R.string.auto_capture_save_expense_button))
                }
                OutlinedButton(
                    onClick = onSaveAsIncome,
                    modifier = Modifier.weight(1f),
                    enabled = draft.amountCents != null
                ) {
                    Text(stringResource(R.string.auto_capture_save_income_button))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveAsReimbursement,
                    modifier = Modifier.weight(1f),
                    enabled = draft.amountCents != null
                ) {
                    Text(stringResource(R.string.auto_capture_reimbursement_button))
                }
                OutlinedButton(
                    onClick = onReviewAsTransfer,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.auto_capture_transfer_button))
                }
                OutlinedButton(
                    onClick = onMarkPassThrough,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.auto_capture_pass_through_button))
                }
                OutlinedButton(
                    onClick = onMarkExcluded,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.auto_capture_exclude_button))
                }
            }
        }
    }
}

@Composable
private fun DraftPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .height(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f)),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

private fun AutoCaptureSource.toDisplayLabel(): String = localizedAutoCaptureSourceLabel(this)

private fun AutoCaptureDraftStatus.toDisplayLabel(): String = localizedAutoCaptureStatusLabel(this)

private fun sourceColor(source: AutoCaptureSource): Color = when (source) {
    AutoCaptureSource.STATEMENT_IMPORT -> InfoBlue
    AutoCaptureSource.OCR -> YellowPrimary
    AutoCaptureSource.NOTIFICATION -> SuccessGreen
}

private fun statusColor(status: AutoCaptureDraftStatus): Color = when (status) {
    AutoCaptureDraftStatus.PENDING -> SuccessGreen
    AutoCaptureDraftStatus.NEEDS_REVIEW -> WarningOrange
    AutoCaptureDraftStatus.RESOLVED -> Color.Gray
}

@Composable
private fun buildCaptureActionsFootnote(
    context: Context,
    uiState: DraftInboxUiState
): String {
    return buildString {
        if (!uiState.autoCaptureEnabled) {
            append(context.getString(R.string.auto_capture_master_toggle_required))
        } else {
            append(context.getString(R.string.auto_capture_capture_tools_ready))
        }
        if (!uiState.statementImportEnabled || !uiState.ocrEnabled) {
            append(" ")
            append(context.getString(R.string.auto_capture_enable_more_tools_hint))
        }
        if (!uiState.notificationCaptureEnabled) {
            append(" ")
            append(context.getString(R.string.auto_capture_notification_tools_hint))
        }
    }.trim()
}
