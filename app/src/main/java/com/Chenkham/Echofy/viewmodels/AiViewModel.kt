package com.Chenkham.Echofy.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.ai.AiCallState
import com.Chenkham.Echofy.ai.AiPresets
import com.Chenkham.Echofy.ai.AiProfile
import com.Chenkham.Echofy.ai.AiSettingsDataStore
import com.Chenkham.Echofy.ai.ChatMessage
import com.Chenkham.Echofy.ai.ChatSession
import com.Chenkham.Echofy.ai.ChatSessionStore
import com.Chenkham.Echofy.ai.EchofyAiManager
import com.Chenkham.Echofy.ai.InputSource
import com.Chenkham.Echofy.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the AI Assistant screen. Manages AI state, chat sessions,
 * provider profiles, and voice activation lifecycle.
 *
 * This ViewModel centralizes state that was previously scattered between
 * EchofyAiManager (singleton), AiSettingsDataStore, and ChatSessionStore.
 */
@HiltViewModel
class AiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── Core Managers ────────────────────────────────────────────
    val aiManager: EchofyAiManager = EchofyAiManager.getInstance(context)
    val aiSettingsStore = AiSettingsDataStore(context)
    private val sessionStore = ChatSessionStore(context)

    // ── AI State Flows (delegated from EchofyAiManager) ─────────
    val callState: StateFlow<AiCallState> = aiManager.callState
    val chatHistory: StateFlow<List<ChatMessage>> = aiManager.chatHistory
    val lastSpokenText: StateFlow<String> = aiManager.lastSpokenText
    val aiResponseText: StateFlow<String> = aiManager.aiResponseText

    /** True while waiting for a spoken command right after "Hey Jarvis" fires. */
    val isAwaitingCommand: StateFlow<Boolean> = aiManager.isAwaitingCommand

    /** Live speech-to-text of the command being spoken. */
    val liveTranscript: StateFlow<String> = aiManager.liveTranscript

    // ── Settings Flows ──────────────────────────────────────────
    val isWakeWordEnabled = aiSettingsStore.isWakeWordEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val profiles = aiSettingsStore.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiPresets.BUILTIN_PROFILES)

    val activeProfileId = aiSettingsStore.activeProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "preset_groq")

    // ── Chat Sessions ───────────────────────────────────────────
    private val _sessions = MutableStateFlow(sessionStore.loadSessions())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow(
        sessionStore.loadSessions().firstOrNull()?.id ?: ""
    )
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    // ── UI State ────────────────────────────────────────────────
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showHistoryDrawer = MutableStateFlow(false)
    val showHistoryDrawer: StateFlow<Boolean> = _showHistoryDrawer.asStateFlow()

    private val _showHeyInfo = MutableStateFlow(false)
    val showHeyInfo: StateFlow<Boolean> = _showHeyInfo.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // ── Voice Activation ────────────────────────────────────────

    /**
     * Hands the live PlayerConnection to the assistant so voice commands can drive
     * playback. Does not start or stop listening.
     *
     * Wake word activation itself is owned by MainActivity so it stays armed on
     * every tab; the manager is a process-wide singleton shared with WakeWordService.
     */
    fun attachPlayerConnection(playerConnection: PlayerConnection?) {
        aiManager.attachPlayerConnection(playerConnection)
    }

    // ── Chat Actions ────────────────────────────────────────────

    fun sendTextMessage(text: String, playerConnection: PlayerConnection?) {
        if (text.isBlank()) return
        aiManager.sendTextMessage(text, playerConnection)

        // Auto-name session after 3+ messages
        viewModelScope.launch {
            val history = chatHistory.value
            if (history.size >= 3) {
                autoNameCurrentSession(history)
            }
        }
    }

    fun clearChat() {
        aiManager.clearChat()
    }

    // ── Push-to-Talk ────────────────────────────────────────────

    fun startPushToTalk(onResult: (String) -> Unit) {
        _isRecording.value = true
        aiManager.startSingleListening { result ->
            _isRecording.value = false
            onResult(result)
        }
    }

    // ── Session Management ──────────────────────────────────────

    fun selectSession(sessionId: String) {
        _activeSessionId.value = sessionId
    }

    fun createNewSession() {
        val newSession = sessionStore.createNewSession()
        _sessions.value = _sessions.value + newSession
        _activeSessionId.value = newSession.id
        aiManager.clearChat()
    }

    fun deleteSession(sessionId: String) {
        val updated = _sessions.value.filter { it.id != sessionId }
        _sessions.value = updated
        sessionStore.saveSessions(updated)
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = updated.firstOrNull()?.id ?: ""
        }
    }

    /**
     * Auto-generates a session title from the first user message.
     * Only runs once per session (when title is still "New Chat").
     */
    private fun autoNameCurrentSession(history: List<ChatMessage>) {
        val currentId = _activeSessionId.value
        val currentSessions = _sessions.value.toMutableList()
        val sessionIndex = currentSessions.indexOfFirst { it.id == currentId }

        if (sessionIndex >= 0 && currentSessions[sessionIndex].title == "New Chat") {
            val firstUserMsg = history.firstOrNull { it.isUser }?.text ?: return
            val autoTitle = firstUserMsg.take(35).let {
                if (firstUserMsg.length > 35) "$it..." else it
            }
            currentSessions[sessionIndex] = currentSessions[sessionIndex].copy(title = autoTitle)
            _sessions.value = currentSessions
            sessionStore.saveSessions(currentSessions)
        }
    }

    // ── Settings Dialog Actions ─────────────────────────────────

    fun toggleSettingsDialog() {
        _showSettingsDialog.value = !_showSettingsDialog.value
    }

    fun dismissSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun toggleHistoryDrawer() {
        _showHistoryDrawer.value = !_showHistoryDrawer.value
    }

    fun dismissHistoryDrawer() {
        _showHistoryDrawer.value = false
    }

    fun toggleHeyInfo() {
        _showHeyInfo.value = !_showHeyInfo.value
    }

    // ── Provider Profile Management ─────────────────────────────

    fun saveProfiles(updatedList: List<AiProfile>, newActiveId: String) {
        viewModelScope.launch {
            aiSettingsStore.saveProfiles(updatedList, newActiveId)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSettingsStore.setWakeWordEnabled(enabled)
        }
    }

    // ── Cleanup ─────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // Save current session state before ViewModel is destroyed
        try {
            sessionStore.saveSessions(_sessions.value)
        } catch (e: Exception) {
            Timber.w(e, "Failed to save chat sessions on ViewModel clear")
        }
    }
}
