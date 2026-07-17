package com.Chenkham.Echofy.lastfm

import com.Chenkham.Echofy.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale

enum class LastFmMethod(
    val apiName: String,
    val requiresSession: Boolean = false,
    val write: Boolean = false,
) {
    ALBUM_ADD_TAGS("album.addTags", requiresSession = true, write = true),
    ALBUM_GET_INFO("album.getInfo"),
    ALBUM_GET_TAGS("album.getTags"),
    ALBUM_GET_TOP_TAGS("album.getTopTags"),
    ALBUM_REMOVE_TAG("album.removeTag", requiresSession = true, write = true),
    ALBUM_SEARCH("album.search"),

    ARTIST_ADD_TAGS("artist.addTags", requiresSession = true, write = true),
    ARTIST_GET_CORRECTION("artist.getCorrection"),
    ARTIST_GET_INFO("artist.getInfo"),
    ARTIST_GET_SIMILAR("artist.getSimilar"),
    ARTIST_GET_TAGS("artist.getTags"),
    ARTIST_GET_TOP_ALBUMS("artist.getTopAlbums"),
    ARTIST_GET_TOP_TAGS("artist.getTopTags"),
    ARTIST_GET_TOP_TRACKS("artist.getTopTracks"),
    ARTIST_REMOVE_TAG("artist.removeTag", requiresSession = true, write = true),
    ARTIST_SEARCH("artist.search"),

    AUTH_GET_MOBILE_SESSION("auth.getMobileSession", write = true),
    AUTH_GET_SESSION("auth.getSession"),
    AUTH_GET_TOKEN("auth.getToken"),

    CHART_GET_TOP_ARTISTS("chart.getTopArtists"),
    CHART_GET_TOP_TAGS("chart.getTopTags"),
    CHART_GET_TOP_TRACKS("chart.getTopTracks"),

    GEO_GET_TOP_ARTISTS("geo.getTopArtists"),
    GEO_GET_TOP_TRACKS("geo.getTopTracks"),

    LIBRARY_GET_ARTISTS("library.getArtists"),

    TAG_GET_INFO("tag.getInfo"),
    TAG_GET_SIMILAR("tag.getSimilar"),
    TAG_GET_TOP_ALBUMS("tag.getTopAlbums"),
    TAG_GET_TOP_ARTISTS("tag.getTopArtists"),
    TAG_GET_TOP_TAGS("tag.getTopTags"),
    TAG_GET_TOP_TRACKS("tag.getTopTracks"),
    TAG_GET_WEEKLY_CHART_LIST("tag.getWeeklyChartList"),

    TRACK_ADD_TAGS("track.addTags", requiresSession = true, write = true),
    TRACK_GET_CORRECTION("track.getCorrection"),
    TRACK_GET_INFO("track.getInfo"),
    TRACK_GET_SIMILAR("track.getSimilar"),
    TRACK_GET_TAGS("track.getTags"),
    TRACK_GET_TOP_TAGS("track.getTopTags"),
    TRACK_LOVE("track.love", requiresSession = true, write = true),
    TRACK_REMOVE_TAG("track.removeTag", requiresSession = true, write = true),
    TRACK_SCROBBLE("track.scrobble", requiresSession = true, write = true),
    TRACK_SEARCH("track.search"),
    TRACK_UNLOVE("track.unlove", requiresSession = true, write = true),
    TRACK_UPDATE_NOW_PLAYING("track.updateNowPlaying", requiresSession = true, write = true),

    USER_GET_FRIENDS("user.getFriends"),
    USER_GET_INFO("user.getInfo"),
    USER_GET_LOVED_TRACKS("user.getLovedTracks"),
    USER_GET_PERSONAL_TAGS("user.getPersonalTags"),
    USER_GET_RECENT_TRACKS("user.getRecentTracks"),
    USER_GET_TOP_ALBUMS("user.getTopAlbums"),
    USER_GET_TOP_ARTISTS("user.getTopArtists"),
    USER_GET_TOP_TAGS("user.getTopTags"),
    USER_GET_TOP_TRACKS("user.getTopTracks"),
    USER_GET_WEEKLY_ALBUM_CHART("user.getWeeklyAlbumChart"),
    USER_GET_WEEKLY_ARTIST_CHART("user.getWeeklyArtistChart"),
    USER_GET_WEEKLY_CHART_LIST("user.getWeeklyChartList"),
    USER_GET_WEEKLY_TRACK_CHART("user.getWeeklyTrackChart"),
}

