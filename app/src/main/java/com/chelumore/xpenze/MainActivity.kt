package com.chelumore.xpenze

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.os.LocaleListCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.chelumore.xpenze.core.analytics.GrowthAnalyticsTracker
import com.chelumore.xpenze.core.data.DataSeeder
import com.chelumore.xpenze.core.navigation.AuthGraph
import com.chelumore.xpenze.core.navigation.MainGraph
import com.chelumore.xpenze.core.navigation.Screen
import com.chelumore.xpenze.core.navigation.XpenzeNavGraph
import com.chelumore.xpenze.core.localization.AppLanguages
import com.chelumore.xpenze.core.ui.util.findActivity
import com.chelumore.xpenze.core.ui.util.hasNotificationsPermissionEnabled
import com.chelumore.xpenze.core.ui.util.launchPlayStoreReview
import com.chelumore.xpenze.core.ui.util.LocalWindowWidthSizeClass
import com.chelumore.xpenze.core.ui.util.openAppNotificationSettings
import com.chelumore.xpenze.core.work.SyncScheduler
import com.chelumore.xpenze.domain.repository.UserRepository
import com.chelumore.xpenze.domain.repository.UserPreferences
import com.chelumore.xpenze.domain.repository.ThemeMode
import com.chelumore.xpenze.core.premium.PremiumEntitlementTracker
import com.chelumore.xpenze.core.premium.shouldShowPremiumCta
import com.chelumore.xpenze.ui.theme.XpenzeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var dataSeeder: DataSeeder

    @Inject
    lateinit var growthAnalyticsTracker: GrowthAnalyticsTracker

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var premiumEntitlementTracker: PremiumEntitlementTracker

    private var lastForegroundSyncAtMs: Long = 0L
    private var externalMochiPrompt by mutableStateOf<String?>(null)
    private var externalReviewDraftId by mutableStateOf<String?>(null)
    private var externalReviewDraftType by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        externalMochiPrompt = intent?.getStringExtra(EXTRA_MOCHI_PROMPT)
        externalReviewDraftId = intent?.getStringExtra(EXTRA_REVIEW_DRAFT_ID)
        externalReviewDraftType = intent?.getStringExtra(EXTRA_REVIEW_DRAFT_TYPE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { dataSeeder.seedIfEmpty() }
        }

        // Apply app locale from persisted preferences with lifecycle scope (not from composables)
        // to avoid repeated locale re-application loops that can cause visual flicker.
        lifecycleScope.launch {
            userRepository.userPreferences
                .map { prefs -> AppLanguages.normalizeTag(prefs.appLanguageTag) }
                .distinctUntilChanged()
                .collect { desiredTag ->
                    val currentTag = AppLanguages.normalizeTag(
                        AppCompatDelegate.getApplicationLocales().toLanguageTags()
                    )
                    if (currentTag != desiredTag) {
                        AppCompatDelegate.setApplicationLocales(
                            LocaleListCompat.forLanguageTags(desiredTag)
                        )
                    }
                }
        }

        setContent {
            val prefs by userRepository.userPreferences.collectAsState(initial = UserPreferences())
            val entitlement by premiumEntitlementTracker.entitlement.collectAsState()
            val appScope = rememberCoroutineScope()
            val darkTheme = when (prefs.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(prefs.isGuestMode, prefs.userId) {
                growthAnalyticsTracker.setAccountType(
                    isGuest = prefs.isGuestMode,
                    isSignedIn = !prefs.userId.isNullOrBlank()
                )
            }

            XpenzeTheme(darkTheme = darkTheme) {
                // Always start in AuthGraph -> Splash decides where to go (Welcome / Verification / Onboarding / Main).
                XpenzeApp(
                    startDestination = AuthGraph,
                    hasPremiumAccess = entitlement.hasPremiumAccess,
                    isTrialEndingSoon = entitlement.isTrialEndingSoon,
                    showAiCoachNewBadge = !prefs.hasSeenAiCoachCta,
                    userId = prefs.userId,
                    isGuestMode = prefs.isGuestMode,
                    isOnboardingComplete = prefs.isOnboardingComplete,
                    notificationsEnabled = prefs.notificationsEnabled,
                    notificationPermissionPromptLastShownAt = prefs.notificationPermissionPromptLastShownAt,
                    appOpenCount = prefs.appOpenCount,
                    rateEligibleActionCount = prefs.rateEligibleActionCount,
                    hasRatedOnPlayStore = prefs.hasRatedOnPlayStore,
                    rateAppPromptLastShownAt = prefs.rateAppPromptLastShownAt,
                    onMarkAiCoachCtaSeen = {
                        appScope.launch { userRepository.setAiCoachCtaSeen(true) }
                    },
                    onNotificationPermissionPromptShown = {
                        appScope.launch {
                            userRepository.setNotificationPermissionPromptLastShownAt(
                                System.currentTimeMillis()
                            )
                        }
                    },
                    onRateAppPromptShown = {
                        appScope.launch {
                            userRepository.setRateAppPromptLastShownAt(
                                System.currentTimeMillis()
                            )
                        }
                    },
                    onRatedOnPlayStore = {
                        appScope.launch {
                            userRepository.setHasRatedOnPlayStore(true)
                        }
                    },
                    onPremiumCtaImpression = { route ->
                        growthAnalyticsTracker.logPremiumCtaImpression(route)
                    },
                    onPremiumCtaClick = { route ->
                        growthAnalyticsTracker.logPremiumCtaClick(route)
                    },
                    onAiCtaImpression = { route, isNewBadgeVisible ->
                        growthAnalyticsTracker.logAiCtaImpression(
                            screenRoute = route,
                            isNewBadgeVisible = isNewBadgeVisible
                        )
                    },
                    onScreenView = growthAnalyticsTracker::logScreenView,
                    onAiCtaClick = { route, isNewBadgeVisible ->
                        growthAnalyticsTracker.logAiCtaClick(
                            screenRoute = route,
                            isNewBadgeVisible = isNewBadgeVisible
                        )
                    },
                    externalMochiPrompt = externalMochiPrompt,
                    onExternalMochiPromptConsumed = {
                        externalMochiPrompt = null
                    },
                    externalReviewDraftId = externalReviewDraftId,
                    externalReviewDraftType = externalReviewDraftType,
                    onExternalReviewDraftConsumed = {
                        externalReviewDraftId = null
                        externalReviewDraftType = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalMochiPrompt = intent.getStringExtra(EXTRA_MOCHI_PROMPT)
        externalReviewDraftId = intent.getStringExtra(EXTRA_REVIEW_DRAFT_ID)
        externalReviewDraftType = intent.getStringExtra(EXTRA_REVIEW_DRAFT_TYPE)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val prefs = userRepository.userPreferences.first()
            val now = System.currentTimeMillis()
            val isSignedIn = !prefs.userId.isNullOrBlank()
            if (!isSignedIn && !prefs.isGuestMode) return@launch

            // Keep app-open count for rate prompt logic only after onboarding is complete.
            if (prefs.isOnboardingComplete) {
                runCatching { userRepository.incrementAppOpenCount() }
            }

            // Guests have no cloud account, so there is nothing to sync.
            if (!isSignedIn) return@launch

            syncScheduler.schedulePeriodic()
            if (now - lastForegroundSyncAtMs > FOREGROUND_SYNC_THROTTLE_MS) {
                lastForegroundSyncAtMs = now
                syncScheduler.scheduleNow()
            }
        }
    }

    companion object {
        const val FOREGROUND_SYNC_THROTTLE_MS = 30_000L
        const val EXTRA_MOCHI_PROMPT = "extra_mochi_prompt"
        const val EXTRA_REVIEW_DRAFT_ID = "extra_review_draft_id"
        const val EXTRA_REVIEW_DRAFT_TYPE = "extra_review_draft_type"
    }
}

private const val NOTIFICATION_PERMISSION_NUDGE_INTERVAL_MS = 3L * 24L * 60L * 60L * 1000L
private const val RATE_PROMPT_INTERVAL_MS = 14L * 24L * 60L * 60L * 1000L
private const val RATE_PROMPT_MIN_APP_OPENS = 5
private const val RATE_PROMPT_MIN_ELIGIBLE_ACTIONS = 1

@Composable
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun XpenzeApp(
    startDestination: Any,
    hasPremiumAccess: Boolean,
    isTrialEndingSoon: Boolean,
    showAiCoachNewBadge: Boolean,
    userId: String?,
    isGuestMode: Boolean,
    isOnboardingComplete: Boolean,
    notificationsEnabled: Boolean,
    notificationPermissionPromptLastShownAt: Long,
    appOpenCount: Int,
    rateEligibleActionCount: Int,
    hasRatedOnPlayStore: Boolean,
    rateAppPromptLastShownAt: Long,
    onMarkAiCoachCtaSeen: () -> Unit,
    onNotificationPermissionPromptShown: () -> Unit,
    onRateAppPromptShown: () -> Unit,
    onRatedOnPlayStore: () -> Unit,
    onPremiumCtaImpression: (String?) -> Unit,
    onPremiumCtaClick: (String?) -> Unit,
    onAiCtaImpression: (String?, Boolean) -> Unit,
    onAiCtaClick: (String?, Boolean) -> Unit,
    onScreenView: (String?) -> Unit,
    externalMochiPrompt: String?,
    onExternalMochiPromptConsumed: () -> Unit,
    externalReviewDraftId: String?,
    externalReviewDraftType: String?,
    onExternalReviewDraftConsumed: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = context.findActivity()
    val reviewScope = rememberCoroutineScope()
    val widthSizeClass = if (activity != null) {
        calculateWindowSizeClass(activity).widthSizeClass
    } else {
        WindowWidthSizeClass.Compact
    }
    
    // Track current route to highlight correct nav item
    // Keep track of the last selected bottom nav item
    var lastSelectedNavItem by remember { mutableIntStateOf(0) }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        onScreenView(currentRoute)
    }

    // Hide BottomBar and FAB on Auth/Onboarding screens
    val isAuthScreen = currentRoute == null ||
            currentRoute?.contains("Splash") == true ||
            currentRoute?.contains("Welcome") == true ||
            currentRoute?.contains("Login") == true ||
            currentRoute?.contains("SignUp") == true ||
            currentRoute?.contains("ForgotPassword") == true ||
            currentRoute?.contains("Verification") == true ||
            currentRoute?.contains("Onboarding") == true

    val hasNotificationsPermissionEnabled = context.hasNotificationsPermissionEnabled()
    var showNotificationPermissionPrompt by rememberSaveable { mutableStateOf(false) }
    var lastPermissionPromptRecordedAt by rememberSaveable { mutableLongStateOf(0L) }
    var showRateAppPrompt by rememberSaveable { mutableStateOf(false) }
    var lastRatePromptRecordedAt by rememberSaveable { mutableLongStateOf(0L) }
    val requestNotificationsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        showNotificationPermissionPrompt = false
        if (!granted || !context.hasNotificationsPermissionEnabled()) {
            context.openAppNotificationSettings()
        }
    }

    LaunchedEffect(
        isAuthScreen,
        userId,
        isGuestMode,
        isOnboardingComplete,
        notificationsEnabled,
        hasNotificationsPermissionEnabled,
        notificationPermissionPromptLastShownAt
    ) {
        val shouldPrompt = !isAuthScreen &&
            (!userId.isNullOrBlank() || isGuestMode) &&
            isOnboardingComplete &&
            !hasNotificationsPermissionEnabled

        if (!shouldPrompt) {
            showNotificationPermissionPrompt = false
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        val lastPromptAt = maxOf(notificationPermissionPromptLastShownAt, lastPermissionPromptRecordedAt)
        if (!showNotificationPermissionPrompt && now - lastPromptAt >= NOTIFICATION_PERMISSION_NUDGE_INTERVAL_MS) {
            showNotificationPermissionPrompt = true
            lastPermissionPromptRecordedAt = now
            onNotificationPermissionPromptShown()
        }
    }

    LaunchedEffect(externalMochiPrompt, userId, isGuestMode, isAuthScreen) {
        val prompt = externalMochiPrompt?.trim().orEmpty()
        if (prompt.isBlank()) return@LaunchedEffect
        if ((userId.isNullOrBlank() && !isGuestMode) || isAuthScreen) return@LaunchedEffect

        navController.navigate(Screen.AiCoachChat(prompt = prompt)) {
            launchSingleTop = true
        }
        onExternalMochiPromptConsumed()
    }

    LaunchedEffect(externalReviewDraftId, externalReviewDraftType, userId, isGuestMode, isAuthScreen) {
        val draftId = externalReviewDraftId?.trim().orEmpty()
        if (draftId.isBlank()) return@LaunchedEffect
        if ((userId.isNullOrBlank() && !isGuestMode) || isAuthScreen) return@LaunchedEffect

        navController.navigate(
            Screen.ReviewDraft(
                draftId = draftId,
                type = externalReviewDraftType?.takeIf { it.isNotBlank() } ?: "EXPENSE"
            )
        ) {
            launchSingleTop = true
        }
        onExternalReviewDraftConsumed()
    }

    LaunchedEffect(
        isAuthScreen,
        userId,
        isGuestMode,
        isOnboardingComplete,
        appOpenCount,
        rateEligibleActionCount,
        hasRatedOnPlayStore,
        rateAppPromptLastShownAt,
        showNotificationPermissionPrompt
    ) {
        val shouldPrompt = !isAuthScreen &&
            (!userId.isNullOrBlank() || isGuestMode) &&
            isOnboardingComplete &&
            appOpenCount >= RATE_PROMPT_MIN_APP_OPENS &&
            rateEligibleActionCount >= RATE_PROMPT_MIN_ELIGIBLE_ACTIONS &&
            !hasRatedOnPlayStore &&
            !showNotificationPermissionPrompt

        if (!shouldPrompt) {
            showRateAppPrompt = false
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        val lastPromptAt = maxOf(rateAppPromptLastShownAt, lastRatePromptRecordedAt)
        if (!showRateAppPrompt && now - lastPromptAt >= RATE_PROMPT_INTERVAL_MS) {
            showRateAppPrompt = true
            lastRatePromptRecordedAt = now
            onRateAppPromptShown()
        }
    }

    val useNavRail = !isAuthScreen && widthSizeClass != WindowWidthSizeClass.Compact

    val selectedNavItem = when {
        currentRoute?.contains("Dashboard") == true -> {
            lastSelectedNavItem = 0
            0
        }
        currentRoute?.contains("Transactions") == true -> {
            lastSelectedNavItem = 1
            1
        }
        currentRoute?.contains("Budgets") == true -> {
            lastSelectedNavItem = 2
            2
        }
        currentRoute?.contains("Reports") == true -> {
            lastSelectedNavItem = 3
            3
        }
        currentRoute?.contains("Settings") == true -> {
            lastSelectedNavItem = 4
            4
        }
        else -> lastSelectedNavItem // Keep last selection for non-bottom-nav screens
    }

    val bottomNavItems = listOf(
        BottomNavItemData(
            label = stringResource(R.string.main_nav_home),
            icon = Icons.Default.Home,
            screen = Screen.Dashboard
        ),
        BottomNavItemData(
            label = stringResource(R.string.main_nav_transactions),
            icon = Icons.Default.Receipt,
            screen = Screen.Transactions
        ),
        BottomNavItemData(
            label = stringResource(R.string.main_nav_budgets),
            icon = Icons.Default.Savings,
            screen = Screen.Budgets
        ),
        BottomNavItemData(
            label = stringResource(R.string.main_nav_reports),
            icon = Icons.Default.BarChart,
            screen = Screen.Reports
        ),
        BottomNavItemData(
            label = stringResource(R.string.main_nav_settings),
            icon = Icons.Default.Settings,
            screen = Screen.Settings
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isAuthScreen && !useNavRail) {
                Column {
                    GrowthBottomActionRow(
                        hasPremiumAccess = hasPremiumAccess,
                        isTrialEndingSoon = isTrialEndingSoon,
                        currentRoute = currentRoute,
                        showAiCoachNewBadge = showAiCoachNewBadge,
                        onNavigateToPremium = {
                            navController.navigate(Screen.Subscription) {
                                launchSingleTop = true
                            }
                        },
                        onNavigateToAiCoach = {
                            navController.navigate(Screen.AiCoachChat()) {
                                launchSingleTop = true
                            }
                        },
                        onMarkAiCoachCtaSeen = onMarkAiCoachCtaSeen,
                        onPremiumCtaImpression = onPremiumCtaImpression,
                        onPremiumCtaClick = onPremiumCtaClick,
                        onAiCtaImpression = onAiCtaImpression,
                        onAiCtaClick = onAiCtaClick,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    NavigationBar {
                        bottomNavItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                selected = selectedNavItem == index,
                                onClick = {
                                    navController.navigate(item.screen) {
                                        // Pop up to the start destination to avoid building up a large stack
                                        popUpTo(Screen.Dashboard) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination
                                        launchSingleTop = true
                                        // Restore state when navigating back
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Only show main FAB on specific screens
            // Dashboard and Transactions need "Add Transaction"
            // Budgets has its own "Add Budget" FAB
            if (!isAuthScreen && currentRoute?.contains("Dashboard") == true) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.AddTransaction())
                    },
                    containerColor = com.chelumore.xpenze.ui.theme.YellowPrimary,
                    contentColor = com.chelumore.xpenze.ui.theme.BlackBackground
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.main_cd_add_transaction)
                    )
                }
            }
        }
    ) { innerPadding ->
        CompositionLocalProvider(LocalWindowWidthSizeClass provides widthSizeClass) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        bottomNavItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedNavItem == index,
                                onClick = {
                                    navController.navigate(item.screen) {
                                        popUpTo(Screen.Dashboard) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                alwaysShowLabel = false
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        GrowthRailActions(
                            hasPremiumAccess = hasPremiumAccess,
                            isTrialEndingSoon = isTrialEndingSoon,
                            currentRoute = currentRoute,
                            showAiCoachNewBadge = showAiCoachNewBadge,
                            onNavigateToPremium = {
                                navController.navigate(Screen.Subscription) {
                                    launchSingleTop = true
                                }
                            },
                            onNavigateToAiCoach = {
                                navController.navigate(Screen.AiCoachChat()) {
                                    launchSingleTop = true
                                }
                            },
                            onMarkAiCoachCtaSeen = onMarkAiCoachCtaSeen,
                            onPremiumCtaImpression = onPremiumCtaImpression,
                            onPremiumCtaClick = onPremiumCtaClick,
                            onAiCtaImpression = onAiCtaImpression,
                            onAiCtaClick = onAiCtaClick
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                XpenzeNavGraph(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.weight(1f)
                )
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
                        val needsRuntimePermission =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                        if (needsRuntimePermission) {
                            requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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

    if (showRateAppPrompt) {
        AlertDialog(
            onDismissRequest = { showRateAppPrompt = false },
            title = { Text(stringResource(R.string.rate_prompt_title)) },
            text = { Text(stringResource(R.string.rate_prompt_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRateAppPrompt = false
                        reviewScope.launch {
                            val reviewLaunched = context.launchPlayStoreReview(activity)
                            if (reviewLaunched) {
                                onRatedOnPlayStore()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.rate_prompt_rate_now))
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showRateAppPrompt = false }) {
                        Text(stringResource(R.string.rate_prompt_later))
                    }
                    TextButton(
                        onClick = {
                            showRateAppPrompt = false
                            onRatedOnPlayStore()
                        }
                    ) {
                        Text(stringResource(R.string.rate_prompt_already_rated))
                    }
                }
            }
        )
    }
}

@Composable
private fun GrowthBottomActionRow(
    hasPremiumAccess: Boolean,
    isTrialEndingSoon: Boolean,
    currentRoute: String?,
    showAiCoachNewBadge: Boolean,
    onNavigateToPremium: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    onMarkAiCoachCtaSeen: () -> Unit,
    onPremiumCtaImpression: (String?) -> Unit,
    onPremiumCtaClick: (String?) -> Unit,
    onAiCtaImpression: (String?, Boolean) -> Unit,
    onAiCtaClick: (String?, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shouldShowPremium = shouldShowPremiumCta(hasPremiumAccess, isTrialEndingSoon)
    val shouldShowAi = currentRoute?.contains("AiCoachChat") != true
    if (!shouldShowPremium && !shouldShowAi) return

    val premiumImpressionRoutes = remember { mutableSetOf<String>() }
    val aiImpressionRoutes = remember { mutableSetOf<String>() }

    val premiumPulseTransition = rememberInfiniteTransition(label = "premiumPulse")
    val premiumPulseScale by premiumPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premiumPulseScale"
    )

    LaunchedEffect(shouldShowPremium, currentRoute) {
        if (shouldShowPremium && !currentRoute.isNullOrBlank() && premiumImpressionRoutes.add(currentRoute)) {
            onPremiumCtaImpression(currentRoute)
        }
    }

    LaunchedEffect(shouldShowAi, currentRoute, showAiCoachNewBadge) {
        if (shouldShowAi && !currentRoute.isNullOrBlank() && aiImpressionRoutes.add(currentRoute)) {
            onAiCtaImpression(currentRoute, showAiCoachNewBadge)
        }
    }

    val dualCtas = shouldShowPremium && shouldShowAi
    val aiOnly = shouldShowAi && !shouldShowPremium
    val compactDualCtaHeight = 50.dp

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (shouldShowPremium) {
            ExtendedFloatingActionButton(
                onClick = {
                    onPremiumCtaClick(currentRoute)
                    onNavigateToPremium()
                },
                containerColor = com.chelumore.xpenze.ui.theme.YellowPrimary,
                contentColor = com.chelumore.xpenze.ui.theme.BlackBackground,
                modifier = (if (dualCtas) Modifier.weight(1f) else Modifier)
                    .then(if (dualCtas) Modifier.height(compactDualCtaHeight) else Modifier)
                    .scale(premiumPulseScale),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            if (isTrialEndingSoon) {
                                R.string.trial_ending_soon_cta
                            } else {
                                R.string.ai_chat_go_premium
                            }
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }

        if (shouldShowAi) {
            ExtendedFloatingActionButton(
                onClick = {
                    onAiCtaClick(currentRoute, showAiCoachNewBadge)
                    if (showAiCoachNewBadge) {
                        onMarkAiCoachCtaSeen()
                    }
                    onNavigateToAiCoach()
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = when {
                    dualCtas -> Modifier
                        .weight(1f)
                        .height(compactDualCtaHeight)
                    aiOnly -> Modifier.fillMaxWidth()
                    else -> Modifier
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null
                    )
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.main_ai_coach_fab),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (showAiCoachNewBadge) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = com.chelumore.xpenze.ui.theme.YellowPrimary,
                                        shape = RoundedCornerShape(100)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.main_new_badge),
                                    color = com.chelumore.xpenze.ui.theme.BlackBackground,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GrowthRailActions(
    hasPremiumAccess: Boolean,
    isTrialEndingSoon: Boolean,
    currentRoute: String?,
    showAiCoachNewBadge: Boolean,
    onNavigateToPremium: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    onMarkAiCoachCtaSeen: () -> Unit,
    onPremiumCtaImpression: (String?) -> Unit,
    onPremiumCtaClick: (String?) -> Unit,
    onAiCtaImpression: (String?, Boolean) -> Unit,
    onAiCtaClick: (String?, Boolean) -> Unit
) {
    val shouldShowPremium = shouldShowPremiumCta(hasPremiumAccess, isTrialEndingSoon)
    val shouldShowAi = currentRoute?.contains("AiCoachChat") != true
    if (!shouldShowPremium && !shouldShowAi) return

    val premiumImpressionRoutes = remember { mutableSetOf<String>() }
    val aiImpressionRoutes = remember { mutableSetOf<String>() }

    val premiumPulseTransition = rememberInfiniteTransition(label = "railPremiumPulse")
    val premiumPulseScale by premiumPulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1100,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "railPremiumPulseScale"
    )

    LaunchedEffect(shouldShowPremium, currentRoute) {
        if (shouldShowPremium && !currentRoute.isNullOrBlank() && premiumImpressionRoutes.add(currentRoute)) {
            onPremiumCtaImpression(currentRoute)
        }
    }

    LaunchedEffect(shouldShowAi, currentRoute, showAiCoachNewBadge) {
        if (shouldShowAi && !currentRoute.isNullOrBlank() && aiImpressionRoutes.add(currentRoute)) {
            onAiCtaImpression(currentRoute, showAiCoachNewBadge)
        }
    }

    if (shouldShowPremium) {
        NavigationRailItem(
            selected = currentRoute?.contains("Subscription") == true,
            onClick = {
                onPremiumCtaClick(currentRoute)
                onNavigateToPremium()
            },
            modifier = Modifier.scale(premiumPulseScale),
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(R.string.settings_cd_premium)
                )
            },
            label = {
                Text(
                    stringResource(
                        if (isTrialEndingSoon) {
                            R.string.trial_ending_soon_cta
                        } else {
                            R.string.ai_chat_go_premium
                        }
                    )
                )
            },
            alwaysShowLabel = true,
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = com.chelumore.xpenze.ui.theme.YellowPrimary,
                selectedTextColor = com.chelumore.xpenze.ui.theme.YellowPrimary,
                indicatorColor = com.chelumore.xpenze.ui.theme.YellowPrimary.copy(alpha = 0.18f),
                unselectedIconColor = com.chelumore.xpenze.ui.theme.YellowPrimary,
                unselectedTextColor = com.chelumore.xpenze.ui.theme.YellowPrimary
            )
        )
    }

    if (shouldShowAi) {
        NavigationRailItem(
            selected = currentRoute?.contains("AiCoachChat") == true,
            onClick = {
                onAiCtaClick(currentRoute, showAiCoachNewBadge)
                if (showAiCoachNewBadge) {
                    onMarkAiCoachCtaSeen()
                }
                onNavigateToAiCoach()
            },
            icon = {
                if (showAiCoachNewBadge) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(stringResource(R.string.main_new_badge))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = stringResource(R.string.main_ai_coach_fab)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = stringResource(R.string.main_ai_coach_fab)
                    )
                }
            },
            label = { Text(stringResource(R.string.main_ai_coach_fab)) },
            alwaysShowLabel = true
        )
    }
}

private data class BottomNavItemData(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)
