package com.Chenkham.Echofy.lyrics

import android.content.Context
import com.Chenkham.lrclib.LrcLib
import com.Chenkham.Echofy.constants.EnableLrcLibKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import timber.log.Timber

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        val cleanTitle = sanitizeTitle(title)
        val cleanArtist = sanitizeArtistName(artist)
        
        Timber.d("LrcLib: Searching for '$cleanTitle' by '$cleanArtist' (Original: '$title' by '$artist')")
        
        val result = LrcLib.getLyrics(cleanTitle, cleanArtist, duration)
        
        // 1. Duration check generic function (reusable)
        fun checkDuration(lyrics: String): Boolean {
             // [length:03:45] or [03:45]
            val lengthRegex = Regex("""\[length:(\d+):(\d+)]""")
            val match = lengthRegex.find(lyrics)
            
            if (match != null) {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val lyricsDuration = (minutes * 60) + seconds
                
                // Allow ±4 seconds tolerance
                if (kotlin.math.abs(lyricsDuration - duration) > 4) {
                    Timber.d("LrcLib: Duration mismatch for '$cleanTitle'. Song: ${duration}s, Lyrics: ${lyricsDuration}s")
                    return false
                }
            }
            return true
        }

        return result.mapCatching { lyrics ->
            if (!checkDuration(lyrics)) {
                throw Exception("Duration mismatch")
            }
            lyrics
        }.recoverCatching {
            // 2. Fallback: Search with Title ONLY if strict search failed
            Timber.d("LrcLib: Strict search failed. Retrying with Title only: '$cleanTitle'")
            val fallbackResult = LrcLib.getLyrics(cleanTitle, "", duration).getOrThrow()
            
            if (!checkDuration(fallbackResult)) {
                throw Exception("Duration mismatch in fallback")
            }
            fallbackResult
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        val cleanTitle = sanitizeTitle(title)
        val cleanArtist = sanitizeArtistName(artist)
        
        Timber.d("LrcLib: Getting all lyrics for '$cleanTitle' by '$cleanArtist'")
        
        LrcLib.getAllLyrics(cleanTitle, cleanArtist, duration, null, callback)
    }

    /**
     * Remove YouTube metadata from artist name
     */
    private fun sanitizeArtistName(artist: String): String {
        return artist
            // Remove view counts, likes, subscribers
            .replace(Regex(""",?\s*[\d.]+[KMB]?\s*views?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex(""",?\s*[\d.]+[KMB]?\s*likes?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex(""",?\s*[\d.]+[KMB]?\s*subscribers?""", RegexOption.IGNORE_CASE), "")
            // Remove "Official" mentions
            .replace(Regex("""\s*-?\s*Official\s*(Artist|Channel)?""", RegexOption.IGNORE_CASE), "")
            // Remove "Topic" suffix common on YouTube
            .replace(Regex("""\s*-\s*Topic\s*$""", RegexOption.IGNORE_CASE), "")
            // Keep only main artist (remove featuring artists for better matching)
            .replace(Regex("""\s*(feat\.|ft\.|featuring|&|,)\s+.*""", RegexOption.IGNORE_CASE), "")
            // Clean up multiple spaces
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd(',')
            .trim()
    }

    /**
     * Clean up title - remove common suffixes and version labels
     */
    private fun sanitizeTitle(title: String): String {
        return title
            // Remove official video/audio labels
            .replace(Regex("""\s*\(Official\s*(Video|Audio|Music Video|Lyric Video)\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[Official\s*(Video|Audio|Music Video|Lyric Video)\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*Official\s*(Video|Audio)""", RegexOption.IGNORE_CASE), "")
            // Remove version labels (keep in parentheses for better matching)
            .replace(Regex("""\s*-\s*(Remix|Remaster|Live|Acoustic|Radio Edit).*""", RegexOption.IGNORE_CASE), "")
            // Remove featuring artists from title (LrcLib prefers clean titles)
            .replace(Regex("""\s+\(feat\..*?\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+\(ft\..*?\)""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
