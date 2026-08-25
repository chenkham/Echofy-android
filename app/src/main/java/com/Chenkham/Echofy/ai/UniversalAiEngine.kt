package com.Chenkham.Echofy.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

data class UniversalAiResult(
    val speakResponse: String,
    val actionType: String = "CHAT",
    val query: String = "",
    val playlistName: String = "",
    val success: Boolean = true,
    val errorMessage: String? = null
)

class UniversalAiEngine {

    private val keyIndex = AtomicInteger(0)

    /**
     * Executes a chat completion query against any OpenAI-compatible Base URL using
     * key rotation and fallback logic across the user's API Key pool.
     */
    suspend fun query(
        baseUrl: String,
        apiKeys: List<String>,
        modelName: String,
        userPrompt: String,
        chatHistory: List<ChatMessage>,
        userContext: String
    ): UniversalAiResult = withContext(Dispatchers.IO) {

        if (apiKeys.isEmpty()) {
            return@withContext UniversalAiResult(
                speakResponse = "Please configure your API key in AI Settings to enable Echofy Buddy.",
                success = false,
                errorMessage = "No API key configured"
            )
        }

        val cleanBaseUrl = baseUrl.trim().removeSuffix("/")
        val endpoint = if (cleanBaseUrl.endsWith("/chat/completions")) cleanBaseUrl else "$cleanBaseUrl/chat/completions"

        val systemPrompt = """
            You are Echofy Buddy, an intelligent, passionate, and friendly music assistant.
            Speak naturally, concisely, and conversationally like a real human friend.
            $userContext

            Guidelines:
            - When the user asks to play a specific song, include JSON marker: {"action": "PLAY_SONG", "query": "song title artist"}
            - For general chat, music recommendations, artist facts, or answers, speak warmly without JSON markers.
            - DO NOT offer to create playlists, do NOT claim to create playlists, and NEVER include playlist creation actions.
        """.trimIndent()

        // Build messages payload
        val messagesArray = JSONArray()
        
        // System message
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        // Conversation history (up to last 10 messages)
        chatHistory.takeLast(10).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                put("content", msg.text)
            })
        }

        // Current User prompt
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        })

        val requestBody = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArray)
            put("temperature", 0.7)
            put("max_tokens", 500)
        }.toString()

        var lastError = "Failed to get AI response"
        val totalKeys = apiKeys.size

        // Try keys in round-robin order with failover
        for (attempt in 0 until totalKeys) {
            val currentIndex = Math.abs(keyIndex.getAndIncrement() % totalKeys)
            val currentKey = apiKeys[currentIndex]

            try {
                val url = URL(endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $currentKey")
                conn.connectTimeout = 10000
                conn.readTimeout = 20000
                conn.doOutput = true

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(requestBody)
                writer.flush()
                writer.close()

                val statusCode = conn.responseCode
                if (statusCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseStr = reader.readText()
                    reader.close()

                    val json = JSONObject(responseStr)
                    val choices = json.optJSONArray("choices")
                    val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()

                    if (!content.isNullOrEmpty()) {
                        return@withContext parseResponse(content)
                    }
                } else if (statusCode == 429 || statusCode == 401) {
                    // Key rate limited or invalid — continue loop to try next key in pool!
                    val errStream = conn.errorStream
                    if (errStream != null) {
                        val reader = BufferedReader(InputStreamReader(errStream))
                        lastError = "Key $currentIndex error ($statusCode): ${reader.readText().take(200)}"
                        reader.close()
                    }
                    continue
                } else {
                    val errStream = conn.errorStream
                    if (errStream != null) {
                        val reader = BufferedReader(InputStreamReader(errStream))
                        lastError = "HTTP $statusCode: ${reader.readText().take(200)}"
                        reader.close()
                    }
                }
            } catch (e: Exception) {
                lastError = "Connection error: ${e.localizedMessage}"
            }
        }

        return@withContext UniversalAiResult(
            speakResponse = "I couldn't reach your AI provider right now ($lastError). Please check your API keys or Base URL in Settings.",
            success = false,
            errorMessage = lastError
        )
    }

    private fun parseResponse(rawContent: String): UniversalAiResult {
        // Check for JSON action markers inside text
        try {
            if (rawContent.contains("PLAY_SONG")) {
                val song = extractJsonField(rawContent, "query") ?: ""
                val speak = rawContent.replace(Regex("\\{.*?\\}"), "").trim()
                return UniversalAiResult(
                    speakResponse = speak.ifEmpty { "Playing $song for you!" },
                    actionType = "PLAY_SONG",
                    query = song
                )
            }
        } catch (_: Exception) {}

        return UniversalAiResult(
            speakResponse = rawContent.replace(Regex("\\{.*?\\}"), "").trim(),
            actionType = "CHAT"
        )
    }

    private fun extractJsonField(text: String, field: String): String? {
        val pattern = "\"$field\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return pattern.find(text)?.groupValues?.getOrNull(1)
    }
}
