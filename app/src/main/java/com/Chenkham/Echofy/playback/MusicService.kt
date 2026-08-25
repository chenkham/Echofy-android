@file:Suppress("DEPRECATION")

package com.Chenkham.Echofy.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import java.util.concurrent.ConcurrentHashMap
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.constants.PlaybackModeKey
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.Chenkham.jossredconnect.JossRedClient
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AudioNormalizationKey
import com.Chenkham.Echofy.constants.AudioQuality
import com.Chenkham.Echofy.constants.AudioQualityKey
import com.Chenkham.Echofy.constants.PlayerStreamClient
import com.Chenkham.Echofy.constants.PlayerStreamClientKey
import com.Chenkham.Echofy.constants.AutoLoadMoreKey
import com.Chenkham.Echofy.constants.AutoSkipNextOnErrorKey
import com.Chenkham.Echofy.constants.DisableLoadMoreWhenRepeatAllKey
import com.Chenkham.Echofy.constants.DiscordTokenKey
import com.Chenkham.Echofy.constants.DiscordUseDetailsKey
import com.Chenkham.Echofy.constants.EnableDiscordRPCKey
import com.Chenkham.Echofy.constants.HideExplicitKey
import com.Chenkham.Echofy.constants.HistoryDuration
import com.Chenkham.Echofy.constants.MediaSessionConstants.CommandToggleLike
import com.Chenkham.Echofy.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.Chenkham.Echofy.constants.MediaSessionConstants.CommandToggleShuffle
import com.Chenkham.Echofy.constants.PauseListenHistoryKey
import com.Chenkham.Echofy.constants.PersistentQueueKey
import com.Chenkham.Echofy.constants.PlayerVolumeKey
import com.Chenkham.Echofy.constants.RepeatModeKey
import com.Chenkham.Echofy.constants.ShowLyricsKey
import com.Chenkham.Echofy.constants.SimilarContent
import com.Chenkham.Echofy.constants.SkipSilenceKey
import com.Chenkham.Echofy.constants.SleepTimerFadeDurationKey
import com.Chenkham.Echofy.constants.SleepTimerFadeOutKey
import com.Chenkham.Echofy.constants.VolumeFadeOnPauseKey
import com.Chenkham.Echofy.constants.ListeningReminderEnabledKey
import com.Chenkham.Echofy.constants.ListeningReminderMinutesKey
import com.Chenkham.Echofy.constants.LongFormMinMinutesKey
import com.Chenkham.Echofy.constants.LongFormPlaybackSpeedKey
import com.Chenkham.Echofy.constants.RememberPlaybackSettingsKey
import com.Chenkham.Echofy.constants.SilentOutroSecondsKey
import com.Chenkham.Echofy.constants.SkipSilentOutroKey
import com.Chenkham.Echofy.constants.SpeedPerContentTypeKey
import com.Chenkham.Echofy.constants.playbackTempoKey
import com.Chenkham.Echofy.constants.playbackPitchKey
import com.Chenkham.Echofy.constants.VolumeLimitEnabledKey
import com.Chenkham.Echofy.constants.MonoAudioKey
import com.Chenkham.Echofy.constants.VocalSuppressionKey
import com.Chenkham.Echofy.constants.AudioBalanceKey
import com.Chenkham.Echofy.constants.SmartResumeEnabledKey
import com.Chenkham.Echofy.constants.SmartResumeMinMinutesKey
import com.Chenkham.Echofy.constants.ResumeOnHeadphonesKey
import com.Chenkham.Echofy.constants.resumePositionKey
import com.Chenkham.Echofy.constants.VolumeLimitPercentKey
import com.Chenkham.Echofy.constants.VideoPlaybackEnabledKey
import com.Chenkham.Echofy.constants.VideoCacheEnabledKey
import com.Chenkham.Echofy.db.*
import com.Chenkham.Echofy.db.daos.*
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.Event
import com.Chenkham.Echofy.db.entities.FormatEntity
import com.Chenkham.Echofy.db.entities.LyricsEntity
import com.Chenkham.Echofy.db.entities.RelatedSongMap
import com.Chenkham.Echofy.di.DownloadCache
import com.Chenkham.Echofy.di.PlayerCache
import com.Chenkham.Echofy.extensions.SilentHandler
import com.Chenkham.Echofy.extensions.collect
import com.Chenkham.Echofy.extensions.collectLatest
import com.Chenkham.Echofy.extensions.currentMetadata
import com.Chenkham.Echofy.extensions.findNextMediaItemById
import com.Chenkham.Echofy.extensions.mediaItems
import com.Chenkham.Echofy.extensions.metadata
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.extensions.toQueue
import com.Chenkham.Echofy.jam.AppwriteTogetherSessionManager
import com.Chenkham.Echofy.jam.JamParticipantRole
import com.Chenkham.Echofy.jam.JamPlaybackSnapshot
import com.Chenkham.Echofy.jam.JamPlaybackTransportState
import com.Chenkham.Echofy.jam.JamQueueItem
import com.Chenkham.Echofy.jam.JamQueueSeed
import com.Chenkham.Echofy.jam.JamSessionPhase
import com.Chenkham.Echofy.lyrics.LyricsHelper
import com.Chenkham.Echofy.models.PersistPlayerState
import com.Chenkham.Echofy.models.PersistQueue
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.EmptyQueue
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.playback.queues.Queue
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.playback.queues.filterExplicit
import com.Chenkham.Echofy.utils.CoilBitmapLoader
import com.Chenkham.Echofy.utils.DiscordRPC
import com.Chenkham.Echofy.utils.NetworkConnectivityObserver
import com.Chenkham.Echofy.utils.YTPlayerUtils
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.enumPreference
import com.Chenkham.Echofy.constants.BassBoostEnabledKey
import com.Chenkham.Echofy.constants.BassBoostStrengthKey
import com.Chenkham.Echofy.constants.EqualizerBandLevelsKey
import com.Chenkham.Echofy.constants.EqualizerEnabledKey
import com.Chenkham.Echofy.constants.EqualizerPresetKey
import com.Chenkham.Echofy.constants.VideoQualityKey
import com.Chenkham.Echofy.constants.VisitorDataKey
import com.Chenkham.Echofy.constants.VisitorDataTimestampKey
import com.Chenkham.Echofy.utils.get
import com.Chenkham.Echofy.utils.reportException

