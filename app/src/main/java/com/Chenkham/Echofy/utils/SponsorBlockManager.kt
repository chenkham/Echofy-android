package com.Chenkham.Echofy.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class SponsorSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long
)

object SponsorBlockManager {
    private const val API_BASE = "https://sponsor.ajay.app/api/skipSegments"
    private val segmentCache = ConcurrentHashMap<String, List<SponsorSegment>>()

    suspend fun fetchSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        segmentCache[videoId]?.let { return@withContext it }

        try {
            val categoriesJson = "[\"sponsor\",\"selfpromo\",\"interaction\",\"intro\",\"outro\",\"preview\",\"music_offtopic\",\"filler\"]"
            val encodedCategories = java.net.URLEncoder.encode(categoriesJson, "UTF-8")
            val urlString = "$API_BASE?videoID=$videoId&categories=$encodedCategories"
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "Echofy-Android-Client")
            }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                val segments = mutableListOf<SponsorSegment>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val category = obj.optString("category", "sponsor")
                    val segmentPair = obj.optJSONArray("segment")
                    if (segmentPair != null && segmentPair.length() >= 2) {
                        val startSec = segmentPair.getDouble(0)
                        val endSec = segmentPair.getDouble(1)
                        segments.add(
                            SponsorSegment(
                                category = category,
                                startMs = (startSec * 1000).toLong(),
                                endMs = (endSec * 1000).toLong()
                            )
                        )
                    }
                }
                segmentCache[videoId] = segments
                Timber.d("SponsorBlock found ${segments.size} segments for videoId=$videoId")
                return@withContext segments
            } else {
                segmentCache[videoId] = emptyList()
            }
        } catch (e: Exception) {
            Timber.d("SponsorBlock fetch skipped or failed: ${e.message}")
            segmentCache[videoId] = emptyList()
        }
        emptyList()
    }

    fun getSkipTarget(videoId: String, currentPositionMs: Long): Long? {
        val segments = segmentCache[videoId] ?: return null
        for (seg in segments) {
            if (currentPositionMs in (seg.startMs - 250L)..(seg.endMs - 250L)) {
                return seg.endMs
            }
        }
        return null
    }

    fun clear() {
        segmentCache.clear()
    }
}
