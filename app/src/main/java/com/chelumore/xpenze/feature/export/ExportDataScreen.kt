package com.chelumore.xpenze.feature.export

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.ui.components.input.DatePickerField
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.YellowPrimary
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ExportDataScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: ExportDataViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    data class PendingSave(
        val bytes: ByteArray,
        val fileName: String,
        val mimeType: String
    )

    var pendingSave by remember { mutableStateOf<PendingSave?>(null) }

    val pickCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        }.onSuccess { csv ->
            if (csv.isBlank()) {
                // ignore
            } else {
                viewModel.importCsv(csv)
            }
        }.onFailure {
            // no-op; ViewModel will show message on failures it owns
        }
    }

    val saveCsvDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val payload = pendingSave
        pendingSave = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(payload.bytes)
                out.flush()
            } ?: throw IllegalStateException("Unable to open destination")
        }.onSuccess {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.export_data_saved, payload.fileName)) }
        }.onFailure { e ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    e.message ?: context.getString(R.string.export_data_save_failed)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ExportDataEvent.Message -> snackbarHostState.showSnackbar(event.text)
                is ExportDataEvent.ShareFile -> shareBytesAsFile(
                    context = context,
                    bytes = event.bytes,
                    fileName = event.fileName,
                    mimeType = event.mimeType
                )
                is ExportDataEvent.SaveFile -> {
                    pendingSave = PendingSave(
                        bytes = event.bytes,
                        fileName = event.fileName,
                        mimeType = event.mimeType
                    )
                    saveCsvDocument.launch(event.fileName)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            XpenzeTopAppBar(title = stringResource(R.string.settings_export_import), onNavigateBack = onNavigateBack)
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
                    .widthIn(max = 820.dp)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

            Text(
                    text = stringResource(R.string.export_data_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Date range (optional)
            Text(
                    text = stringResource(R.string.export_data_optional_date_range),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DatePickerField(
                        label = stringResource(R.string.export_data_start),
                    dateMillis = uiState.startDateMillis,
                    onDateMillisChange = { viewModel.setStartDate(it) },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                        label = stringResource(R.string.export_data_end),
                    dateMillis = uiState.endDateMillis,
                    onDateMillisChange = { viewModel.setEndDate(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (uiState.startDateMillis != null || uiState.endDateMillis != null) {
                Text(
                    text = stringResource(R.string.export_data_clear_date_range),
                    color = YellowPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(enabled = !uiState.isWorking) { viewModel.clearRange() }
                        .padding(top = 4.dp)
                )
            }

            // Export
            SectionCard(title = stringResource(R.string.export_data_section_export)) {
                if (uiState.hasPremiumAccess) {
                    Button(
                        onClick = viewModel::exportCsv,
                        enabled = !uiState.isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowPrimary,
                            contentColor = BlackBackground,
                            disabledContainerColor = YellowPrimary.copy(alpha = 0.35f),
                            disabledContentColor = BlackBackground.copy(alpha = 0.7f)
                        )
                    ) {
                        if (uiState.isWorking) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = BlackBackground, strokeWidth = 2.dp)
                        } else {
                            androidx.compose.material3.Icon(Icons.Default.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(stringResource(R.string.export_data_share_csv), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = viewModel::saveCsv,
                        enabled = !uiState.isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.export_data_save_csv), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.export_data_supported),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.export_data_premium_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToSubscription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YellowPrimary,
                            contentColor = BlackBackground
                        )
                    ) {
                        Text(stringResource(R.string.export_data_upgrade_cta), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Import
            SectionCard(title = stringResource(R.string.export_data_section_import)) {
                Button(
                    onClick = { pickCsv.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                    enabled = !uiState.isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (uiState.isWorking) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = YellowPrimary, strokeWidth = 2.dp)
                    } else {
                        androidx.compose.material3.Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stringResource(R.string.export_data_choose_csv), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.export_data_import_tip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
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


