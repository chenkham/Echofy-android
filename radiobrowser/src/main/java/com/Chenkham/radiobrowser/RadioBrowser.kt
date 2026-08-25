package com.Chenkham.radiobrowser

import com.Chenkham.radiobrowser.models.RadioStation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Radio Browser API client for 30,000+ internet radio stations.
 * Free, no API key required. Community-maintained database.
 */
object RadioBrowser {
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
                // Use one of the public API servers (rotates via DNS)
                url("https://de1.api.radio-browser.info/json/")
            }

            expectSuccess = false
        }
    }

    /**
     * Search stations by name.
     */
    suspend fun searchByName(
        name: String,
        limit: Int = 50,
    ) = runCatching {
        client
            .get("stations/byname/$name") {
                parameter("limit", limit)
                parameter("order", "votes")
                parameter("reverse", "true")
            }.body<List<RadioStation>>()
    }

    /**
     * Get stations by tag (genre). Common tags: pop, rock, jazz, classical,
     * news, talk, sports, 80s, 90s, electronic, hip-hop, country.
     */
    suspend fun getByTag(
        tag: String,
        limit: Int = 50,
    ) = runCatching {
        client
            .get("stations/bytag/$tag") {
                parameter("limit", limit)
                parameter("order", "votes")
                parameter("reverse", "true")
            }.body<List<RadioStation>>()
    }

    /**
     * Get stations by country code (ISO 3166-1 alpha-2).
     * E.g. "US", "GB", "DE", "FR", "IN", "BR", "JP", "AU".
     */
    suspend fun getByCountry(
        countryCode: String,
        limit: Int = 50,
    ) = runCatching {
        client
            .get("stations/bycountrycode/$countryCode") {
                parameter("limit", limit)
                parameter("order", "votes")
                parameter("reverse", "true")
            }.body<List<RadioStation>>()
    }

    /**
     * Get stations by language code (ISO 639).
     * E.g. "en", "es", "fr", "de", "hi", "zh", "ar", "pt", "ja".
     */
    suspend fun getByLanguage(
        languageCode: String,
        limit: Int = 50,
    ) = runCatching {
        client
            .get("stations/bylanguage/$languageCode") {
                parameter("limit", limit)
                parameter("order", "votes")
                parameter("reverse", "true")
            }.body<List<RadioStation>>()
    }

    /**
     * Get top voted stations (most popular).
     */
    suspend fun getTopStations(limit: Int = 50) = runCatching {
        client
            .get("stations/topvote/$limit")
            .body<List<RadioStation>>()
    }

    /**
     * Get trending stations (most clicks recently).
     */
    suspend fun getTrendingStations(limit: Int = 50) = runCatching {
        client
            .get("stations/topclick/$limit")
            .body<List<RadioStation>>()
    }

    /**
     * Notify Radio Browser that a station was clicked/played.
     * This helps keep usage statistics accurate for the community.
     */
    suspend fun registerClick(stationUuid: String) = runCatching {
        client.post("url/$stationUuid")
        Unit
    }
}

