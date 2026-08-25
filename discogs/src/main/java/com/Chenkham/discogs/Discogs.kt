package com.Chenkham.discogs

import com.Chenkham.discogs.models.DiscogsRelease
import com.Chenkham.discogs.models.DiscogsSearchResponse
import com.Chenkham.discogs.models.ReleaseInfo
import com.Chenkham.discogs.models.ReleaseTrack
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Discogs client for physical release data: pressings, labels, catalog numbers
 * and alternate cover art.
 *
 * Authenticates with a personal access token, which Discogs issues free from
 * the developer settings page. Rate limit is 60 requests/minute when authenticated.
 */
object Discogs {
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
                url("https://api.discogs.com/")
                // Discogs rejects requests without an identifying User-Agent
                header("User-Agent", "Echofy/1.0 +https://github.com/Chenkham/Echofy")
            }

            expectSuccess = false
        }
    }

    private var lastRequestTime = 0L
    private const val MIN_REQUEST_INTERVAL_MS = 1100L

    private suspend fun rateLimit() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            kotlinx.coroutines.delay(MIN_REQUEST_INTERVAL_MS - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    /**
     * Search releases matching an artist and album.
     */
    suspend fun searchRelease(
        artist: String,
        album: String,
        token: String,
        limit: Int = 10,
    ) = runCatching {
        require(token.isNotBlank()) { "Discogs token is required" }
        rateLimit()

        client
            .get("database/search") {
                header("Authorization", "Discogs token=$token")
                parameter("artist", artist)
                parameter("release_title", album)
                parameter("type", "release")
                parameter("per_page", limit)
            }.body<DiscogsSearchResponse>()
            .results
    }

    /**
     * Full details for a single release id.
     */
    suspend fun getRelease(
        releaseId: Long,
        token: String,
    ) = runCatching {
        require(token.isNotBlank()) { "Discogs token is required" }
        rateLimit()

        val release = client
            .get("releases/$releaseId") {
                header("Authorization", "Discogs token=$token")
            }.body<DiscogsRelease>()

        release.toReleaseInfo()
    }

    /**
     * Convenience path used by the album screen: search, then expand the top hit.
     */
    suspend fun findReleaseInfo(
        artist: String,
        album: String,
        token: String,
    ) = runCatching {
        val results = searchRelease(artist, album, token).getOrThrow()
        val best = results.firstOrNull() ?: error("No Discogs release found for \"$album\"")
        getRelease(best.id, token).getOrThrow()
    }

    private fun DiscogsRelease.toReleaseInfo() = ReleaseInfo(
        id = id,
        title = title,
        year = year?.takeIf { it > 0 },
        country = country?.takeIf { it.isNotBlank() },
        genres = genres,
        styles = styles,
        labelName = labels.firstOrNull()?.name,
        catalogNumber = labels.firstOrNull()?.catno?.takeIf { it.isNotBlank() && it != "none" },
        formats = formats.mapNotNull { format ->
            val descriptors = format.descriptions.joinToString(", ")
            listOfNotNull(
                format.name?.takeIf { it.isNotBlank() },
                descriptors.takeIf { it.isNotBlank() },
            ).joinToString(" - ").takeIf { it.isNotBlank() }
        },
        coverImages = images.mapNotNull { it.uri?.takeIf { uri -> uri.isNotBlank() } },
        tracklist = tracklist.map {
            ReleaseTrack(
                position = it.position.orEmpty(),
                title = it.title.orEmpty(),
                duration = it.duration.orEmpty(),
            )
        },
        discogsUrl = uri?.takeIf { it.isNotBlank() },
    )
}