data class LastFmCredentials(
    val apiKey: String,
    val apiSecret: String,
    val sessionKey: String,
    val username: String = "",
) {
    val isComplete: Boolean
        get() = apiKey.isNotBlank() && apiSecret.isNotBlank() && sessionKey.isNotBlank()
}

data class LastFmSession(
    val username: String,
    val sessionKey: String,
    val subscriber: Boolean,
)

data class LastFmTrack(
    val artist: String,
    val track: String,
    val album: String? = null,
    val albumArtist: String? = null,
    val durationSeconds: Int? = null,
    val musicBrainzId: String? = null,
)

class LastFmException(
    val code: Int,
    override val message: String,
) : Exception(message)

object LastFmGateway {
    private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"
    private const val AUTH_URL = "https://www.last.fm/api/auth/"
    private const val USER_AGENT =
        "Echofy-Android/3.2.2 (Last.fm integration; https://github.com/Arturo254/Echofy)"

    val supportedMethods: List<LastFmMethod> = LastFmMethod.entries

    fun buildAuthorizationUrl(apiKey: String, token: String): String =
        "$AUTH_URL?api_key=${apiKey.urlEncoded()}&token=${token.urlEncoded()}"

    suspend fun getToken(apiKey: String, apiSecret: String): String {
        val response = call(
            method = LastFmMethod.AUTH_GET_TOKEN,
            apiKey = apiKey,
            apiSecret = apiSecret,
            signed = true,
        )
        return response.optString("token").ifBlank {
            throw LastFmException(0, "Last.fm did not return an auth token")
        }
    }

    suspend fun getSession(apiKey: String, apiSecret: String, token: String): LastFmSession {
        val response = call(
            method = LastFmMethod.AUTH_GET_SESSION,
            apiKey = apiKey,
            apiSecret = apiSecret,
            parameters = mapOf("token" to token),
            signed = true,
            forcePost = true,
        )
        val session = response.getJSONObject("session")
        return LastFmSession(
            username = session.optString("name"),
            sessionKey = session.getString("key"),
            subscriber = session.optInt("subscriber", 0) == 1,
        )
    }

    suspend fun getMobileSession(username: String, password: String): LastFmSession {
        val response = call(
            method = LastFmMethod.AUTH_GET_MOBILE_SESSION,
            apiKey = BuildConfig.LASTFM_API_KEY,
            apiSecret = BuildConfig.LASTFM_SECRET,
            parameters = mapOf("username" to username, "password" to password),
            signed = true,
            forcePost = true,
        )
        val session = response.getJSONObject("session")
        return LastFmSession(
            username = session.optString("name"),
            sessionKey = session.getString("key"),
            subscriber = session.optInt("subscriber", 0) == 1,
        )
    }

    suspend fun updateNowPlaying(
        credentials: LastFmCredentials,
        track: LastFmTrack,
    ): JSONObject = call(
        method = LastFmMethod.TRACK_UPDATE_NOW_PLAYING,
        credentials = credentials,
        parameters = track.toParameters(includeDuration = true),
        signed = true,
        forcePost = true,
    )

    suspend fun scrobble(
        credentials: LastFmCredentials,
        track: LastFmTrack,
        timestampSeconds: Long,
    ): JSONObject = call(
        method = LastFmMethod.TRACK_SCROBBLE,
        credentials = credentials,
        parameters = track.toParameters(includeDuration = true) + mapOf(
            "timestamp" to timestampSeconds.toString()
        ),
        signed = true,
        forcePost = true,
    )

