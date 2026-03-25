@file:Suppress("DEPRECATION")

package com.Chenkham.Echofy.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import java.util.concurrent.ConcurrentHashMap
import androidx.media3.common.MediaItem
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
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.jossredconnect.JossRedClient
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AudioNormalizationKey
import com.Chenkham.Echofy.constants.AudioQualityKey
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
import com.Chenkham.Echofy.lyrics.LyricsHelper
import com.Chenkham.Echofy.models.PersistPlayerState
import com.Chenkham.Echofy.models.PersistQueue
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.EmptyQueue
import com.Chenkham.Echofy.playback.queues.Queue
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.playback.queues.filterExplicit
import com.Chenkham.Echofy.utils.CoilBitmapLoader
import com.Chenkham.Echofy.utils.DiscordRPC
import com.Chenkham.Echofy.utils.NetworkConnectivityObserver
import com.Chenkham.Echofy.utils.YTPlayerUtils
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.enumPreference
import com.Chenkham.Echofy.constants.AudioQualityKey
import com.Chenkham.Echofy.constants.AutoLoadMoreKey
import com.Chenkham.Echofy.constants.AutoSkipNextOnErrorKey
import com.Chenkham.Echofy.constants.BassBoostEnabledKey
import com.Chenkham.Echofy.constants.BassBoostStrengthKey
import com.Chenkham.Echofy.constants.DisableLoadMoreWhenRepeatAllKey
import com.Chenkham.Echofy.constants.DiscordUseDetailsKey
import com.Chenkham.Echofy.constants.EqualizerBandLevelsKey
import com.Chenkham.Echofy.constants.EqualizerEnabledKey
import com.Chenkham.Echofy.constants.EqualizerPresetKey
import com.Chenkham.Echofy.constants.HideExplicitKey
import com.Chenkham.Echofy.constants.PauseListenHistoryKey
import com.Chenkham.Echofy.constants.PersistentQueueKey
import com.Chenkham.Echofy.constants.PlayerVolumeKey
import com.Chenkham.Echofy.constants.RepeatModeKey
import com.Chenkham.Echofy.constants.VideoCacheEnabledKey
import com.Chenkham.Echofy.constants.VideoQualityKey
import com.Chenkham.Echofy.constants.VisitorDataKey
import com.Chenkham.Echofy.constants.VisitorDataTimestampKey
import com.Chenkham.Echofy.utils.get
import com.Chenkham.Echofy.utils.reportException
import androidx.datastore.preferences.core.booleanPreferencesKey

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

    private val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: android.media.audiofx.Equalizer? = null
    private var bassBoost: android.media.audiofx.BassBoost? = null

    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: Job? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val songUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    
    // Add prefetch cache for upcoming songs
    private val prefetchJobs = ConcurrentHashMap<String, Job>()

    fun prefetchPlaybackData(mediaId: String, preloadToCache: Boolean = false) {
        if (songUrlCache.containsKey(mediaId)) {
            // URL already cached, but maybe preload bytes if requested
            if (preloadToCache) {
                val cached = songUrlCache[mediaId]
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
                // Use FAST path for prefetch - completes in 200-500ms instead of 1-3s
                val playbackData = YTPlayerUtils.fastStartPlayerResponse(
                    mediaId,
                    audioQuality = currentAudioQuality,
                    videoQuality = currentVideoQuality,
                    connectivityManager = connectivityManager,
                    videoMode = videoMode
                ).getOrNull()
                
                if (playbackData != null) {
                    val cacheKey = if (videoMode) "${mediaId}_video" else mediaId
                    songUrlCache[cacheKey] = playbackData.streamUrl to 
                        System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                    
                    // Preload first 128KB to disk cache for instant playback
                    if (preloadToCache) {
                        preloadBytesToCache(mediaId, playbackData.streamUrl)
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
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-131071") // First 128KB
                .build()
            
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
                // Max buffer: 5 MINUTES (download ahead at full 2+ MB/s)
                5 * 60_000,
                // Buffer to START: 250ms = INSTANT (just enough to prevent stutter)
                250,
                // Buffer after rebuffer: 1 second (quick recovery)
                1_000
            )
            // NO byte limit = full bandwidth utilization
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setPrioritizeTimeOverSizeThresholds(false)
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

        // Initialize WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Echofy:MusicServiceWakeLock").apply {
             setReferenceCounted(false)
        }

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

        playerVolume.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                // Use the globally cached discordUseDetails variable instead of reading runBlocking from DataStore.
                discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, discordUseDetails)
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
                            discordRpc?.updateSong(it, player.currentPosition, player.playbackParameters.speed, discordUseDetails)
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
                            discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, useDetails)
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
                    songUrlCache.remove(song.id)
                    withContext(Dispatchers.Main) {
                        val currentPos = player.currentPosition
                        val wasPlaying = player.isPlaying
                        
                        // Properly stop and release resources before switching
                        player.pause()
                        
                        // Small delay to allow MediaCodec to properly release
                        delay(100)
                        
                        // Clear video surface only if switching TO audio mode
                        if (!isSwitchedToVideo) {
                            player.clearVideoSurface()
                        }
                        
                        // Stop after pause to ensure clean state
                        player.stop()
                        
                        // Another small delay for codec cleanup
                        delay(50)
                        
                        // Prepare new media
                        player.prepare()
                        
                        // Seek after preparation is ready
                        player.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_READY) {
                                    player.seekTo(currentPos)
                                    if (wasPlaying) {
                                        player.play()
                                    }
                                    player.removeListener(this)
                                }
                            }
                        })
                    }
                }
            }

        // Observe VideoQuality changes to reload
        dataStore.data
            .map { it[VideoQualityKey] }
            .distinctUntilChanged()
            .drop(1)
            .collect(scope) {
                if (videoMode) {
                    currentSong.value?.let { song ->
                        songUrlCache.remove(song.id)
                        withContext(Dispatchers.Main) {
                            val currentPos = player.currentPosition
                            val wasPlaying = player.isPlaying
                            
                            // Safely stop before reloading
                            player.pause()
                            delay(100)
                            player.stop()
                            delay(50)
                            
                            player.prepare()
                            
                            // Seek after preparation is ready
                            player.addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(state: Int) {
                                    if (state == Player.STATE_READY) {
                                        player.seekTo(currentPos)
                                        if (wasPlaying) {
                                            player.play()
                                        }
                                        player.removeListener(this)
                                    }
                                }
                            })
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

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        lastPlaybackSpeed = -1.0f // forzar actualización de canción

        setupLoudnessEnhancer()

        discordUpdateJob?.cancel()

        // Resetear errores consecutivos cuando hay transición exitosa
        consecutivePlaybackErr = 0
        
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
        // Save state when playback state changes
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }

        // Cuando termina la reproducciÃ³n, ocultar notificaciÃ³n si la cola estÃ¡ vacÃ­a
        if (playbackState == Player.STATE_ENDED) {
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

            // Fix: When another media app (e.g. Spotify) steals audio focus and releases it,
            // ExoPlayer may end up in STATE_IDLE. Pressing play sets playWhenReady=true
            // but the player won't actually play without prepare().
            if (player.playbackState == Player.STATE_IDLE && player.currentMediaItem != null) {
                player.prepare()
            }
        }

        // Actualizar notificaciÃ³n cuando cambia el estado de reproducciÃ³n
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

        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
            // Forzar actualizaciÃ³n de notificaciÃ³n para asegurar que la imagen se cargue
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
                        discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, discordUseDetails)
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

        // Retry on parsing/source errors and expired sockets
        if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        ) {
            val mediaId = player.currentMediaItem?.mediaId
            if (mediaId != null) {
                Log.w(TAG, "Attempting retry for $mediaId after IO error: ${error.errorCodeName}")
                songUrlCache.remove(mediaId)
                scope.launch(Dispatchers.IO) {
                    try {
                        playerCache.removeResource(mediaId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear cache", e)
                    }
                }
                scope.launch(Dispatchers.Main) {
                    val currentPos = player.currentPosition
                    player.stop()
                    player.prepare()
                    player.seekTo(currentPos)
                    player.play()
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
                            OkHttpDataSource.Factory(
                                okHttpClient
                            ).setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
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
                            OkHttpDataSource.Factory(
                                okHttpClient
                            ).setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
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
            val mediaId = dataSpec.key ?: error("No media id")
            val cacheKey = if (videoMode) "${mediaId}_video" else mediaId
            
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
                return@Factory if (videoMode) {
                    dataSpec.withUri(it.first.toUri()).buildUpon().setKey(cacheKey).build()
                } else {
                    dataSpec.withUri(it.first.toUri())
                }
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
                    return@Factory if (videoMode) {
                        dataSpec.withUri(it.first.toUri()).buildUpon().setKey(cacheKey).build()
                    } else {
                        dataSpec.withUri(it.first.toUri())
                    }
                }
            }

            // Intentar YouTube primero (fuente principal)
            val ytLogTag = "YouTube"

            try {
                // FAST-START: Use fast path that skips validation and fallback clients
                // This returns in ~200-500ms instead of 1-3s
                val playbackData = runBlocking(Dispatchers.IO) {
                    YTPlayerUtils.fastStartPlayerResponse(
                        mediaId,
                        audioQuality = currentAudioQuality,
                        videoQuality = currentVideoQuality,
                        connectivityManager = connectivityManager,
                        videoMode = videoMode
                    )
                }.getOrElse { fastError ->
                    Timber.tag(ytLogTag).d("Fast-start failed, trying full path: ${fastError.message}")
                    // Fall back to full path only if fast-start fails
                    runBlocking(Dispatchers.IO) {
                        YTPlayerUtils.playerResponseForPlayback(
                            mediaId,
                            audioQuality = currentAudioQuality,
                            videoQuality = currentVideoQuality,
                            connectivityManager = connectivityManager,
                            videoMode = videoMode
                        )
                    }.getOrElse { throwable ->
                    when (throwable) {
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
                }

                val format = playbackData.format

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
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            playbackUrl = playbackData.streamUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, playbackData) }

                val streamUrl = playbackData.streamUrl

                songUrlCache[cacheKey] =
                    streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                
                // Return DataSpec with the custom key if in video mode
                val finalDataSpec = dataSpec.withUri(streamUrl.toUri())
                    .subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
                
                return@Factory if (videoMode) {
                    finalDataSpec.buildUpon().setKey(cacheKey).build()
                } else {
                    finalDataSpec
                }
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

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor())
            },
        )

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
                        emptyArray(),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
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

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            return
        }

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
        if (player.playWhenReady && (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)) {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(12 * 60 * 60 * 1000L) // 12 hours timeout to be safe
                Log.d(TAG, "WakeLock acquired")
            }
        } else {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        }
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
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
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

        private const val TAG = "MusicService"
    }
}