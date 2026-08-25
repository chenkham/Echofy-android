package com.Chenkham.tastedive

import com.Chenkham.tastedive.models.Recommendation
import com.Chenkham.tastedive.models.TasteDiveResponse
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
 * TasteDive client for similar-artist and cross-domain recommendations.
 * Requires a free API key, supplied by the caller from user settings.
 */
object TasteDive {
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
                url("https://tastedive.com/api/")
            }

            expectSuccess = false
        }
    }

    /**
     * Artists similar to [artistName].
     */
    suspend fun getSimilarArtists(
        artistName: String,
        apiKey: String,
        limit: Int = 12,
    ) = query(
        seed = "music:$artistName",
        apiKey = apiKey,
        type = "music",
        limit = limit,
    )

    /**
     * Recommendations in another medium seeded from an artist, e.g. movies for
     * someone who likes this band.
     */
    suspend fun getCrossDomain(
        artistName: String,
        apiKey: String,
        targetType: String,
        limit: Int = 12,
    ) = query(
        seed = "music:$artistName",
        apiKey = apiKey,
        type = targetType,
        limit = limit,
    )

    private suspend fun query(
        seed: String,
        apiKey: String,
        type: String,
        limit: Int,
    ) = runCatching {
        require(apiKey.isNotBlank()) { "TasteDive API key is required" }

        client
            .get("similar") {
                parameter("q", seed)
                parameter("type", type)
                parameter("limit", limit)
                parameter("info", 1)
                parameter("k", apiKey)
            }.body<TasteDiveResponse>()
            .similar
            ?.results
            .orEmpty()
            .map {
                Recommendation(
                    name = it.name,
                    type = it.type,
                    teaser = it.teaser?.takeIf { teaser -> teaser.isNotBlank() },
                    wikipediaUrl = it.wikipediaUrl?.takeIf { url -> url.isNotBlank() },
                )
            }
    }
}
