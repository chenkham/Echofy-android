package com.Chenkham.genius

import com.Chenkham.genius.models.GeniusSearchResponse
import com.Chenkham.genius.models.GeniusSongMatch
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Genius client.
 *
 * Genius deliberately does not expose lyrics through its JSON API; the API only
 * returns a song page URL. Lyric text therefore has to be read out of the song
 * page HTML, which is what [getLyrics] does. That makes this provider inherently
 * more fragile than LrcLib or KuGou, so it is registered last in the chain.
 *
 * Lyrics returned here are always plain text with no timestamps.
 */
object Genius {
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
                header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36",
                )
            }

            expectSuccess = false
        }
    }

    /**
     * Search Genius for a song. [accessToken] is a free Genius API client token.
     */
    suspend fun search(
        title: String,
        artist: String,
        accessToken: String,
    ) = runCatching {
        require(accessToken.isNotBlank()) { "Genius access token is required" }

        val query = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")

        client
            .get("https://api.genius.com/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
            }.body<GeniusSearchResponse>()
            .response
            ?.hits
            .orEmpty()
            .mapNotNull { hit ->
                val song = hit.result ?: return@mapNotNull null
                GeniusSongMatch(
                    id = song.id,
                    title = song.title,
                    artistName = song.primaryArtist?.name.orEmpty(),
                    url = song.url,
                    artworkUrl = song.songArtImageUrl,
                )
            }
    }

    /**
     * Fetch plain-text lyrics for the best matching song.
     */
    suspend fun getLyrics(
        title: String,
        artist: String,
        accessToken: String,
    ) = runCatching {
        val matches = search(title, artist, accessToken).getOrThrow()
        val best = pickBestMatch(matches, title, artist)
            ?: error("No Genius match for \"$title\"")

        val html = client.get(best.url).bodyAsText()
        val lyrics = extractLyricsFromHtml(html)

        if (lyrics.isNullOrBlank()) error("Could not extract lyrics from Genius page")
        lyrics
    }

    /**
     * Prefer a match whose artist also lines up, since Genius search happily
     * returns covers and remixes for a bare title query.
     */
    private fun pickBestMatch(
        matches: List<GeniusSongMatch>,
        title: String,
        artist: String,
    ): GeniusSongMatch? {
        if (matches.isEmpty()) return null
        if (artist.isBlank()) return matches.first()

        val wantedArtist = artist.normalizeForMatch()
        val wantedTitle = title.normalizeForMatch()

        return matches.firstOrNull {
            val hitArtist = it.artistName.normalizeForMatch()
            val hitTitle = it.title.normalizeForMatch()
            (hitArtist.contains(wantedArtist) || wantedArtist.contains(hitArtist)) &&
                (hitTitle.contains(wantedTitle) || wantedTitle.contains(hitTitle))
        } ?: matches.firstOrNull {
            val hitArtist = it.artistName.normalizeForMatch()
            hitArtist.contains(wantedArtist) || wantedArtist.contains(hitArtist)
        }
    }

    private fun String.normalizeForMatch() =
        lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .trim()

    /**
     * Genius renders lyrics into one or more `<div data-lyrics-container="true">`
     * blocks. Pull those out and convert the inline markup back to plain text.
     */
    private fun extractLyricsFromHtml(html: String): String? {
        val containers = LYRICS_CONTAINER_REGEX.findAll(html)
            .map { it.groupValues[1] }
            .toList()

        if (containers.isEmpty()) return null

        return containers
            .joinToString("\n") { it.htmlToPlainText() }
            .lines()
            .map { it.trim() }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun String.htmlToPlainText() =
        replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</div>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")

    private val LYRICS_CONTAINER_REGEX =
        Regex(
            "<div[^>]*data-lyrics-container=\"true\"[^>]*>(.*?)</div>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )
}
