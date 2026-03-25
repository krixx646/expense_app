package com.chelumore.xpenze.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar

@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = { XpenzeTopAppBar(title = stringResource(R.string.settings_privacy_policy), onNavigateBack = onNavigateBack) },
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

            Text(
                text = "Effective date: January 2, 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionTitle("Overview")
            Body(
                "Xpenze is an offline-first expense tracker. Your budgets, accounts, categories, and transactions are stored locally on your device."
            )

            SectionTitle("What data we store")
            Body(
                "The app stores the information you enter (for example: transactions, budgets, categories, and account balances). If you sign in, the app may store your account identifier and email locally to keep you signed in."
            )

            SectionTitle("What data we share")
            Body(
                "Xpenze does not sell your data. If you use authentication, your sign-in is handled by Firebase Authentication, which may process your email and authentication tokens to sign you in."
            )

            SectionTitle("Permissions")
            Body(
                "If you attach receipt images or choose a profile photo, the app requests access only to the specific file(s) you select through the system file picker."
            )

            SectionTitle("Notifications")
            Body(
                "If you enable notifications, the app may show budget alerts and daily summaries on your device. You can disable notifications at any time in Settings."
            )

            SectionTitle("Changes")
            Body(
                "We may update this policy as the app evolves. Any updates will be posted in-app with a new effective date."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth()
    )
}


