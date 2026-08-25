package com.Chenkham.Echofy.ui.screens.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.ai.AiCallState
import com.Chenkham.Echofy.ai.AttachedFile
import com.Chenkham.Echofy.ai.FileAttachmentParser
import com.Chenkham.Echofy.viewmodels.AiViewModel
import kotlinx.coroutines.launch

/**
 * Main AI Assistant Screen composable.
 * Displays flush AI header, Google Assistant interactive voice overlay, chat messages or welcome view, and bottom input bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // ── ViewModel State ──────────────────────────────────────────
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showHistoryDrawer by viewModel.showHistoryDrawer.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    val callState by viewModel.callState.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()

    // ── Local Screen State ───────────────────────────────────────
    var textInput by remember { mutableStateOf("") }
    var attachedFile by remember { mutableStateOf<AttachedFile?>(null) }
    val listState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch { attachedFile = FileAttachmentParser.parseFile(context, uri) }
        }
    }

    // ── Voice Activation Lifecycle ───────────────────────────────
    // Wake word activation is owned by MainActivity so it works on every tab.
    // This screen only hands over the live PlayerConnection for command execution.
    LaunchedEffect(playerConnection) {
        viewModel.attachPlayerConnection(playerConnection)
    }

    // ── Auto-scroll on new messages ──────────────────────────────
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    // ── Dialogs ──────────────────────────────────────────────────
    if (showSettingsDialog) {
        MultiProviderSettingsDialog(
            profiles = profiles,
            activeProfileId = activeProfileId,
            onDismiss = viewModel::dismissSettingsDialog,
            onSaveProfiles = { updatedList, newActiveId ->
                viewModel.saveProfiles(updatedList, newActiveId)
                viewModel.dismissSettingsDialog()
            }
        )
    }

    if (showHistoryDrawer) {
        ChatHistoryDrawer(
            sessions = sessions,
            activeSessionId = activeSessionId,
            onDismiss = viewModel::dismissHistoryDrawer,
            onSelectSession = viewModel::selectSession,
            onNewChat = viewModel::createNewSession,
            onDeleteSession = viewModel::deleteSession
        )
    }

    // ── Main Layout ──────────────────────────────────────────────
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomInset)
            .imePadding(),
        color = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {

            // 1. Top Bar (Clean AI Header, flush at top)
            AiTopBar(
                onHistoryClick = viewModel::toggleHistoryDrawer
            )

            // 2. Main Content Area (Chat Messages or Welcome View)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (chatHistory.isEmpty()) {
                    AiWelcomeView(
                        onSuggestionClick = { suggestion ->
                            viewModel.sendTextMessage(suggestion, playerConnection)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(chatHistory, key = { it.id }) { msg ->
                            ChatBubble(msg)
                        }

                        if (callState == AiCallState.THINKING) {
                            item { ThinkingIndicator() }
                        }
                    }
                }
            }

            // 3. Bottom Input Bar
            AiInputBar(
                textInput = textInput,
                onTextChange = { textInput = it },
                attachedFile = attachedFile,
                onAttachClick = { filePickerLauncher.launch("*/*") },
                onRemoveAttachment = { attachedFile = null },
                onSend = {
                    val fullMsg = buildInputMessage(textInput, attachedFile)
                    if (fullMsg.isNotBlank()) {
                        viewModel.sendTextMessage(fullMsg, playerConnection)
                        textInput = ""
                        attachedFile = null
                        keyboardController?.hide()
                    }
                },
                isRecording = isRecording,
                onMicClick = {
                    viewModel.startPushToTalk { result ->
                        if (result.isNotBlank()) {
                            textInput = result
                        }
                    }
                }
            )
        }
    }
}

private fun buildInputMessage(textInput: String, attachedFile: AttachedFile?): String {
    return buildString {
        append(textInput.trim())
        if (attachedFile != null) {
            append("\n\n[Uploaded Document: ${attachedFile.fileName}]\n${attachedFile.textContent}")
        }
    }.trim()
}
