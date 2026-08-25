package com.Chenkham.Echofy.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings_v2")

object AiSettingsKeys {
    val PROFILES_JSON = stringPreferencesKey("ai_profiles_json")
    val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
    val DRIVING_MODE_ENABLED = booleanPreferencesKey("driving_mode_enabled")
    val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    val INCLUDE_HISTORY_CONTEXT = booleanPreferencesKey("include_history_context")
    val INCLUDE_LIKES_CONTEXT = booleanPreferencesKey("include_likes_context")
    val MAX_CHAT_HISTORY = stringPreferencesKey("max_chat_history")
    val AI_MEMORY_NOTES = stringPreferencesKey("ai_memory_notes")
}

data class AiProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val modelName: String,
    val apiKeysRaw: String
)

object AiPresets {
    val BUILTIN_PROFILES = listOf(
        AiProfile(
            id = "preset_groq",
            name = "Groq (Fast & Free Tier)",
            baseUrl = "https://api.groq.com/openai/v1",
            modelName = "llama-3.3-70b-versatile",
            apiKeysRaw = ""
        ),
        AiProfile(
            id = "preset_nvidia",
            name = "NVIDIA NIM Cloud",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            modelName = "meta/llama-3.3-70b-instruct",
            apiKeysRaw = ""
        ),
        AiProfile(
            id = "preset_openai",
            name = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            modelName = "gpt-4o-mini",
            apiKeysRaw = ""
        ),
        AiProfile(
            id = "preset_deepseek",
            name = "DeepSeek AI",
            baseUrl = "https://api.deepseek.com/v1",
            modelName = "deepseek-chat",
            apiKeysRaw = ""
        ),
        AiProfile(
            id = "preset_ollama",
            name = "Local Ollama",
            baseUrl = "http://localhost:11434/v1",
            modelName = "llama3",
            apiKeysRaw = "local"
        )
    )
}

class AiSettingsDataStore(private val context: Context) {

    val isWakeWordEnabled: Flow<Boolean> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.WAKE_WORD_ENABLED] ?: false
    }

    /**
     * Driving mode keeps the wake word detector running continuously so the user
     * never has to touch the phone. It implies wake word listening regardless of
     * [isWakeWordEnabled].
     */
    val isDrivingModeEnabled: Flow<Boolean> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.DRIVING_MODE_ENABLED] ?: false
    }

    val isTtsEnabled: Flow<Boolean> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.TTS_ENABLED] ?: true
    }

    val isIncludeHistoryContextEnabled: Flow<Boolean> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.INCLUDE_HISTORY_CONTEXT] ?: true
    }

    val isIncludeLikesContextEnabled: Flow<Boolean> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.INCLUDE_LIKES_CONTEXT] ?: true
    }

    val maxChatHistory: Flow<String> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.MAX_CHAT_HISTORY] ?: "20"
    }

    val aiMemoryNotes: Flow<String> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.AI_MEMORY_NOTES] ?: ""
    }

    val profiles: Flow<List<AiProfile>> = context.aiDataStore.data.map { prefs ->
        val rawJson = prefs[AiSettingsKeys.PROFILES_JSON] ?: ""
        if (rawJson.isBlank()) {
            AiPresets.BUILTIN_PROFILES
        } else {
            deserializeProfiles(rawJson)
        }
    }

    val activeProfileId: Flow<String> = context.aiDataStore.data.map { prefs ->
        prefs[AiSettingsKeys.ACTIVE_PROFILE_ID] ?: "preset_groq"
    }

    /**
     * Enabling the general-purpose assistant leaves driving mode off, since the two
     * are mutually exclusive input models (wake word vs. always-on recognition).
     * Disabling it while driving mode is active is a no-op: driving mode depends on
     * the assistant, so the user must turn driving mode off first.
     */
    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.aiDataStore.edit { prefs ->
            val driving = prefs[AiSettingsKeys.DRIVING_MODE_ENABLED] ?: false
            if (!enabled && driving) return@edit
            prefs[AiSettingsKeys.WAKE_WORD_ENABLED] = enabled
            if (enabled) prefs[AiSettingsKeys.DRIVING_MODE_ENABLED] = false
        }
    }

    /**
     * Driving mode implies the assistant is on, so enabling it turns the wake word
     * toggle on too and the UI locks that switch until driving mode is turned off.
     */
    suspend fun setDrivingModeEnabled(enabled: Boolean) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.DRIVING_MODE_ENABLED] = enabled
            if (enabled) prefs[AiSettingsKeys.WAKE_WORD_ENABLED] = true
        }
    }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.TTS_ENABLED] = enabled
        }
    }

    suspend fun setIncludeHistoryContextEnabled(enabled: Boolean) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.INCLUDE_HISTORY_CONTEXT] = enabled
        }
    }

    suspend fun setIncludeLikesContextEnabled(enabled: Boolean) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.INCLUDE_LIKES_CONTEXT] = enabled
        }
    }

    suspend fun setMaxChatHistory(max: String) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.MAX_CHAT_HISTORY] = max
        }
    }

    suspend fun setAiMemoryNotes(notes: String) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.AI_MEMORY_NOTES] = notes
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    suspend fun saveProfiles(profilesList: List<AiProfile>, activeId: String? = null) {
        val serialized = serializeProfiles(profilesList)
        context.aiDataStore.edit { prefs ->
            prefs[AiSettingsKeys.PROFILES_JSON] = serialized
            if (activeId != null) {
                prefs[AiSettingsKeys.ACTIVE_PROFILE_ID] = activeId
            }
        }
    }

    fun parseKeysList(rawKeys: String): List<String> {
        return rawKeys.split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun serializeProfiles(list: List<AiProfile>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("baseUrl", p.baseUrl)
                put("modelName", p.modelName)
                put("apiKeysRaw", p.apiKeysRaw)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun deserializeProfiles(jsonStr: String): List<AiProfile> {
        val list = mutableListOf<AiProfile>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AiProfile(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Custom Provider"),
                        baseUrl = obj.optString("baseUrl", "https://api.openai.com/v1"),
                        modelName = obj.optString("modelName", "gpt-4o-mini"),
                        apiKeysRaw = obj.optString("apiKeysRaw", "")
                    )
                )
            }
        } catch (_: Exception) {}

        return list.ifEmpty { AiPresets.BUILTIN_PROFILES }
    }
}
