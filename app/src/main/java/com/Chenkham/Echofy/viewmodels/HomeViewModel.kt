package com.Chenkham.Echofy.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.innertube.pages.ExplorePage
import com.arturo254.opentune.innertube.pages.HomePage
import com.arturo254.opentune.innertube.utils.completed
import com.Chenkham.Echofy.constants.HiddenGemsEnabledKey
import com.Chenkham.Echofy.constants.BecauseYouListenedEnabledKey
import com.Chenkham.Echofy.constants.TimeMachineEnabledKey
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.utils.currentUtcOffsetSeconds
import com.Chenkham.Echofy.constants.MoodPlaylist
import com.Chenkham.Echofy.constants.MoodPlaylistsEnabledKey
import java.time.LocalTime
import com.Chenkham.Echofy.db.entities.Album
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.db.entities.LocalItem
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.models.SimilarRecommendation
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val isLoadingMore = MutableStateFlow(false)

    // PERFORMANCE: Prevent duplicate concurrent loads
    private val loadMutex = Mutex()
    private val hasInitialLoadCompleted = AtomicBoolean(false)
    private var lastLoadTimestamp = 0L
    private val minLoadInterval = 5000L // 5 seconds minimum between loads

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)

    /** Rarely played library songs, only populated when the user enables hidden gems. */
    val hiddenGems = MutableStateFlow<List<Song>?>(null)

    /** Songs played in the same week of a previous year, gated behind the time machine toggle. */
    val timeMachine = MutableStateFlow<List<Song>?>(null)

    /** "Because you listened to [Artist]" — recommendations based on recent listening. */
    val becauseYouListenedArtist = MutableStateFlow<String?>(null)
    val becauseYouListenedSongs = MutableStateFlow<List<Song>?>(null)

    /** The year the [timeMachine] row is showing, used for its title. */
    val timeMachineYear = MutableStateFlow(0)

    /**
     * The mood mix matching the current hour, gated behind the mood playlists toggle.
     * Only one bucket is surfaced at a time so Home does not fill with five near-identical
     * rows; [moodPlaylist] carries which one is showing so the UI can title it.
     */
    val moodPlaylist = MutableStateFlow<MoodPlaylist?>(null)
    val moodSongs = MutableStateFlow<List<Song>?>(null)

    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val topCharts = MutableStateFlow<List<YTItem>?>(null)
    val viral50 = MutableStateFlow<List<YTItem>?>(null)
    val chartsCountryName = MutableStateFlow<String>("Global")

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    val likedSongIds = database.getLikedSongIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val librarySongIds = database.getLibrarySongIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val bookmarkedAlbumIds = database.getBookmarkedAlbumIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())


    fun loadMoreSections() {
        val cont = homePage.value?.continuation ?: return
        if (isLoadingMore.value) return
        isLoadingMore.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                YouTube.home(continuation = cont).onSuccess { nextHome ->
                    val current = homePage.value ?: return@onSuccess
                    val updatedSections = current.sections + nextHome.sections
                    homePage.value = current.copy(
                        sections = updatedSections,
                        continuation = nextHome.continuation
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoadingMore.value = false
            }
        }
    }

    private suspend fun load() {
        // PERFORMANCE: Prevent duplicate concurrent loads
        if (!loadMutex.tryLock()) {
            android.util.Log.d("HomeViewModel", "Load already in progress, skipping duplicate call")
            return
        }

        try {
            // Throttle loads to prevent excessive API calls
            val now = System.currentTimeMillis()
            if (hasInitialLoadCompleted.get() && (now - lastLoadTimestamp) < minLoadInterval) {
                android.util.Log.d("HomeViewModel", "Throttling load - too soon since last load")
                return
            }
            lastLoadTimestamp = now

            isLoading.value = true

            // PERFORMANCE FIX: Reduced wait time and use non-blocking approach
            // Wait up to 1.5 seconds max, but don't block unnecessarily
            var maxWaitAttempts = 15 // Wait up to 1.5 seconds
            while (YouTube.visitorData == null && maxWaitAttempts > 0) {
                kotlinx.coroutines.delay(100)
                maxWaitAttempts--
            }

            val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2

            // PERFORMANCE FIX: Parallelize local database operations using coroutineScope + async
            coroutineScope {
                val quickPicksDeferred = async(Dispatchers.IO) {
                    val pref = appContext.dataStore.data.map { it[com.Chenkham.Echofy.constants.QuickPicksKey] ?: com.Chenkham.Echofy.constants.QuickPicks.QUICK_PICKS.name }.first()
                    if (pref == com.Chenkham.Echofy.constants.QuickPicks.LAST_LISTEN.name) {
                        // History/seed-based recommendations
                        database.quickPicks().first().filterNot { it.song.isLocal }.shuffled().take(20)
                            .ifEmpty { database.allSongs().first().filterNot { it.song.isLocal }.shuffled().take(20) }
                    } else {
                        // Truly random quick picks across entire music library
                        database.allSongs().first().filterNot { it.song.isLocal }.shuffled().take(20)
                            .ifEmpty { database.quickPicks().first().filterNot { it.song.isLocal }.shuffled().take(20) }
                    }
                }

                val forgottenFavoritesDeferred = async(Dispatchers.IO) {
                    database.forgottenFavorites().first().shuffled().take(20)
                }

                val keepListeningDeferred = async(Dispatchers.IO) {
                    val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
                        .first().shuffled().take(10)
                    val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
                        .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
                    val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
                        .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
                        .shuffled().take(5)
                    (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()
                }

                // Discovery rows are opt-in, so skip their queries entirely when off.
                val prefs = appContext.dataStore.data.first()

                val hiddenGemsDeferred = if (prefs[HiddenGemsEnabledKey] == true) {
                    async(Dispatchers.IO) { database.hiddenGems().first() }
                } else null

                val timeMachineDeferred = if (prefs[TimeMachineEnabledKey] ?: true) {
                    async(Dispatchers.IO) {
                        val lastYear = LocalDate.now().minusYears(1)
                        val songs = database.songsPlayedBetween(
                            fromDate = lastYear.minusDays(3).toString(),
                            toDate = lastYear.plusDays(3).toString(),
                            offsetSeconds = currentUtcOffsetSeconds(),
                        ).first()
                        lastYear.year to songs
                    }
                } else null

                val becauseYouListenedDeferred = if (prefs[BecauseYouListenedEnabledKey] ?: true) {
                    async(Dispatchers.IO) {
                        database.topRecentArtistName(fromTimeStamp)?.let { artist ->
                            artist to database.songsByArtistName(artist).first()
                        }
                    }
                } else null

                val moodDeferred = if (prefs[MoodPlaylistsEnabledKey] ?: true) {
                    async(Dispatchers.IO) {
                        val offset = currentUtcOffsetSeconds()
                        val hour = LocalTime.now().hour
                        // Pick the bucket covering the current hour, so the row reflects
                        // what the user tends to play right now. Focus has no window and is
                        // the fallback when no clock bucket matches.
                        val bucket = MoodPlaylist.entries
                            .filter { it != MoodPlaylist.FOCUS }
                            .find { mood ->
                                if (mood.wrapsMidnight) {
                                    hour >= mood.startHour || hour <= mood.endHour
                                } else {
                                    hour in mood.startHour..mood.endHour
                                }
                            } ?: MoodPlaylist.FOCUS

                        val songs = when {
                            bucket == MoodPlaylist.FOCUS ->
                                database.longestPlayedSongs().first()

                            bucket.wrapsMidnight ->
                                database.songsPlayedBetweenHoursWrapping(
                                    startHour = bucket.startHour,
                                    endHour = bucket.endHour,
                                    offsetSeconds = offset,
                                ).first()

                            else ->
                                database.songsPlayedBetweenHours(
                                    startHour = bucket.startHour,
                                    endHour = bucket.endHour,
                                    offsetSeconds = offset,
                                ).first()
                        }
                        bucket to songs
                    }
                } else null

                // Await all database operations
                quickPicks.value = quickPicksDeferred.await()
                forgottenFavorites.value = forgottenFavoritesDeferred.await()
                keepListening.value = keepListeningDeferred.await()
                hiddenGems.value = hiddenGemsDeferred?.await()
                timeMachineDeferred?.await()?.let { (year, songs) ->
                    timeMachineYear.value = year
                    timeMachine.value = songs
                }
                becauseYouListenedDeferred?.await()?.let { (artist, songs) ->
                    becauseYouListenedArtist.value = artist
                    becauseYouListenedSongs.value = songs
                }
                moodDeferred?.await()?.let { (bucket, songs) ->
                    moodPlaylist.value = bucket
                    moodSongs.value = songs
                }
            }

        allLocalItems.value =
            (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
                .filter { it is Song || it is Album }

        // PERFORMANCE FIX: Parallelize ALL network operations for instant home screen loading
        coroutineScope {            // 1. Account playlists (if logged in)
            val accountPlaylistsDeferred: kotlinx.coroutines.Deferred<List<PlaylistItem>?>? = if (YouTube.cookie != null) {
                async(Dispatchers.IO) {
                    YouTube.library("FEmusic_liked_playlists").completed().getOrNull()
                        ?.items?.filterIsInstance<PlaylistItem>()
                        ?.filterNot { it.id == "SE" }
                }
            } else null

            // 2. YouTube Home page
            val homePageDeferred = async(Dispatchers.IO) {
                YouTube.home().onSuccess { 
                    Timber.d("YouTube.home() loaded")
                }.onFailure {
                    Timber.w("YouTube.home() failed: ${it.message}")
                    reportException(it)
                }.getOrNull()
            }

            // 3. YouTube Explore page
            val explorePageDeferred = async(Dispatchers.IO) {
                YouTube.explore().onFailure { reportException(it) }.getOrNull()
            }

            // 4. Charts and Viral 50 Page
            val prefs = appContext.dataStore.data.first()
            val chartsDeferred = if ((prefs[com.Chenkham.Echofy.constants.ShowTopChartsHomeKey] ?: false) ||
                (prefs[com.Chenkham.Echofy.constants.ShowViral50HomeKey] ?: false)) {
                async(Dispatchers.IO) {
                    YouTube.getChartsPage().getOrNull()
                }
            } else null

            // 5. Get artists for similar recommendations (parallel fetch)
            val mostPlayedArtistsDeferred = async(Dispatchers.IO) {
                database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                    .filter { it.artist.isYouTubeArtist }
                    .shuffled().take(3)
            }

            // 6. Get songs for similar recommendations (parallel fetch)
            val mostPlayedSongsDeferred = async(Dispatchers.IO) {
                database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                    .filter { it.album != null }
                    .shuffled().take(2)
            }

            // Await primary data first for faster initial render
            val resolvedPlaylists = accountPlaylistsDeferred?.await()
            if (resolvedPlaylists != null) {
                accountPlaylists.value = resolvedPlaylists
            }
            homePage.value = homePageDeferred.await()
            
            // Process charts page
            val rawChartsPage = chartsDeferred?.await()
            if (rawChartsPage != null) {
                val chartSections = rawChartsPage.sections
                chartSections.forEach { section ->
                    if (section.title.contains("Top", ignoreCase = true) || section.title.contains("Chart", ignoreCase = true)) {
                        if (topCharts.value == null) topCharts.value = section.items
                    } else if (section.title.contains("Trending", ignoreCase = true) || section.title.contains("Viral", ignoreCase = true)) {
                        if (viral50.value == null) viral50.value = section.items
                    }
                }
                if (topCharts.value == null && chartSections.isNotEmpty()) {
                    topCharts.value = chartSections.firstOrNull()?.items
                }
                if (viral50.value == null && chartSections.size > 1) {
                    viral50.value = chartSections.getOrNull(1)?.items
                }
            }

            // Process explore page
            val rawExplorePage = explorePageDeferred.await()
            if (rawExplorePage != null) {
                val artistsData = database.artistsBookmarkedByCreateDateAsc().first()
                val artists = artistsData.map(Artist::id).toHashSet()
                val favouriteArtists = artistsData
                    .filter { it.artist.bookmarkedAt != null }
                    .map { it.id }
                    .toHashSet()
                explorePage.value = rawExplorePage.copy(
                    newReleaseAlbums = rawExplorePage.newReleaseAlbums
                        .sortedBy { album ->
                            if (album.artists.orEmpty().any { it.id in favouriteArtists }) 0
                            else if (album.artists.orEmpty().any { it.id in artists }) 1
                            else 2
                        }
                )
            }

            // Now fetch similar recommendations in parallel
            val mostPlayedArtists = mostPlayedArtistsDeferred.await()
            val mostPlayedSongs = mostPlayedSongsDeferred.await()

            // Parallelize artist recommendation fetches
            val artistRecommendationsDeferred = mostPlayedArtists.map { artist ->
                async(Dispatchers.IO) {
                    val items = mutableListOf<YTItem>()
                    YouTube.artist(artist.id).onSuccess { page ->
                        items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                        items += page.sections.lastOrNull()?.items.orEmpty()
                    }
                    if (items.isNotEmpty()) {
                        SimilarRecommendation(title = artist, items = items.shuffled())
                    } else null
                }
            }

            // Parallelize song recommendation fetches
            val songRecommendationsDeferred = mostPlayedSongs.map { song ->
                async(Dispatchers.IO) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        val page = YouTube.related(endpoint).getOrNull()
                        if (page != null) {
                            val items = (page.songs.shuffled().take(8) +
                                    page.albums.shuffled().take(4) +
                                    page.artists.shuffled().take(4) +
                                    page.playlists.shuffled().take(4)).shuffled()
                            if (items.isNotEmpty()) {
                                SimilarRecommendation(title = song, items = items)
                            } else null
                        } else null
                    } else null
                }
            }

            // Await all recommendations
            val artistRecommendations = artistRecommendationsDeferred.mapNotNull { it.await() }
            val songRecommendations = songRecommendationsDeferred.mapNotNull { it.await() }
            similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()
        }

        allYtItems.value = (similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()).distinctBy { it.id }

            isLoading.value = false
            hasInitialLoadCompleted.set(true)
        } finally {
            loadMutex.unlock()
        }
    }

        fun toggleChip(chip: HomePage.Chip) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = selectedChip.value
            if (current?.title == chip.title) {
                selectedChip.value = null
                isLoading.value = true
                val page = YouTube.home().getOrNull()
                if (page != null) {
                    homePage.value = page
                }
                isLoading.value = false
            } else {
                selectedChip.value = chip
                val params = chip.endpoint?.params
                if (params != null) {
                    isLoading.value = true
                    val page = YouTube.home(params = params).getOrNull()
                    if (page != null) {
                        homePage.value = page.copy(chips = homePage.value?.chips ?: page.chips)
                    }
                    isLoading.value = false
                }
            }
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            // Reset throttle for manual refresh
            lastLoadTimestamp = 0L
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}