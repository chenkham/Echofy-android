package com.Chenkham.Echofy.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AudioBookmark(
    val positionMs: Long,
    val formattedTime: String,
    val note: String = ""
)

object AudioBookmarkManager {
    private const val PREFS_NAME = "echofy_audio_bookmarks"
    private val bookmarksCache = mutableMapOf<String, MutableStateFlow<List<AudioBookmark>>>()

    fun observeBookmarks(songId: String): StateFlow<List<AudioBookmark>> {
        return bookmarksCache.getOrPut(songId) {
            MutableStateFlow(emptyList())
        }.asStateFlow()
    }

    fun loadBookmarks(context: Context, songId: String) {
        val flow = bookmarksCache.getOrPut(songId) { MutableStateFlow(emptyList()) }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(songId, null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<AudioBookmark>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pos = obj.getLong("pos")
                val label = obj.optString("label", formatMs(pos))
                val note = obj.optString("note", "")
                list.add(AudioBookmark(pos, label, note))
            }
            flow.value = list.sortedBy { it.positionMs }
        } catch (_: Exception) {
            // gracefully fallback
        }
    }

    fun addBookmark(context: Context, songId: String, positionMs: Long, note: String = ""): AudioBookmark {
        val flow = bookmarksCache.getOrPut(songId) { MutableStateFlow(emptyList()) }
        val formatted = formatMs(positionMs)
        val newBookmark = AudioBookmark(positionMs, formatted, note)
        val currentList = flow.value.toMutableList()
        currentList.removeAll { Math.abs(it.positionMs - positionMs) < 1000 }
        currentList.add(newBookmark)
        currentList.sortBy { it.positionMs }
        flow.value = currentList
        saveToPrefs(context, songId, currentList)
        return newBookmark
    }

    fun deleteBookmark(context: Context, songId: String, positionMs: Long) {
        val flow = bookmarksCache.getOrPut(songId) { MutableStateFlow(emptyList()) }
        val currentList = flow.value.toMutableList()
        currentList.removeAll { it.positionMs == positionMs }
        flow.value = currentList
        saveToPrefs(context, songId, currentList)
    }

    private fun saveToPrefs(context: Context, songId: String, list: List<AudioBookmark>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { bm ->
            val obj = JSONObject()
            obj.put("pos", bm.positionMs)
            obj.put("label", bm.formattedTime)
            obj.put("note", bm.note)
            array.put(obj)
        }
        prefs.edit().putString(songId, array.toString()).apply()
    }

    fun formatMs(pos: Long): String {
        val totalSec = (pos / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
