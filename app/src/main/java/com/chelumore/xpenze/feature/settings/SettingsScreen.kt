package com.chelumore.xpenze.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toFormattedDateTime
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAccessMessage
import com.chelumore.xpenze.core.ui.components.input.AccountPicker
import com.chelumore.xpenze.core.ui.components.input.CurrencyPicker
import com.chelumore.xpenze.core.ui.components.input.DropdownItem
import com.chelumore.xpenze.core.ui.components.input.DropdownSelector
import com.chelumore.xpenze.core.ui.components.input.ExpenseTextField
import com.chelumore.xpenze.core.ui.components.layout.ConfirmationDialog
import com.chelumore.xpenze.core.ui.util.hasNotificationListenerAccess
import com.chelumore.xpenze.core.ui.util.openNotificationListenerSettings
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.feature.autocapture.AutoCaptureTutorialDialog
import com.chelumore.xpenze.feature.autocapture.NotificationCaptureAppFilterCard
import com.chelumore.xpenze.feature.autocapture.NotificationCaptureAppSelectionSheet
import com.chelumore.xpenze.feature.autocapture.rememberNotificationCaptureAppOptions
import com.chelumore.xpenze.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToExportData: () -> Unit = {},
    onNavigateToDraftInbox: () -> Unit = {},
    onNavigateToDraftInboxAppPicker: () -> Unit = {},
    onNavigateToAutoCaptureSetup: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToRecurringRules: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onSignedOut: () -> Unit = {},
    onNavigateToSignIn: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var confirmClearData by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmContact by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showNotificationAppPicker by remember { mutableStateOf(false) }
    var showAutoCaptureTutorial by rememberSaveable { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var notificationPermissionTarget by remember { mutableStateOf<NotificationPermissionTarget?>(null) }
    var showNotificationListenerDialog by remember { mutableStateOf(false) }
    var pendingNotificationListenerEnable by rememberSaveable { mutableStateOf(false) }
    var hasNotificationListenerAccess by remember { mutableStateOf(context.hasNotificationListenerAccess()) }
    val installedNotificationApps = rememberNotificationCaptureAppOptions()
    val lifecycleOwner = LocalLifecycleOwner.current
    val autoCaptureNotificationEnabled by rememberUpdatedState(uiState.autoCaptureNotificationEnabled)

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val access = context.hasNotificationListenerAccess()
                hasNotificationListenerAccess = access
                if (pendingNotificationListenerEnable) {
                    pendingNotificationListenerEnable = false
                    viewModel.toggleAutoCaptureNotification(access)
                } else if (!access && autoCaptureNotificationEnabled) {
                    viewModel.toggleAutoCaptureNotification(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pickProfilePhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.updateUserPhotoUrl(uri.toString())
    }

    val requestNotificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val target = notificationPermissionTarget
        notificationPermissionTarget = null
        if (granted) {
            when (target) {
                NotificationPermissionTarget.MASTER_TOGGLE -> viewModel.toggleNotifications(true)
                NotificationPermissionTarget.BUDGET_ALERTS -> viewModel.toggleBudgetAlerts(true)
                NotificationPermissionTarget.DAILY_REMINDER -> viewModel.toggleDailySummary(true)
                null -> viewModel.toggleNotifications(true)
            }
        } else {
            when (target) {
                NotificationPermissionTarget.MASTER_TOGGLE,
                null -> viewModel.toggleNotifications(false)
                NotificationPermissionTarget.BUDGET_ALERTS -> viewModel.toggleBudgetAlerts(false)
                NotificationPermissionTarget.DAILY_REMINDER -> viewModel.toggleDailySummary(false)
            }
            scope.launch {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.settings_notification_permission_denied)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsViewModel.SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
                SettingsViewModel.SettingsEvent.AccountDeleted -> onSignedOut()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold, fontSize = 28.sp) },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.settings_cd_help))
                    }
                    IconButton(
                        onClick = {
                            // Open Android notification settings for this app.
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.settings_cd_notifications))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Profile Card
            item {
                ProfileCard(
                    state = uiState,
                    onSignOut = {
                        confirmSignOut = true
                    },
                    onSignIn = onNavigateToSignIn,
                    onEditProfile = {
                        editName = uiState.userName
                        showEditProfile = true
                    },
                    onChangePhoto = { pickProfilePhoto.launch(arrayOf("image/*")) }
                )
            }

            // Preferences
            item {
                SettingsSection(title = stringResource(R.string.settings_section_preferences)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DropdownSelector(
                            label = stringResource(R.string.settings_app_language),
                            items = appLanguageItems(),
                            selectedId = AppLanguages.normalizeTag(uiState.appLanguageTag),
                            onSelected = { tag ->
                                viewModel.setAppLanguage(tag ?: AppLanguages.FALLBACK_LANGUAGE_TAG)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            allowClearSelection = false
                        )
                        DropdownSelector(
                            label = stringResource(R.string.settings_account_card_style),
                            items = accountCardStyleItems(),
                            selectedId = uiState.accountCardStyle.name,
                            onSelected = { selectedId ->
                                val style = selectedId
                                    ?.let { id -> runCatching { AccountCardStyle.valueOf(id) }.getOrNull() }
                                    ?: AccountCardStyle.MATTE
                                viewModel.setAccountCardStyle(style)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            allowClearSelection = false
                        )
                        CurrencyPicker(
                            label = stringResource(R.string.settings_default_currency),
                            selectedCurrencyCode = uiState.currency,
                            onSelected = viewModel::setDefaultCurrency,
                            modifier = Modifier.fillMaxWidth()
                        )
                        AccountPicker(
                            label = stringResource(R.string.settings_default_account),
                            accounts = uiState.accounts,
                            selectedAccountId = uiState.defaultAccountId,
                            onSelected = viewModel::setDefaultAccount,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = stringResource(R.string.settings_select_account),
                            allowClearSelection = false
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ThemeSelector(
                        selectedTheme = uiState.themeMode,
                        onThemeSelected = viewModel::setTheme
                    )
                }
            }

            // Notifications
            item {
                SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                    SwitchSettingsItem(
                        label = stringResource(R.string.settings_enable_notifications),
                        subtitle = stringResource(R.string.settings_enable_notifications_subtitle),
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.toggleNotifications(false)
                                return@SwitchSettingsItem
                            }

                            // Android 13+ requires runtime permission for notifications.
                            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

                            if (needsPermission) {
                                notificationPermissionTarget = NotificationPermissionTarget.MASTER_TOGGLE
                                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleNotifications(true)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SwitchSettingsItem(
                        label = stringResource(R.string.settings_budget_alerts),
                        subtitle = stringResource(R.string.settings_budget_alerts_subtitle),
                        checked = uiState.budgetAlertsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.toggleBudgetAlerts(false)
                                return@SwitchSettingsItem
                            }

                            val needsPermission = uiState.notificationsEnabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

                            if (needsPermission) {
                                notificationPermissionTarget = NotificationPermissionTarget.BUDGET_ALERTS
                                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleBudgetAlerts(true)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SwitchSettingsItem(
                        label = stringResource(R.string.settings_daily_summary),
                        subtitle = stringResource(R.string.settings_daily_summary_subtitle),
                        checked = uiState.dailySummaryEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.toggleDailySummary(false)
                                return@SwitchSettingsItem
                            }

                            val needsPermission = uiState.notificationsEnabled &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

                            if (needsPermission) {
                                notificationPermissionTarget = NotificationPermissionTarget.DAILY_REMINDER
                                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleDailySummary(true)
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.auto_capture_settings_section)) {
                    if (!uiState.hasPremiumAccess) {
                        IconSettingsItem(
                            icon = Icons.Default.AutoAwesome,
                            label = stringResource(R.string.auto_capture_settings_title),
                            subtitle = stringResource(R.string.auto_capture_settings_premium_subtitle),
                            iconColor = YellowPrimary,
                            onClick = onNavigateToAutoCaptureSetup
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        IconSettingsItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            label = stringResource(R.string.auto_capture_tutorial_replay_label),
                            subtitle = stringResource(R.string.auto_capture_tutorial_replay_subtitle),
                            iconColor = YellowPrimary,
                            onClick = { showAutoCaptureTutorial = true }
                        )
                    } else {
                        SwitchSettingsItem(
                            label = stringResource(R.string.auto_capture_master_label),
                            subtitle = stringResource(R.string.auto_capture_master_subtitle),
                            checked = uiState.autoCaptureEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !uiState.autoCaptureDisclosureAccepted) {
                                    onNavigateToAutoCaptureSetup()
                                } else {
                                    viewModel.toggleAutoCapture(enabled)
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SwitchSettingsItem(
                            label = stringResource(R.string.auto_capture_statement_toggle),
                            subtitle = stringResource(R.string.auto_capture_statement_subtitle),
                            checked = uiState.autoCaptureStatementImportEnabled,
                            enabled = uiState.autoCaptureEnabled,
                            onCheckedChange = viewModel::toggleAutoCaptureStatementImport
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SwitchSettingsItem(
                            label = stringResource(R.string.auto_capture_ocr_toggle),
                            subtitle = stringResource(R.string.auto_capture_ocr_subtitle),
                            checked = uiState.autoCaptureOcrEnabled,
                            enabled = uiState.autoCaptureEnabled,
                            onCheckedChange = viewModel::toggleAutoCaptureOcr
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SwitchSettingsItem(
                            label = stringResource(R.string.auto_capture_notification_toggle),
                            subtitle = if (hasNotificationListenerAccess) {
                                stringResource(R.string.auto_capture_notification_subtitle_on)
                            } else {
                                stringResource(R.string.auto_capture_notification_subtitle_off)
                            },
                            checked = uiState.autoCaptureNotificationEnabled && hasNotificationListenerAccess,
                            enabled = uiState.autoCaptureEnabled,
                            onCheckedChange = { enabled ->
                                if (!enabled) {
                                    pendingNotificationListenerEnable = false
                                    viewModel.toggleAutoCaptureNotification(false)
                                } else if (!hasNotificationListenerAccess) {
                                    showNotificationListenerDialog = true
                                } else {
                                    viewModel.toggleAutoCaptureNotification(enabled)
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        NotificationCaptureAppFilterCard(
                            languageTag = uiState.appLanguageTag,
                            selectedPackages = uiState.autoCaptureNotificationAllowedPackages,
                            installedApps = installedNotificationApps,
                            enabled = uiState.autoCaptureEnabled,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            onChooseApps = {
                                if (uiState.isSignedIn && uiState.hasPremiumAccess) {
                                    showNotificationAppPicker = true
                                }
                            },
                            onClearSelection = {
                                if (uiState.isSignedIn && uiState.hasPremiumAccess) {
                                    viewModel.setAutoCaptureNotificationAllowedPackages(emptySet())
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SwitchSettingsItem(
                            label = stringResource(R.string.auto_capture_merchant_toggle),
                            subtitle = stringResource(R.string.auto_capture_merchant_subtitle),
                            checked = uiState.autoCaptureMerchantMemoryEnabled,
                            enabled = uiState.autoCaptureEnabled,
                            onCheckedChange = viewModel::toggleAutoCaptureMerchantMemory
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        SwitchSettingsItem(
                            label = stringResource(R.string.auto_capture_recurring_toggle),
                            subtitle = stringResource(R.string.auto_capture_recurring_subtitle),
                            checked = uiState.autoCaptureRecurringSuggestionsEnabled,
                            enabled = uiState.autoCaptureEnabled,
                            onCheckedChange = viewModel::toggleAutoCaptureRecurringSuggestions
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        IconSettingsItem(
                            icon = Icons.Default.Inventory2,
                            label = stringResource(R.string.auto_capture_draft_inbox_title),
                            subtitle = stringResource(R.string.auto_capture_draft_inbox_subtitle),
                            iconColor = InfoBlue,
                            onClick = onNavigateToDraftInbox
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        IconSettingsItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            label = stringResource(R.string.auto_capture_tutorial_replay_label),
                            subtitle = stringResource(R.string.auto_capture_tutorial_replay_subtitle),
                            iconColor = YellowPrimary,
                            onClick = { showAutoCaptureTutorial = true }
                        )
                    }
                }
            }

            // Data & Management
            item {
                SettingsSection(title = stringResource(R.string.settings_section_data)) {
                    IconSettingsItem(
                        icon = Icons.Default.AccountBalance,
                        label = stringResource(R.string.settings_accounts),
                        subtitle = stringResource(R.string.settings_accounts_subtitle),
                        iconColor = SuccessGreen,
                        onClick = onNavigateToAccounts
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    IconSettingsItem(
                        icon = Icons.Default.Category,
                        label = stringResource(R.string.settings_categories),
                        subtitle = stringResource(R.string.settings_categories_subtitle),
                        iconColor = InfoBlue,
                        onClick = onNavigateToCategories
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    IconSettingsItem(
                        icon = Icons.Outlined.Repeat,
                        label = stringResource(R.string.settings_recurring_rules),
                        subtitle = stringResource(R.string.settings_recurring_rules_subtitle),
                        iconColor = YellowPrimary,
                        onClick = onNavigateToRecurringRules
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    IconSettingsItem(
                        icon = Icons.Default.CloudSync,
                        label = stringResource(R.string.settings_cloud_sync_title),
                        subtitle = when {
                            uiState.isSyncing -> stringResource(R.string.common_syncing)
                            uiState.syncError != null -> stringResource(
                                R.string.settings_cloud_sync_error,
                                uiState.syncError.orEmpty()
                            )
                            uiState.lastSyncTimeMillis > 0L -> stringResource(
                                R.string.settings_cloud_sync_last,
                                uiState.lastSyncTimeMillis.toFormattedDateTime()
                            )
                            uiState.isSignedIn -> stringResource(R.string.settings_cloud_sync_idle)
                            else -> stringResource(R.string.settings_cloud_sync_signed_out)
                        },
                        iconColor = WarningOrange,
                        onClick = viewModel::syncNow
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    IconSettingsItem(
                        icon = Icons.Default.CloudUpload,
                        label = stringResource(R.string.settings_export_import),
                        subtitle = stringResource(R.string.settings_export_import_subtitle),
                        iconColor = InfoBlue,
                        onClick = onNavigateToExportData
                    )
                }
            }

            // Subscription
            item {
                SettingsSection(title = stringResource(R.string.settings_section_subscription)) {
                    SubscriptionCard(
                        isPremium = uiState.isPremium,
                        hasPremiumAccess = uiState.hasPremiumAccess,
                        isTrialActive = uiState.isTrialActive,
                        isTrialEndingSoon = uiState.isTrialEndingSoon,
                        trialDaysRemaining = uiState.trialDaysRemaining,
                        trialExpiresAt = uiState.trialExpiresAt,
                        onManage = onNavigateToSubscription
                    )
                }
            }

            // About
            item {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    SettingsItem(label = stringResource(R.string.settings_app_version), value = uiState.appVersion, showArrow = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsItem(label = stringResource(R.string.settings_privacy_policy), onClick = onNavigateToPrivacyPolicy, showArrow = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsItem(label = stringResource(R.string.settings_terms_of_service), onClick = onNavigateToTerms, showArrow = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsItem(label = stringResource(R.string.settings_open_source_licenses), onClick = onNavigateToLicenses, showArrow = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsItem(label = stringResource(R.string.settings_help_support), onClick = onNavigateToHelp, showArrow = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    SettingsItem(
                        label = stringResource(R.string.settings_contact_feedback),
                        onClick = { confirmContact = true },
                        showArrow = true
                    )
                }
            }

            // Danger Zone
            item {
                SettingsSection(title = stringResource(R.string.settings_section_danger)) {
                    TextButton(
                        onClick = { confirmClearData = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Text(stringResource(R.string.settings_clear_all_data))
                    }
                    if (uiState.isSignedIn) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        TextButton(
                            onClick = { confirmDeleteAccount = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                        ) {
                            Text(stringResource(R.string.settings_delete_account))
                        }
                    }
                }
            }
        }
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
            initialSelection = uiState.autoCaptureNotificationAllowedPackages,
            onDismiss = { showNotificationAppPicker = false },
            onSaveSelection = { packageNames ->
                viewModel.setAutoCaptureNotificationAllowedPackages(packageNames)
                showNotificationAppPicker = false
            }
        )
    }

    if (showAutoCaptureTutorial) {
        val canChooseBankApps = uiState.isSignedIn && uiState.hasPremiumAccess
        AutoCaptureTutorialDialog(
            canChooseBankApps = canChooseBankApps,
            onDismiss = { showAutoCaptureTutorial = false },
            onChooseBankApps = {
                showAutoCaptureTutorial = false
                if (canChooseBankApps) {
                    onNavigateToDraftInboxAppPicker()
                }
            }
        )
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text(stringResource(R.string.settings_dialog_clear_title)) },
            text = { Text(stringResource(R.string.settings_dialog_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearData = false
                        viewModel.clearAllData()
                    }
                ) { Text(stringResource(R.string.common_clear), color = ErrorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            title = { Text(stringResource(R.string.settings_dialog_delete_account_title)) },
            text = { Text(stringResource(R.string.settings_dialog_delete_account_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDeleteAccount = false
                        viewModel.deleteAccount()
                    }
                ) { Text(stringResource(R.string.common_delete), color = ErrorRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAccount = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.settings_dialog_sign_out_title)) },
            text = { Text(stringResource(R.string.settings_dialog_sign_out_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSignOut = false
                        viewModel.signOut()
                        onSignedOut()
                    }
                ) { Text(stringResource(R.string.common_sign_out), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (confirmContact) {
        ConfirmationDialog(
            title = stringResource(R.string.settings_dialog_contact_title),
            message = stringResource(R.string.settings_dialog_contact_message),
            confirmText = stringResource(R.string.settings_dialog_open_email),
            dismissText = stringResource(R.string.common_cancel),
            onConfirm = {
                confirmContact = false
                val subject = context.getString(R.string.settings_feedback_subject, uiState.appVersion)
                val body = context.getString(R.string.settings_feedback_body, uiState.appVersion)
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@xpenze.app")
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            },
            onDismiss = { confirmContact = false }
        )
    }

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text(stringResource(R.string.settings_dialog_edit_profile_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExpenseTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = stringResource(R.string.settings_name_label),
                        placeholder = stringResource(R.string.settings_name_placeholder),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.userPhotoUrl != null) {
                        TextButton(onClick = { viewModel.updateUserPhotoUrl(null) }) {
                            Text(stringResource(R.string.settings_remove_photo), color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editName.trim().isNotBlank(),
                    onClick = {
                        showEditProfile = false
                        viewModel.updateUserName(editName)
                    }
                ) {
                    Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    state: SettingsUiState,
    onSignOut: () -> Unit,
    onSignIn: () -> Unit,
    onEditProfile: () -> Unit,
    onChangePhoto: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    if (state.isPremium || state.hasFounderReward) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.isPremium) {
                                ProfileBadgeChip(
                                    text = stringResource(R.string.settings_premium_badge),
                                    containerColor = YellowPrimary.copy(alpha = 0.18f),
                                    contentColor = YellowPrimary
                                )
                            }
                            if (state.hasFounderReward) {
                                ProfileBadgeChip(
                                    text = stringResource(R.string.settings_founder_badge),
                                    containerColor = YellowPrimary.copy(alpha = 0.18f),
                                    contentColor = YellowPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = if (state.isSignedIn) {
                            state.userName
                        } else {
                            stringResource(R.string.settings_guest_backup_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.isSignedIn) {
                        Text(
                            text = state.userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        // Removing a photo lives in the Edit Profile dialog, which guests do not
                        // get, so don't let them set one they could never clear.
                        .then(
                            if (state.isSignedIn) {
                                Modifier.clickable { onChangePhoto() }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!state.userPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = state.userPhotoUrl,
                            contentDescription = stringResource(R.string.settings_cd_profile_photo),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = state.userName.firstOrNull()?.toString()
                                ?: stringResource(R.string.settings_profile_initial_fallback),
                            fontWeight = FontWeight.Bold,
                            color = YellowPrimary,
                            fontSize = 22.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state.isSignedIn) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onEditProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = BlackBackground),
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.settings_edit_profile), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        shape = RoundedCornerShape(100),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.common_sign_out), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_guest_backup_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = BlackBackground),
                    shape = RoundedCornerShape(100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_guest_sign_in), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileBadgeChip(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(100)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (showArrow) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BlackBackground,
                checkedTrackColor = YellowPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun ThemeSelector(selectedTheme: ThemeMode, onThemeSelected: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(
            modifier = Modifier
                .background(Color.Black, RoundedCornerShape(8.dp))
                .padding(2.dp)
        ) {
            ThemeOption(stringResource(R.string.settings_theme_system), selectedTheme == ThemeMode.SYSTEM) { onThemeSelected(ThemeMode.SYSTEM) }
            ThemeOption(stringResource(R.string.settings_theme_light), selectedTheme == ThemeMode.LIGHT) { onThemeSelected(ThemeMode.LIGHT) }
            ThemeOption(stringResource(R.string.settings_theme_dark), selectedTheme == ThemeMode.DARK) { onThemeSelected(ThemeMode.DARK) }
        }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconSettingsItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = BlackBackground)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SubscriptionCard(
    isPremium: Boolean,
    hasPremiumAccess: Boolean,
    isTrialActive: Boolean,
    isTrialEndingSoon: Boolean,
    trialDaysRemaining: Int,
    trialExpiresAt: Long,
    onManage: () -> Unit
) {
    val statusText = when {
        isPremium -> stringResource(R.string.settings_subscription_status_premium)
        isTrialEndingSoon -> stringResource(
            R.string.settings_subscription_status_trial_ending,
            trialDaysRemaining
        )
        isTrialActive -> stringResource(
            R.string.settings_subscription_status_trial,
            trialExpiresAt.toFormattedDateTime()
        )
        hasPremiumAccess -> stringResource(R.string.settings_subscription_status_access)
        else -> stringResource(R.string.settings_subscription_status_free)
    }
    val ctaText = when {
        isPremium -> stringResource(R.string.settings_manage_subscription)
        isTrialEndingSoon -> stringResource(R.string.settings_keep_premium)
        isTrialActive -> stringResource(R.string.settings_manage_subscription)
        else -> stringResource(R.string.settings_upgrade_to_premium)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    stringResource(R.string.settings_subscription_plan),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTrialEndingSoon) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.Star,
                contentDescription = stringResource(R.string.settings_cd_premium),
                tint = YellowPrimary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onManage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = BlackBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(ctaText, fontWeight = FontWeight.Bold)
        }
    }
}

private fun appLanguageItems(): List<DropdownItem> = AppLanguages.options.map { option ->
    DropdownItem(
        id = option.tag,
        label = option.label
    )
}

@Composable
private fun accountCardStyleItems(): List<DropdownItem> = listOf(
    DropdownItem(
        id = AccountCardStyle.MATTE.name,
        label = stringResource(R.string.settings_account_card_style_matte)
    ),
    DropdownItem(
        id = AccountCardStyle.METALLIC.name,
        label = stringResource(R.string.settings_account_card_style_metallic)
    ),
    DropdownItem(
        id = AccountCardStyle.NEON.name,
        label = stringResource(R.string.settings_account_card_style_neon)
    ),
    DropdownItem(
        id = AccountCardStyle.SUNSET.name,
        label = stringResource(R.string.settings_account_card_style_sunset)
    ),
    DropdownItem(
        id = AccountCardStyle.OCEAN.name,
        label = stringResource(R.string.settings_account_card_style_ocean)
    ),
    DropdownItem(
        id = AccountCardStyle.ROYAL.name,
        label = stringResource(R.string.settings_account_card_style_royal)
    )
)

private enum class NotificationPermissionTarget {
    MASTER_TOGGLE,
    BUDGET_ALERTS,
    DAILY_REMINDER
}

