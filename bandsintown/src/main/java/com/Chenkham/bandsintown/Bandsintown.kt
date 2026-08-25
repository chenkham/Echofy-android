package com.Chenkham.bandsintown

import com.Chenkham.bandsintown.models.BandsintownEvent
import com.Chenkham.bandsintown.models.Concert
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
 * Bandsintown client for upcoming concert dates.
 *
 * Requires an app id, which Bandsintown issues free on request. Callers pass it
 * in so the key can live in user settings rather than being baked into source.
 */
object Bandsintown {
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
                url("https://rest.bandsintown.com/")
            }

            expectSuccess = false
        }
    }

    /**
     * Upcoming events for [artistName]. Returns an empty list when the artist
     * has no dates announced, which is a normal outcome rather than an error.
     */
    suspend fun getUpcomingEvents(
        artistName: String,
        appId: String,
    ) = runCatching {
        require(appId.isNotBlank()) { "Bandsintown app id is required" }

        val encoded = artistName.replace("/", "%252F")
        val events = client
            .get("artists/$encoded/events") {
                parameter("app_id", appId)
                parameter("date", "upcoming")
            }.body<List<BandsintownEvent>>()

        events.map { it.toConcert(artistName) }
    }

    private fun BandsintownEvent.toConcert(artistName: String) = Concert(
        id = id,
        artistName = artistName,
        venueName = venue?.name.orEmpty(),
        city = venue?.city.orEmpty(),
        country = venue?.country.orEmpty(),
        datetime = datetime,
        ticketUrl = offers.firstOrNull { it.type == "Tickets" }?.url ?: url,
        lineup = lineup,
    )
}
