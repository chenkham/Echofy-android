/*
 * Echofy Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.Chenkham.Echofy.lyrics

import timber.log.Timber

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.Chenkham.Echofy.constants.PreferredLyricsProvider
import com.Chenkham.Echofy.constants.PreferredLyricsProviderKey
import com.Chenkham.Echofy.constants.ProviderOrderKey
import com.Chenkham.Echofy.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.Chenkham.Echofy.extensions.toEnum
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.reportException
import com.Chenkham.Echofy.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders =
        listOf(
                        BetterLyricsProvider,
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider,
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            Timber.tag("LyricsHelper").d("Found lyrics in cache for ${mediaMetadata.title}")
            return cached.lyrics
        }
        
        Timber.tag("LyricsHelper").d("Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString { it.name }}, Album: ${mediaMetadata.album?.title})")

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            Timber.tag("LyricsHelper").d("Network unavailable, aborting lyrics fetch")
            return LYRICS_NOT_FOUND
        }

        val ordered = orderedProviders()
        val providers = if (preferredProviderOnly) listOf(ordered.first()) else ordered
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val deferred = scope.async {
            for (provider in providers) {
                val enabled = provider.isEnabled(context)
                
                if (enabled) {
                    try {
                        val result = provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                        )
                        result.onSuccess { lyrics ->
                            if (isMeaningfulLyrics(lyrics)) {
                                return@async lyrics
                            }
                        }.onFailure {
                            reportException(it)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            return@async LYRICS_NOT_FOUND
        }

        val lyrics = deferred.await()
        scope.cancel()
        return lyrics
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        val providers = orderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                            if (isMeaningfulLyrics(lyrics)) {
                                val result = LyricsResult(provider.name, lyrics)
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    suspend fun getLyricsStreaming(
        mediaMetadata: MediaMetadata,
        forceRefresh: Boolean = false,
        onResult: (LyricsResult) -> Unit,
    ) {
        if (forceRefresh) {
            val songArtists = mediaMetadata.artists.joinToString { it.name }
            val cacheKey = "$songArtists-${mediaMetadata.title}".replace(" ", "")
            cache.remove(cacheKey)
        }
        val songArtists = mediaMetadata.artists.joinToString { it.name }
        getAllLyrics(
            mediaId = mediaMetadata.id,
            songTitle = mediaMetadata.title,
            songArtists = songArtists,
            songAlbum = mediaMetadata.album?.title,
            duration = mediaMetadata.duration,
            callback = onResult,
        )
    }

    suspend fun getLyricsFromProvider(
        mediaMetadata: MediaMetadata,
        providerName: String,
    ): String? {
        val providers = orderedProviders()
        val target = providers.firstOrNull { it.name.equals(providerName, ignoreCase = true) } ?: return null
        return try {
            val res = target.getLyrics(
                mediaMetadata.id,
                mediaMetadata.title,
                mediaMetadata.artists.joinToString { it.name },
                mediaMetadata.duration,
            )
            res.getOrNull()
        } catch (e: Exception) {
            null
        }
    }


    private fun PreferredLyricsProvider.toLyricsProvider(): LyricsProvider = when (this) {
        PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
        PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
        PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
        PreferredLyricsProvider.GENIUS -> GeniusLyricsProvider
        PreferredLyricsProvider.YOUTUBE -> YouTubeLyricsProvider
        PreferredLyricsProvider.YOUTUBE_SUBTITLES -> YouTubeSubtitleLyricsProvider
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val savedOrder = context.dataStore.data
            .first()[ProviderOrderKey]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { PreferredLyricsProvider.valueOf(name) }.getOrNull() }
            ?.map { it.toLyricsProvider() }

        if (!savedOrder.isNullOrEmpty()) {
            val allProviders = savedOrder.toMutableList()
            PreferredLyricsProvider.entries.forEach { enumProvider ->
                val provider = enumProvider.toLyricsProvider()
                if (provider !in allProviders) allProviders.add(provider)
            }
            return allProviders
        }

        val preferred = context.dataStore.data
            .first()[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.LRCLIB)

        val first = preferred.toLyricsProvider()
        return listOf(first) + baseProviders.filterNot { it == first }
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)