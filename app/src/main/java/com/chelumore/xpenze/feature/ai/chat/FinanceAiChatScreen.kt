package com.chelumore.xpenze.feature.ai.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chelumore.xpenze.R
import com.chelumore.xpenze.core.growth.MochiPremiumCopywriter
import com.chelumore.xpenze.core.growth.mochiSuggestedPromptTitle
import com.chelumore.xpenze.core.growth.PremiumEmotionVariant
import com.chelumore.xpenze.core.ui.components.growth.MochiPremiumNudgeCard
import com.chelumore.xpenze.core.ui.components.layout.XpenzeTopAppBar
import com.chelumore.xpenze.ui.theme.BlackBackground
import com.chelumore.xpenze.ui.theme.InfoBlue
import com.chelumore.xpenze.ui.theme.YellowPrimary
import kotlinx.coroutines.launch

@Composable
fun FinanceAiChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    initialPrompt: String? = null,
    viewModel: FinanceAiChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val chatListState = rememberLazyListState()
    var showNewChatDialog by remember { mutableStateOf(false) }
    val copiedMessageText = stringResource(R.string.ai_chat_message_copied)
    val newChatStartedText = stringResource(R.string.ai_chat_new_chat_started)

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            viewModel.prefillInput(initialPrompt)
        }
    }

    LaunchedEffect(uiState.hasPremiumAccess, uiState.messages.size, uiState.isSending) {
        if (!uiState.hasPremiumAccess) return@LaunchedEffect
        val extraItems = if (uiState.isSending) 1 else 0
        val targetIndex = uiState.messages.lastIndex + extraItems
        if (targetIndex >= 0) {
            runCatching { chatListState.animateScrollToItem(targetIndex) }
        }
    }

    Scaffold(
        topBar = {
            XpenzeTopAppBar(
                title = stringResource(R.string.ai_chat_title),
                onNavigateBack = onNavigateBack,
                titleContent = {
                    MochiChatTitle()
                },
                actions = {
                    IconButton(onClick = { showNewChatDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.ai_chat_cd_new_chat)
                        )
                    }
                }
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
                    .fillMaxHeight()
            ) {
                if (!uiState.hasPremiumAccess) {
                    LockedAiCoachCard(
                        variant = uiState.nudgeVariant,
                        onNavigateToSubscription = onNavigateToSubscription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    return@Column
                }

                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            MochiWelcomeCard(
                                appLanguageTag = uiState.appLanguageTag,
                                suggestedPrompts = uiState.suggestedPrompts,
                                isSending = uiState.isSending,
                                onPromptClick = viewModel::sendSuggestedPrompt
                            )
                        }
                    }
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = { text ->
                                clipboardManager.setText(AnnotatedString(text))
                                scope.launch {
                                    snackbarHostState.showSnackbar(copiedMessageText)
                                }
                            }
                        )
                    }
                    if (uiState.isSending) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = YellowPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.ai_chat_thinking),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::setInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    placeholder = { Text(stringResource(R.string.ai_chat_placeholder)) },
                    minLines = 1,
                    maxLines = 5,
                    shape = RoundedCornerShape(32.dp),
                    enabled = !uiState.isSending,
                    trailingIcon = {
                        IconButton(
                            onClick = viewModel::sendMessage,
                            enabled = uiState.inputText.isNotBlank() && !uiState.isSending
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.ai_chat_send),
                                tint = if (uiState.inputText.isNotBlank() && !uiState.isSending) {
                                    YellowPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text(stringResource(R.string.ai_chat_new_chat_title)) },
            text = { Text(stringResource(R.string.ai_chat_new_chat_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewChatDialog = false
                        viewModel.startNewChat()
                        scope.launch {
                            snackbarHostState.showSnackbar(newChatStartedText)
                        }
                    }
                ) {
                    Text(stringResource(R.string.ai_chat_new_chat_confirm), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun MochiWelcomeCard(
    appLanguageTag: String,
    suggestedPrompts: List<String>,
    isSending: Boolean,
    onPromptClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_chat_welcome_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.ai_chat_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = mochiSuggestedPromptTitle(appLanguageTag),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            suggestedPrompts.forEach { prompt ->
                Button(
                    onClick = { onPromptClick(prompt) },
                    enabled = !isSending,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (suggestedPrompts.isEmpty()) {
                Text(
                    text = stringResource(R.string.ai_chat_welcome_examples),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.ai_chat_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MochiChatTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(YellowPrimary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🐱",
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = stringResource(R.string.ai_chat_title),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(
    message: FinanceChatMessageUi,
    onCopy: (String) -> Unit
) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bg = if (message.isUser) YellowPrimary else MaterialTheme.colorScheme.surface
    val fg = if (message.isUser) BlackBackground else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val blocks = remember(message.text) { parseMessageBlocks(message.text) }
        Box(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .background(bg, RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = { onCopy(message.text) },
                    onLongClick = { onCopy(message.text) }
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                blocks.forEachIndexed { index, block ->
                    when (block) {
                        is MessageContentBlock.Paragraph -> {
                            Text(
                                text = block.text,
                                color = fg,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is MessageContentBlock.MarkdownTable -> {
                            MarkdownTableBlock(
                                table = block,
                                isUser = message.isUser,
                                textColor = fg
                            )
                        }
                    }
                    if (index != blocks.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onCopy(message.text) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_chat_copy),
                            color = fg.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    IconButton(
                        onClick = { onCopy(message.text) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.ai_chat_copy),
                            tint = fg.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTableBlock(
    table: MessageContentBlock.MarkdownTable,
    isUser: Boolean,
    textColor: androidx.compose.ui.graphics.Color
) {
    val columnCount = table.header.size.coerceAtLeast(1)
    val gridColor = if (isUser) {
        BlackBackground.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    }
    val headerBackground = if (isUser) {
        BlackBackground.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    }
    val rowBackground = if (isUser) {
        BlackBackground.copy(alpha = 0.04f)
    } else {
        MaterialTheme.colorScheme.background.copy(alpha = 0.90f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .background(gridColor, RoundedCornerShape(12.dp))
                .padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            TableRow(
                cells = table.header,
                backgroundColor = headerBackground,
                textColor = textColor,
                isHeader = true,
                columnCount = columnCount
            )
            table.rows.forEachIndexed { rowIndex, row ->
                TableRow(
                    cells = normalizeTableRow(row, columnCount),
                    backgroundColor = if (rowIndex % 2 == 0) rowBackground else rowBackground.copy(alpha = 0.86f),
                    textColor = textColor,
                    isHeader = false,
                    columnCount = columnCount
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    backgroundColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    isHeader: Boolean,
    columnCount: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        normalizeTableRow(cells, columnCount).forEach { cell ->
            Box(
                modifier = Modifier
                    .widthIn(min = 116.dp)
                    .background(backgroundColor)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = cell,
                    color = textColor,
                    style = if (isHeader) {
                        MaterialTheme.typography.labelMedium
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                    fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = if (isHeader) FontFamily.Default else FontFamily.Monospace
                )
            }
        }
    }
}

private sealed interface MessageContentBlock {
    data class Paragraph(val text: String) : MessageContentBlock
    data class MarkdownTable(
        val header: List<String>,
        val rows: List<List<String>>
    ) : MessageContentBlock
}

private fun parseMessageBlocks(text: String): List<MessageContentBlock> {
    val lines = text.replace("\r\n", "\n").lines()
    val blocks = mutableListOf<MessageContentBlock>()
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        paragraphLines.joinToString("\n")
            .split(Regex("""\n\s*\n"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { blocks += MessageContentBlock.Paragraph(it) }
        paragraphLines.clear()
    }

    var index = 0
    while (index < lines.size) {
        if (looksLikeMarkdownTableStart(lines, index)) {
            flushParagraph()
            val header = parseTableCells(lines[index])
            index += 2
            val rows = mutableListOf<List<String>>()
            while (index < lines.size && looksLikeMarkdownTableRow(lines[index])) {
                rows += parseTableCells(lines[index])
                index += 1
            }
            if (header.size >= 2 && rows.isNotEmpty()) {
                blocks += MessageContentBlock.MarkdownTable(
                    header = header,
                    rows = rows
                )
            } else {
                paragraphLines += lines[index - 2]
                paragraphLines += lines[index - 1]
                rows.forEach { row -> paragraphLines += row.joinToString(" | ") }
            }
        } else {
            paragraphLines += lines[index]
            index += 1
        }
    }

    flushParagraph()
    return blocks.ifEmpty { listOf(MessageContentBlock.Paragraph(text.trim())) }
}

private fun looksLikeMarkdownTableStart(lines: List<String>, index: Int): Boolean {
    if (index + 1 >= lines.size) return false
    val header = lines[index]
    val separator = lines[index + 1]
    return looksLikeMarkdownTableRow(header) &&
        parseTableCells(header).size >= 2 &&
        isMarkdownTableSeparator(separator)
}

private fun looksLikeMarkdownTableRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.contains('|') && parseTableCells(trimmed).size >= 2
}

private fun isMarkdownTableSeparator(line: String): Boolean {
    val trimmed = line.trim().trim('|')
    if (trimmed.isBlank()) return false
    return trimmed.split('|')
        .map { it.trim() }
        .all { cell -> cell.matches(Regex(""":?-{3,}:?""")) }
}

private fun parseTableCells(line: String): List<String> {
    val trimmed = line.trim().trim('|')
    if (trimmed.isBlank()) return emptyList()
    return trimmed.split('|').map { it.trim() }
}

private fun normalizeTableRow(cells: List<String>, columnCount: Int): List<String> {
    return (cells + List(columnCount) { "" }).take(columnCount)
}

@Composable
private fun LockedAiCoachCard(
    variant: PremiumEmotionVariant,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = MochiPremiumCopywriter.lockedFeature(
        variant = variant,
        featureName = stringResource(R.string.ai_chat_title)
    )
    MochiPremiumNudgeCard(
        copy = copy,
        modifier = modifier,
        primaryCtaText = stringResource(R.string.ai_chat_go_premium),
        onPrimaryCta = onNavigateToSubscription
    )
}
