package com.Chenkham.songlink

import com.Chenkham.songlink.models.PlatformLink
import com.Chenkham.songlink.models.SongLinks
import com.Chenkham.songlink.models.SonglinkResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Songlink / Odesli client. Converts a music link on one service into the
 * equivalent links on every other service.
 *
 * The public tier works without an API key at roughly 10 requests/minute.
 * Supplying [apiKey] raises that limit.
 */
object Songlink {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            defaultRequest {
                url("https://api.song.link/v1-alpha.1/")
            }

            expectSuccess = false
        }
    }

    /** Platforms worth surfacing, in the order users expect to see them. */
    private val PLATFORM_NAMES = linkedMapOf(
        "spotify" to "Spotify",
        "youtubeMusic" to "YouTube Music",
        "youtube" to "YouTube",
        "appleMusic" to "Apple Music",
        "amazonMusic" to "Amazon Music",
        "deezer" to "Deezer",
        "tidal" to "Tidal",
        "soundcloud" to "SoundCloud",
        "pandora" to "Pandora",
        "napster" to "Napster",
    )

    /**
     * Resolve any supported music URL (Spotify, Apple Music, Deezer, ...) into
     * the full cross-platform link set.
     */
    suspend fun resolveUrl(
        url: String,
        userCountry: String = "US",
        apiKey: String? = null,
    ) = runCatching {
        val response = client
            .get("links") {
                parameter("url", url)
                parameter("userCountry", userCountry)
                apiKey?.takeIf { it.isNotBlank() }?.let { parameter("key", it) }
            }.body<SonglinkResponse>()

        response.toSongLinks()
    }

    /**
     * Resolve directly from a YouTube video id. Saves callers from having to
     * build the watch URL themselves.
     */
    suspend fun resolveYouTubeVideo(
        videoId: String,
        userCountry: String = "US",
        apiKey: String? = null,
    ) = resolveUrl(
        url = "https://music.youtube.com/watch?v=$videoId",
        userCountry = userCountry,
        apiKey = apiKey,
    )

    private fun SonglinkResponse.toSongLinks(): SongLinks {
        val entity = entitiesByUniqueId[entityUniqueId]
            ?: entitiesByUniqueId.values.firstOrNull()

        val platforms = PLATFORM_NAMES.mapNotNull { (key, displayName) ->
            val link = linksByPlatform[key] ?: return@mapNotNull null
            PlatformLink(
                platform = key,
                displayName = displayName,
                url = link.url,
            )
        }

        return SongLinks(
            pageUrl = pageUrl,
            title = entity?.title,
            artistName = entity?.artistName,
            thumbnailUrl = entity?.thumbnailUrl,
            platforms = platforms,
        )
    }
}
