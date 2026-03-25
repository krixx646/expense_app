package com.chelumore.xpenze.feature.reports

import android.graphics.Paint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.growth.buildMochiPromptForOpportunity
import com.chelumore.xpenze.core.growth.formatPremiumOutcomeSummary
import com.chelumore.xpenze.core.growth.MochiPremiumCopywriter
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.growth.PremiumOpportunity
import com.chelumore.xpenze.core.growth.premiumOpportunityActionLabel
import com.chelumore.xpenze.core.growth.premiumOpportunityEmptyText
import com.chelumore.xpenze.core.growth.premiumOpportunitySectionTitle
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumBundleLabel
import com.chelumore.xpenze.core.localization.localizedAutoCapturePremiumFeatureLabel
import com.chelumore.xpenze.core.ui.components.ads.AdPlacement
import com.chelumore.xpenze.core.ui.components.ads.FreeTierAdSlot
import com.chelumore.xpenze.core.ui.components.growth.MochiPremiumNudgeCard
import com.chelumore.xpenze.core.ui.components.growth.MochiValueTrackerCard
import com.chelumore.xpenze.core.ui.components.growth.PremiumOpportunityCard
import com.chelumore.xpenze.ui.theme.*
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToAdvancedReports: () -> Unit = {},
    onNavigateToAiChat: (String?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var exportMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportsEvent.ShareFile -> {
                    shareBytesAsFile(
                        context = context,
                        bytes = event.bytes,
                        fileName = event.fileName,
                        mimeType = event.mimeType
                    )
                }

                is ReportsEvent.Message -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_nav_reports), fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState.hasPremiumAccess) {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.settings_export_import))
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reports_export_csv)) },
                                onClick = {
                                    exportMenuExpanded = false
                                    viewModel.exportCsv()
                                }
                            )
                        }
                    } else {
                        IconButton(onClick = onNavigateToSubscription) {
                            Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.reports_csv_premium_cd))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 960.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Period Selector
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportPeriod.values()) { period ->
                            PeriodFilterChip(
                                label = localizedReportPeriodLabel(period),
                                selected = uiState.selectedPeriod == period,
                                onClick = { viewModel.setPeriod(period) }
                            )
                        }
                    }
                }

                item {
                    FreeTierAdSlot(
                        hasPremiumAccess = uiState.hasPremiumAccess,
                        placement = AdPlacement.REPORTS
                    )
                }

                // Compare Switch
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reports_compare_toggle_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.selectedPeriod == ReportPeriod.ALL_TIME) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.reports_compare_toggle_disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.isCompareEnabled && uiState.selectedPeriod != ReportPeriod.ALL_TIME,
                            enabled = uiState.selectedPeriod != ReportPeriod.ALL_TIME,
                            onCheckedChange = viewModel::toggleCompare,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BlackBackground,
                                checkedTrackColor = YellowPrimary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

            // Summary Cards (Income / Expense)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryCard(
                        title = stringResource(R.string.dashboard_income),
                        amount = uiState.totalIncome,
                        currencyCode = uiState.currencyCode,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = stringResource(R.string.dashboard_expense),
                        amount = uiState.totalExpense,
                        currencyCode = uiState.currencyCode,
                        color = ErrorRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Net Cash Flow
            item {
                SummaryCard(
                    title = stringResource(R.string.reports_net_cash_flow),
                    amount = uiState.netCashFlow,
                    currencyCode = uiState.currencyCode,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Cash Flow Summary Table
            item {
                CashFlowSummaryTable(uiState)
            }

            // Spending by Category Chart
            item {
                SpendingCategoryChart(uiState.categorySpending, uiState.totalExpense, uiState.currencyCode)
            }

            // Top Spending Categories List
            item {
                Text(
                    text = stringResource(R.string.reports_top_spending_categories),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.topCategories.forEachIndexed { index, cat ->
                        TopCategoryItem(index + 1, cat, uiState.currencyCode)
                    }
                }
            }

            // Monthly Trend Chart
            item {
                MonthlyTrendChart(
                    points = uiState.monthlyTrend,
                    currencyCode = uiState.currencyCode
                )
            }

                if (uiState.hasPremiumAccess) {
                    item {
                        AiInsightsSection(
                            hasPremiumAccess = uiState.hasPremiumAccess,
                            isTrialActive = uiState.isTrialActive,
                            nudgeVariant = uiState.nudgeVariant,
                            currencyCode = uiState.currencyCode,
                            appLanguageTag = uiState.appLanguageTag,
                            monthlyInsights = uiState.monthlyInsights,
                            budgetSuggestions = uiState.budgetSuggestions,
                            savingsOpportunities = uiState.savingsOpportunities,
                            onUpgrade = onNavigateToSubscription,
                            onOpenChat = onNavigateToAiChat
                        )
                    }

                    item {
                        AdvancedReportsSection(
                            hasPremiumAccess = uiState.hasPremiumAccess,
                            isTrialActive = uiState.isTrialActive,
                            nudgeVariant = uiState.nudgeVariant,
                            onUpgrade = onNavigateToSubscription,
                            onOpen = onNavigateToAdvancedReports
                        )
                    }
                } else {
                    item {
                        ReportsPremiumSection(
                            nudgeVariant = uiState.nudgeVariant,
                            currencyCode = uiState.currencyCode,
                            appLanguageTag = uiState.appLanguageTag,
                            previewOpportunity = uiState.savingsOpportunities.firstOrNull(),
                            onUpgrade = onNavigateToSubscription
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedReportPeriodLabel(period: ReportPeriod): String {
    return when (period) {
        ReportPeriod.THIS_MONTH -> stringResource(R.string.period_this_month)
        ReportPeriod.LAST_MONTH -> stringResource(R.string.period_last_month)
        ReportPeriod.THIS_YEAR -> stringResource(R.string.period_this_year)
        ReportPeriod.ALL_TIME -> stringResource(R.string.period_all_time)
    }
}

@Composable
private fun PeriodFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = YellowPrimary,
            selectedLabelColor = BlackBackground,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = null,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
    currencyCode: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount.toCurrencyString(currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun CashFlowSummaryTable(state: ReportsUiState) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.reports_cash_flow_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            CashFlowRow(stringResource(R.string.reports_total_income), state.totalIncome, state.currencyCode, SuccessGreen)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            CashFlowRow(stringResource(R.string.reports_total_expense), state.totalExpense, state.currencyCode, ErrorRed)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            CashFlowRow(stringResource(R.string.reports_net_cash_flow), state.netCashFlow, state.currencyCode, MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CashFlowRow(label: String, amount: Long, currencyCode: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount.toCurrencyString(currencyCode), style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SpendingCategoryChart(categories: List<CategorySpendingUi>, total: Long, currencyCode: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_spending_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.reports_breakdown_below),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Donut Chart
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 30.dp.toPx()
                    var startAngle = -90f
                    
                    categories.forEach { cat ->
                        val sweepAngle = cat.percentage * 360f
                        drawArc(
                            color = cat.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                            size = Size(size.width, size.height),
                            topLeft = Offset(0f, 0f)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.dashboard_total_short), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(total.toCurrencyString(currencyCode), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { cat ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(cat.color))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.categoryName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(cat.amount.toCurrencyString(currencyCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCategoryItem(rank: Int, category: CategorySpendingUi, currencyCode: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            Column {
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.reports_percent_of_total, (category.percentage * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = category.amount.toCurrencyString(currencyCode),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (category.percentageChange != null) {
                Text(
                    text = "${if (category.percentageChange > 0) "+" else ""}${String.format("%.1f", category.percentageChange)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (category.percentageChange > 0) ErrorRed else SuccessGreen // More spending is usually bad (Red), less is good (Green)
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun MonthlyTrendChart(
    points: List<TrendPoint>,
    currencyCode: String
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val appLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val zoneId = remember { ZoneId.systemDefault() }
    val hasActivity = remember(points) {
        points.any { it.incomeCents != 0L || it.expenseCents != 0L || it.netCents != 0L }
    }
    val peakIncome = remember(points) {
        points.maxByOrNull { it.incomeCents }?.takeIf { it.incomeCents > 0L }
    }
    val peakExpense = remember(points) {
        points.maxByOrNull { it.expenseCents }?.takeIf { it.expenseCents > 0L }
    }
    val statDateFormatter = remember(appLocale) {
        DateTimeFormatter.ofPattern("MMM d", appLocale)
    }
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val verticalGridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val zeroLineColor = YellowPrimary.copy(alpha = 0.18f)
    val xAxisIndices = remember(points) {
        listOf(0, points.lastIndex / 2, points.lastIndex)
            .filter { it in points.indices }
            .distinct()
    }
    val xAxisDateFormatter = remember(points, appLocale, zoneId) {
        val firstDate = points.firstOrNull()?.let { Instant.ofEpochMilli(it.dayStartMillis).atZone(zoneId).toLocalDate() }
        val lastDate = points.lastOrNull()?.let { Instant.ofEpochMilli(it.dayStartMillis).atZone(zoneId).toLocalDate() }
        val totalDays = if (firstDate != null && lastDate != null) {
            ChronoUnit.DAYS.between(firstDate, lastDate)
        } else {
            0L
        }
        when {
            firstDate != null && lastDate != null && firstDate.year != lastDate.year -> {
                DateTimeFormatter.ofPattern("MMM yyyy", appLocale)
            }
            totalDays > 120 -> DateTimeFormatter.ofPattern("MMM d", appLocale)
            else -> DateTimeFormatter.ofPattern("MMM d", appLocale)
        }
    }
    val axisLabelPaint = remember(appLocale, density, axisLabelColor) {
        Paint().apply {
            isAntiAlias = true
            textSize = with(density) { 11.sp.toPx() }
            color = axisLabelColor.toArgb()
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.reports_monthly_trend_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.reports_monthly_trend_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (peakIncome != null || peakExpense != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    peakIncome?.let { point ->
                        TrendInsightCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.reports_highest_income_day),
                            amountText = point.incomeCents.toCurrencyString(currencyCode),
                            dateText = Instant.ofEpochMilli(point.dayStartMillis)
                                .atZone(zoneId)
                                .toLocalDate()
                                .format(statDateFormatter),
                            accentColor = SuccessGreen
                        )
                    }
                    peakExpense?.let { point ->
                        TrendInsightCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.reports_highest_spending_day),
                            amountText = point.expenseCents.toCurrencyString(currencyCode),
                            dateText = Instant.ofEpochMilli(point.dayStartMillis)
                                .atZone(zoneId)
                                .toLocalDate()
                                .format(statDateFormatter),
                            accentColor = ErrorRed
                        )
                    }
                }
            }

            if (points.size < 2 || !hasActivity) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasActivity) {
                            stringResource(R.string.reports_not_enough_data)
                        } else {
                            stringResource(R.string.reports_no_money_movement)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                val incomeValues = remember(points) { points.map { it.incomeCents / 100f } }
                val expenseValues = remember(points) { points.map { it.expenseCents / 100f } }
                val netValues = remember(points) { points.map { it.netCents / 100f } }
                val allValues = remember(incomeValues, expenseValues, netValues) {
                    incomeValues + expenseValues + netValues
                }
                val rawMin = remember(allValues) { minOf(0f, allValues.minOrNull() ?: 0f) }
                val rawMax = remember(allValues) { max(0f, allValues.maxOrNull() ?: 0f) }
                val paddedMin = remember(rawMin, rawMax) {
                    if (rawMin < 0f) rawMin - (abs(rawMin) * 0.12f) else rawMin
                }
                val paddedMax = remember(rawMax) {
                    if (rawMax > 0f) rawMax + (rawMax * 0.12f) else 1f
                }
                val axisValues = remember(rawMin, rawMax) {
                    buildList {
                        add(rawMax)
                        if (rawMin < 0f && rawMax > 0f) add(0f)
                        add(rawMin)
                    }.distinctBy { (it * 100).toInt() }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    val leftPadding = 52.dp.toPx()
                    val rightPadding = 10.dp.toPx()
                    val topPadding = 10.dp.toPx()
                    val bottomPadding = 12.dp.toPx()
                    val plotWidth = (size.width - leftPadding - rightPadding).coerceAtLeast(1f)
                    val plotHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
                    val chartLeft = leftPadding
                    val chartRight = size.width - rightPadding
                    val chartTop = topPadding
                    val chartBottom = size.height - bottomPadding
                    val stepX = if (points.size == 1) 0f else plotWidth / (points.size - 1)
                    val valueRange = (paddedMax - paddedMin).takeIf { it > 0f } ?: 1f

                    fun xForIndex(index: Int): Float = chartLeft + (index * stepX)

                    fun yForValue(value: Float): Float {
                        val normalized = (value - paddedMin) / valueRange
                        return chartBottom - (normalized * plotHeight)
                    }

                    axisValues.forEach { value ->
                        val y = yForValue(value)
                        drawLine(
                            color = if (value == 0f) zeroLineColor else gridLineColor,
                            start = Offset(chartLeft, y),
                            end = Offset(chartRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (value == 0f) {
                                PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            } else {
                                null
                            }
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            formatTrendAxisValue(value, currencyCode, appLocale),
                            0f,
                            y + (axisLabelPaint.textSize / 3f),
                            axisLabelPaint
                        )
                    }

                    xAxisIndices.forEach { index ->
                        val x = xForIndex(index)
                        drawLine(
                            color = verticalGridColor,
                            start = Offset(x, chartTop),
                            end = Offset(x, chartBottom),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    fun drawTrendLine(
                        values: List<Float>,
                        color: Color,
                        dashed: Boolean = false
                    ) {
                        val path = androidx.compose.ui.graphics.Path()
                        values.forEachIndexed { index, value ->
                            val point = Offset(xForIndex(index), yForValue(value))
                            if (index == 0) {
                                path.moveTo(point.x, point.y)
                            } else {
                                path.lineTo(point.x, point.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = if (dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                } else {
                                    null
                                }
                            )
                        )
                    }

                    drawTrendLine(netValues, YellowPrimary, dashed = true)
                    drawTrendLine(incomeValues, SuccessGreen)
                    drawTrendLine(expenseValues, ErrorRed)

                    listOf(
                        incomeValues.lastOrNull() to SuccessGreen,
                        expenseValues.lastOrNull() to ErrorRed,
                        netValues.lastOrNull() to YellowPrimary
                    ).forEach { (value, color) ->
                        if (value == null) return@forEach
                        drawCircle(
                            color = color,
                            radius = 4.dp.toPx(),
                            center = Offset(xForIndex(points.lastIndex), yForValue(value))
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    xAxisIndices.forEach { index ->
                        Text(
                            text = Instant.ofEpochMilli(points[index].dayStartMillis)
                                .atZone(zoneId)
                                .toLocalDate()
                                .format(xAxisDateFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(stringResource(R.string.dashboard_income), SuccessGreen)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(stringResource(R.string.dashboard_expense), ErrorRed)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(stringResource(R.string.reports_net_short), YellowPrimary, dashed = true)
            }
        }
    }
}

@Composable
private fun TrendInsightCard(
    title: String,
    amountText: String,
    dateText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = 16.dp, height = 8.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (dashed) {
                    PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                } else {
                    null
                }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatTrendAxisValue(
    value: Float,
    currencyCode: String,
    locale: Locale
): String {
    if (value == 0f) return "0"
    val symbol = runCatching { Currency.getInstance(currencyCode).getSymbol(locale) }
        .getOrDefault(currencyCode)
    val absValue = abs(value)
    val compact = when {
        absValue >= 1_000_000f -> String.format(locale, "%.1fM", absValue / 1_000_000f)
        absValue >= 1_000f -> String.format(locale, "%.1fK", absValue / 1_000f)
        else -> String.format(locale, "%.0f", absValue)
    }
    return if (value < 0f) {
        "-$symbol$compact"
    } else {
        "$symbol$compact"
    }
}

@Composable
private fun ReportsPremiumSection(
    nudgeVariant: PremiumEmotionVariant,
    currencyCode: String,
    appLanguageTag: String,
    previewOpportunity: PremiumOpportunity?,
    onUpgrade: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.reports_premium_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        LockedPremiumCard(
            variant = nudgeVariant,
            featureName = localizedAutoCapturePremiumBundleLabel(appLanguageTag),
            onUpgrade = onUpgrade
        )
        previewOpportunity?.let { opportunity ->
            PremiumOpportunityCard(
                opportunity = opportunity,
                currencyCode = currencyCode,
                appLanguageTag = appLanguageTag,
                actionText = premiumOpportunityActionLabel(
                    languageTag = appLanguageTag,
                    hasPremiumAccess = false
                ),
                onAction = onUpgrade
            )
        }
        LockedReportItem(localizedAutoCapturePremiumFeatureLabel(appLanguageTag))
        LockedReportItem(stringResource(R.string.subscription_benefit_export))
        LockedReportItem(stringResource(R.string.reports_locked_feature_mochi_chat))
        LockedReportItem(stringResource(R.string.reports_locked_feature_monthly_insights))
        LockedReportItem(stringResource(R.string.reports_locked_feature_budget_suggestions))
        LockedReportItem(stringResource(R.string.reports_locked_feature_recurring_totals))
        LockedReportItem(stringResource(R.string.reports_locked_feature_recurring_income_vs_expense))
        LockedReportItem(stringResource(R.string.reports_locked_feature_date_range_analysis))
    }
}

@Composable
private fun AdvancedReportsSection(
    hasPremiumAccess: Boolean,
    isTrialActive: Boolean,
    nudgeVariant: PremiumEmotionVariant,
    onUpgrade: () -> Unit,
    onOpen: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reports_recurring_reports_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFF332F00), // Dark Yellow
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = YellowPrimary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.reports_premium_badge), style = MaterialTheme.typography.labelSmall, color = YellowPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        if (hasPremiumAccess) {
            Text(
                text = if (isTrialActive) {
                    stringResource(R.string.reports_trial_recurring_reports)
                } else {
                    stringResource(R.string.reports_recurring_reports_message)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = YellowPrimary, contentColor = BlackBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.reports_open_recurring_reports), fontWeight = FontWeight.Bold)
            }
        } else {
            LockedPremiumCard(
                variant = nudgeVariant,
                featureName = stringResource(R.string.reports_feature_recurring_reports),
                onUpgrade = onUpgrade
            )
            LockedReportItem(stringResource(R.string.reports_locked_feature_recurring_totals))
            LockedReportItem(stringResource(R.string.reports_locked_feature_recurring_income_vs_expense))
            LockedReportItem(stringResource(R.string.reports_locked_feature_date_range_analysis))
        }
    }
}

@Composable
private fun AiInsightsSection(
    hasPremiumAccess: Boolean,
    isTrialActive: Boolean,
    nudgeVariant: PremiumEmotionVariant,
    currencyCode: String,
    appLanguageTag: String,
    monthlyInsights: List<MonthlyInsightUi>,
    budgetSuggestions: List<BudgetSuggestionUi>,
    savingsOpportunities: List<PremiumOpportunity>,
    onUpgrade: () -> Unit,
    onOpenChat: (String?) -> Unit
) {
    val summary = remember(savingsOpportunities, currencyCode, appLanguageTag) {
        formatPremiumOutcomeSummary(
            opportunities = savingsOpportunities,
            currencyCode = currencyCode,
            languageTag = appLanguageTag,
            hasPremiumAccess = true
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reports_mochi_insights_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color(0xFF332F00),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = YellowPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.reports_premium_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = YellowPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (!hasPremiumAccess) {
            LockedPremiumCard(
                variant = nudgeVariant,
                featureName = stringResource(R.string.ai_chat_title),
                onUpgrade = onUpgrade
            )
            return@Column
        }

        if (isTrialActive) {
            Text(
                text = stringResource(R.string.reports_trial_mochi_active),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedButton(
            onClick = { onOpenChat(null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.reports_open_mochi), fontWeight = FontWeight.Bold)
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    premiumOpportunitySectionTitle(appLanguageTag),
                    fontWeight = FontWeight.SemiBold
                )
                MochiValueTrackerCard(
                    opportunities = savingsOpportunities,
                    currencyCode = currencyCode,
                    appLanguageTag = appLanguageTag,
                    hasPremiumAccess = true
                )
                summary?.let {
                    Text(
                        text = it.headline,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = it.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (savingsOpportunities.isEmpty()) {
                    Text(
                        text = premiumOpportunityEmptyText(appLanguageTag),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    savingsOpportunities.forEach { opportunity ->
                        PremiumOpportunityCard(
                            opportunity = opportunity,
                            currencyCode = currencyCode,
                            appLanguageTag = appLanguageTag,
                            actionText = premiumOpportunityActionLabel(
                                languageTag = appLanguageTag,
                                hasPremiumAccess = true
                            ),
                            onAction = {
                                onOpenChat(
                                    buildMochiPromptForOpportunity(
                                        opportunity = opportunity,
                                        currencyCode = currencyCode,
                                        languageTag = appLanguageTag
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.reports_monthly_spending_insights_title), fontWeight = FontWeight.SemiBold)
                if (monthlyInsights.isEmpty()) {
                    Text(
                        text = stringResource(R.string.reports_not_enough_data_track_more),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    monthlyInsights.forEach { insight ->
                        MonthlyInsightRow(insight, currencyCode)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.reports_budget_suggestions_last_three_months), fontWeight = FontWeight.SemiBold)
                if (budgetSuggestions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.reports_no_suggestions_track_more),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    budgetSuggestions.forEach { suggestion ->
                        BudgetSuggestionRow(suggestion, currencyCode)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedPremiumCard(
    variant: PremiumEmotionVariant,
    featureName: String,
    onUpgrade: () -> Unit
) {
    val copy = MochiPremiumCopywriter.lockedFeature(
        variant = variant,
        featureName = featureName
    )
    MochiPremiumNudgeCard(
        copy = copy,
        primaryCtaText = stringResource(R.string.ai_chat_go_premium),
        onPrimaryCta = onUpgrade
    )
}

@Composable
private fun MonthlyInsightRow(
    insight: MonthlyInsightUi,
    currencyCode: String
) {
    val direction = if (insight.changePercent >= 0) {
        stringResource(R.string.reports_more)
    } else {
        stringResource(R.string.reports_less)
    }
    val color = if (insight.changePercent >= 0) ErrorRed else SuccessGreen
    val percent = kotlin.math.abs(insight.changePercent).toInt()
    Text(
        text = stringResource(
            R.string.reports_monthly_insight_row,
            insight.categoryName,
            percent,
            direction,
            insight.currentCents.toCurrencyString(currencyCode)
        ),
        color = color,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun BudgetSuggestionRow(
    suggestion: BudgetSuggestionUi,
    currencyCode: String
) {
    Text(
        text = stringResource(
            R.string.reports_budget_suggestion_row,
            suggestion.categoryName,
            suggestion.suggestedBudgetCents.toCurrencyString(currencyCode),
            suggestion.averageMonthlyCents.toCurrencyString(currencyCode)
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun LockedReportItem(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.reports_locked_cd), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun shareBytesAsFile(
    context: Context,
    bytes: ByteArray,
    fileName: String,
    mimeType: String
) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportDir, fileName)
    file.writeBytes(bytes)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, context.getString(R.string.export_data_share_csv))
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

