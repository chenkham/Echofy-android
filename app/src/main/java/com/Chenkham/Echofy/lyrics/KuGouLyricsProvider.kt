package com.Chenkham.Echofy.lyrics

import android.content.Context
import com.Chenkham.kugou.KuGou
import com.Chenkham.Echofy.constants.EnableKugouKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import timber.log.Timber

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> {
        return KuGou.getLyricsWithMetadata(title, artist, duration).recoverCatching {
             // Fallback: Search with Title ONLY if strict search failed
             Timber.d("KuGou: Strict search failed. Retrying with Title only: '$title'")
             KuGou.getLyricsWithMetadata(title, "", duration).getOrThrow()
        }.mapCatching { (lyrics, matchedSongName) ->
            // Title validation: Ensure the matched song name resembles the requested title
            if (matchedSongName.isNotBlank()) {
                val normalizedTitle = normalizeForComparison(title)
                val normalizedMatched = normalizeForComparison(matchedSongName)
                
                // Check if titles share significant overlap
                if (!titlesMatch(normalizedTitle, normalizedMatched)) {
                    Timber.d("KuGou: Rejected - title mismatch. Requested: '$title', Got: '$matchedSongName'")
                    throw Exception("Title mismatch: Requested '$title', Got '$matchedSongName'")
                }
                Timber.d("KuGou: Title validated. Matched: '$matchedSongName'")
            }
            
            // Verify accuracy using LRC length tag if present
            // [length:03:45] or [03:45]
            val lengthRegex = Regex("""\[length:(\d+):(\d+)]""")
            val match = lengthRegex.find(lyrics)
            
            if (match != null) {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val lyricsDuration = (minutes * 60) + seconds
                
                // Allow ±4 seconds tolerance
                if (kotlin.math.abs(lyricsDuration - duration) > 4) {
                    throw Exception("Duration mismatch: Song $duration s, Lyrics $lyricsDuration s")
                }
            }
            
            // Content validation: Reject too short or single-line lyrics
            // Count actual lyric lines (lines with timestamps that have text content)
            val timedLineRegex = Regex("""\[\d{2}:\d{2}[.\d]*\](.+)""")
            val timedLines = timedLineRegex.findAll(lyrics)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotBlank() }
                .toList()
            
            // Reject if less than 5 actual lyric lines
            if (timedLines.size < 5) {
                Timber.d("KuGou: Rejected - too few lines (${timedLines.size})")
                throw Exception("Too few lyric lines: ${timedLines.size}")
            }
            
            // Reject if average line length is too short (likely spam/garbage)
            val avgLineLength = timedLines.map { it.length }.average()
            if (avgLineLength < 3) {
                Timber.d("KuGou: Rejected - avg line too short ($avgLineLength)")
                throw Exception("Average line too short: $avgLineLength chars")
            }
            
            lyrics
        }
    }
    
    /**
     * Normalize string for comparison: lowercase, remove special chars, extra spaces
     */
    private fun normalizeForComparison(text: String): String {
        return text.lowercase()
            .replace(Regex("""\(.*?\)"""), "") // Remove parentheses content
            .replace(Regex("""\[.*?\]"""), "") // Remove brackets content
            .replace(Regex("""[^\w\s]"""), "") // Remove special characters
            .replace(Regex("""\s+"""), " ")    // Collapse whitespace
            .trim()
    }
    
    /**
     * Check if two titles match with fuzzy comparison
     * Returns true if titles share significant word overlap or one contains the other
     */
    private fun titlesMatch(title1: String, title2: String): Boolean {
        // Direct containment check
        if (title1.contains(title2) || title2.contains(title1)) {
            return true
        }
        
        // Word overlap check
        val words1 = title1.split(" ").filter { it.length > 2 }.toSet()
        val words2 = title2.split(" ").filter { it.length > 2 }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) {
            // If one title is very short, check character-level similarity
            return title1.take(10) == title2.take(10) || 
                   title1.takeLast(10) == title2.takeLast(10)
        }
        
        val intersection = words1.intersect(words2)
        val minWords = minOf(words1.size, words2.size)
        
        // At least 50% word overlap required
        return intersection.size >= (minWords * 0.5)
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit
    ) {
        KuGou.getAllPossibleLyricsOptions(title, artist, duration, callback)
    }
}