    suspend fun love(credentials: LastFmCredentials, track: LastFmTrack): JSONObject = call(
        method = LastFmMethod.TRACK_LOVE,
        credentials = credentials,
        parameters = track.toParameters(includeDuration = false),
        signed = true,
        forcePost = true,
    )

    suspend fun unlove(credentials: LastFmCredentials, track: LastFmTrack): JSONObject = call(
        method = LastFmMethod.TRACK_UNLOVE,
        credentials = credentials,
        parameters = track.toParameters(includeDuration = false),
        signed = true,
        forcePost = true,
    )

    suspend fun call(
        method: LastFmMethod,
        credentials: LastFmCredentials,
        parameters: Map<String, String?> = emptyMap(),
        signed: Boolean = method.requiresSession || method.write,
        forcePost: Boolean? = null,
    ): JSONObject = call(
        method = method,
        apiKey = credentials.apiKey,
        apiSecret = credentials.apiSecret,
        sessionKey = credentials.sessionKey,
        parameters = parameters,
        signed = signed,
        forcePost = forcePost,
    )

    suspend fun call(
        method: LastFmMethod,
        apiKey: String,
        apiSecret: String = "",
        sessionKey: String = "",
        parameters: Map<String, String?> = emptyMap(),
        signed: Boolean = method.requiresSession || method.write,
        forcePost: Boolean? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw LastFmException(10, "Last.fm API key is required")
        if (method.requiresSession && sessionKey.isBlank()) {
            throw LastFmException(9, "Last.fm session key is required")
        }
        if (signed && apiSecret.isBlank()) {
            throw LastFmException(13, "Last.fm API secret is required for signed calls")
        }

        val requestParameters = linkedMapOf<String, String>()
        requestParameters["method"] = method.apiName
        requestParameters["api_key"] = apiKey
        requestParameters["format"] = "json"
        parameters.forEach { (key, value) ->
            if (!value.isNullOrBlank()) {
                requestParameters[key] = value
            }
        }
        if (method.requiresSession && "sk" !in requestParameters) {
            requestParameters["sk"] = sessionKey
        }
        if (signed) {
            requestParameters["api_sig"] = createSignature(requestParameters, apiSecret)
        }

        execute(
            parameters = requestParameters,
            post = forcePost ?: (method.write || method.requiresSession || method == LastFmMethod.AUTH_GET_SESSION),
        )
    }

    private fun execute(parameters: Map<String, String>, post: Boolean): JSONObject {
        val body = parameters.entries.joinToString("&") { (key, value) ->
            "${key.urlEncoded()}=${value.urlEncoded()}"
        }
        val url = if (post) BASE_URL else "$BASE_URL?$body"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = if (post) "POST" else "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            if (post) {
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
        }

        if (post) {
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val responseJson = runCatching { JSONObject(responseBody) }.getOrElse {
            throw LastFmException(statusCode, responseBody.ifBlank { "Empty response from Last.fm" })
        }

        if (responseJson.has("error")) {
            throw LastFmException(
                code = responseJson.optInt("error", statusCode),
                message = responseJson.optString("message", "Last.fm request failed"),
            )
        }
        if (statusCode !in 200..299) {
            throw LastFmException(statusCode, responseJson.optString("message", responseBody))
        }

        return responseJson
    }

    private fun createSignature(parameters: Map<String, String>, apiSecret: String): String {
        val signatureBase = buildString {
            parameters
                .filterKeys { it != "format" && it != "callback" && it != "api_sig" }
                .toSortedMap()
                .forEach { (key, value) ->
                    append(key)
                    append(value)
                }
            append(apiSecret)
        }

        val bytes = MessageDigest.getInstance("MD5").digest(signatureBase.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(Locale.US, it.toInt() and 0xff) }
    }

    private fun LastFmTrack.toParameters(includeDuration: Boolean): Map<String, String?> = mapOf(
        "artist" to artist,
        "track" to track,
        "album" to album,
        "albumArtist" to albumArtist,
        "duration" to durationSeconds?.takeIf { includeDuration && it > 0 }?.toString(),
        "mbid" to musicBrainzId,
    )

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")
}
