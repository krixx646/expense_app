package com.chelumore.xpenze.feature.accounts.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.common.toCurrencyString
import com.chelumore.xpenze.core.ui.components.display.AccountCard
import com.chelumore.xpenze.core.ui.components.input.DropdownItem
import com.chelumore.xpenze.core.ui.components.input.DropdownSelector
import com.chelumore.xpenze.domain.model.Transaction
import com.chelumore.xpenze.domain.model.TransactionType
import com.chelumore.xpenze.domain.repository.AccountCardStyle
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.ErrorRed
import com.chelumore.xpenze.ui.theme.SuccessGreen
import com.chelumore.xpenze.ui.theme.YellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToTransfer: () -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(accountId) {
        viewModel.load(accountId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.account_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(accountId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.transaction_form_title_edit))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = YellowPrimary)
            }
            return@Scaffold
        }

        val account = uiState.account
        if (account == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.account_detail_not_found), color = MaterialTheme.colorScheme.onBackground)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AccountCard(
                    account = account,
                    style = uiState.accountCardStyleOverride ?: uiState.globalCardStyle
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        DropdownSelector(
                            label = stringResource(R.string.account_detail_style_label),
                            items = accountCardStyleItems(),
                            selectedId = uiState.accountCardStyleOverride?.name,
                            onSelected = { selectedId ->
                                val style = selectedId?.let { value ->
                                    runCatching { AccountCardStyle.valueOf(value) }.getOrNull()
                                }
                                viewModel.setCardStyleOverride(style)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = stringResource(
                                R.string.account_detail_inherit_global,
                                accountCardStyleLabel(uiState.globalCardStyle)
                            ),
                            allowClearSelection = true,
                            clearLabel = stringResource(
                                R.string.account_detail_inherit_global,
                                accountCardStyleLabel(uiState.globalCardStyle)
                            )
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.dashboard_transfer),
                        icon = Icons.Default.SwapHoriz,
                        container = YellowPrimary,
                        content = BlackBackground,
                        onClick = onNavigateToTransfer
                    )
                    ActionPill(
                        modifier = Modifier.weight(1f),
                        label = if (account.isArchived) {
                            stringResource(R.string.account_detail_unarchive)
                        } else {
                            stringResource(R.string.account_detail_archive)
                        },
                        icon = null,
                        container = MaterialTheme.colorScheme.surface,
                        content = MaterialTheme.colorScheme.onSurface,
                        onClick = viewModel::toggleArchive
                    )
                }
            }

            item {
                ActionPill(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.settings_delete_account),
                    icon = Icons.Default.Delete,
                    container = ErrorRed.copy(alpha = 0.12f),
                    content = ErrorRed,
                    onClick = viewModel::deleteAccount
                )
            }

            item {
                Text(
                    text = stringResource(R.string.dashboard_recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.transactions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                stringResource(R.string.dashboard_no_transactions_yet),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.account_detail_activity_here),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.transactions.size) { index ->
                    TransactionRow(
                        tx = uiState.transactions[index],
                        accountId = accountId,
                        currency = account.currency
                    )
                }
            }
        }
    }
}

@Composable
private fun accountCardStyleItems(): List<DropdownItem> = listOf(
    DropdownItem(id = AccountCardStyle.MATTE.name, label = accountCardStyleLabel(AccountCardStyle.MATTE)),
    DropdownItem(id = AccountCardStyle.METALLIC.name, label = accountCardStyleLabel(AccountCardStyle.METALLIC)),
    DropdownItem(id = AccountCardStyle.NEON.name, label = accountCardStyleLabel(AccountCardStyle.NEON)),
    DropdownItem(id = AccountCardStyle.SUNSET.name, label = accountCardStyleLabel(AccountCardStyle.SUNSET)),
    DropdownItem(id = AccountCardStyle.OCEAN.name, label = accountCardStyleLabel(AccountCardStyle.OCEAN)),
    DropdownItem(id = AccountCardStyle.ROYAL.name, label = accountCardStyleLabel(AccountCardStyle.ROYAL))
)

@Composable
private fun accountCardStyleLabel(style: AccountCardStyle): String = when (style) {
    AccountCardStyle.MATTE -> stringResource(R.string.settings_account_card_style_matte)
    AccountCardStyle.METALLIC -> stringResource(R.string.settings_account_card_style_metallic)
    AccountCardStyle.NEON -> stringResource(R.string.settings_account_card_style_neon)
    AccountCardStyle.SUNSET -> stringResource(R.string.settings_account_card_style_sunset)
    AccountCardStyle.OCEAN -> stringResource(R.string.settings_account_card_style_ocean)
    AccountCardStyle.ROYAL -> stringResource(R.string.settings_account_card_style_royal)
}

@Composable
private fun ActionPill(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text(label, color = content, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransactionRow(tx: Transaction, accountId: String, currency: String) {
    val (sign, color) = when (tx.type) {
        TransactionType.INCOME -> "+" to SuccessGreen
        TransactionType.EXPENSE -> "-" to ErrorRed
        TransactionType.TRANSFER -> {
            if (tx.accountId == accountId) "-" to ErrorRed else "+" to SuccessGreen
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                val subtitle = when (tx.type) {
                    TransactionType.TRANSFER -> stringResource(R.string.dashboard_transfer)
                    TransactionType.INCOME -> stringResource(R.string.dashboard_income)
                    TransactionType.EXPENSE -> stringResource(R.string.dashboard_expense)
                }
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = "$sign${tx.money.amountCents.toCurrencyString(currency)}",
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}


