package com.Chenkham.mixcloud

import com.Chenkham.mixcloud.models.DjMix
import com.Chenkham.mixcloud.models.MixcloudCloudcast
import com.Chenkham.mixcloud.models.MixcloudResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Mixcloud client for DJ mixes, radio shows and long-form podcasts.
 * The public read API requires no key.
 */
object Mixcloud {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                    contentType = ContentType.Any,
                )
            }

            defaultRequest {
                url("https://api.mixcloud.com/")
            }

            expectSuccess = false
        }
    }

    /** Genre tags surfaced as browsing chips. */
    val TAGS = linkedMapOf(
        "house" to "House",
        "techno" to "Techno",
        "hip-hop" to "Hip Hop",
        "disco" to "Disco",
        "drum-and-bass" to "Drum & Bass",
        "ambient" to "Ambient",
        "jazz" to "Jazz",
        "funk" to "Funk",
        "chillout" to "Chillout",
        "electronic" to "Electronic",
    )

    /**
     * Search mixes by free text.
     */
    suspend fun search(
        query: String,
        limit: Int = 30,
    ) = runCatching {
        client
            .get("search/") {
                parameter("q", query)
                parameter("type", "cloudcast")
                parameter("limit", limit)
            }.body<MixcloudResponse>()
            .data
            .map { it.toDjMix() }
    }

    /**
     * Popular mixes for a genre tag, e.g. "house".
     */
    suspend fun getByTag(
        tag: String,
        limit: Int = 30,
    ) = runCatching {
        client
            .get("discover/$tag/popular/") {
                parameter("limit", limit)
            }.body<MixcloudResponse>()
            .data
            .map { it.toDjMix() }
    }

    /**
     * Currently popular mixes across Mixcloud.
     */
    suspend fun getPopular(limit: Int = 30) = runCatching {
        client
            .get("popular/") {
                parameter("limit", limit)
            }.body<MixcloudResponse>()
            .data
            .map { it.toDjMix() }
    }

    private fun MixcloudCloudcast.toDjMix() = DjMix(
        key = key,
        title = name,
        artistName = user?.name.orEmpty(),
        durationSeconds = audioLength,
        thumbnailUrl = pictures?.extraLarge
            ?: pictures?.large
            ?: pictures?.medium,
        playCount = playCount,
        tags = tags.map { it.name },
        webUrl = url,
    )
}
