package com.chelumore.xpenze.feature.autocapture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.autocapture.NotificationCaptureListenerService
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAccessMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationSetupPromptMessage
import com.chelumore.xpenze.core.localization.localizedAutoCaptureSetupCopy
import com.chelumore.xpenze.core.ui.components.layout.ConfirmationDialog
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.core.ui.util.hasNotificationListenerAccess
import com.chelumore.xpenze.core.ui.util.isXiaomiFamilyDevice
import com.chelumore.xpenze.core.ui.util.openNotificationListenerSettings
import com.chelumore.xpenze.core.ui.util.openXiaomiAutoStartSettings
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.WarningOrange
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun AutoCaptureSetupScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToDraftInbox: () -> Unit = {},
    viewModel: AutoCaptureSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showNotificationListenerDialog by remember { mutableStateOf(false) }
    var showNotificationAppPicker by remember { mutableStateOf(false) }
    var pendingNotificationListenerEnable by rememberSaveable { mutableStateOf(false) }
    var navigateToDraftInboxAfterNotificationPrompt by rememberSaveable { mutableStateOf(false) }
    var hasNotificationListenerAccess by remember { mutableStateOf(context.hasNotificationListenerAccess()) }
    val installedNotificationApps = rememberNotificationCaptureAppOptions()
    val lifecycleOwner = LocalLifecycleOwner.current
    val autoCaptureNotificationEnabled by rememberUpdatedState(uiState.autoCaptureNotificationEnabled)
    val showXiaomiAutoStartCard = remember(
        uiState.autoCaptureEnabled,
        uiState.autoCaptureNotificationEnabled,
        hasNotificationListenerAccess
    ) {
        context.isXiaomiFamilyDevice() &&
            uiState.autoCaptureEnabled &&
            uiState.autoCaptureNotificationEnabled &&
            hasNotificationListenerAccess
    }
    val copy = remember(uiState.appLanguageTag, uiState.hasPremiumAccess, uiState.autoCaptureEnabled) {
        localizedAutoCaptureSetupCopy(
            languageTag = uiState.appLanguageTag,
            hasPremiumAccess = uiState.hasPremiumAccess,
            autoCaptureEnabled = uiState.autoCaptureEnabled
        )
    }
    val notificationSetupBody = when {
        !uiState.hasPremiumAccess -> stringResource(R.string.auto_capture_settings_premium_subtitle)
        !uiState.autoCaptureEnabled -> localizedAutoCaptureNotificationSetupPromptMessage(uiState.appLanguageTag)
        hasNotificationListenerAccess -> stringResource(R.string.auto_capture_notification_subtitle_on)
        else -> stringResource(R.string.auto_capture_notification_subtitle_off)
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
                    if (navigateToDraftInboxAfterNotificationPrompt) {
                        navigateToDraftInboxAfterNotificationPrompt = false
                        onNavigateToDraftInbox()
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

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AutoCaptureSetupEvent.Enabled -> {
                    if (hasNotificationListenerAccess) {
                        viewModel.toggleAutoCaptureNotification(true)
                        onNavigateToDraftInbox()
                    } else {
                        navigateToDraftInboxAfterNotificationPrompt = true
                        showNotificationListenerDialog = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            XpenzeTopAppBar(
                title = copy.screenTitle,
                onNavigateBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = YellowPrimary.copy(alpha = 0.14f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(YellowPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = YellowPrimary
                            )
                        }
                        Text(
                            text = copy.heroTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = copy.heroBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AutoCaptureInfoSection(
                    title = copy.whatItDoesTitle,
                    items = copy.whatItDoesItems,
                    icon = Icons.Default.AutoAwesome,
                    accent = YellowPrimary
                )
                AutoCaptureInfoSection(
                    title = copy.whatItNeverDoesTitle,
                    items = copy.whatItNeverDoesItems,
                    icon = Icons.Default.Lock,
                    accent = WarningOrange
                )
                AutoCaptureInfoSection(
                    title = copy.mistakesTitle,
                    items = copy.mistakesItems,
                    icon = Icons.Default.CheckCircle,
                    accent = SuccessGreen
                )
                AutoCaptureInfoSection(
                    title = copy.permissionsTitle,
                    items = copy.permissionsItems,
                    icon = Icons.Default.Notifications,
                    accent = InfoBlue
                )
                NotificationCaptureSetupCard(
                    title = stringResource(R.string.auto_capture_notification_toggle),
                    subtitle = notificationSetupBody,
                    checked = uiState.autoCaptureEnabled &&
                        uiState.autoCaptureNotificationEnabled &&
                        hasNotificationListenerAccess,
                    enabled = uiState.hasPremiumAccess && uiState.autoCaptureEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            pendingNotificationListenerEnable = false
                            navigateToDraftInboxAfterNotificationPrompt = false
                            viewModel.toggleAutoCaptureNotification(false)
                        } else if (!hasNotificationListenerAccess) {
                            showNotificationListenerDialog = true
                        } else {
                            viewModel.toggleAutoCaptureNotification(true)
                            NotificationCaptureListenerService.requestRebindIfAllowed(context)
                        }
                    }
                )
                NotificationCaptureAppFilterCard(
                    languageTag = uiState.appLanguageTag,
                    selectedPackages = uiState.autoCaptureNotificationAllowedPackages,
                    installedApps = installedNotificationApps,
                    enabled = uiState.hasPremiumAccess && uiState.autoCaptureEnabled,
                    onChooseApps = { showNotificationAppPicker = true },
                    onClearSelection = {
                        viewModel.setAutoCaptureNotificationAllowedPackages(emptySet())
                    }
                )
                if (showXiaomiAutoStartCard) {
                    NotificationCaptureAutoStartCard(
                        onOpenSettings = { context.openXiaomiAutoStartSettings() }
                    )
                }
                AutoCaptureInfoSection(
                    title = copy.turnOnNowTitle,
                    items = copy.turnOnNowItems,
                    icon = Icons.Default.Settings,
                    accent = YellowPrimary
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                when {
                                    !uiState.hasPremiumAccess -> onNavigateToSubscription()
                                    uiState.autoCaptureEnabled -> onNavigateToDraftInbox()
                                    else -> viewModel.enableAutomaticMode()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YellowPrimary,
                                contentColor = BlackBackground
                            )
                        ) {
                            Text(
                                text = copy.primaryCta,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = copy.secondaryCta,
                                fontWeight = FontWeight.SemiBold
                            )
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
            onDismiss = {
                showNotificationListenerDialog = false
                if (navigateToDraftInboxAfterNotificationPrompt) {
                    navigateToDraftInboxAfterNotificationPrompt = false
                    onNavigateToDraftInbox()
                }
            }
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
}

@Composable
private fun NotificationCaptureSetupCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(InfoBlue.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = InfoBlue
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
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
}

@Composable
private fun AutoCaptureInfoSection(
    title: String,
    items: List<String>,
    icon: ImageVector,
    accent: Color
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { item ->
                    AutoCaptureBulletRow(item)
                }
            }
        }
    }
}

@Composable
private fun AutoCaptureBulletRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(YellowPrimary, CircleShape)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
