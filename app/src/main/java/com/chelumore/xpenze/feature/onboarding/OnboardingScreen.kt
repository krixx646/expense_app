package com.chelumore.xpenze.feature.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.autocapture.NotificationCaptureListenerService
import com.chelumore.xpenze.core.ui.components.input.AmountInputField
import com.chelumore.xpenze.core.ui.components.input.CurrencyPicker
import com.chelumore.xpenze.core.ui.components.input.DropdownItem
import com.chelumore.xpenze.core.ui.components.input.DropdownSelector
import com.chelumore.xpenze.core.ui.components.input.ExpenseTextField
import com.chelumore.xpenze.core.ui.components.input.LanguagePicker
import com.chelumore.xpenze.core.ui.util.SystemBars
import com.chelumore.xpenze.core.ui.util.hasNotificationListenerAccess
import com.chelumore.xpenze.core.ui.util.hasNotificationsPermissionEnabled
import com.chelumore.xpenze.core.ui.util.openAppNotificationSettings
import com.chelumore.xpenze.core.ui.util.openNotificationListenerSettings
import com.chelumore.xpenze.domain.model.AccountType
import com.chelumore.xpenze.domain.model.BudgetPeriod
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.DarkSurface
import com.chelumore.xpenze.ui.theme.XpenzeTheme
import com.chelumore.xpenze.ui.theme.YellowPrimary

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    XpenzeTheme(darkTheme = true, dynamicColor = false) {
        val context = LocalContext.current
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var showNotificationPermissionPrompt by remember { mutableStateOf(false) }
        val hasNotificationPermission = context.hasNotificationsPermissionEnabled()

        val requestNotificationPermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            showNotificationPermissionPrompt = false
            if (!granted) {
                context.openAppNotificationSettings()
            }
        }

        SystemBars(color = BlackBackground, darkIcons = false)

        LaunchedEffect(uiState.completed) {
            if (uiState.completed) {
                viewModel.consumeCompleted()
                onFinished()
            }
        }

        LaunchedEffect(uiState.step, hasNotificationPermission) {
            if (uiState.step == OnboardingStep.DONE && !hasNotificationPermission) {
                showNotificationPermissionPrompt = true
                viewModel.markNotificationPermissionPromptShown()
            }
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, uiState.step) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME &&
                    uiState.step == OnboardingStep.AUTO_CAPTURE
                ) {
                    val granted = context.hasNotificationListenerAccess()
                    viewModel.refreshNotificationListenerAccess(granted)
                    if (granted) {
                        NotificationCaptureListenerService.requestRebindIfAllowed(context)
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(uiState.step) {
            if (uiState.step == OnboardingStep.AUTO_CAPTURE) {
                viewModel.refreshNotificationListenerAccess(context.hasNotificationListenerAccess())
            }
        }

        val canGoBack = uiState.step != OnboardingStep.INTRO && !uiState.isSaving
        val canContinue = when (uiState.step) {
            OnboardingStep.INTRO -> true
            OnboardingStep.LANGUAGE -> uiState.appLanguageTag.isNotBlank()
            OnboardingStep.CURRENCY -> uiState.currencyCode.isNotBlank()
            OnboardingStep.ACCOUNT -> true
            OnboardingStep.BUDGET -> true
            OnboardingStep.AUTO_CAPTURE -> true
            OnboardingStep.DONE -> !uiState.isSaving
        } && !uiState.isSaving

        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (canGoBack) viewModel.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = if (canGoBack) Color.White else Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(R.string.onboarding_setup_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = stepLabel(uiState.step),
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            },
            containerColor = BlackBackground,
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(vertical = 16.dp)
                    ) {
                        if (!uiState.error.isNullOrBlank()) {
                            Text(
                                text = uiState.error.orEmpty(),
                                color = Color(0xFFFF6B6B),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Button(
                            onClick = {
                                when (uiState.step) {
                                    OnboardingStep.DONE -> viewModel.completeOnboarding()
                                    else -> viewModel.goNext()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = canContinue,
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YellowPrimary,
                                contentColor = BlackBackground,
                                disabledContainerColor = YellowPrimary.copy(alpha = 0.35f),
                                disabledContentColor = BlackBackground.copy(alpha = 0.7f)
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = BlackBackground,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = when (uiState.step) {
                                        OnboardingStep.DONE -> stringResource(R.string.common_finish)
                                        else -> stringResource(R.string.common_continue)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    when (uiState.step) {
                        OnboardingStep.INTRO -> IntroStep()
                        OnboardingStep.LANGUAGE -> LanguageStep(
                            selectedLanguageTag = uiState.appLanguageTag,
                            onSelected = viewModel::setAppLanguage
                        )
                        OnboardingStep.CURRENCY -> CurrencyStep(
                            currency = uiState.currencyCode,
                            onSelected = viewModel::setCurrency
                        )
                        OnboardingStep.ACCOUNT -> AccountStep(
                            currency = uiState.currencyCode,
                            accountName = uiState.accountName,
                            accountType = uiState.accountType,
                            initialBalanceText = uiState.initialBalanceText,
                            onAccountName = viewModel::setAccountName,
                            onAccountType = viewModel::setAccountType,
                            onInitialBalance = viewModel::setInitialBalance
                        )
                        OnboardingStep.BUDGET -> BudgetStep(
                            currency = uiState.currencyCode,
                            addBudget = uiState.addBudget,
                            budgetName = uiState.budgetName,
                            budgetAmountText = uiState.budgetAmountText,
                            budgetPeriod = uiState.budgetPeriod,
                            onAddBudget = viewModel::setAddBudget,
                            onBudgetName = viewModel::setBudgetName,
                            onBudgetAmount = viewModel::setBudgetAmount,
                            onBudgetPeriod = viewModel::setBudgetPeriod
                        )
                        OnboardingStep.AUTO_CAPTURE -> AutoCaptureStep(
                            uiState = uiState,
                            onEnableCapture = {
                                context.openNotificationListenerSettings()
                            }
                        )
                        OnboardingStep.DONE -> DoneStep(uiState = uiState)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showNotificationPermissionPrompt) {
            AlertDialog(
                onDismissRequest = { showNotificationPermissionPrompt = false },
                title = { Text(stringResource(R.string.notifications_prompt_title)) },
                text = {
                    Text(stringResource(R.string.notifications_prompt_body))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val canRequestRuntimePermission =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                            if (canRequestRuntimePermission) {
                                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                showNotificationPermissionPrompt = false
                                context.openAppNotificationSettings()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.notifications_prompt_enable))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationPermissionPrompt = false }) {
                        Text(stringResource(R.string.notifications_prompt_later))
                    }
                }
            )
        }
    }
}

@Composable
private fun IntroStep() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, shape = MaterialTheme.shapes.large)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = YellowPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun LanguageStep(
    selectedLanguageTag: String,
    onSelected: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_language_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_language_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(18.dp))

    LanguagePicker(
        label = stringResource(R.string.onboarding_language_label),
        selectedLanguageTag = selectedLanguageTag,
        onSelected = onSelected,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CurrencyStep(
    currency: String,
    onSelected: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_currency_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_currency_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(18.dp))

    CurrencyPicker(
        label = stringResource(R.string.onboarding_currency_label),
        selectedCurrencyCode = currency,
        onSelected = onSelected,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AccountStep(
    currency: String,
    accountName: String,
    accountType: AccountType,
    initialBalanceText: String,
    onAccountName: (String) -> Unit,
    onAccountType: (AccountType) -> Unit,
    onInitialBalance: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_account_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_account_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(18.dp))

    ExpenseTextField(
        value = accountName,
        onValueChange = onAccountName,
        label = stringResource(
            R.string.common_optional_format,
            stringResource(R.string.onboarding_account_name_label)
        ),
        placeholder = accountTypeDisplayName(accountType),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_account_name_helper),
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(14.dp))

    DropdownSelector(
        label = stringResource(R.string.onboarding_account_type_label),
        items = accountTypeItems(),
        selectedId = accountType.name,
        onSelected = { id ->
            val type = runCatching { AccountType.valueOf(id ?: AccountType.CASH.name) }.getOrDefault(AccountType.CASH)
            onAccountType(type)
        },
        modifier = Modifier.fillMaxWidth(),
        allowClearSelection = false
    )

    Spacer(modifier = Modifier.height(14.dp))

    AmountInputField(
        amountText = initialBalanceText,
        onAmountTextChange = onInitialBalance,
        label = stringResource(R.string.onboarding_account_balance_label),
        currencyCode = currency,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = when (accountType) {
            AccountType.CREDIT_CARD -> stringResource(R.string.onboarding_account_balance_hint_credit_card)
            else -> stringResource(R.string.onboarding_account_balance_hint_default)
        },
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )
}

@Composable
private fun BudgetStep(
    currency: String,
    addBudget: Boolean,
    budgetName: String,
    budgetAmountText: String,
    budgetPeriod: BudgetPeriod,
    onAddBudget: (Boolean) -> Unit,
    onBudgetName: (String) -> Unit,
    onBudgetAmount: (String) -> Unit,
    onBudgetPeriod: (BudgetPeriod) -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_budget_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_budget_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_budget_helper),
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(18.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.onboarding_budget_toggle),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = addBudget,
            onCheckedChange = onAddBudget
        )
    }

    if (addBudget) {
        Spacer(modifier = Modifier.height(14.dp))

        ExpenseTextField(
            value = budgetName,
            onValueChange = onBudgetName,
            label = stringResource(
                R.string.common_optional_format,
                stringResource(R.string.onboarding_budget_name_label)
            ),
            placeholder = budgetDefaultName(budgetPeriod),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_budget_name_helper),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(14.dp))

        AmountInputField(
            amountText = budgetAmountText,
            onAmountTextChange = onBudgetAmount,
            label = stringResource(R.string.onboarding_budget_amount_label),
            currencyCode = currency,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        DropdownSelector(
            label = stringResource(R.string.onboarding_budget_period_label),
            items = budgetPeriodItems(),
            selectedId = budgetPeriod.name,
            onSelected = { id ->
                val period = runCatching { BudgetPeriod.valueOf(id ?: BudgetPeriod.MONTHLY.name) }.getOrDefault(BudgetPeriod.MONTHLY)
                onBudgetPeriod(period)
            },
            modifier = Modifier.fillMaxWidth(),
            allowClearSelection = false
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_budget_period_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun AutoCaptureStep(
    uiState: OnboardingUiState,
    onEnableCapture: () -> Unit
) {
    Text(
        text = stringResource(R.string.onboarding_auto_capture_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.onboarding_auto_capture_body),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
    Spacer(modifier = Modifier.height(16.dp))

    when (uiState.trialBanner) {
        OnboardingTrialBanner.STARTED -> TrialBanner(
            text = stringResource(R.string.onboarding_auto_capture_trial_started)
        )
        OnboardingTrialBanner.FOUNDER_STARTED -> TrialBanner(
            text = stringResource(R.string.onboarding_auto_capture_founder_started)
        )
        OnboardingTrialBanner.ALREADY_ACTIVE -> TrialBanner(
            text = stringResource(R.string.onboarding_auto_capture_trial_active)
        )
        OnboardingTrialBanner.SIGN_IN_NEEDED -> TrialBanner(
            text = stringResource(R.string.onboarding_auto_capture_guest_hint),
            accent = Color(0xFFFFB74D)
        )
        OnboardingTrialBanner.NONE -> Unit
    }

    if (uiState.isClaimingTrial) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = YellowPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.onboarding_auto_capture_claiming_trial),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))
    AutoCaptureDiagram()
    Spacer(modifier = Modifier.height(18.dp))

    if (uiState.notificationListenerGranted && uiState.notificationCaptureEnabled) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF81C784)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.onboarding_auto_capture_enabled),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF81C784),
                fontWeight = FontWeight.SemiBold
            )
        }
    } else {
        Button(
            onClick = onEnableCapture,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = YellowPrimary,
                contentColor = BlackBackground
            )
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.onboarding_auto_capture_enable_cta),
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_auto_capture_skip_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun TrialBanner(
    text: String,
    accent: Color = YellowPrimary
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun AutoCaptureDiagram() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.onboarding_auto_capture_diagram_title),
            style = MaterialTheme.typography.labelLarge,
            color = YellowPrimary,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlackBackground, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(YellowPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = YellowPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_auto_capture_diagram_notif_app),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.onboarding_auto_capture_diagram_notif_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = YellowPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.onboarding_auto_capture_diagram_arrow),
                style = MaterialTheme.typography.labelMedium,
                color = YellowPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BlackBackground, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(YellowPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        tint = YellowPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_auto_capture_diagram_draft_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.onboarding_auto_capture_diagram_draft_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun DoneStep(uiState: OnboardingUiState) {
    Text(
        text = stringResource(R.string.onboarding_done_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = when {
            uiState.isTrialActive ||
                uiState.trialBanner == OnboardingTrialBanner.STARTED ||
                uiState.trialBanner == OnboardingTrialBanner.FOUNDER_STARTED ||
                uiState.trialBanner == OnboardingTrialBanner.ALREADY_ACTIVE -> {
                stringResource(R.string.onboarding_done_subtitle_trial)
            }
            else -> stringResource(R.string.onboarding_done_subtitle)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray
    )
}

@Composable
private fun stepLabel(step: OnboardingStep): String = when (step) {
    OnboardingStep.INTRO -> "1/7"
    OnboardingStep.LANGUAGE -> "2/7"
    OnboardingStep.CURRENCY -> "3/7"
    OnboardingStep.ACCOUNT -> "4/7"
    OnboardingStep.BUDGET -> "5/7"
    OnboardingStep.AUTO_CAPTURE -> "6/7"
    OnboardingStep.DONE -> "7/7"
}

@Composable
private fun accountTypeItems(): List<DropdownItem> = AccountType.values().map { type ->
    DropdownItem(
        id = type.name,
        label = when (type) {
            AccountType.CASH -> stringResource(R.string.account_type_cash)
            AccountType.BANK -> stringResource(R.string.account_type_bank)
            AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
            AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
            AccountType.INVESTMENT -> stringResource(R.string.account_type_investment)
            AccountType.OTHER -> stringResource(R.string.account_type_other)
        }
    )
}

@Composable
private fun budgetPeriodItems(): List<DropdownItem> = BudgetPeriod.values()
    .filterNot { it == BudgetPeriod.CUSTOM }
    .map { period ->
        DropdownItem(
            id = period.name,
            label = when (period) {
                BudgetPeriod.DAILY -> stringResource(R.string.budget_period_daily)
                BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
                BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
                BudgetPeriod.YEARLY -> stringResource(R.string.budget_period_yearly)
                BudgetPeriod.CUSTOM -> period.name.lowercase()
            }
        )
    }

@Composable
private fun accountTypeDisplayName(type: AccountType): String = when (type) {
    AccountType.CASH -> stringResource(R.string.account_type_cash)
    AccountType.BANK -> stringResource(R.string.account_type_bank)
    AccountType.CREDIT_CARD -> stringResource(R.string.account_type_credit_card)
    AccountType.SAVINGS -> stringResource(R.string.account_type_savings)
    AccountType.INVESTMENT -> stringResource(R.string.account_type_investment)
    AccountType.OTHER -> stringResource(R.string.account_type_other)
}

@Composable
private fun budgetDefaultName(period: BudgetPeriod): String = when (period) {
    BudgetPeriod.DAILY -> stringResource(R.string.budget_default_daily_name)
    BudgetPeriod.WEEKLY -> stringResource(R.string.budget_default_weekly_name)
    BudgetPeriod.MONTHLY -> stringResource(R.string.budget_default_monthly_name)
    BudgetPeriod.YEARLY -> stringResource(R.string.budget_default_yearly_name)
    BudgetPeriod.CUSTOM -> stringResource(R.string.budget_default_custom_name)
}

