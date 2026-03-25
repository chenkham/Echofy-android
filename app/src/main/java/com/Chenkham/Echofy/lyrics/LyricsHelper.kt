package com.Chenkham.Echofy.lyrics

import android.content.Context
import android.util.LruCache
import com.Chenkham.Echofy.constants.PreferredLyricsProvider
import com.Chenkham.Echofy.constants.PreferredLyricsProviderKey
import com.Chenkham.Echofy.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.Chenkham.Echofy.extensions.toEnum
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private var lyricsProviders =
        listOf(
            LrcLibLyricsProvider,
            YouTubeSubtitleLyricsProvider, // timed LRC via transcript — always prefer over plain text
            KuGouLyricsProvider,
            YouTubeLyricsProvider          // plain text, no timestamps — last resort only
        )
    
    val preferred =
        context.dataStore.data
            .map {
                it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
            }.distinctUntilChanged()
            .map { pref ->
                lyricsProviders = when (pref) {
                    PreferredLyricsProvider.LRCLIB -> listOf(
                        LrcLibLyricsProvider,
                        YouTubeSubtitleLyricsProvider, // timed — before KuGou
                        KuGouLyricsProvider,
                        YouTubeLyricsProvider          // plain text — last resort
                    )
                    PreferredLyricsProvider.KUGOU -> listOf(
                        KuGouLyricsProvider,
                        YouTubeSubtitleLyricsProvider, // timed — before LrcLib fallback
                        LrcLibLyricsProvider,
                        YouTubeLyricsProvider          // plain text — last resort
                    )
                }
            }

    
    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private val songProviderCache = LruCache<String, String>(MAX_CACHE_SIZE) // Track which provider worked for each song

    /**
     * Get lyrics with streaming callback for progressive loading
     * Calls callback immediately with first available result, continues fetching others
     * @param callback Called with each provider result as it becomes available
     */
    suspend fun getLyricsStreaming(
        mediaMetadata: MediaMetadata,
        forceRefresh: Boolean = false,
        callback: (LyricsResult) -> Unit
    ) {
        val songId = mediaMetadata.id
        
        // Check cache only if not forcing refresh
        if (!forceRefresh) {
            cache.get(songId)?.let { cachedResults ->
                cachedResults.forEach { callback(it) }
                return
            }
        } else {
            Timber.d("Force refreshing lyrics for: ${mediaMetadata.title}")
            cache.remove(songId)
        }
        
        val enabledProviders = lyricsProviders.filter { it.isEnabled(context) }
        if (enabledProviders.isEmpty()) {
            return
        }
        
        val results = mutableListOf<LyricsResult>()
        
        // Launch parallel requests
        coroutineScope {
            val rawTitle = mediaMetadata.title
            val title = sanitizeTitle(rawTitle)
            val rawArtist = mediaMetadata.artists.joinToString { it.name }
            val artist = sanitizeArtistName(rawArtist)
            val duration = mediaMetadata.duration
            
            enabledProviders.map { provider ->
                async {
                    try {
                        provider.getLyrics(songId, title, artist, duration).getOrNull()?.let { lyrics ->
                            val result = LyricsResult(provider.name, lyrics)
                            results.add(result)
                            // Call callback immediately for progressive loading
                            callback(result)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }.awaitAll()
        }
        
        // Cache all results for this song
        if (results.isNotEmpty()) {
            cache.put(songId, results)
        }
    }

    /**
     * Get lyrics with parallel provider requests for faster loading
     * Returns first successful result
     * @param forceRefresh if true, bypasses cache and fetches fresh lyrics
     */
    suspend fun getLyrics(mediaMetadata: MediaMetadata, forceRefresh: Boolean = false): String {
        // Check cache only if not forcing refresh
        if (!forceRefresh) {
            val cached = cache.get(mediaMetadata.id)?.firstOrNull()
            if (cached != null) {
                Timber.d("Lyrics cache hit for: ${mediaMetadata.title}")
                return cached.lyrics
            }
        } else {
            Timber.d("Force refreshing lyrics for: ${mediaMetadata.title}")
            // Clear cache for this song
            cache.remove(mediaMetadata.id)
            songProviderCache.remove(mediaMetadata.id)
        }
        
        val enabledProviders = lyricsProviders.filter { it.isEnabled(context) }
        
        if (enabledProviders.isEmpty()) {
            return LYRICS_NOT_FOUND
        }
        
        // Try parallel requests for faster loading
        return try {
            getLyricsParallel(mediaMetadata, enabledProviders)
        } catch (e: Exception) {
            Timber.w("Parallel lyrics fetch failed, falling back to sequential")
            getLyricsSequential(mediaMetadata, enabledProviders)
        }
    }
    
    /**
     * Parallel lyrics fetch - tries all providers simultaneously
     * Returns first successful result for speed
     */
    private suspend fun getLyricsParallel(
        mediaMetadata: MediaMetadata,
        providers: List<LyricsProvider>
    ): String = coroutineScope {
        val rawTitle = mediaMetadata.title
        val title = sanitizeTitle(rawTitle)
        val rawArtist = mediaMetadata.artists.joinToString { it.name }
        val artist = sanitizeArtistName(rawArtist)
        val duration = mediaMetadata.duration
        val id = mediaMetadata.id
        
        Timber.d("Starting parallel lyrics fetch for: '$title' by '$artist' (raw: '$rawTitle' by '$rawArtist')")
        
        // Launch all provider requests in parallel
        val results = providers.map { provider ->
            async {
                try {
                    provider.getLyrics(id, title, artist, duration).getOrNull()
                } catch (e: Exception) {
                    reportException(e)
                    null
                }
            }
        }.awaitAll()
        
        // Return first non-null result
        val firstResult = results.firstNotNullOfOrNull { it }
        if (firstResult != null) {
            Timber.d("Got lyrics from parallel fetch")
            return@coroutineScope firstResult
        }
        
        // Fallback: Retry with blank artist (messy artist names often block matches)
        Timber.d("No lyrics found. Retrying with blank artist for: $title")
        val fallbackResults = providers.map { provider ->
            async {
                try {
                    provider.getLyrics(id, title, "", duration).getOrNull()
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll()
        
        fallbackResults.firstNotNullOfOrNull { it }?.also {
            Timber.d("Got lyrics from fallback (blank artist)")
        } ?: LYRICS_NOT_FOUND
    }
    
    /**
     * Sequential fallback - original behavior
     */
    private suspend fun getLyricsSequential(
        mediaMetadata: MediaMetadata,
        providers: List<LyricsProvider>
    ): String {
        val title = sanitizeTitle(mediaMetadata.title)
        val artist = sanitizeArtistName(mediaMetadata.artists.joinToString { it.name })
        providers.forEach { provider ->
            provider
                .getLyrics(
                    mediaMetadata.id,
                    title,
                    artist,
                    mediaMetadata.duration,
                ).onSuccess { lyrics ->
                    return lyrics
                }.onFailure {
                    reportException(it)
                }
        }
        return LYRICS_NOT_FOUND
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }
        val allResult = mutableListOf<LyricsResult>()
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                    val result = LyricsResult(provider.name, lyrics)
                    allResult += result
                    callback(result)
                }
            }
        }
        cache.put(cacheKey, allResult)
    }

    /**
     * Clear cached lyrics for a specific song ID
     * Useful when user wants to refetch lyrics
     */
    fun clearLyricsCache(songId: String) {
        cache.remove(songId)
        songProviderCache.remove(songId)
        Timber.d("Cleared lyrics cache for song: $songId")
    }
    
    /**
     * Get all available providers
     */
    fun getAvailableProviders(): List<LyricsProvider> = lyricsProviders
    
    /**
     * Get provider name
     */
    fun getProviderName(provider: LyricsProvider): String = provider.name
    
    /**
     * Get lyrics from a specific provider (for manual provider selection)
     */
    suspend fun getLyricsFromProvider(
        mediaMetadata: MediaMetadata,
        providerName: String
    ): String? {
        val provider = lyricsProviders.find { it.name == providerName } ?: return null
        
        if (!provider.isEnabled(context)) {
            Timber.w("Provider $providerName is disabled")
            return null
        }
        
        return try {
            provider.getLyrics(
                mediaMetadata.id,
                mediaMetadata.title,
                mediaMetadata.artists.joinToString { it.name },
                mediaMetadata.duration
            ).getOrNull()
        } catch (e: Exception) {
            reportException(e)
            null
        }
    }
    
    /**
     * Get all cached results for a song (for provider switching UI)
     */
    fun getCachedResults(songId: String): List<LyricsResult>? {
        return cache.get(songId)
    }
    
    /**
     * Sanitize song title before passing to lyrics providers
     * Removes parenthetical content like "(From "Aashiqui)" and other metadata
     */
    private fun sanitizeTitle(title: String): String {
        var cleaned = title.trim()
        
        // Remove content in parentheses (From "Movie Name", Official Video, etc.)
        cleaned = cleaned.replace(Regex("""\s*\(.*?\)"""), "")
        
        // Remove content in square brackets [Official Video], [HD], etc.
        cleaned = cleaned.replace(Regex("""\s*\[.*?\]"""), "")
        
        // Remove common suffixes
        cleaned = cleaned
            .replace(Regex("""\s*-\s*Official\s*(Video|Audio|Music Video|Lyric Video)?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\|\s*.*$"""), "") // Remove everything after |
            
        // Clean up multiple spaces
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        
        Timber.d("Title sanitized: '$title' -> '$cleaned'")
        return cleaned
    }

    /**
     * Sanitize artist name before passing to lyrics providers
     * Handles patterns like "Song, Arijit Singh" where "Song" is a literal word
     */
    private fun sanitizeArtistName(artist: String): String {
        var cleaned = artist.trim()
        
        // Handle "Song, Artist Name" pattern
        // If first part before comma is literally "Song", use the part after comma
        if (cleaned.contains(",")) {
            val parts = cleaned.split(",").map { it.trim() }
            if (parts.isNotEmpty()) {
                val firstPart = parts[0].lowercase()
                // If first word is a generic term, skip to second part
                if (firstPart == "song" || firstPart == "songs" || 
                    firstPart == "music" || firstPart == "audio" ||
                    firstPart == "video" || firstPart == "official") {
                    cleaned = parts.drop(1).joinToString(", ")
                }
            }
        }
        
        // Additional cleanup (YouTube metadata, etc.)
        cleaned = cleaned
            // Remove view counts, likes, subscribers
            .replace(Regex(""",?\s*[\d.]+[KMB]?\s*views?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex(""",?\s*[\d.]+[KMB]?\s*likes?""", RegexOption.IGNORE_CASE), "")
            .replace(Regex(""",?\s*[\d.]+[KMB]?\s*subscribers?""", RegexOption.IGNORE_CASE), "")
            // Remove "Official" mentions
            .replace(Regex("""\s*-?\s*Official\s*(Artist|Channel)?""", RegexOption.IGNORE_CASE), "")
            // Remove "Topic" suffix common on YouTube
            .replace(Regex("""\s*-\s*Topic\s*$""", RegexOption.IGNORE_CASE), "")
            // Clean up multiple spaces and commas
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd(',')
            .trim()
        
        Timber.d("Artist sanitized: '$artist' -> '$cleaned'")
        return cleaned
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
