package com.Chenkham.Echofy.ai

import java.util.UUID

/**
 * Source of the user's input to the AI assistant.
 */
enum class InputSource {
    TEXT,
    VOICE,
    HEY_COMMAND,
}

/**
 * State machine representing the live voice/call session with the AI agent.
 */
enum class AiCallState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
}

/**
 * Available backend models for Echofy AI intelligence.
 */
enum class AiModelOption(
    val displayName: String,
    val modelId: String,
    val contextWindow: String,
    val description: String,
) {
    DEFAULT("Default", "default", "128K", "Free & Unlimited Cloud Agent"),
    FAST("Fast", "openai", "128K", "Fast conversational model"),
    LLAMA("Llama", "llama", "128K", "High precision music reasoning"),
}

/**
 * Download/readiness status for offline or cached AI models.
 */
enum class AiModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
}

/**
 * Single chat message in the conversation thread.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Structured action parsed from conversational input.
 */
data class AgentAction(
    val speakResponse: String,
    val actionType: String,
    val query: String = "",
    val playlistName: String = "",
    val volumeLevel: Int = 70,
)
