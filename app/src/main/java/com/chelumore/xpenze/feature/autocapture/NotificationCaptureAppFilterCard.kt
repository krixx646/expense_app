package com.chelumore.xpenze.feature.autocapture

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppFilterAllSummary
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppFilterChooseButton
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppFilterResetButton
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppFilterSelectedSummary
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppFilterTitle
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppPickerBody
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppPickerEmptyResults
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppPickerSearchPlaceholder
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppPickerSelectedCount
import com.chelumore.xpenze.core.localization.localizedAutoCaptureNotificationAppPickerTitle
import com.chelumore.xpenze.core.ui.components.layout.XpenzeBottomSheet
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.YellowPrimary
import java.util.Locale

data class NotificationCaptureAppOption(
    val packageName: String,
    val label: String
)

@Composable
fun rememberNotificationCaptureAppOptions(): List<NotificationCaptureAppOption> {
    val context = LocalContext.current
    return remember(context) {
        loadNotificationCaptureAppOptions(context)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationCaptureAppFilterCard(
    languageTag: String,
    selectedPackages: Set<String>,
    installedApps: List<NotificationCaptureAppOption>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onChooseApps: () -> Unit,
    onClearSelection: () -> Unit
) {
    val selectedLabels = remember(selectedPackages, installedApps) {
        resolveSelectedAppLabels(selectedPackages, installedApps)
    }
    val summary = if (selectedPackages.isEmpty()) {
        localizedAutoCaptureNotificationAppFilterAllSummary(languageTag)
    } else {
        localizedAutoCaptureNotificationAppFilterSelectedSummary(selectedPackages.size, languageTag)
    }

    Card(
        modifier = modifier.alpha(if (enabled) 1f else 0.58f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(InfoBlue.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = InfoBlue
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = localizedAutoCaptureNotificationAppFilterTitle(languageTag),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedLabels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedLabels.take(3).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onChooseApps,
                    enabled = enabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = BlackBackground
                    )
                ) {
                    Text(
                        text = localizedAutoCaptureNotificationAppFilterChooseButton(languageTag),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (selectedPackages.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearSelection,
                        enabled = enabled,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = localizedAutoCaptureNotificationAppFilterResetButton(languageTag),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCaptureAppSelectionSheet(
    languageTag: String,
    installedApps: List<NotificationCaptureAppOption>,
    initialSelection: Set<String>,
    onDismiss: () -> Unit,
    onSaveSelection: (Set<String>) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPackages by rememberSaveable {
        mutableStateOf(initialSelection.toList())
    }
    val filteredApps = remember(query, installedApps) {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { option ->
                option.label.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    option.packageName.lowercase(Locale.ROOT).contains(normalizedQuery)
            }
        }
    }

    XpenzeBottomSheet(
        onDismissRequest = onDismiss,
        title = localizedAutoCaptureNotificationAppPickerTitle(languageTag)
    ) {
        Text(
            text = localizedAutoCaptureNotificationAppPickerBody(languageTag),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = {
                Text(localizedAutoCaptureNotificationAppPickerSearchPlaceholder(languageTag))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = localizedAutoCaptureNotificationAppPickerSelectedCount(
                selectedCount = selectedPackages.size,
                languageTag = languageTag
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
        ) {
            if (filteredApps.isEmpty()) {
                Text(
                    text = localizedAutoCaptureNotificationAppPickerEmptyResults(languageTag),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { option ->
                        NotificationCaptureAppRow(
                            option = option,
                            checked = option.packageName in selectedPackages,
                            onToggle = {
                                val next = selectedPackages.toMutableSet()
                                if (!next.add(option.packageName)) {
                                    next.remove(option.packageName)
                                }
                                selectedPackages = next.toList()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.common_cancel), fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = { onSaveSelection(selectedPackages.toSet()) },
                enabled = selectedPackages.isNotEmpty(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = YellowPrimary,
                    contentColor = BlackBackground
                )
            ) {
                Text(stringResource(R.string.common_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotificationCaptureAppRow(
    option: NotificationCaptureAppOption,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (checked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(InfoBlue.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = option.label.take(1).uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = InfoBlue
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = option.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() }
        )
    }
}

private fun loadNotificationCaptureAppOptions(context: Context): List<NotificationCaptureAppOption> {
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    return context.packageManager.queryIntentActivities(launcherIntent, 0)
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName?.trim().orEmpty()
            if (packageName.isBlank() || packageName == context.packageName) return@mapNotNull null
            val label = resolveInfo.loadLabel(context.packageManager)
                ?.toString()
                ?.trim()
                ?.ifBlank { packageName }
                ?: packageName
            NotificationCaptureAppOption(
                packageName = packageName,
                label = label
            )
        }
        .distinctBy { it.packageName }
        .sortedWith(
            compareBy<NotificationCaptureAppOption> { it.label.lowercase(Locale.ROOT) }
                .thenBy { it.packageName }
        )
}

private fun resolveSelectedAppLabels(
    selectedPackages: Set<String>,
    installedApps: List<NotificationCaptureAppOption>
): List<String> {
    if (selectedPackages.isEmpty()) return emptyList()
    val appMap = installedApps.associateBy { it.packageName }
    return selectedPackages
        .map { packageName -> appMap[packageName]?.label ?: packageName }
        .sortedBy { it.lowercase(Locale.ROOT) }
}
