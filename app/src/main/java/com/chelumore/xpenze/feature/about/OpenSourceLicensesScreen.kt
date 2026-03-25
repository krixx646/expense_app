package com.chelumore.xpenze.feature.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.ui.theme.YellowPrimary

private data class LicenseItem(
    val name: String,
    val licenseName: String,
    val url: String
)

@Composable
fun OpenSourceLicensesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val licenses = listOf(
        LicenseItem("Kotlin", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
        LicenseItem("Jetpack Compose", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
        LicenseItem("AndroidX Room", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
        LicenseItem("Hilt", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
        LicenseItem("WorkManager", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
        LicenseItem("Firebase SDK", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
        LicenseItem("Coil", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
    )

    Scaffold(
        topBar = { XpenzeTopAppBar(title = stringResource(R.string.settings_open_source_licenses), onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Xpenze uses open source software. Licenses are provided by their respective authors.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            licenses.forEach { item ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.openUrl(item.url) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = YellowPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(item.licenseName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun android.content.Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}