import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.NonCancellable

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback


    
    private var wakeLock: PowerManager.WakeLock? = null

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, exception ->
        Timber.tag(TAG).e(exception, "Global Coroutine Exception caught in MusicService")
    }

    private var scope = CoroutineScope(Dispatchers.Main + exceptionHandler + Job())

    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        AudioQuality.AUTO
    )
    private val preferredStreamClient by enumPreference(
        this,
        PlayerStreamClientKey,
        PlayerStreamClient.ANDROID_VR
    )
    private val avoidStreamCodecs: Set<String> by lazy {
        if (deviceSupportsMimeType("audio/opus")) emptySet() else setOf("opus")
    }
    private var lastLoginRecoveryPrompt: Pair<String, Long>? = null

    private fun promptLoginRecovery(mediaId: String, targetUrl: String) {
        val now = System.currentTimeMillis()
        val lastPrompt = lastLoginRecoveryPrompt
        if (lastPrompt?.first == mediaId && now - lastPrompt.second < 10000L) return
        lastLoginRecoveryPrompt = mediaId to now

        val deepLink = android.net.Uri.parse("echofy://login?url=${android.net.Uri.encode(targetUrl)}")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, deepLink, this, com.Chenkham.Echofy.MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching {
            startActivity(intent)
        }
    }

    private fun deviceSupportsMimeType(mimeType: String): Boolean {
        return runCatching {
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        }.getOrDefault(false)
    }


    lateinit var wifiJamManager: WifiJamManager
    lateinit var jamSessionManager: AppwriteTogetherSessionManager

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.Chenkham.Echofy.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val playerVolume = MutableStateFlow(1f)

    /** Loop start point in ms, or null when no start has been marked. */
    val abLoopStart = MutableStateFlow<Long?>(null)

    /** Loop end point in ms. The loop is only active once both points are set. */
    val abLoopEnd = MutableStateFlow<Long?>(null)

    private var abLoopJob: Job? = null

    /** Handles mono downmixing and left/right balance for accessibility. */
    private val monoBalanceProcessor = MonoBalanceAudioProcessor()

    /**
     * The volume the player should settle on once any transient fade finishes. Kept in sync by
     * the volume collector so fades always ramp back to the user's chosen level.
     */
    @Volatile
    private var targetVolume = 1f

    private var fadeJob: Job? = null

    /** Mirrors [VolumeFadeOnPauseKey] so pause/resume can ramp instead of cutting. */
    @Volatile
    private var fadeOnPauseEnabled = false

    /**
     * Pauses with a short volume ramp when the user has enabled fading, otherwise pauses
     * immediately. The volume is always restored so the next resume starts at full level.
     */
    fun pauseWithFade() {
        if (!fadeOnPauseEnabled || !player.isPlaying) {
            player.pause()
            return
        }
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                rampVolume(from = targetVolume, to = 0f)
                player.pause()
            } finally {
                player.volume = targetVolume
            }
        }
    }

    /**
     * Resumes playback, ramping the volume up from silence when fading is enabled so the
     * track eases in rather than starting at full level.
     */
    fun playWithFade() {
        if (!fadeOnPauseEnabled) {
            player.play()
            return
        }
        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                player.volume = 0f
                player.play()
                rampVolume(from = 0f, to = targetVolume)
            } finally {
                player.volume = targetVolume
            }
        }
    }

    /** Linearly moves the player volume between two levels over [FADE_DURATION_MS]. */
    private suspend fun rampVolume(from: Float, to: Float) {
        val steps = 12
        val stepDelay = FADE_DURATION_MS / steps
        for (step in 1..steps) {
            player.volume = from + (to - from) * (step.toFloat() / steps)
            delay(stepDelay)
        }
    }

    private var listeningReminderJob: Job? = null
    private var silentOutroJob: Job? = null

    /**
     * Starts the safe-listening nudge. The timer counts uninterrupted playback only — it is
     * cancelled whenever the user pauses, so a break resets the clock.
     */
    private fun startListeningReminder() {
        listeningReminderJob?.cancel()
        listeningReminderJob = scope.launch {
            val prefs = dataStore.data.first()
            if (prefs[ListeningReminderEnabledKey] != true) return@launch
            val minutes = (prefs[ListeningReminderMinutesKey] ?: 60).coerceAtLeast(5)

            while (isActive) {
                delay(minutes * 60_000L)
                if (!player.isPlaying) break
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        this@MusicService,
                        getString(R.string.listening_reminder_message, minutes),
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }
    }

    /**
     * Advances to the next track once the trailing outro of the current one is reached.
     *
     * ExoPlayer exposes no silence detector for future audio, so rather than analysing
     * samples this treats the configured number of seconds at the very end of a track as
     * the outro. That covers the common case of long fades and dead air after the last
     * note without touching the decode path.
     */
    private fun startSilentOutroSkip() {
        silentOutroJob?.cancel()
        silentOutroJob = scope.launch {
            if (dataStore.data.first()[SkipSilentOutroKey] != true) return@launch

            while (isActive) {
                delay(1000)
                if (!player.isPlaying) continue

                val duration = player.duration
                if (duration == C.TIME_UNSET || duration <= 0L) continue
                // Very short tracks would be cut in half by a fixed-size outro window.
                if (duration < 60_000L) continue

                val outroMs = ((dataStore.data.first()[SilentOutroSecondsKey] ?: 5)
                    .coerceIn(1, 30)) * 1000L

                if (player.currentPosition >= duration - outroMs && player.hasNextMediaItem()) {
                    player.seekToNext()
                }
            }
        }
    }

    /**
     * Resumes playback when headphones are reconnected, mirroring the automatic pause that
     * [ExoPlayer.Builder.setHandleAudioBecomingNoisy] performs on disconnect. Only acts when
     * the user has opted in and something was already queued.
     */
    private val headphoneConnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioManager.ACTION_HEADSET_PLUG) return
            // state 1 means plugged in; 0 is a disconnect, already handled by ExoPlayer.
            if (intent.getIntExtra("state", 0) != 1) return

            scope.launch {
                val enabled = dataStore.data.first()[ResumeOnHeadphonesKey] ?: false
                if (enabled && player.mediaItemCount > 0 && !player.isPlaying) {
                    player.play()
                }
            }
        }
    }

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    @Volatile
    private var currentVideoQuality = "Auto"
    @Volatile
    private var currentAudioQuality = com.Chenkham.Echofy.constants.AudioQuality.AUTO

    private var sameItemRetryId: String? = null
    private var sameItemRetryCount: Int = 0

    @Volatile private var isQueuePersistent = true
    @Volatile private var hideExplicit = false
    @Volatile private var videoCacheEnabled = true
    @Volatile private var discordUseDetails = false
    @Volatile private var autoLoadMore = true
    @Volatile private var disableLoadMoreWhenRepeatAll = false
    @Volatile private var autoSkipNextOnError = false
    @Volatile private var pauseListenHistory = false
    @Volatile private var pauseRemoteListenHistory = false
    @Volatile private var equalizerEnabled = false
    @Volatile private var bassBoostEnabled = false
    @Volatile private var bassBoostStrength = 500
    @Volatile private var equalizerPreset = 0
    @Volatile private var equalizerBandLevels = ""
    @Volatile private var crossfadeEnabled = false
    @Volatile private var shakeToSkipEnabled = false
    @Volatile private var hapticBassEnabled = false

    private var shakeDetector: com.Chenkham.Echofy.utils.ShakeDetector? = null

    private val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null

    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: Job? = null
    private var jamPlaybackHeartbeatJob: Job? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val playbackUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val songUrlCache = playbackUrlCache
    private val songUrlUserAgentCache = ConcurrentHashMap<String, String>()
    // Media IDs that got HTTP 403 from fast-start URLs — skip fast-start and use full validated path on retry
    private val skipFastStartIds = ConcurrentHashMap.newKeySet<String>()
    
    // Add prefetch cache for upcoming songs
    private val prefetchJobs = ConcurrentHashMap<String, Job>()
    private var lastJamQueueSyncFingerprint: String? = null
    private var lastGuestJamQueueFingerprint: String? = null
    private var lastAppliedJamRoomId: String? = null
    private var jamAddMode = false

    /**
     * Drops every cached stream URL for a song — plain, video and audio-companion keys —
     * along with their User-Agents, so the next play resolves fresh URLs.
     */
    private fun invalidateCachedUrls(mediaId: String) {
        listOf(mediaId, "${mediaId}_video", "$mediaId$AUDIO_COMPANION_SUFFIX").forEach { key ->
            songUrlCache.remove(key)
            songUrlUserAgentCache.remove(key)
        }
    }

    /**
     * Forces ExoPlayer to rebuild its MediaSources for the current queue.
     *
     * ExoPlayer builds a MediaSource once per media item, when the item is set, and reuses it
     * across [Player.prepare] calls. Video mode needs the source rebuilt, because only
     * createMediaSource() attaches the companion audio track via MergingMediaSource. Calling
     * stop() + prepare() would replay the previously built source, which in video mode carries
     * a video-only stream and therefore plays without sound. Re-setting the media items is what
     * makes the factory run again.
     */
    private fun rebuildCurrentQueue() {
        if (player.mediaItemCount == 0) return
        val items = player.mediaItems
        val index = player.currentMediaItemIndex.coerceIn(0, player.mediaItemCount - 1)
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val wasPlaying = player.playWhenReady

        player.setMediaItems(items, index, positionMs)
        player.prepare()
        player.playWhenReady = wasPlaying
    }

    /**
     * Removes the buffered bytes of a song from the player cache, for every key the resolver
     * may have written it under. Downloaded songs live in [downloadCache] and are left alone.
     */
    private fun evictPlayerCache(mediaId: String) {
        listOf(mediaId, "${mediaId}_video", "$mediaId$AUDIO_COMPANION_SUFFIX").forEach { key ->
            try {
                playerCache.removeResource(key)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to evict player cache for $key", e)
            }
        }
    }

    /**
     * Builds the resolved DataSpec for a cached stream url. The User-Agent recorded when the
     * url was minted must be replayed on every request: YouTube binds the PO token in the url
     * to that exact client, so serving a cached url with a different User-Agent returns 403.
     */
    private fun resolvedDataSpec(
        dataSpec: DataSpec,
        streamUrl: String,
        cacheKey: String,
    ): DataSpec {
        val builder = dataSpec.withUri(streamUrl.toUri()).buildUpon()
        songUrlUserAgentCache[cacheKey]?.let {
            builder.setHttpRequestHeaders(mapOf("User-Agent" to it))
        }
        if (videoMode) builder.setKey(cacheKey)
        return builder.build()
    }

    fun prefetchPlaybackData(mediaId: String, preloadToCache: Boolean = false) {
        if (mediaId.startsWith(RADIO_MEDIA_ID_PREFIX) || mediaId.startsWith(AMBIENT_MEDIA_ID_PREFIX)) return

        if (playbackUrlCache.containsKey(mediaId)) {
            if (preloadToCache) {
                val cached = playbackUrlCache[mediaId]
                if (cached != null && cached.second > System.currentTimeMillis()) {
                    scope.launch(Dispatchers.IO) {
                        preloadBytesToCache(mediaId, cached.first)
                    }
                }
            }
            return
        }
        if (prefetchJobs.containsKey(mediaId)) return

        prefetchJobs[mediaId] = scope.launch(Dispatchers.IO) {
            try {
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = currentAudioQuality,
                    connectivityManager = connectivityManager,
                    preferredStreamClient = preferredStreamClient,
                    avoidCodecs = avoidStreamCodecs,
                    isVideo = videoMode,
                    videoQuality = currentVideoQuality,
                ).getOrNull()

                if (playbackData != null) {
                    val expiresAt = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                    val targetKey = if (videoMode) "${mediaId}_video" else mediaId
                    playbackUrlCache[targetKey] = playbackData.streamUrl to expiresAt

                    if (preloadToCache) {
                        preloadBytesToCache(targetKey, playbackData.streamUrl)
                    }
                }
            } finally {
                prefetchJobs.remove(mediaId)
            }
        }
    }

    /**
     * Preload first 128KB of a song to disk cache for truly instant playback.
     * This is what enables YouTube Music-style "zero-latency" starts.
     */
    private suspend fun preloadBytesToCache(mediaId: String, url: String) {
        // Skip if cache not initialized
        if (!::playerCache.isInitialized) {
            Timber.tag(TAG).w("Preload: playerCache not initialized yet")
            return
        }
        
        // Skip if already in cache
        try {
            if (playerCache.isCached(mediaId, 0, 128 * 1024)) {
                Timber.tag(TAG).d("Preload: $mediaId already in cache")
                return
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Preload: Failed to check cache for $mediaId")
            return
        }
        
        try {
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-131071") // First 128KB
            
            songUrlUserAgentCache[mediaId]?.let {
                requestBuilder.header("User-Agent", it)
            }
            
            val request = requestBuilder.build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        // Cache operations should be synchronized to prevent concurrent access issues
                        synchronized(playerCache) {
                            try {
                                // Write to cache using CacheDataSink with correct constructor
                                val factory = androidx.media3.datasource.cache.CacheDataSink.Factory()
                                    .setCache(playerCache)
                                val dataSink = factory.createDataSink()
                                try {
                                    val spec = androidx.media3.datasource.DataSpec.Builder()
                                        .setUri(android.net.Uri.parse(url))
                                        .setPosition(0)
                                        .setLength(bytes.size.toLong())
                                        .setKey(mediaId)
                                        .setFlags(androidx.media3.datasource.DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN)
                                        .build()
                                    dataSink.open(spec)
                                    dataSink.write(bytes, 0, bytes.size)
                                    Timber.tag(TAG).d("Preloaded ${bytes.size} bytes for $mediaId")
                                } finally {
                                    dataSink.close()
                                }
                            } catch (e: Exception) {
                                Timber.tag(TAG).w(e, "Preload: cache write failed for $mediaId")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to preload bytes for $mediaId")
        }
    }
    
    /**
     * Prefetch next N songs in the queue for instant skip experience.
     * YouTube Music prefetches 2-3 songs ahead.
     */
    private fun prefetchQueueAhead(count: Int = 3) {
        val currentIndex = player.currentMediaItemIndex
        val mediaItemCount = player.mediaItemCount
        
        for (i in 1..count) {
            val nextIndex = if (player.shuffleModeEnabled) {
                // Get next item in shuffle order
                player.currentTimeline.getNextWindowIndex(
                    currentIndex + i - 1,
                    Player.REPEAT_MODE_OFF,
                    true
                )
            } else {
                currentIndex + i
            }
            
            if (nextIndex in 0 until mediaItemCount) {
                val mediaId = player.getMediaItemAt(nextIndex).mediaId
                // Preload to cache for first 2 songs (most likely to be played next)
                prefetchPlaybackData(mediaId, preloadToCache = i <= 2)
            }
        }
    }

    @Volatile
    private var videoMode = false

    // OPTIMIZED OkHttpClient for maximum network throughput (YouTube Music-like 2+ MB/s)
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            // Connection pooling - keep 5 connections alive for faster subsequent requests
            .connectionPool(okhttp3.ConnectionPool(5, 30, TimeUnit.SECONDS))
            // Enable HTTP/2 for multiplexing multiple streams over one connection
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            // Faster timeouts for instant start
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS) // Longer read for large audio chunks
            .writeTimeout(15, TimeUnit.SECONDS)
            // Keep connections alive for faster reuse
            .retryOnConnectionFailure(true)
            // Disable slow DNS lookup by using system DNS
            .dns(okhttp3.Dns.SYSTEM)
            .build()
    }

    private var consecutivePlaybackErr = 0

    private var isForegroundStarted = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForegroundStarted) {
            isForegroundStarted = true
            EchofyMediaNotificationProvider.createNotificationChannel(this, CHANNEL_ID, R.string.music_player)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        EchofyMediaNotificationProvider.createNotificationChannel(this, CHANNEL_ID, R.string.music_player)
        setMediaNotificationProvider(
            EchofyMediaNotificationProvider(this),
        )
        
        // INSTANT START LoadControl - Buffer 250ms then PLAY, download rest at full speed
        // Combined with fast prefetch = sub-1 second playback start
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                // Min buffer while playing: 30 seconds
                30_000,
                // Max buffer: 2 MINUTES (reduced to save RAM)
                2 * 60_000,
                // Buffer to START: 2000ms (to prevent stutter on slow networks)
                2_000,
                // Buffer after rebuffer: 4000ms (quick recovery with enough buffer to keep playing)
                4_000
            )
            // Use default target buffer bytes (around 32MB) to prevent out of memory issues
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        
        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setLoadControl(loadControl)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    true,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                }

        // Resume playback when headphones are reconnected, if the user opted in.
        ContextCompat.registerReceiver(
            this,
            headphoneConnectReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Echofy:MusicServiceWakeLock").apply {
             setReferenceCounted(false)
        }

        // Shake to Skip — premium gesture feature
        shakeDetector = com.Chenkham.Echofy.utils.ShakeDetector {
            if (shakeToSkipEnabled && player.nextMediaItemIndex != androidx.media3.common.C.INDEX_UNSET) {
                player.seekToNextMediaItem()
                // Light haptic acknowledgement
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(80)
                }
            }
        }
        shakeDetector?.let { com.Chenkham.Echofy.utils.ShakeDetector.register(this, it) }

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()

        // Initialize repeat mode asynchronously
        scope.launch {
            val repeatMode = dataStore.data.map { it[RepeatModeKey] ?: REPEAT_MODE_OFF }.first()
            player.repeatMode = repeatMode
        }

        // Cache quality settings for playback resolution
        scope.launch {
            dataStore.data.map { it[VideoQualityKey] ?: "Auto" }
                .distinctUntilChanged()
                .collect { currentVideoQuality = it }
        }
        scope.launch {
            dataStore.data
                .map { it[AudioQualityKey]?.let { q -> try { com.Chenkham.Echofy.constants.AudioQuality.valueOf(q) } catch(e: Exception) { com.Chenkham.Echofy.constants.AudioQuality.AUTO } } ?: com.Chenkham.Echofy.constants.AudioQuality.AUTO }
                .distinctUntilChanged()
                .collect { currentAudioQuality = it }
        }

        // Cache other settings caches
        scope.launch { dataStore.data.map { it[PersistentQueueKey] ?: true }.distinctUntilChanged().collect { isQueuePersistent = it } }
        scope.launch { dataStore.data.map { it[HideExplicitKey] ?: false }.distinctUntilChanged().collect { hideExplicit = it } }
        scope.launch { dataStore.data.map { it[VideoCacheEnabledKey] ?: true }.distinctUntilChanged().collect { this@MusicService.videoCacheEnabled = it } }
        scope.launch { dataStore.data.map { it[DiscordUseDetailsKey] ?: false }.distinctUntilChanged().collect { discordUseDetails = it } }
        scope.launch { dataStore.data.map { it[AutoLoadMoreKey] ?: true }.distinctUntilChanged().collect { autoLoadMore = it } }
        scope.launch { dataStore.data.map { it[DisableLoadMoreWhenRepeatAllKey] ?: false }.distinctUntilChanged().collect { disableLoadMoreWhenRepeatAll = it } }
        scope.launch { dataStore.data.map { it[AutoSkipNextOnErrorKey] ?: false }.distinctUntilChanged().collect { autoSkipNextOnError = it } }
        scope.launch { dataStore.data.map { it[PauseListenHistoryKey] ?: false }.distinctUntilChanged().collect { pauseListenHistory = it } }
        scope.launch { dataStore.data.map { it[PauseRemoteListenHistoryKey] ?: false }.distinctUntilChanged().collect { pauseRemoteListenHistory = it } }
        scope.launch { dataStore.data.map { it[EqualizerEnabledKey] ?: false }.distinctUntilChanged().collect { equalizerEnabled = it } }
        scope.launch { dataStore.data.map { it[BassBoostEnabledKey] ?: false }.distinctUntilChanged().collect { bassBoostEnabled = it } }
        scope.launch { dataStore.data.map { it[BassBoostStrengthKey] ?: 500 }.distinctUntilChanged().collect { bassBoostStrength = it } }
        scope.launch { dataStore.data.map { it[EqualizerPresetKey] ?: 0 }.distinctUntilChanged().collect { equalizerPreset = it } }
        scope.launch { dataStore.data.map { it[EqualizerBandLevelsKey] ?: "" }.distinctUntilChanged().collect { equalizerBandLevels = it } }
        scope.launch {
            dataStore.data.map { it[com.Chenkham.Echofy.constants.CrossfadeEnabledKey] ?: false }.distinctUntilChanged().collect { enabled ->
                crossfadeEnabled = enabled
                // Apply crossfade via ExoPlayer transition period
                if (::player.isInitialized) {
                    Timber.tag(TAG).d("Crossfade preference changed: $enabled")
                }
            }
        }
        scope.launch {
            dataStore.data.map { it[com.Chenkham.Echofy.constants.ShakeToSkipKey] ?: false }.distinctUntilChanged().collect { shakeToSkipEnabled = it }
        }
        scope.launch {
            dataStore.data.map { it[com.Chenkham.Echofy.constants.HapticBassBeatsKey] ?: false }.distinctUntilChanged().collect { hapticBassEnabled = it }
        }
        
        // Async volume init
        scope.launch {
            dataStore.data.map { it[PlayerVolumeKey] ?: 1f }.collect { playerVolume.value = it }
        }

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        // Observar conectividad de red
        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    // Reintentar reproducciÃ³n cuando vuelve la conexiÃ³n
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        // Applies the user volume, clamped by the safe-listening ceiling when enabled.
        combine(
            playerVolume,
            dataStore.data
                .map { (it[VolumeLimitEnabledKey] ?: false) to (it[VolumeLimitPercentKey] ?: 85) }
                .distinctUntilChanged(),
        ) { volume, (limitEnabled, limitPercent) ->
            if (limitEnabled) volume.coerceAtMost(limitPercent / 100f) else volume
        }.collectLatest(scope) {
            targetVolume = it
            // Never stomp on an in-flight fade; it restores targetVolume when it finishes.
            if (fadeJob?.isActive != true) {
                player.volume = it
            }
        }

        dataStore.data
            .map { it[VolumeFadeOnPauseKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { fadeOnPauseEnabled = it }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        wifiJamManager = WifiJamManager(scope)
        jamSessionManager = AppwriteTogetherSessionManager(applicationContext, scope)
        startJamPlaybackHeartbeat()
        scope.launch {
            jamSessionManager.sessionState.collect { state ->
                val session = state.session
                val roomId = session?.roomCode?.roomId
                if (roomId == null || state.phase == JamSessionPhase.IDLE || state.phase == JamSessionPhase.ERROR) {
                    lastAppliedJamRoomId = null
                    lastGuestJamQueueFingerprint = null
                    return@collect
                }
                if (roomId == lastAppliedJamRoomId) return@collect
                lastAppliedJamRoomId = roomId
                lastJamQueueSyncFingerprint = null
                lastGuestJamQueueFingerprint = null

                if (session.role == JamParticipantRole.HOST) {
                    if (state.phase == JamSessionPhase.HOSTING) {
                        constrainHostPlayerToCurrentSong()
                        syncJamStateNow()
                    }
                } else {
                    if (state.phase == JamSessionPhase.JOINED) {
                        player.stop()
                        player.clearMediaItems()
                        jamSessionManager.remotePlayback.value?.let(::applyRemoteJamPlayback)
                        applySharedJamQueueToGuestPlayer(jamSessionManager.queueSnapshot.value)
                    }
                }
            }
        }

        // Guest handling: listen for incoming jam events and sync playback
        scope.launch {
            wifiJamManager.incomingEvents.collect { event ->
                if (event == null) return@collect
                
                // Only act on events if we are currently a guest
                if (wifiJamManager.isGuest.value) {
                    when (event.type) {
                        "SYNC" -> {
                            // If it's a new media ID, set it and prepare
                            if (event.mediaId.isNotBlank() && player.currentMediaItem?.mediaId != event.mediaId) {
                                // Add single item for now. 
                                // Ideally, we'd also sync the entire queue if needed.
                                val mediaItem = MediaItem.Builder()
                                    .setMediaId(event.mediaId)
                                    .build()
                                player.setMediaItem(mediaItem, event.positionMs)
                                player.prepare()
                            } else {
                                // Just sync position if we drifted too far (e.g. > 1 sec)
                                if (kotlin.math.abs(player.currentPosition - event.positionMs) > 1000) {
                                    player.seekTo(event.positionMs)
                                }
                            }
                            player.setPlaybackSpeed(event.playbackSpeed)
                            // Play state sync is handled by PLAY / PAUSE events
                        }
                        "PLAY" -> {
                            player.playWhenReady = true
                        }
                        "PAUSE" -> {
                            player.playWhenReady = false
                        }
                    }
                }
            }
        }

        scope.launch {
            jamSessionManager.remotePlayback.collect { snapshot ->
                if (snapshot == null) return@collect
                val session = jamSessionManager.sessionState.value.session ?: return@collect
                if (
                    session.role == JamParticipantRole.GUEST ||
                    (session.role == JamParticipantRole.HOST &&
                        jamAllowsGuestControls() &&
                        snapshot.issuedByParticipantId != session.participantId)
                ) {
                    applyRemoteJamPlayback(snapshot)
                }
            }
        }

        scope.launch {
            jamSessionManager.queueSnapshot.collect { queueItems ->
                val session = jamSessionManager.sessionState.value.session ?: return@collect
                if (session.role == JamParticipantRole.HOST) {
                    applySharedJamQueueToHostPlayer(queueItems)
                } else {
                    applySharedJamQueueToGuestPlayer(queueItems)
                }
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, isPaused = false)
            } else {
                discordRpc?.closeRPC()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        // Sleep timer fade-out preferences
        dataStore.data
            .map { (it[SleepTimerFadeOutKey] ?: true) to (it[SleepTimerFadeDurationKey] ?: 30) }
            .distinctUntilChanged()
            .collectLatest(scope) { (fadeEnabled, fadeSeconds) ->
                sleepTimer.fadeOutEnabled = fadeEnabled
                sleepTimer.fadeDurationSeconds = fadeSeconds
            }

        // Mono downmix and channel balance for accessibility, plus karaoke vocal
        // suppression, which shares the same processor.
        dataStore.data
            .map {
                Triple(
                    it[MonoAudioKey] ?: false,
                    it[AudioBalanceKey] ?: 0f,
                    it[VocalSuppressionKey] ?: 0f,
                )
            }
            .distinctUntilChanged()
            .collectLatest(scope) { (mono, balance, vocalSuppression) ->
                monoBalanceProcessor.monoEnabled = mono
                monoBalanceProcessor.balance = balance
                monoBalanceProcessor.vocalSuppression = vocalSuppression
            }

        // Safe-listening volume cap is applied together with the user volume above.

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            setupLoudnessEnhancer()
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            discordRpc?.updateSong(it, player.currentPosition, player.playbackParameters.speed, isPaused = false)
                        }
                    }
                }
            }

        // Observar cambios en DiscordUseDetailsKey
        dataStore.data
            .map { it[DiscordUseDetailsKey] ?: false }
            .debounce(1000)
            .distinctUntilChanged()
            .collect(scope) { useDetails ->
                if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                    currentSong.value?.let { song ->
                        discordUpdateJob?.cancel()
                        discordUpdateJob = scope.launch {
                            delay(1000)
                            discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, isPaused = false)
                        }
                    }
                }
            }

        // Initialize videoMode cache and keep it updated
        // Respect VideoPlaybackEnabledKey - when disabled, always use audio mode
        scope.launch {
            dataStore.data
                .map { prefs -> 
                    val videoPlaybackEnabled = prefs[VideoPlaybackEnabledKey] ?: true
                    val isVideoModeSelected = prefs[PlaybackModeKey] == PlaybackMode.VIDEO.name
                    // Only use video mode if both conditions are met
                    videoPlaybackEnabled && isVideoModeSelected
                }
                .distinctUntilChanged()
                .collect { isVideoMode ->
                    videoMode = isVideoMode
                }
        }

        // Observar cambios en PlaybackMode para recargar la canción si es necesario
        dataStore.data
            .map { it[PlaybackModeKey] }
            .distinctUntilChanged()
            .drop(1) // Ignorar el valor inicial
            .collect(scope) { modeString: String? ->
                val isSwitchedToVideo = modeString == PlaybackMode.VIDEO.name
                val videoPlaybackEnabled = dataStore.get(VideoPlaybackEnabledKey, true)
                
                // Force update videoMode immediately to avoid race condition with separate collector
                videoMode = isSwitchedToVideo && videoPlaybackEnabled
                
                // Limpiar caché y recargar canción actual para obtener el formato correcto (video/audio)
                currentSong.value?.let { song ->
                    invalidateCachedUrls(song.id)
                    withContext(Dispatchers.Main) {
                        if (!isSwitchedToVideo) {
                            player.clearVideoSurface()
                        }
                        // Instantly rebuild queue at exact positionMs with zero artificial delay
                        rebuildCurrentQueue()
                    }
                }
            }

        // Observe VideoQuality changes to reload
        dataStore.data
            .map { it[VideoQualityKey] }
            .distinctUntilChanged()
            .drop(1)
            .collect(scope) { newQuality ->
                if (videoMode) {
                    currentSong.value?.let { song ->
                        // Apply the new quality before reloading. The collector that mirrors this
                        // key into currentVideoQuality runs independently, so relying on it here
                        // would race and re-resolve the stream at the previous quality.
                        currentVideoQuality = newQuality ?: "Auto"

                        // Drop every cached url for this song: video mode resolves under
                        // "<id>_video" (and "<id>_audio" for the companion track), so removing
                        // only "<id>" would leave the old-quality url in place.
                        invalidateCachedUrls(song.id)

                        // Cached bytes must go too, otherwise the resolver's isCached() check
                        // short-circuits and the player replays the old quality from disk.
                        evictPlayerCache(song.id)

                        withContext(Dispatchers.Main) {
                            // Instantly rebuild queue at exact positionMs with new video quality
                            rebuildCurrentQueue()
                        }
                    }
                }
            }

        // Observe Equalizer preference changes to apply in real-time
        dataStore.data
            .map { 
                listOf(
                    it[com.Chenkham.Echofy.constants.EqualizerEnabledKey],
                    it[com.Chenkham.Echofy.constants.EqualizerPresetKey],
                    it[com.Chenkham.Echofy.constants.EqualizerBandLevelsKey], // Band levels for real-time updates
                    it[com.Chenkham.Echofy.constants.BassBoostEnabledKey],
                    it[com.Chenkham.Echofy.constants.BassBoostStrengthKey]
                )
            }
            .distinctUntilChanged()
            .drop(1) // Skip initial value
            .collect(scope) { 
                Timber.tag(TAG).d("EQ preferences changed, re-applying...")
                setupEqualizer()
            }

        if (isQueuePersistent) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                // Convertir de vuelta al tipo de cola apropiado
                val restoredQueue = queue.toQueue()
                playQueue(
                    queue = restoredQueue,
                    playWhenReady = false,
                )
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map { it.toMediaItem() }
            }

            // Restaurar estado del reproductor
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistPlayerState
                    }
                }
            }.onSuccess { playerState ->
                // Restaurar configuraciÃ³n del reproductor despuÃ©s de cargar la cola
                scope.launch {
                    delay(1000) // Esperar a que la cola se cargue
                    player.repeatMode = playerState.repeatMode
                    player.shuffleModeEnabled = playerState.shuffleModeEnabled
                    player.volume = playerState.volume

                    // Restaurar posiciÃ³n si sigue siendo vÃ¡lida
                    if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                        player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                    }
                }
            }
        }

        // Save queue periodically to prevent loss from crash or force kill
        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (isQueuePersistent) {
                    saveQueueToDisk()
                }
            }
        }

        // Save queue more frequently when playing
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
    }



    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        /**
         * Auto skip to the next media item on error.
         *
         * To prevent a "runaway diesel engine" scenario, force the user to take action after
         * too many errors come up too quickly. Pause to show player "stopped" state.
         * Threshold is MAX_CONSECUTIVE_ERR (5) real errors before pausing.
         */
        consecutivePlaybackErr += 1
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            Log.w(TAG, "Auto-skipping to next track after error (attempt $consecutivePlaybackErr/$MAX_CONSECUTIVE_ERR)")
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }

        Log.w(TAG, "Too many consecutive errors ($consecutivePlaybackErr), pausing playback")
        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        Log.w(TAG, "Playback error occurred — pausing. User must press play to retry.")
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                // 1) Repeat/Loop button — will appear on the LEFT side
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                // 2) Heart/Like button — will appear on the RIGHT side
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.heart_fill else R.drawable.heart)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main + exceptionHandler + Job())
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            // Prefetch BEFORE setting media item to get URL as early as possible
            val mediaId = queue.preloadItem!!.id
            prefetchPlaybackData(mediaId)
            
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch {
            try {
                val initialStatus =
                    withContext(Dispatchers.IO) {
                        queue.getInitialStatus().filterExplicit(hideExplicit)
                    }
                if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
                if (initialStatus.title != null) {
                    queueTitle = initialStatus.title
                }
                if (initialStatus.items.isEmpty()) {
                    Timber.tag(TAG).w("playQueue: initialStatus.items is empty, radio may have failed")
                    return@launch
                }
                
                // Prefetch the item about to be played
                val startIndex = if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0
                initialStatus.items.getOrNull(startIndex)?.mediaId?.let { prefetchPlaybackData(it) }
                
                if (queue.preloadItem != null) {
                    player.addMediaItems(
                        0,
                        initialStatus.items.subList(0, initialStatus.mediaItemIndex)
                    )
                    player.addMediaItems(
                        initialStatus.items.subList(
                            initialStatus.mediaItemIndex + 1,
                            initialStatus.items.size
                        )
                    )
                } else {
                    player.setMediaItems(
                        initialStatus.items,
                        if (initialStatus.mediaItemIndex >
                            0
                        ) {
                            initialStatus.mediaItemIndex
                        } else {
                            0
                        },
                        initialStatus.position,
                    )
                    player.prepare()
                    player.playWhenReady = playWhenReady
                    
                    // YouTube Music-style: Prefetch next songs immediately on queue start
                    prefetchQueueAhead(3)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "playQueue failed: ${e.message}")
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return

        // Save current song
        val currentSong = player.currentMediaItem

        // Remove other songs from queue
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            // Add radio songs after current song
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                YouTube
                    .next(WatchEndpoint(playlistId = playlistId))
                    .onSuccess {
                        YouTube
                            .next(WatchEndpoint(playlistId = it.endpoint.playlistId))
                            .onSuccess {
                                automixItems.value =
                                    it.items.map { song ->
                                        song.toMediaItem()
                                    }
                            }
                    }
            }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        // Si la cola estÃ¡ vacÃ­a o el reproductor estÃ¡ inactivo, reproducir inmediatamente
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            player.play()
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        // Insertar items inmediatamente despuÃ©s del item actual en el espacio de ventana/Ã­ndice
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            // Reconstruir orden aleatorio para que los items reciÃ©n insertados se reproduzcan a continuaciÃ³n
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                // Los Ã­ndices reciÃ©n insertados son un rango contiguo [insertIndex, insertIndex + items.size)
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                // Recopilar el orden de recorrido aleatorio existente excluyendo el Ã­ndice actual
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() // preservar el orden hacia adelante original

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                // Construir nuevo orden aleatorio: actual -> reciÃ©n insertados (en orden de inserciÃ³n) -> resto
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                // Llenar cualquier Ã­ndice faltante (seguridad) para asegurar una permutaciÃ³n completa
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    fun seedJamQueueFromPlayerIfNeeded() {
        publishFirebaseJamQueueSnapshot()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLike())
            }
        }
    }

    /**
     * Marks the loop start at the current playback position.
     */
    fun setAbLoopStart() {
        abLoopStart.value = player.currentPosition
        // A start after the existing end would make the loop invalid.
        abLoopEnd.value?.let { end ->
            if (end <= player.currentPosition) abLoopEnd.value = null
        }
        restartAbLoopWatcher()
    }

    /**
     * Marks the loop end at the current playback position and starts looping.
     */
    fun setAbLoopEnd() {
        val start = abLoopStart.value ?: return
        val position = player.currentPosition
        if (position <= start) return
        abLoopEnd.value = position
        restartAbLoopWatcher()
    }

    fun clearAbLoop() {
        abLoopStart.value = null
        abLoopEnd.value = null
        abLoopJob?.cancel()
        abLoopJob = null
    }

    /**
     * Polls playback position and seeks back to the start point whenever the end point is
     * passed. Polling is only active while both points are set.
     */
    private fun restartAbLoopWatcher() {
        abLoopJob?.cancel()

        val start = abLoopStart.value
        val end = abLoopEnd.value
        if (start == null || end == null) return

        abLoopJob = scope.launch {
            while (isActive) {
                val currentStart = abLoopStart.value
                val currentEnd = abLoopEnd.value
                if (currentStart == null || currentEnd == null) break

                if (player.isPlaying && player.currentPosition >= currentEnd) {
                    player.seekTo(currentStart)
                }
                delay(100)
            }
        }
    }

    /**
     * Saves the position of long-form content (DJ mixes, live sets, podcasts) so it can be
     * picked up where the user left off instead of restarting from zero. Short songs are
     * ignored because restarting them is the expected behaviour.
     */
    private fun saveResumePositionForCurrentSong() {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        val duration = player.duration
        val position = player.currentPosition
        if (duration <= 0L) return

        scope.launch {
            val prefs = dataStore.data.first()
            if (prefs[SmartResumeEnabledKey] != true) return@launch

            val minMinutes = prefs[SmartResumeMinMinutesKey] ?: 15
            if (duration < minMinutes * 60_000L) return@launch

            dataStore.edit { settings ->
                // Near the start or the end there is nothing useful to resume.
                if (position < 60_000L || position > duration - 60_000L) {
                    settings.remove(resumePositionKey(mediaId))
                } else {
                    settings[resumePositionKey(mediaId)] = position
                }
            }
        }
    }

    /**
     * Restores a previously saved position for long-form content.
     */
    private fun restoreResumePosition(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId ?: return

        scope.launch {
            val prefs = dataStore.data.first()
            if (prefs[SmartResumeEnabledKey] != true) return@launch

            val saved = prefs[resumePositionKey(mediaId)] ?: return@launch
            if (saved > 0L) {
                player.seekTo(saved)
            }
        }
    }

    /**
     * Restores the tempo and pitch the user last chose for this specific song, when
     * "remember playback settings" is enabled. Songs without a saved value fall back to
     * normal speed and pitch so settings never leak between tracks.
     */
    private fun restorePlaybackSettingsForCurrentSong(mediaItem: MediaItem?) {
        val mediaId = mediaItem?.mediaId ?: return

        scope.launch {
            val prefs = dataStore.data.first()
            val perTrack = prefs[RememberPlaybackSettingsKey] == true
            val perContentType = prefs[SpeedPerContentTypeKey] == true
            if (!perTrack && !perContentType) return@launch

            val savedTempo = if (perTrack) prefs[playbackTempoKey(mediaId)] else null
            val contentTypeSpeed = if (perContentType) longFormSpeedFor(prefs) else null

            // A tempo saved for this exact song always wins over the content-type default.
            val tempo = savedTempo ?: contentTypeSpeed ?: 1f
            val pitch = (if (perTrack) prefs[playbackPitchKey(mediaId)] else null) ?: 1f

            if (player.playbackParameters.speed != tempo ||
                player.playbackParameters.pitch != pitch
            ) {
                player.playbackParameters = PlaybackParameters(tempo, pitch)
            }
        }
    }

    /**
     * Speed to use for the current track when "speed per content type" is on. Long-form
     * items such as podcasts and DJ sets get the user's long-form speed; regular songs
     * stay at normal speed. Returns null while the duration is still unknown so the
     * current speed is left untouched.
     */
    private fun longFormSpeedFor(prefs: Preferences): Float? {
        val durationMs = player.duration
        if (durationMs == C.TIME_UNSET || durationMs <= 0L) return null

        val minMinutes = prefs[LongFormMinMinutesKey] ?: 20
        val isLongForm = durationMs >= minMinutes * 60_000L

        return if (isLongForm) {
            (prefs[LongFormPlaybackSpeedKey] ?: 1.5f).coerceIn(0.25f, 3f)
        } else {
            1f
        }
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Log.w(TAG, "setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }

        // Crear o recrear enhancer si es necesario
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Log.d(TAG, "LoudnessEnhancer created for sessionId=$audioSessionId")
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }

        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }

                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }

                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }

                    val loudnessDb = format?.loudnessDb

                    if (loudnessDb == null) {
                        withContext(Dispatchers.Main) {
                            loudnessEnhancer?.enabled = false
                            Log.d(TAG, "setupLoudnessEnhancer: loudnessDb is null, enhancer disabled")
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        val targetGain = (-loudnessDb * 100).toInt()
                        val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                        try {
                            loudnessEnhancer?.setTargetGain(clampedGain)
                            loudnessEnhancer?.enabled = true
                            Log.d(TAG, "LoudnessEnhancer gain applied: $clampedGain mB")
                        } catch (e: Exception) {
                            reportException(e)
                            releaseLoudnessEnhancer()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                        Log.d(TAG, "setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    /**
     * Sets up the built-in equalizer and bass boost based on saved preferences.
     */
    private fun setupEqualizer() {
        val audioSessionId = player.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Log.w(TAG, "setupEqualizer: invalid audioSessionId ($audioSessionId)")
            return
        }

        scope.launch {
            try {
                val eqEnabled = equalizerEnabled
                val bassEnabled = bassBoostEnabled
                val bassStrength = bassBoostStrength.toShort()
                val presetIndex = equalizerPreset.toShort()
                val bandLevelsJson = equalizerBandLevels

                withContext(Dispatchers.Main) {
                    // Setup Equalizer
                    if (equalizer == null) {
                        try {
                            equalizer = android.media.audiofx.Equalizer(0, audioSessionId)
                            Log.d(TAG, "Equalizer created for sessionId=$audioSessionId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to create Equalizer: ${e.message}")
                            equalizer = null
                        }
                    }

                    equalizer?.let { eq ->
                        eq.enabled = eqEnabled
                        if (eqEnabled && presetIndex >= 0 && presetIndex < eq.numberOfPresets) {
                            try {
                                eq.usePreset(presetIndex)
                                Log.d(TAG, "Equalizer preset applied: $presetIndex")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to apply EQ preset: ${e.message}")
                            }
                        } else if (eqEnabled && bandLevelsJson.isNotEmpty()) {
                            // Apply custom band levels from JSON
                            try {
                                val levels = bandLevelsJson.removeSurrounding("[", "]")
                                    .split(",")
                                    .mapNotNull { it.trim().toShortOrNull() }
                                levels.forEachIndexed { band, level ->
                                    if (band < eq.numberOfBands) {
                                        eq.setBandLevel(band.toShort(), level)
                                    }
                                }
                                Log.d(TAG, "Equalizer custom bands applied")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to apply custom EQ bands: ${e.message}")
                            }
                        }
                    }

                    // Setup Bass Boost
                    if (bassBoost == null) {
                        try {
                            bassBoost = android.media.audiofx.BassBoost(0, audioSessionId)
                            Log.d(TAG, "BassBoost created for sessionId=$audioSessionId")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to create BassBoost: ${e.message}")
                            bassBoost = null
                        }
                    }

                    bassBoost?.let { bb ->
                        bb.enabled = bassEnabled
                        if (bassEnabled) {
                            try {
                                bb.setStrength(bassStrength)
                                Log.d(TAG, "BassBoost strength applied: $bassStrength")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to set BassBoost strength: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "setupEqualizer failed: ${e.message}")
            }
        }
    }

    private fun releaseEqualizer() {
        try {
            equalizer?.release()
            bassBoost?.release()
            Log.d(TAG, "Equalizer and BassBoost released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing Equalizer/BassBoost: ${e.message}")
        } finally {
            equalizer = null
            bassBoost = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        setupEqualizer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        releaseEqualizer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    
    private fun notifyWidgetsPlaybackChanged() {
        runCatching {
            val intent = Intent("com.Chenkham.Echofy.ACTION_STATE_CHANGED").apply {
                setPackage(packageName)
            }
            sendBroadcast(intent)

            val lyricsIntent = Intent(this, com.Chenkham.Echofy.ui.widgets.LyricsWidget::class.java).apply {
                action = "com.Chenkham.Echofy.ACTION_STATE_CHANGED"
            }
            sendBroadcast(lyricsIntent)

            val musicWidgetIntent = Intent(this, com.Chenkham.Echofy.MusicWidget::class.java).apply {
                action = "com.Chenkham.Echofy.ACTION_STATE_CHANGED"
            }
            sendBroadcast(musicWidgetIntent)

            val compactWidgetIntent = Intent(this, com.Chenkham.Echofy.CompactMusicWidget::class.java).apply {
                action = "com.Chenkham.Echofy.ACTION_STATE_CHANGED"
            }
            sendBroadcast(compactWidgetIntent)
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        lastPlaybackSpeed = -1.0f // forzar actualización de canción
        notifyWidgetsPlaybackChanged()

        restorePlaybackSettingsForCurrentSong(mediaItem)
        restoreResumePosition(mediaItem)

        // Loop points belong to a single song, so drop them when the track changes.
        clearAbLoop()

        // WiFi Jam Broadcast
        if (this::wifiJamManager.isInitialized && wifiJamManager.isHost.value && mediaItem != null) {
            wifiJamManager.broadcastEvent(
                WifiJamEvent(
                    type = "SYNC",
                    mediaId = mediaItem.mediaId,
                    positionMs = 0L,
                    playbackSpeed = player.playbackParameters.speed
                )
            )
        }
        if (mediaItem != null) {
            publishFirebaseJamPlayback(
                playbackState = currentJamPlaybackState(),
                positionMs = 0L,
                mediaId = mediaItem.mediaId,
            )
        }
        publishFirebaseJamQueueSnapshot()

        setupLoudnessEnhancer()

        discordUpdateJob?.cancel()

        // Resetear errores consecutivos cuando hay transición exitosa
        consecutivePlaybackErr = 0
        sameItemRetryCount = 0
        sameItemRetryId = null
        
        // Clear cache for skipped songs when video cache is disabled
        // This prevents storing buffered data from songs that were skipped
        if (!videoCacheEnabled && 
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            // User skipped the song - clear the previous song's cache
            val previousIndex = player.currentMediaItemIndex - 1
            if (previousIndex >= 0 && previousIndex < player.mediaItemCount) {
                val previousMediaId = player.getMediaItemAt(previousIndex).mediaId
                scope.launch(Dispatchers.IO) {
                    try {
                        playerCache.removeResource(previousMediaId)
                        Timber.d("Cleared cache for skipped song: $previousMediaId")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to clear cache for skipped song")
                    }
                }
            }
        }

        // Auto cargar más canciones
        if (autoLoadMore &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage()
        ) {
            val lastItemIndex = player.mediaItemCount - 1
            if (player.currentMediaItemIndex >= lastItemIndex - 1 ||
                (player.currentMediaItemIndex >= lastItemIndex - 2 && !player.isPlaying)
            ) {
                if (
                    !(disableLoadMoreWhenRepeatAll && player.repeatMode == REPEAT_MODE_ALL)
                ) {
                    // Load more songs (from queue generator or recommendation)
                    scope.launch(Dispatchers.IO) {
                        currentQueue.nextPage().filterExplicit(hideExplicit)
                            .let { newItems ->
                                withContext(Dispatchers.Main) {
                                    if (player.playbackState != STATE_IDLE) {
                                        player.addMediaItems(newItems.drop(1))
                                    }
                                }
                            }
                    }
                }
            }
        }

        // YouTube Music-style: Prefetch next 3 songs for instant skip experience
        prefetchQueueAhead(3)

        // Save state when media item changes
        if (isQueuePersistent) {
            scope.launch {
                delay(500) // Pequeño delay para asegurar que el estado esté estable
                saveQueueToDisk()
            }
        }
        

    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        notifyWidgetsPlaybackChanged()
        // Save state when playback state changes
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }

        // Cuando termina la reproducciÃ³n, ocultar notificaciÃ³n si la cola estÃ¡ vacÃ­a
        if (playbackState == Player.STATE_ENDED) {
            val session = if (this::jamSessionManager.isInitialized) {
                jamSessionManager.sessionState.value.session
            } else {
                null
            }
            if (session?.role == JamParticipantRole.HOST && jamSessionManager.queueSnapshot.value.isNotEmpty()) {
                jamSessionManager.popNextQueueItem()?.let { selection ->
                    val syncedQueue = buildList {
                        add(selection.nextItem.toMediaMetadata().toMediaItem())
                        addAll(selection.remainingQueue.map { it.toMediaMetadata().toMediaItem() })
                    }
                    player.setMediaItems(syncedQueue)
                    player.prepare()
                    player.play()
                    lastJamQueueSyncFingerprint = null
                }
            }
            scope.launch {
                delay(1000)
                if (!player.isPlaying && player.mediaItemCount == 0) {
                    // Limpiar metadata para forzar actualizaciÃ³n de notificaciÃ³n
                    currentMediaMetadata.value = null
                }
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            setupLoudnessEnhancer()
            startListeningReminder()
            startSilentOutroSkip()

            // Fix: When another media app (e.g. Spotify) steals audio focus and releases it,
            // ExoPlayer may end up in STATE_IDLE. Pressing play sets playWhenReady=true
            // but the player won't actually play without prepare().
            if (player.playbackState == Player.STATE_IDLE && player.currentMediaItem != null) {
                player.prepare()
            }
        } else {
            listeningReminderJob?.cancel()
            silentOutroJob?.cancel()
        }

        // WiFi Jam Broadcast
        if (this::wifiJamManager.isInitialized && wifiJamManager.isHost.value) {
            wifiJamManager.broadcastEvent(
                WifiJamEvent(
                    type = if (playWhenReady) "PLAY" else "PAUSE",
                    mediaId = player.currentMediaItem?.mediaId ?: "",
                    positionMs = player.currentPosition,
                    playbackSpeed = player.playbackParameters.speed
                )
            )
        }

        // Actualizar notificaciÃ³n cuando cambia el estado de reproducciÃ³n
        publishFirebaseJamPlayback(
            playbackState = if (playWhenReady) {
                JamPlaybackTransportState.PLAYING
            } else {
                JamPlaybackTransportState.PAUSED
            },
        )
        scope.launch {
            delay(300)
            updateNotification()
        }
        

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            handleWakeLock()
        }

        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                openAudioEffectSession()
            } else {
                closeAudioEffectSession()
            }
        }

        if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
            
            // WiFi Jam Broadcast
            if (this::wifiJamManager.isInitialized && wifiJamManager.isHost.value) {
                wifiJamManager.broadcastEvent(
                    WifiJamEvent(
                        type = "SYNC",
                        mediaId = player.currentMediaItem?.mediaId ?: "",
                        positionMs = player.currentPosition,
                        playbackSpeed = player.playbackParameters.speed
                    )
                )
            }
            
            // Forzar actualizaciÃ³n de notificaciÃ³n para asegurar que la imagen se cargue
            publishFirebaseJamPlayback()
            publishFirebaseJamQueueSnapshot()
            scope.launch {
                delay(200)
                updateNotification()
            }
        }

        // ActualizaciÃ³n de Discord RPC
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                currentSong.value?.let { song ->
                    scope.launch {
                        discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, isPaused = false)
                    }
                }
            }
            // Send empty activity to the Discord RPC if the player is not playing
            else if (!events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)){
                scope.launch {
                    discordRpc?.stopActivity()
                }
            }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Si la cola estÃ¡ vacÃ­a, no mezclar
            if (player.mediaItemCount == 0) return

            // Siempre poner el item que se estÃ¡ reproduciendo primero
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }

        // Save state when shuffle mode changes
        if (isQueuePersistent) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Save state when repeat mode changes
        if (isQueuePersistent) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Log.e(TAG, "Player error: ${error.errorCodeName} (${error.errorCode}), message: ${error.message}", error)

        if (!isNetworkConnected.value) {
            waitOnNetworkError()
            return
        }

        // Retry on parsing/source errors and expired sockets.
        // UNSUPPORTED (3003) is included because a truncated or expired stream URL makes the
        // extractor read a bogus atom length ("Skipping atom with length > 2147483647"), which
        // surfaces as unsupported rather than malformed. Refreshing the URL recovers it.
        if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        ) {
            val mediaId = player.currentMediaItem?.mediaId
            if (mediaId != null) {
                if (sameItemRetryId == mediaId) {
                    sameItemRetryCount++
                } else {
                    sameItemRetryId = mediaId
                    sameItemRetryCount = 1
                }

                if (sameItemRetryCount > 3) {
                    Log.e(TAG, "Max retry limit reached ($sameItemRetryCount) for $mediaId after ${error.errorCodeName}. Auto-skipping or stopping.")
                    sameItemRetryCount = 0
                    sameItemRetryId = null
                    scope.launch(Dispatchers.Main) {
                        if (autoSkipNextOnError && player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                        } else {
                            player.stop()
                        }
                    }
                    return
                }

                Log.w(TAG, "Attempting retry ($sameItemRetryCount/3) for $mediaId after IO error: ${error.errorCodeName}")
                invalidateCachedUrls(mediaId)
                // Mark this media ID to skip fast-start on retry — the fast-start URL was rejected
                skipFastStartIds.add(mediaId)
                scope.launch(Dispatchers.IO) {
                    evictPlayerCache(mediaId)

                    // On HTTP 403, visitorData was already nulled by PlayerConnection.
                    // Fetch fresh visitorData BEFORE re-preparing so the new stream URL
                    // is minted with a valid session context.
                    if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                        try {
                            val result = YouTube.visitorData()
                            result.onSuccess { newData ->
                                Log.d(TAG, "VisitorData refreshed after IO 403: ${newData.take(20)}...")
                                YouTube.visitorData = newData
                                dataStore.edit { prefs ->
                                    prefs[VisitorDataKey] = newData
                                    prefs[VisitorDataTimestampKey] = System.currentTimeMillis()
                                }
                            }.onFailure { fetchError ->
                                Log.e(TAG, "Failed to refresh visitorData on IO retry", fetchError)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception refreshing visitorData on IO retry", e)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val currentPos = player.currentPosition
                        player.stop()
                        player.prepare()
                        player.seekTo(currentPos)
                        player.play()
                    }
                }
                return
            }
        }

        // On REMOTE_ERROR (YouTube API rejection — often caused by expired/corrupt visitor data),
        // refresh visitor data then retry once before giving up.
        if (error.errorCode == PlaybackException.ERROR_CODE_REMOTE_ERROR) {
            val mediaId = player.currentMediaItem?.mediaId
            if (mediaId != null) {
                Log.w(TAG, "REMOTE_ERROR for $mediaId — refreshing visitor data and retrying")
                songUrlCache.remove(mediaId)
                scope.launch(Dispatchers.IO) {
                    try {
                        playerCache.removeResource(mediaId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear cache on REMOTE_ERROR", e)
                    }
                    // Refresh visitor data
                    val result = YouTube.visitorData()
                    result.onSuccess { newData ->
                        Log.d(TAG, "VisitorData refreshed after REMOTE_ERROR: ${newData.take(20)}...")
                        YouTube.visitorData = newData
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { prefs ->
                                prefs[VisitorDataKey] = newData
                                prefs[VisitorDataTimestampKey] = System.currentTimeMillis()
                            }
                        }
                    }.onFailure { fetchError ->
                        Log.e(TAG, "Failed to refresh visitor data after REMOTE_ERROR", fetchError)
                        reportException(fetchError)
                    }
                    // Retry playback on main thread
                    withContext(Dispatchers.Main) {
                        val currentPos = player.currentPosition
                        player.stop()
                        player.prepare()
                        player.seekTo(currentPos)
                        player.play()
                    }
                }
                return
            }
        }

        if (autoSkipNextOnError) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource
                    .Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(okHttpClient)
                                .setDefaultRequestProperties(mapOf("User-Agent" to DEFAULT_STREAM_USER_AGENT)),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    // Data saver: doesn't write to playerCache when video cache disabled
    private fun createNonWritingCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource
                    .Factory()
                    .setCache(playerCache)
                    .setCacheWriteDataSinkFactory(null) // Don't write to player cache
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(okHttpClient)
                                .setDefaultRequestProperties(mapOf("User-Agent" to DEFAULT_STREAM_USER_AGENT)),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        val videoCacheEnabled = dataStore.get(VideoCacheEnabledKey, true)
        val cacheFactory = if (!videoCacheEnabled && videoMode) {
            createNonWritingCacheDataSource()
        } else {
            createCacheDataSource()
        }
        return ResolvingDataSource.Factory(cacheFactory) { dataSpec ->
            val rawKey = dataSpec.key ?: error("No media id")

            // Internet radio and ambient items already carry a real, playable stream URL in
            // their DataSpec because MediaItemExt sets it via setUri(). Sending them through
            // the YouTube resolution path below made ExoPlayer ask YouTube for a video whose
            // id is "radio:<uuid>", which always fails with "This video is unavailable" and
            // then retries until playback gives up. Return the spec untouched instead.
            if (rawKey.startsWith(RADIO_MEDIA_ID_PREFIX) || rawKey.startsWith(AMBIENT_MEDIA_ID_PREFIX)) {
                return@Factory dataSpec
            }

            val wantsAudioCompanion = rawKey.endsWith(AUDIO_COMPANION_SUFFIX)
            val mediaId = if (wantsAudioCompanion) rawKey.removeSuffix(AUDIO_COMPANION_SUFFIX) else rawKey
            val cacheKey = when {
                wantsAudioCompanion -> rawKey
                videoMode -> "${mediaId}_video"
                else -> mediaId
            }
            
            // Check if already cached using the correct key
            if (downloadCache.isCached(
                    cacheKey,
                    dataSpec.position,
                    if (dataSpec.length >= 0) dataSpec.length else 1
                ) ||
                playerCache.isCached(cacheKey, dataSpec.position, CHUNK_LENGTH)
            ) {
                // If using video mode and cached, create DataSpec with correct key
                if (videoMode) {
                     return@Factory dataSpec.buildUpon().setKey(cacheKey).build()
                }
                
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }

            songUrlCache[cacheKey]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO + NonCancellable) { recoverSong(mediaId) }
                return@Factory resolvedDataSpec(dataSpec, it.first, cacheKey)
            }

            // Wait for prefetch if in progress, but with a timeout
            prefetchJobs[mediaId]?.let { job ->
                try {
                    runBlocking { withTimeout(2000) { job.join() } }
                } catch (_: TimeoutCancellationException) {
                    Timber.d("Prefetch timeout for $mediaId, proceeding with direct fetch")
                }
                // Re-check cache after prefetch completion
                songUrlCache[cacheKey]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                    scope.launch(Dispatchers.IO + NonCancellable) { recoverSong(mediaId) }
                    return@Factory resolvedDataSpec(dataSpec, it.first, cacheKey)
                }
            }

            // Intentar YouTube primero (fuente principal)
            val ytLogTag = "YouTube"

            try {
                val playbackData = runBlocking(Dispatchers.IO) {
                    YTPlayerUtils.playerResponseForPlayback(
                        mediaId,
                        audioQuality = currentAudioQuality,
                        connectivityManager = connectivityManager,
                        preferredStreamClient = preferredStreamClient,
                        avoidCodecs = avoidStreamCodecs,
                        isVideo = videoMode && !wantsAudioCompanion,
                        videoQuality = currentVideoQuality,
                    )
                }.getOrElse { throwable ->
                    when (throwable) {
                        is YTPlayerUtils.LoginRequiredForPlaybackException -> {
                            promptLoginRecovery(mediaId, throwable.targetUrl)
                            throw PlaybackException(
                                getString(R.string.playback_requires_youtube_music_confirmation),
                                throwable,
                                PlaybackException.ERROR_CODE_REMOTE_ERROR
                            )
                        }

                        is PlaybackException -> throw throwable

                        is ConnectException, is UnknownHostException -> {
                            throw PlaybackException(
                                getString(R.string.error_no_internet),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                            )
                        }

                        is SocketTimeoutException -> {
                            throw PlaybackException(
                                getString(R.string.error_timeout),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                            )
                        }

                        else -> throw PlaybackException(
                            getString(R.string.error_unknown),
                            throwable,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                }

                val format = playbackData.format
                val loudnessDb = playbackData.audioConfig?.loudnessDb
                val perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb

                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"") ?: "unknown",
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate ?: 0,
                            contentLength = format.contentLength ?: 0L,
                            loudnessDb = loudnessDb,
                            playbackUrl = playbackData.streamUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, playbackData) }

                val expiresAt = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                val streamUrl = playbackData.streamUrl

                playbackUrlCache[cacheKey] = streamUrl to expiresAt

                val length = if (dataSpec.length >= 0) minOf(dataSpec.length, CHUNK_LENGTH) else CHUNK_LENGTH
                return@Factory resolvedDataSpec(dataSpec, streamUrl, cacheKey).subrange(dataSpec.uriPositionOffset, length)
            } catch (e: Exception) {
                Timber.tag(ytLogTag).e(e, "YouTube playback error, trying JossRed as fallback")

                // Verificar si la fuente alternativa estÃ¡ habilitada
                val useAlternativeSource = runBlocking {
                    dataStore.data.map { preferences ->
                        val JossRedMultimedia = booleanPreferencesKey("JossRedMultimedia")
                        preferences[JossRedMultimedia] ?: false
                    }.first()
                }

                // Si la fuente alternativa estÃ¡ deshabilitada, relanzar la excepciÃ³n
                if (!useAlternativeSource) {
                    throw e
                }

                // Fuente alternativa: JossRed (fallback)
                val JRlogTag = "JossRed"
                try {
                    val alternativeUrl = runCatching {
                        runBlocking(Dispatchers.IO) {
                            withTimeout(5000) {
                                JossRedClient.getStreamingUrl(mediaId)
                            }
                        }
                    }.getOrNull()

                    if (alternativeUrl != null) {
                        // Verificar accesibilidad de URL con una solicitud HEAD
                        val client = OkHttpClient.Builder()
                            .connectTimeout(2, TimeUnit.SECONDS)
                            .readTimeout(2, TimeUnit.SECONDS)
                            .build()

                        val request = Request.Builder()
                            .url(alternativeUrl)
                            .head()
                            .build()

                        try {
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                Timber.tag(JRlogTag)
                                    .i("Using JossRed URL as fallback: $alternativeUrl")
                                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                                return@Factory dataSpec.withUri(alternativeUrl.toUri())
                            } else {
                                Timber.tag(JRlogTag)
                                    .w("JossRed URL unreachable (HTTP ${response.code}), throwing original error")
                                throw e
                            }
                        } catch (jrException: Exception) {
                            Timber.tag(JRlogTag).e(
                                jrException,
                                "Error verifying JossRed URL, throwing original error"
                            )
                            throw e
                        }
                    } else {
                        throw e
                    }
                } catch (jrException: Exception) {
                    when (jrException) {
                        is JossRedClient.JossRedException -> {
                            Timber.tag(JRlogTag)
                                .w("JossRed error: ${jrException.message}, throwing original error")
                        }

                        is TimeoutCancellationException -> {
                            Timber.tag(JRlogTag).w("JossRed timeout, throwing original error")
                        }

                        else -> {
                            Timber.tag(JRlogTag)
                                .e(jrException, "JossRed error, throwing original error")
                        }
                    }
                    throw e
                }
            }
        }
    }

    private fun createMediaSourceFactory(): MediaSource.Factory {
        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setMp4ExtractorFlags(
                Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS
            )
            .setFragmentedMp4ExtractorFlags(
                FragmentedMp4Extractor.FLAG_ENABLE_EMSG_TRACK or
                FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS or
                FragmentedMp4Extractor.FLAG_WORKAROUND_EVERY_VIDEO_FRAME_IS_SYNC_FRAME
            )

        val baseFactory = DefaultMediaSourceFactory(
            createDataSourceFactory(),
            extractorsFactory,
        )

        // In video mode, adaptive formats are video-only (no audio). We merge a
        // companion audio source so the player has both video and audio tracks.
        return object : MediaSource.Factory {
            override fun setDrmSessionManagerProvider(
                drmSessionManagerProvider: androidx.media3.exoplayer.drm.DrmSessionManagerProvider
            ): MediaSource.Factory {
                baseFactory.setDrmSessionManagerProvider(drmSessionManagerProvider)
                return this
            }

            override fun setLoadErrorHandlingPolicy(
                loadErrorHandlingPolicy: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
            ): MediaSource.Factory {
                baseFactory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
                return this
            }

            override fun getSupportedTypes(): IntArray = baseFactory.supportedTypes

            override fun createMediaSource(mediaItem: MediaItem): MediaSource {
                if (videoMode) {
                    // Video source: resolver maps mediaId → "${mediaId}_video" cache key
                    val videoSource = baseFactory.createMediaSource(mediaItem)

                    // Companion audio source: key suffix tells the resolver to
                    // return the audio stream URL instead of the video one.
                    val audioItem = mediaItem.buildUpon()
                        .setCustomCacheKey("${mediaItem.mediaId}$AUDIO_COMPANION_SUFFIX")
                        .build()
                    val audioSource = baseFactory.createMediaSource(audioItem)

                    Log.d(TAG, "Video mode: merging video + companion audio for ${mediaItem.mediaId}")

                    // adjustPeriodTimeOffsets and clipDurations must both be true. The two
                    // streams are fetched separately and rarely share an identical duration or
                    // start timestamp, and with the defaults (both false) the audio period is
                    // left unaligned with the video period, so playback renders video silently.
                    return MergingMediaSource(
                        /* adjustPeriodTimeOffsets = */ true,
                        /* clipDurations = */ true,
                        videoSource,
                        audioSource,
                    )
                }
                return baseFactory.createMediaSource(mediaItem)
            }
        }
    }

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        arrayOf(monoBalanceProcessor),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        if (!videoMode) return
        if (tracks.groups.isEmpty()) {
            Log.w(TAG, "Tracks: no track groups (merge produced nothing)")
            return
        }
        tracks.groups.forEach { group ->
            val type = when (group.type) {
                C.TRACK_TYPE_AUDIO -> "AUDIO"
                C.TRACK_TYPE_VIDEO -> "VIDEO"
                else -> "OTHER(${group.type})"
            }
            for (i in 0 until group.length) {
                Log.d(
                    TAG,
                    "Tracks: $type format=${group.getTrackFormat(i).sampleMimeType} " +
                        "supported=${group.isTrackSupported(i)} selected=${group.isTrackSelected(i)}",
                )
            }
        }
        if (tracks.groups.none { it.type == C.TRACK_TYPE_AUDIO }) {
            Log.e(TAG, "Tracks: NO AUDIO TRACK GROUP - companion audio source was not merged")
        }
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (playbackStats.totalPlayTimeMs >= (
                    dataStore[HistoryDuration]?.times(1000f)
                        ?: 30000f
                    ) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }
            // Use cached value
            if (!pauseRemoteListenHistory) {
                CoroutineScope(Dispatchers.IO).launch {
                    val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                        ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                            .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    playbackUrl?.let {
                        YouTube.registerPlayback(null, playbackUrl)
                            .onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun seekToNextJamAware() {
        val session = if (this::jamSessionManager.isInitialized) {
            jamSessionManager.sessionState.value.session
        } else {
            null
        }
        val canControlPlayback = session == null ||
            session.role == JamParticipantRole.HOST ||
            jamAllowsGuestControls()
        if (!canControlPlayback) {
            return
        }
        if (session?.role == JamParticipantRole.HOST && jamSessionManager.queueSnapshot.value.isNotEmpty()) {
            jamSessionManager.popNextQueueItem()?.let { selection ->
                val syncedQueue = buildList {
                    add(selection.nextItem.toMediaMetadata().toMediaItem())
                    addAll(selection.remainingQueue.map { it.toMediaMetadata().toMediaItem() })
                }
                player.setMediaItems(syncedQueue)
                player.prepare()
                player.playWhenReady = true
                lastJamQueueSyncFingerprint = null
            }
            return
        }

        if (player.hasNextMediaItem()) {
            player.seekToNext()
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun seekToPreviousJamAware() {
        val session = if (this::jamSessionManager.isInitialized) {
            jamSessionManager.sessionState.value.session
        } else {
            null
        }
        val canControlPlayback = session == null ||
            session.role == JamParticipantRole.HOST ||
            jamAllowsGuestControls()
        if (!canControlPlayback) {
            return
        }
        if (player.hasPreviousMediaItem() || player.currentPosition > 3_000) {
            player.seekToPrevious()
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun currentJamPlaybackState(): JamPlaybackTransportState =
        when {
            player.playbackState == Player.STATE_BUFFERING -> JamPlaybackTransportState.BUFFERING
            player.playWhenReady -> JamPlaybackTransportState.PLAYING
            else -> JamPlaybackTransportState.PAUSED
        }

    private fun publishFirebaseJamPlayback(
        playbackState: JamPlaybackTransportState = currentJamPlaybackState(),
        positionMs: Long = player.currentPosition,
        mediaId: String = player.currentMediaItem?.mediaId.orEmpty(),
    ) {
        if (!this::jamSessionManager.isInitialized) return
        val currentMetadata = player.currentMediaItem?.metadata ?: currentMediaMetadata.value
        jamSessionManager.publishPlaybackState(
            mediaId = mediaId,
            title = currentMetadata?.title.orEmpty(),
            artist = currentMetadata?.artists?.joinToString { it.name }.orEmpty(),
            thumbnailUrl = currentMetadata?.thumbnailUrl.orEmpty(),
            durationSeconds = currentMetadata?.duration ?: -1,
            positionMs = positionMs,
            playbackSpeed = player.playbackParameters.speed,
            playbackState = playbackState,
        )
    }

    private fun startJamPlaybackHeartbeat() {
        jamPlaybackHeartbeatJob?.cancel()
        jamPlaybackHeartbeatJob = scope.launch {
            while (isActive) {
                delay(JAM_PLAYBACK_HEARTBEAT_MS)
                val session = jamSessionManager.sessionState.value.session ?: continue
                if (session.role != JamParticipantRole.HOST) continue
                if (player.currentMediaItem == null || player.playbackState == Player.STATE_IDLE) continue
                publishFirebaseJamPlayback()
            }
        }
    }

    private fun publishFirebaseJamQueueSnapshot() {
        if (!this::jamSessionManager.isInitialized) return
        val session = jamSessionManager.sessionState.value.session ?: return
        if (session.role == JamParticipantRole.HOST) {
            jamSessionManager.reconcileHostQueue(buildJamQueueSeeds())
        }
    }

    private fun buildJamQueueSeeds(): List<JamQueueSeed> {
        val startIndex = when {
            player.mediaItemCount == 0 -> return emptyList()
            player.currentMediaItemIndex in 0 until player.mediaItemCount -> player.currentMediaItemIndex + 1
            else -> 0
        }
        return player.mediaItems
            .drop(startIndex.coerceAtLeast(0))
            .mapNotNull { mediaItem ->
                val metadata = mediaItem.metadata ?: return@mapNotNull null
                JamQueueSeed(
                    mediaId = mediaItem.mediaId,
                    title = metadata.title,
                    artist = metadata.artists.joinToString { it.name },
                    thumbnailUrl = metadata.thumbnailUrl.orEmpty(),
                    durationSeconds = metadata.duration,
                )
            }
    }

    private fun buildJamQueueFingerprint(
        roomId: String,
        queueSeeds: List<JamQueueSeed>,
    ): String {
        val queuePart = queueSeeds.joinToString(separator = "|") { seed ->
            listOf(
                seed.mediaId,
                seed.title,
                seed.artist,
                seed.durationSeconds.toString(),
            ).joinToString(separator = "~")
        }
        return "$roomId::$queuePart"
    }

    private fun updateCurrentQueueFromMediaItems(mediaItems: List<MediaItem>) {
        currentQueue = ListQueue(
            title = "Jam",
            items = mediaItems,
            startIndex = 0,
            position = 0L,
        )
    }

    internal fun applySharedJamQueueToHostPlayer(queueItems: List<JamQueueItem>) {
        val session = jamSessionManager.sessionState.value.session ?: return
        if (session.role != JamParticipantRole.HOST) return

        val remoteQueueSeeds = queueItems.map { item ->
            JamQueueSeed(
                mediaId = item.mediaId,
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl,
                durationSeconds = item.durationSeconds,
            )
        }
        val remoteFingerprint = buildJamQueueFingerprint(
            roomId = session.roomCode.roomId,
            queueSeeds = remoteQueueSeeds,
        )
        val localFingerprint = buildJamQueueFingerprint(
            roomId = session.roomCode.roomId,
            queueSeeds = buildJamQueueSeeds(),
        )
        if (remoteFingerprint == localFingerprint) {
            lastJamQueueSyncFingerprint = remoteFingerprint
            return
        }

        lastJamQueueSyncFingerprint = remoteFingerprint
        val newItems = queueItems.map { it.toMediaMetadata().toMediaItem() }

        if (player.currentMediaItem == null) {
            player.setMediaItems(newItems)
            updateCurrentQueueFromMediaItems(newItems)
            player.prepare()
            player.playWhenReady = true
        } else {
            val startIndex = player.currentMediaItemIndex + 1
            if (player.mediaItemCount > startIndex) {
                player.removeMediaItems(startIndex, player.mediaItemCount)
            }
            player.addMediaItems(startIndex, newItems)
            updateCurrentQueueFromMediaItems(player.mediaItems)
        }
    }

    private fun applySharedJamQueueToGuestPlayer(queueItems: List<JamQueueItem>) {
        val session = jamSessionManager.sessionState.value.session ?: return
        if (session.role != JamParticipantRole.GUEST) return

        val remotePlayback = jamSessionManager.remotePlayback.value
        val remoteQueueSeeds = queueItems.map { item ->
            JamQueueSeed(
                mediaId = item.mediaId,
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl,
                durationSeconds = item.durationSeconds,
            )
        }
        val remoteFingerprint = buildJamQueueFingerprint(
            roomId = session.roomCode.roomId,
            queueSeeds = remoteQueueSeeds,
        )
        if (remoteFingerprint == lastGuestJamQueueFingerprint) return

        val currentMediaItem = player.currentMediaItem ?: remotePlayback
            ?.takeIf { it.mediaId.isNotBlank() }
            ?.toMediaMetadata()
            ?.toMediaItem()
        if (currentMediaItem == null && queueItems.isEmpty()) {
            return
        }
        
        lastGuestJamQueueFingerprint = remoteFingerprint
        val newItems = queueItems.map { it.toMediaMetadata().toMediaItem() }

        if (player.currentMediaItem == null) {
            val rebuiltQueue = buildList {
                currentMediaItem?.let(::add)
                addAll(newItems)
            }
            if (rebuiltQueue.isEmpty()) return
            player.setMediaItems(rebuiltQueue)
            updateCurrentQueueFromMediaItems(rebuiltQueue)
            player.prepare()
        } else {
            val startIndex = player.currentMediaItemIndex + 1
            if (player.mediaItemCount > startIndex) {
                player.removeMediaItems(startIndex, player.mediaItemCount)
            }
            player.addMediaItems(startIndex, newItems)
            updateCurrentQueueFromMediaItems(player.mediaItems)
        }
    }

    private fun applyRemoteJamPlayback(snapshot: JamPlaybackSnapshot) {
        if (snapshot.mediaId.isBlank()) return

        // Add a small predictive offset to compensate for Appwrite Realtime delivery latency (~300ms typical)
        val deliveryCompensationMs = if (snapshot.playbackState == JamPlaybackTransportState.PLAYING) 300L else 0L
        val targetPosition = snapshot.expectedPositionAt(jamSessionManager.currentServerTimeMs()) + deliveryCompensationMs

        if (player.currentMediaItem?.mediaId != snapshot.mediaId) {
            val mediaItem = snapshot.toMediaMetadata().toMediaItem()
            player.setMediaItems(listOf(mediaItem), 0, targetPosition)
            updateCurrentQueueFromMediaItems(listOf(mediaItem))
            player.prepare()
            lastGuestJamQueueFingerprint = null
        } else if (kotlin.math.abs(player.currentPosition - targetPosition) > 200L) {
            // Tighter threshold: sync if more than 200ms off (was 1000ms)
            player.seekTo(targetPosition)
        }

        player.setPlaybackSpeed(snapshot.playbackSpeed)
        player.playWhenReady = snapshot.playbackState != JamPlaybackTransportState.PAUSED
    }

    fun syncJamStateNow() {
        if (!this::jamSessionManager.isInitialized) return
        publishFirebaseJamPlayback()
    }

    fun currentPlaybackQueueForJam(): List<com.Chenkham.Echofy.models.MediaMetadata> {
        if (player.mediaItemCount == 0) return emptyList()
        val startIndex = when {
            player.currentMediaItemIndex in 0 until player.mediaItemCount -> player.currentMediaItemIndex
            else -> 0
        }
        return player.mediaItems
            .drop(startIndex)
            .mapNotNull { it.metadata }
    }

    fun replacePlayerQueueForJam(items: List<com.Chenkham.Echofy.models.MediaMetadata>) {
        if (items.isEmpty()) return
        val playWhenReady = player.playWhenReady || player.currentMediaItem == null
        val mediaItems = items.map { it.toMediaItem() }
        player.setMediaItems(mediaItems)
        updateCurrentQueueFromMediaItems(mediaItems)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    fun seedJamQueue(items: List<com.Chenkham.Echofy.models.MediaMetadata>) {
        if (!this::jamSessionManager.isInitialized) return
        jamSessionManager.replaceQueue(items)
        lastJamQueueSyncFingerprint = null
    }

    private fun constrainHostPlayerToCurrentSong() {
        val currentMediaItem = player.currentMediaItem ?: return
        val shouldPlay = player.playWhenReady
        val currentPositionMs = if (player.currentPosition >= 0) player.currentPosition else 0L
        val mediaItems = listOf(currentMediaItem)
        player.setMediaItems(mediaItems, 0, currentPositionMs)
        updateCurrentQueueFromMediaItems(mediaItems)
        player.prepare()
        player.playWhenReady = shouldPlay
    }

    fun enqueueItemsIntoJam(items: List<MediaItem>): Boolean {
        if (!this::jamSessionManager.isInitialized) return false
        val session = jamSessionManager.sessionState.value.session ?: return false
        val jamItems = items.mapNotNull { mediaItem ->
            val metadata = mediaItem.metadata ?: return@mapNotNull null
            jamSessionManager.enqueueSong(metadata).getOrNull()
        }
        if (jamItems.isEmpty()) return false
        if (session.role == JamParticipantRole.HOST) {
            applySharedJamQueueToHostPlayer(jamSessionManager.queueSnapshot.value)
        }
        return true
    }

    fun enableJamAddMode() {
        jamAddMode = true
    }

    private fun consumeJamAddMode(queue: Queue): Boolean {
        if (!jamAddMode) return false
        jamAddMode = false

        val selectedItem = when {
            queue.preloadItem != null -> queue.preloadItem?.toMediaItem()
            queue is ListQueue -> queue.items.getOrNull(queue.startIndex)
            else -> null
        }

        if (selectedItem != null) {
            return enqueueItemsIntoJam(listOf(selectedItem))
        }

        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { queue.getInitialStatus() }
            }.getOrNull()
                ?.let { status ->
                    status.items.getOrNull(status.mediaItemIndex.coerceAtLeast(0))
                }
                ?.let { item ->
                    enqueueItemsIntoJam(listOf(item))
                }
        }

        return true
    }

    fun jamAllowsGuestControls(): Boolean =
        if (this::jamSessionManager.isInitialized) {
            jamSessionManager.roomMeta.value?.allowGuestControls ?: true
        } else {
            true
        }

    fun canCurrentJamParticipantControlPlayback(): Boolean {
        val role = currentJamParticipantRole() ?: return true
        return role == JamParticipantRole.HOST || jamAllowsGuestControls()
    }

    fun isJamSessionActive(): Boolean {
        if (!this::jamSessionManager.isInitialized) return false
        val phase = jamSessionManager.sessionState.value.phase
        return phase == JamSessionPhase.HOSTING || phase == JamSessionPhase.JOINED
    }

    fun currentJamParticipantRole(): JamParticipantRole? =
        if (this::jamSessionManager.isInitialized) jamSessionManager.sessionState.value.session?.role else null

    fun handleJamAwarePlayQueue(queue: Queue): Boolean {
        if (!isJamSessionActive()) return false
        if (consumeJamAddMode(queue)) {
            return true
        }
        Log.d(TAG, "Ignoring local playQueue while Jam is active")
        return true
    }

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            return
        }

        saveResumePositionForCurrentSong()

        try {
            val persistQueue =
                PersistQueue(
                    title = queueTitle,
                    items = player.mediaItems.mapNotNull { it.metadata },
                    mediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                    position = if (player.currentPosition >= 0) player.currentPosition else 0,
                )
            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixItems.value.mapNotNull { it.metadata },
                    mediaItemIndex = 0,
                    position = 0,
                )

            // Save player state
            val playerState = PersistPlayerState(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                currentPosition = if (player.currentPosition >= 0) player.currentPosition else 0,
                playWhenReady = player.playWhenReady, // Estado de reproducciÃ³n (si estÃ¡ listo para reproducir)
                playbackState = player.playbackState // Estado actual del reproductor
            )

            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving queue to disk", it)
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving automix to disk", it)
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(playerState)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving player state to disk", it)
                reportException(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveQueueToDisk", e)
            reportException(e)
        }
    }

    private fun handleWakeLock() {
        // ExoPlayer already manages a wake lock via setWakeMode(C.WAKE_MODE_NETWORK) and
        // releases it as soon as playback stops. Holding a second PARTIAL_WAKE_LOCK here was
        // redundant, and because it was acquired with a 12 hour timeout any missed release
        // path kept the CPU awake for half a day. Kept as a no-op so the existing call site
        // in onEvents stays valid.
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        if (isQueuePersistent) {
            saveQueueToDisk()
        }
        
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        releaseLoudnessEnhancer()
        runCatching { unregisterReceiver(headphoneConnectReceiver) }
        shakeDetector?.let { com.Chenkham.Echofy.utils.ShakeDetector.unregister(this, it) }
        shakeDetector = null
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        if (this::jamSessionManager.isInitialized) {
            jamSessionManager.shutdown()
        }
        jamPlaybackHeartbeatJob?.cancel()
        player.release()
        super.onDestroy()
    }

    override fun startForegroundService(service: Intent?): ComponentName? {
        return try {
            super.startForegroundService(service)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                Timber.tag(TAG).e(e, "ForegroundServiceStartNotAllowedException suppressed")
                null
            } else {
                throw e
            }
        }
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        /**
         * Do not stop the service if playback is active.
         * This allows the music to continue playing even if the user swipes away the app (clears recents).
         * The service will still be stopped if the user pauses and dismisses the notification.
         */
        // Removing stopSelf() to allow the media session to persist when the app is swiped away.
        // It's safe to do this because the user can always dismiss the notification themselves, which handles service shutdown.
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        /** How long the pause/resume volume ramp lasts, in milliseconds. */
        private const val FADE_DURATION_MS = 360L

        const val RADIO_MEDIA_ID_PREFIX = "radio:"
        const val AMBIENT_MEDIA_ID_PREFIX = "ambient:"
        const val AUDIO_COMPANION_SUFFIX = "_audio"

        /**
         * Fallback User-Agent for stream requests. Only used when the resolver could not
         * record the User-Agent of the client that minted the URL. It must be applied via
         * setDefaultRequestProperties (not setUserAgent), because setUserAgent appends a
         * second User-Agent header instead of replacing the per-request one, which makes
         * YouTube reject the PO token bound to the original client with HTTP 403.
         */
        const val DEFAULT_STREAM_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"

        // Constants for audio normalization
        private const val MAX_GAIN_MB = 800 // Maximum gain in millibels (8 dB)
        private const val MIN_GAIN_MB = -800 // Minimum gain in millibels (-8 dB)

        private const val JAM_PLAYBACK_HEARTBEAT_MS = 3_000L
        private const val TAG = "MusicService"
    }
}
