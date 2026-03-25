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
fun TermsOfServiceScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = { XpenzeTopAppBar(title = stringResource(R.string.settings_terms_of_service), onNavigateBack = onNavigateBack) },
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
                text = "Last updated: January 2, 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Title("Using the app")
            Body("Xpenze is provided to help you track personal finances. You are responsible for the accuracy of the information you enter and any decisions you make based on the app.")

            Title("No financial advice")
            Body("Xpenze does not provide financial, tax, or legal advice. Consider consulting a qualified professional if you need guidance.")

            Title("Availability")
            Body("We aim to keep the app reliable, but we do not guarantee uninterrupted operation. Features may change over time.")

            Title("Your data")
            Body("Your data is stored locally on your device. You are responsible for maintaining backups (for example, by exporting your data).")

            Title("Liability")
            Body("To the extent permitted by law, Xpenze is provided “as is” without warranties of any kind. We are not liable for damages arising from your use of the app.")

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Title(text: String) {
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


