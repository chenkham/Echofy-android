package com.Chenkham.Echofy.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.Chenkham.innertube.NewPipeUtils
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.YouTubeClient
import com.Chenkham.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.Chenkham.innertube.models.YouTubeClient.Companion.ANDROID_TESTSUITE
import com.Chenkham.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.Chenkham.innertube.models.YouTubeClient.Companion.IOS
import com.Chenkham.innertube.models.YouTubeClient.Companion.MOBILE
import com.Chenkham.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.Chenkham.innertube.models.YouTubeClient.Companion.WEB
import com.Chenkham.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.Chenkham.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.Chenkham.innertube.models.response.PlayerResponse
import com.Chenkham.Echofy.constants.AudioQuality
import okhttp3.OkHttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    // Expose available qualities to UI (PlayerMenu)
    val availableQualities = MutableStateFlow<List<String>>(emptyList())

    /**
     * The main client is used for metadata and initial streams. Do not use
     * other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets
     * (loudnessDb). Creditos completos de el commit completo a metrolist.
     *
     * [com.metrolist.innertube.models.YouTubeClient.WEB_REMIX] should be
     * preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    /**
     * Clients used for fallback streams in case the streams of the main client
     * do not work.
     *
     * Order matters: prefer clients that return direct URLs (no cipher/signatureCipher)
     * first, so NewPipe JS deobfuscation failures don't block playback.
     * ANDROID_MUSIC, ANDROID_TESTSUITE, ANDROID_VR_NO_AUTH, IOS and MOBILE all return
     * direct stream URLs without needing NewPipe signature deobfuscation.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_NO_AUTH,       // no-auth, direct URL — fast & reliable
        ANDROID_MUSIC,            // direct URL, no cipher
        ANDROID_TESTSUITE,        // direct URL, no cipher, no auth needed
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR,
        MOBILE
    )

    /**
     * Clients to try (in order) for fast-start playback. All of these return
     * direct stream URLs so we don't depend on NewPipe JS parsing.
     */
    private val FAST_START_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_NO_AUTH,
        ANDROID_MUSIC,
        ANDROID_TESTSUITE,
        IOS,
        MOBILE,
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    /**
     * FAST-START: Quick player response for instant playback.
     * Iterates through [FAST_START_CLIENTS] to find a working stream without URL
     * validation, keeping the response time fast (~200-800ms).
     */
    suspend fun fastStartPlayerResponse(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        videoQuality: String = "Auto",
        connectivityManager: ConnectivityManager,
        videoMode: Boolean = false,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("FAST-START: Quick fetch for $videoId")

        var lastReason: String? = null
        for ((index, client) in FAST_START_CLIENTS.withIndex()) {
            Timber.tag(logTag).d("FAST-START: Trying client ${index + 1}/${FAST_START_CLIENTS.size}: ${client.clientName}")
            val response = YouTube.player(videoId, playlistId, client, null).getOrNull() ?: continue
            if (response.playabilityStatus.status == "OK") {
                val result = runCatching {
                    processResponse(response, videoId, audioQuality, videoQuality, connectivityManager, videoMode)
                }.getOrNull()
                if (result != null) {
                    Timber.tag(logTag).d("FAST-START: SUCCESS with client ${client.clientName}")
                    return@runCatching result
                }
            }
            lastReason = response.playabilityStatus.reason
            Timber.tag(logTag).w("FAST-START: Client ${client.clientName} failed: $lastReason")
        }

        // All fast-start clients failed — throw so MusicService can fall back to full fetch
        throw PlaybackException(
            lastReason ?: "All fast-start clients failed",
            null,
            PlaybackException.ERROR_CODE_REMOTE_ERROR
        )
    }

    private fun processResponse(
        response: PlayerResponse,
        videoId: String,
        audioQuality: AudioQuality, 
        videoQuality: String,
        connectivityManager: ConnectivityManager,
        videoMode: Boolean
    ): PlaybackData {
        val format = findFormat(response, audioQuality, videoQuality, connectivityManager, videoMode)
            ?: throw Exception("No format found")
        
        val streamUrl = findUrlOrNull(format, videoId)
            ?: throw Exception("No stream URL found")
        
        val expiresIn = response.streamingData?.expiresInSeconds
            ?: throw Exception("No expiration time")
        
        Timber.tag(logTag).d("FAST-START: Got URL in quick path for $videoId")
        
        return PlaybackData(
            response.playerConfig?.audioConfig,
            response.videoDetails,
            format,
            streamUrl,
            expiresIn
        )
    }

    /**
     * Custom player response intended to use for playback. Metadata like
     * audioConfig and videoDetails are from [MAIN_CLIENT]. Format & stream can
     * be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        videoQuality: String = "Auto",
        connectivityManager: ConnectivityManager,
        videoMode: Boolean = false,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag)
            .d("Fetching player response for videoId: $videoId, playlistId: $playlistId, videoMode: $videoMode, videoQuality: $videoQuality")
        /**
         * This is required for some clients to get working streams however it
         * should not be forced for the [MAIN_CLIENT] because the response of the
         * [MAIN_CLIENT] is required even if the streams won't work from this
         * client. This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }
        Timber.tag(logTag)
            .d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        Timber.tag(logTag)
            .d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient
            if (clientIndex == -1) {
                // try with streams from main client first
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                // after main client use fallback clients
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag)
                    .d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    // skip client if it requires login but user is not logged in
                    Timber.tag(logTag)
                        .d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Timber.tag(logTag)
                    .d("Fetching player response for fallback client: ${client.clientName}")
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                Timber.tag(logTag)
                    .d("Player response status OK for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")

                format =
                    findFormat(
                        streamPlayerResponse,
                        audioQuality,
                        videoQuality,
                        connectivityManager,
                        videoMode
                    )

                if (format == null) {
                    Timber.tag(logTag)
                        .d("No suitable format found for client: ${if (clientIndex == -1) MAIN_CLIENT.clientName else STREAM_FALLBACK_CLIENTS[clientIndex].clientName}")
                    continue
                }

                Timber.tag(logTag).d("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")

                streamUrl = findUrlOrNull(format, videoId)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    Timber.tag(logTag).d("Stream expiration time not found")
                    continue
                }

                Timber.tag(logTag).d("Stream expires in: $streamExpiresInSeconds seconds")

                // Validate MAIN_CLIENT too; if it fails, fall back to the next client
                val shouldSkipValidation = clientIndex == STREAM_FALLBACK_CLIENTS.size - 1
                val isValid = if (shouldSkipValidation) {
                    Timber.tag(logTag)
                        .d("Using last fallback client without validation: ${client.clientName}")
                    true
                } else {
                    validateStatus(streamUrl, client.userAgent)
                }

                if (isValid) {
                    Timber.tag(logTag)
                        .d("Stream validated successfully with client: ${client.clientName}")
                    break
                } else {
                    Timber.tag(logTag)
                        .d("Stream validation failed for client: ${client.clientName}")
                }
            } else {
                Timber.tag(logTag)
                    .d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find format")
            throw Exception("Could not find format")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag)
            .d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    /**
     * Simple player response intended to use for metadata only. Stream URLs of
     * this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag)
            .d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = MAIN_CLIENT)
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        videoQuality: String,
        connectivityManager: ConnectivityManager,
        videoMode: Boolean
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag)
            .d("Finding format with audioQuality: $audioQuality, videoQuality: $videoQuality, network metered: ${connectivityManager.isActiveNetworkMetered}, videoMode: $videoMode")

        val format = if (videoMode) {
            // Find combined formats (video + audio) for direct playback
            // or high quality video formats
            val muxedFormats = playerResponse.streamingData?.formats
            val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats?.filter { it.mimeType.startsWith("video/") }

            // We currently use muxed if available, else adaptive
            val usableFormats = muxedFormats ?: adaptiveFormats
            
            val availableQualitiesLabels = usableFormats?.map { "${it.height}p" }?.distinct()?.sortedByDescending { it.replace("p", "").toIntOrNull() ?: 0 } ?: emptyList()
            availableQualities.tryEmit(availableQualitiesLabels)
            
            Timber.tag(logTag).d("Available muxed formats for video: $availableQualitiesLabels (Fallback to adaptive: ${muxedFormats == null})")

            val formats = usableFormats

            if (videoQuality == "Auto") {
                // If Metered (Data), use lowest quality.
                // If Unmetered (WiFi), use 720p or closest to it (balance between quality and buffer speed)
                if (connectivityManager.isActiveNetworkMetered) {
                     formats?.minByOrNull { it.bitrate }
                } else {
                     // Prefer 720p for WiFi Auto, or max if not available
                     val targetHeight = 720
                     formats?.minByOrNull { 
                        kotlin.math.abs((it.height ?: 0) - targetHeight) 
                     } ?: formats?.maxByOrNull { it.bitrate }
                }
            } else {
                // Determine target height from quality label (e.g., "1080p" -> 1080)
                val targetHeight = videoQuality.replace("p", "").toIntOrNull() ?: 720
                
                // Find format closest to target quality
                formats?.minByOrNull { 
                    kotlin.math.abs((it.height ?: 0) - targetHeight) 
                } ?: formats?.maxByOrNull { it.bitrate }
            }
        } else {
            playerResponse.streamingData?.adaptiveFormats
                ?.filter { it.isAudio }
                ?.maxByOrNull {
                    it.bitrate * when (audioQuality) {
                        AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                        AudioQuality.HIGH -> 1
                        AudioQuality.LOW -> -1
                    } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
                }
        }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}, height: ${format.height}, qualityLabel: ${format.qualityLabel}")
        } else {
            Timber.tag(logTag).d("No suitable format found")
        }

        return format
    }

    /**
     * Checks if the stream url returns a successful status. If this returns
     * true the url is likely to work. If this returns false the url might
     * cause an error during playback.
     */
    private fun validateStatus(url: String, userAgent: String?): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = okhttp3.Request.Builder()
                .head()
                .url(url)
            
            if (userAgent != null) {
                requestBuilder.header("User-Agent", userAgent)
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag)
                .d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which
     * reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports
     * exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }
}