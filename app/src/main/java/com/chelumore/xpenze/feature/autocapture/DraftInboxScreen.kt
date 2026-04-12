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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.autocapture.NotificationCaptureListenerService
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.localization.localizedAutoCaptureManageSetupBody
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAccessMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationToolsHint
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSourceLabel
import com.chelumore.xpenze.core.localization.localizedAutoCaptureStatusLabel
import com.chelumore.xpenze.core.ui.components.input.AccountPicker
import com.chelumore.xpenze.core.ui.components.layout.ConfirmationDialog
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.core.ui.util.hasNotificationListenerAccess
import com.chelumore.xpenze.core.ui.util.isXiaomiFamilyDevice
import com.chelumore.xpenze.core.ui.util.openNotificationListenerSettings
import com.chelumore.xpenze.core.ui.util.openXiaomiAutoStartSettings
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
    onNavigateToAutoCaptureSetup: () -> Unit,
    viewModel: DraftInboxViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val installedNotificationApps = rememberNotificationCaptureAppOptions()
    val autoCaptureNotificationEnabled by rememberUpdatedState(uiState.notificationCaptureEnabled)
    var showAutoCaptureDisclosureDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationListenerDialog by remember { mutableStateOf(false) }
    var showNotificationAppPicker by remember { mutableStateOf(false) }
    var pendingNotificationListenerEnable by rememberSaveable { mutableStateOf(false) }
    var hasNotificationListenerAccess by remember { mutableStateOf(context.hasNotificationListenerAccess()) }
    val showXiaomiAutoStartCard = remember(
        uiState.autoCaptureEnabled,
        uiState.notificationCaptureEnabled,
        hasNotificationListenerAccess
    ) {
        context.isXiaomiFamilyDevice() &&
            uiState.autoCaptureEnabled &&
            uiState.notificationCaptureEnabled &&
            hasNotificationListenerAccess
    }

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

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val access = context.hasNotificationListenerAccess()
                hasNotificationListenerAccess = access
                if (pendingNotificationListenerEnable) {
                    pendingNotificationListenerEnable = false
                    viewModel.toggleAutoCaptureNotification(access)
                    if (access) {
                        NotificationCaptureListenerService.requestRebindIfAllowed(context)
                    }
                } else if (!access && autoCaptureNotificationEnabled) {
                    viewModel.toggleAutoCaptureNotification(false)
                } else if (access && autoCaptureNotificationEnabled) {
                    NotificationCaptureListenerService.requestRebindIfAllowed(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                        isWorking = uiState.isWorking,
                        onReview = {
                            onNavigateToReviewDraft(
                                draft.id,
                                (draft.suggestedType ?: TransactionType.EXPENSE).name
                            )
                        },
                        onSaveAsExpense = { viewModel.saveAsExpense(draft.id) },
                        onSaveAsIncome = { viewModel.saveAsIncome(draft.id) },
                        onMarkExcluded = { viewModel.markExcluded(draft.id) }
                    )
                }
            }
            item {
                AutomaticSetupCard(
                    uiState = uiState,
                    hasNotificationListenerAccess = hasNotificationListenerAccess,
                    installedNotificationApps = installedNotificationApps,
                    onOpenSetup = onNavigateToAutoCaptureSetup,
                    onUpgrade = onNavigateToSubscription,
                    onMasterToggle = { enabled ->
                        if (enabled && !uiState.autoCaptureDisclosureAccepted) {
                            showAutoCaptureDisclosureDialog = true
                        } else {
                            viewModel.toggleAutoCapture(enabled)
                        }
                    },
                    onStatementToggle = viewModel::toggleAutoCaptureStatementImport,
                    onOcrToggle = viewModel::toggleAutoCaptureOcr,
                    onNotificationToggle = { enabled ->
                        if (!enabled) {
                            pendingNotificationListenerEnable = false
                            viewModel.toggleAutoCaptureNotification(false)
                        } else if (!hasNotificationListenerAccess) {
                            showNotificationListenerDialog = true
                        } else {
                            viewModel.toggleAutoCaptureNotification(true)
                            NotificationCaptureListenerService.requestRebindIfAllowed(context)
                        }
                    },
                    onUseGeneralNotifications = {
                        viewModel.setAutoCaptureNotificationAllowedPackages(emptySet())
                    },
                    onUseSpecificNotifications = { showNotificationAppPicker = true },
                    onMerchantToggle = viewModel::toggleAutoCaptureMerchantMemory,
                    onRecurringToggle = viewModel::toggleAutoCaptureRecurringSuggestions,
                    onChooseNotificationApps = { showNotificationAppPicker = true },
                    onClearNotificationApps = {
                        viewModel.setAutoCaptureNotificationAllowedPackages(emptySet())
                    },
                    showXiaomiAutoStartCard = showXiaomiAutoStartCard,
                    onOpenXiaomiAutoStartSettings = { context.openXiaomiAutoStartSettings() }
                )
            }
        }
    }

    if (showAutoCaptureDisclosureDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.auto_capture_disclosure_title),
            message = stringResource(R.string.auto_capture_disclosure_message),
            confirmText = stringResource(R.string.auto_capture_disclosure_confirm),
            dismissText = stringResource(R.string.common_cancel),
            onConfirm = {
                showAutoCaptureDisclosureDialog = false
                viewModel.setAutoCaptureDisclosureAccepted()
                viewModel.toggleAutoCapture(true)
            },
            onDismiss = { showAutoCaptureDisclosureDialog = false }
        )
    }

    if (showNotificationListenerDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.auto_capture_notification_access_title),
            message = localizedAutoCaptureNotificationAccessMessage(uiState.appLanguageTag),
            confirmText = stringResource(R.string.auto_capture_notification_access_confirm),
            dismissText = stringResource(R.string.common_cancel),
            onConfirm = {
                showNotificationListenerDialog = false
                pendingNotificationListenerEnable = true
                context.openNotificationListenerSettings()
            },
            onDismiss = { showNotificationListenerDialog = false }
        )
    }

    if (showNotificationAppPicker) {
        NotificationCaptureAppSelectionSheet(
            languageTag = uiState.appLanguageTag,
            installedApps = installedNotificationApps,
            initialSelection = uiState.notificationAllowedPackages,
            onDismiss = { showNotificationAppPicker = false },
            onSaveSelection = { packageNames ->
                viewModel.setAutoCaptureNotificationAllowedPackages(packageNames)
                showNotificationAppPicker = false
            }
        )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.auto_capture_draft_inbox_heading),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                DraftPill(
                    label = count.toString(),
                    color = if (count > 0) WarningOrange else SuccessGreen
                )
            }
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
private fun AutomaticSetupCard(
    uiState: DraftInboxUiState,
    hasNotificationListenerAccess: Boolean,
    installedNotificationApps: List<NotificationCaptureAppOption>,
    onOpenSetup: () -> Unit,
    onUpgrade: () -> Unit,
    onMasterToggle: (Boolean) -> Unit,
    onStatementToggle: (Boolean) -> Unit,
    onOcrToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onUseGeneralNotifications: () -> Unit,
    onUseSpecificNotifications: () -> Unit,
    onMerchantToggle: (Boolean) -> Unit,
    onRecurringToggle: (Boolean) -> Unit,
    onChooseNotificationApps: () -> Unit,
    onClearNotificationApps: () -> Unit,
    showXiaomiAutoStartCard: Boolean,
    onOpenXiaomiAutoStartSettings: () -> Unit
) {
    val isSpecificNotificationMode = uiState.notificationAllowedPackages.isNotEmpty()

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
                text = stringResource(R.string.auto_capture_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (uiState.hasPremiumAccess) {
                    localizedAutoCaptureManageSetupBody()
                } else {
                    stringResource(R.string.auto_capture_settings_premium_subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                AutoCaptureSettingRow(
                    label = stringResource(R.string.auto_capture_master_label),
                    subtitle = stringResource(R.string.auto_capture_master_subtitle),
                    checked = uiState.autoCaptureEnabled,
                    enabled = true,
                    onCheckedChange = onMasterToggle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AutoCaptureSettingRow(
                    label = stringResource(R.string.auto_capture_statement_toggle),
                    subtitle = stringResource(R.string.auto_capture_statement_subtitle),
                    checked = uiState.statementImportEnabled,
                    enabled = uiState.autoCaptureEnabled,
                    onCheckedChange = onStatementToggle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AutoCaptureSettingRow(
                    label = stringResource(R.string.auto_capture_ocr_toggle),
                    subtitle = stringResource(R.string.auto_capture_ocr_subtitle),
                    checked = uiState.ocrEnabled,
                    enabled = uiState.autoCaptureEnabled,
                    onCheckedChange = onOcrToggle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AutoCaptureSettingRow(
                    label = stringResource(R.string.auto_capture_notification_toggle),
                    subtitle = if (hasNotificationListenerAccess) {
                        stringResource(R.string.auto_capture_notification_subtitle_on)
                    } else {
                        stringResource(R.string.auto_capture_notification_subtitle_off)
                    },
                    checked = uiState.notificationCaptureEnabled && hasNotificationListenerAccess,
                    enabled = uiState.autoCaptureEnabled,
                    onCheckedChange = onNotificationToggle
                )
                NotificationCaptureModeSelector(
                    isSpecificMode = isSpecificNotificationMode,
                    enabled = uiState.autoCaptureEnabled && uiState.notificationCaptureEnabled,
                    onUseGeneral = onUseGeneralNotifications,
                    onUseSpecific = onUseSpecificNotifications
                )
                NotificationCaptureAppFilterCard(
                    languageTag = uiState.appLanguageTag,
                    selectedPackages = uiState.notificationAllowedPackages,
                    installedApps = installedNotificationApps,
                    enabled = uiState.autoCaptureEnabled &&
                        uiState.notificationCaptureEnabled &&
                        isSpecificNotificationMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    onChooseApps = onChooseNotificationApps,
                    onClearSelection = onClearNotificationApps
                )
                if (showXiaomiAutoStartCard) {
                    NotificationCaptureAutoStartCard(
                        modifier = Modifier.padding(top = 4.dp),
                        onOpenSettings = onOpenXiaomiAutoStartSettings
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AutoCaptureSettingRow(
                    label = stringResource(R.string.auto_capture_merchant_toggle),
                    subtitle = stringResource(R.string.auto_capture_merchant_subtitle),
                    checked = uiState.merchantMemoryEnabled,
                    enabled = uiState.autoCaptureEnabled,
                    onCheckedChange = onMerchantToggle
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                AutoCaptureSettingRow(
                    label = stringResource(R.string.auto_capture_recurring_toggle),
                    subtitle = stringResource(R.string.auto_capture_recurring_subtitle),
                    checked = uiState.recurringSuggestionsEnabled,
                    enabled = uiState.autoCaptureEnabled,
                    onCheckedChange = onRecurringToggle
                )
            }
            OutlinedButton(
                onClick = onOpenSetup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.auto_capture_guide_button))
            }
        }
    }
}

@Composable
private fun AutoCaptureSettingRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BlackBackground,
                checkedTrackColor = YellowPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun NotificationCaptureModeSelector(
    isSpecificMode: Boolean,
    enabled: Boolean,
    onUseGeneral: () -> Unit,
    onUseSpecific: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.auto_capture_notification_mode_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isSpecificMode) {
                Button(
                    onClick = onUseGeneral,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = BlackBackground
                    )
                ) {
                    Text(
                        text = stringResource(R.string.auto_capture_notification_mode_general),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onUseGeneral,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.auto_capture_notification_mode_general),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isSpecificMode) {
                Button(
                    onClick = onUseSpecific,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = BlackBackground
                    )
                ) {
                    Text(
                        text = stringResource(R.string.auto_capture_notification_mode_specific),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onUseSpecific,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.auto_capture_notification_mode_specific),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Text(
            text = if (isSpecificMode) {
                stringResource(R.string.auto_capture_notification_mode_specific_hint)
            } else {
                stringResource(R.string.auto_capture_notification_mode_general_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    isWorking: Boolean,
    onReview: () -> Unit,
    onSaveAsExpense: () -> Unit,
    onSaveAsIncome: () -> Unit,
    onMarkExcluded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        val previewText = draft.notes?.takeIf { it.isNotBlank() } ?: draft.rawText?.takeIf { it.isNotBlank() }

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = draft.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    draft.amountCents?.let { amount ->
                        Text(
                            text = amount.toCurrencyString(draft.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
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

            if (!draft.needsReviewReason.isNullOrBlank()) {
                DraftSupportingTextCard(
                    text = draft.needsReviewReason,
                    accent = WarningOrange
                )
            }

            if (!previewText.isNullOrBlank()) {
                DraftSupportingTextCard(
                    text = previewText,
                    accent = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryDraftActionButton(
                    onClick = onReview,
                    enabled = !isWorking,
                    label = stringResource(R.string.auto_capture_review_button),
                    modifier = Modifier.weight(1f)
                )
                SecondaryDraftActionButton(
                    onClick = onSaveAsExpense,
                    enabled = !isWorking && draft.amountCents != null,
                    label = stringResource(R.string.auto_capture_save_expense_button),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryDraftActionButton(
                    onClick = onSaveAsIncome,
                    enabled = !isWorking && draft.amountCents != null,
                    label = stringResource(R.string.auto_capture_save_income_button),
                    modifier = Modifier.weight(1f)
                )
                SecondaryDraftActionButton(
                    onClick = onMarkExcluded,
                    enabled = !isWorking,
                    label = stringResource(R.string.auto_capture_exclude_button),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PrimaryDraftActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    label: String
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = YellowPrimary,
            contentColor = BlackBackground
        )
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SecondaryDraftActionButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    label: String
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DraftSupportingTextCard(
    text: String,
    accent: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.10f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DraftPill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 28.dp),
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
                color = color,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
            append(localizedAutoCaptureNotificationToolsHint())
        }
    }.trim()
}
