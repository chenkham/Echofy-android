package com.Chenkham.musicbrainz

import com.Chenkham.musicbrainz.models.ArtistInfo
import com.Chenkham.musicbrainz.models.ArtistSearchResponse
import com.Chenkham.musicbrainz.models.MusicBrainzArtist
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
 * MusicBrainz client for open music metadata. No API key required.
 * Rate limit: 1 req/sec (enforced by client delay). Requires a User-Agent.
 */
object MusicBrainz {
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
                url("https://musicbrainz.org/ws/2/")
                // MusicBrainz requires a User-Agent identifying the app
                headers.append("User-Agent", "Echofy/4.9.0 (https://chenkham.github.io; chenkhamchowlu@gmail.com)")
                headers.append("Accept", "application/json")
            }

            expectSuccess = false
        }
    }

    private var lastRequestTime = 0L
    private const val MIN_REQUEST_INTERVAL_MS = 1000L

    private suspend fun rateLimit() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            kotlinx.coroutines.delay(MIN_REQUEST_INTERVAL_MS - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    /**
     * Search for an artist by name. Returns up to [limit] matches.
     */
    suspend fun searchArtist(
        name: String,
        limit: Int = 5,
    ) = runCatching {
        rateLimit()
        client
            .get("artist") {
                parameter("query", "artist:\"$name\"")
                parameter("limit", limit)
                parameter("fmt", "json")
            }.body<ArtistSearchResponse>()
            .artists
    }

    /**
     * Get detailed artist info by MBID, including tags (genres), relations
     * (official homepage, Wikipedia), and life span.
     */
    suspend fun getArtist(mbid: String) = runCatching {
        rateLimit()
        val response = client
            .get("artist/$mbid") {
                parameter("inc", "tags+url-rels")
                parameter("fmt", "json")
            }.body<MusicBrainzArtist>()

        // Flatten to UI-friendly model
        ArtistInfo(
            mbid = response.id,
            name = response.name,
            disambiguation = response.disambiguation,
            country = response.country,
            type = response.type,
            beginDate = response.lifeSpan?.begin,
            endDate = response.lifeSpan?.end,
            isEnded = response.lifeSpan?.ended ?: false,
            genres = response.tags
                .sortedByDescending { it.count }
                .map { it.name }
                .take(10),
            officialHomepage = response.relations
                .firstOrNull { it.type == "official homepage" }
                ?.url?.resource,
            wikipediaUrl = response.relations
                .firstOrNull { it.type == "wikipedia" && it.url?.resource?.contains("en.wikipedia") == true }
                ?.url?.resource,
        )
    }
}
