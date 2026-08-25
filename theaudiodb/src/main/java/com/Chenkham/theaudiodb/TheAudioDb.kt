package com.Chenkham.theaudiodb

import com.Chenkham.theaudiodb.models.AudioDbAlbum
import com.Chenkham.theaudiodb.models.AudioDbAlbumResponse
import com.Chenkham.theaudiodb.models.AudioDbArtistInfo
import com.Chenkham.theaudiodb.models.AudioDbArtistResponse
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
 * TheAudioDB client. Free tier uses the public test key "2".
 *
 * Provides the biography and high-resolution artwork that MusicBrainz lacks,
 * so the two are merged rather than treated as alternatives.
 */
object TheAudioDb {
    private const val PUBLIC_TEST_KEY = "2"

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
                url("https://www.theaudiodb.com/api/v1/json/")
            }

            expectSuccess = false
        }
    }

    /**
     * Look up artist extras by name. [apiKey] falls back to the free public key.
     */
    suspend fun getArtist(
        name: String,
        apiKey: String? = null,
    ) = runCatching {
        val key = apiKey?.takeIf { it.isNotBlank() } ?: PUBLIC_TEST_KEY
        val response = client
            .get("$key/search.php") {
                parameter("s", name)
            }.body<AudioDbArtistResponse>()

        val artist = response.artists?.firstOrNull()
            ?: error("Artist not found on TheAudioDB")

        AudioDbArtistInfo(
            id = artist.idArtist,
            name = artist.strArtist,
            biography = artist.biographyEn?.takeIf { it.isNotBlank() },
            thumbUrl = artist.thumb?.takeIf { it.isNotBlank() },
            fanartUrls = listOfNotNull(
                artist.fanart?.takeIf { it.isNotBlank() },
                artist.fanart2?.takeIf { it.isNotBlank() },
                artist.fanart3?.takeIf { it.isNotBlank() },
            ),
            bannerUrl = artist.banner?.takeIf { it.isNotBlank() },
            logoUrl = artist.logo?.takeIf { it.isNotBlank() },
            genre = artist.genre?.takeIf { it.isNotBlank() },
            style = artist.style?.takeIf { it.isNotBlank() },
            mood = artist.mood?.takeIf { it.isNotBlank() },
            formedYear = artist.formedYear?.takeIf { it.isNotBlank() && it != "0" },
            country = artist.country?.takeIf { it.isNotBlank() },
            website = artist.website?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Album details including description and cover art.
     */
    suspend fun searchAlbum(
        artist: String,
        album: String,
        apiKey: String? = null,
    ) = runCatching {
        val key = apiKey?.takeIf { it.isNotBlank() } ?: PUBLIC_TEST_KEY
        client
            .get("$key/searchalbum.php") {
                parameter("s", artist)
                parameter("a", album)
            }.body<AudioDbAlbumResponse>()
            .album
            ?: emptyList<AudioDbAlbum>()
    }
}
