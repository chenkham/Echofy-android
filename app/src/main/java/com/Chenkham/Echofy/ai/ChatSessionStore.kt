package com.Chenkham.Echofy.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

class ChatSessionStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("echofy_chat_sessions_prefs", Context.MODE_PRIVATE)

    fun loadSessions(): List<ChatSession> {
        val rawStr = prefs.getString("sessions_json", null) ?: return listOf(createNewSession())
        val list = mutableListOf<ChatSession>()
        try {
            val arr = JSONArray(rawStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "New Chat")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

                val msgsArr = obj.optJSONArray("messages") ?: JSONArray()
                val msgsList = mutableListOf<ChatMessage>()
                for (j in 0 until msgsArr.length()) {
                    val mObj = msgsArr.getJSONObject(j)
                    msgsList.add(
                        ChatMessage(
                            id = mObj.optString("id", UUID.randomUUID().toString()),
                            text = mObj.optString("text", ""),
                            isUser = mObj.optBoolean("isUser", false),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }

                list.add(ChatSession(id = id, title = title, timestamp = timestamp, messages = msgsList))
            }
        } catch (_: Exception) {}

        return list.ifEmpty { listOf(createNewSession()) }
    }

    fun saveSessions(sessions: List<ChatSession>) {
        try {
            val arr = JSONArray()
            sessions.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("timestamp", s.timestamp)

                    val msgsArr = JSONArray()
                    s.messages.forEach { m ->
                        msgsArr.put(JSONObject().apply {
                            put("id", m.id)
                            put("text", m.text)
                            put("isUser", m.isUser)
                            put("timestamp", m.timestamp)
                        })
                    }
                    put("messages", msgsArr)
                }
                arr.put(obj)
            }
            prefs.edit().putString("sessions_json", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun createNewSession(): ChatSession {
        return ChatSession(
            title = "New Chat",
            timestamp = System.currentTimeMillis(),
            messages = emptyList()
        )
    }
}
