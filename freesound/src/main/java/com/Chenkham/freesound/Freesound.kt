package com.Chenkham.freesound

import com.Chenkham.freesound.models.AmbientSound
import com.Chenkham.freesound.models.FreesoundSearchResponse
import com.Chenkham.freesound.models.FreesoundSound
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
 * Freesound client for ambient audio, white noise and field recordings.
 *
 * Requires a free API key. Only the preview streams are used, which are plain
 * MP3 URLs playable without further authentication.
 */
object Freesound {
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
                url("https://freesound.org/apiv2/")
            }

            expectSuccess = false
        }
    }

    /** Fields we need back; Freesound omits most of these unless asked. */
    private const val FIELDS = "id,name,description,username,duration,license,tags,previews,images"

    /** Curated categories surfaced in the Ambient Sounds screen. */
    val CATEGORIES = linkedMapOf(
        "rain" to "Rain",
        "ocean waves" to "Ocean",
        "forest birds" to "Forest",
        "white noise" to "White Noise",
        "thunderstorm" to "Thunder",
        "fireplace" to "Fireplace",
        "wind" to "Wind",
        "cafe ambience" to "Cafe",
        "night crickets" to "Night",
        "stream water" to "Stream",
    )

    /**
     * Free-text search across Freesound.
     */
    suspend fun search(
        query: String,
        apiKey: String,
        pageSize: Int = 30,
        minDurationSeconds: Int = 30,
    ) = runCatching {
        require(apiKey.isNotBlank()) { "Freesound API key is required" }

        client
            .get("search/text/") {
                header("Authorization", "Token $apiKey")
                parameter("query", query)
                parameter("fields", FIELDS)
                parameter("page_size", pageSize)
                parameter("filter", "duration:[$minDurationSeconds TO *]")
                parameter("sort", "rating_desc")
            }.body<FreesoundSearchResponse>()
            .results
            .mapNotNull { it.toAmbientSound() }
    }

    /**
     * Sounds for one of the curated [CATEGORIES] keys.
     */
    suspend fun getCategory(
        category: String,
        apiKey: String,
        pageSize: Int = 30,
    ) = search(
        query = category,
        apiKey = apiKey,
        pageSize = pageSize,
    )

    /**
     * Drops results with no playable preview, since they cannot be used.
     */
    private fun FreesoundSound.toAmbientSound(): AmbientSound? {
        val stream = previews?.previewHqMp3
            ?: previews?.previewLqMp3
            ?: return null

        return AmbientSound(
            id = id,
            name = name,
            description = description?.takeIf { it.isNotBlank() },
            author = username,
            durationSeconds = duration,
            license = license,
            tags = tags,
            streamUrl = stream,
            waveformUrl = images?.waveformM,
        )
    }
}
