package com.Chenkham.ytmusicapi

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.Locale

/**
 * InnerTube client for making requests to YouTube Music's internal API.
 * Based on ytmusicapi's setup.py and _send_request implementation.
 */
class InnerTubeClient(
    private val locale: Locale = Locale.getDefault(),
    private val visitorData: String? = null,
    private val cookie: String? = null
) {
    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1/"
        private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        
        // WEB_REMIX client context (YouTube Music web client)
        private const val CLIENT_NAME = "WEB_REMIX"
        private const val CLIENT_VERSION = "1.20241111.01.00"
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(ContentEncoding) {
            gzip()
            deflate()
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            header("Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            header("X-Goog-Api-Key", API_KEY)
            cookie?.let { header("Cookie", it) }
            visitorData?.let { header("X-Goog-Visitor-Id", it) }
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Build the context object for InnerTube requests
     */
    private fun buildContext(): JsonObject = buildJsonObject {
        putJsonObject("client") {
            put("clientName", CLIENT_NAME)
            put("clientVersion", CLIENT_VERSION)
            put("hl", locale.language)
            put("gl", locale.country.ifEmpty { "US" })
            put("platform", "DESKTOP")
            put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            visitorData?.let { put("visitorData", it) }
        }
        putJsonObject("user") {
            put("lockedSafetyMode", false)
        }
    }

    /**
     * Send a browse request to InnerTube
     */
    suspend fun browse(
        browseId: String,
        params: String? = null,
        formData: Map<String, List<String>>? = null
    ): JsonObject {
        val body = buildJsonObject {
            put("context", buildContext())
            put("browseId", browseId)
            params?.let { put("params", it) }
            formData?.let { fd ->
                putJsonObject("formData") {
                    fd.forEach { (key, values) ->
                        putJsonArray(key) {
                            values.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
                        }
                    }
                }
            }
        }

        val response = httpClient.post("${BASE_URL}browse") {
            setBody(body.toString())
        }

        return json.parseToJsonElement(response.bodyAsText()) as JsonObject
    }

    /**
     * Close the HTTP client
     */
    fun close() {
        httpClient.close()
    }
}
