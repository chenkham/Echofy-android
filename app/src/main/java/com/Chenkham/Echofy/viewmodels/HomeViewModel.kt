package com.Chenkham.Echofy.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.innertube.models.YTItem
import com.Chenkham.innertube.pages.ExplorePage
import com.Chenkham.innertube.pages.HomePage
import com.Chenkham.innertube.utils.completedLibraryPage
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.Album
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.db.entities.LocalItem
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.models.SimilarRecommendation
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    // PERFORMANCE: Prevent duplicate concurrent loads
    private val loadMutex = Mutex()
    private val hasInitialLoadCompleted = AtomicBoolean(false)
    private var lastLoadTimestamp = 0L
    private val minLoadInterval = 5000L // 5 seconds minimum between loads

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

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
                    database.quickPicks().first().shuffled().take(20)
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

                // Await all database operations
                quickPicks.value = quickPicksDeferred.await()
                forgottenFavorites.value = forgottenFavoritesDeferred.await()
                keepListening.value = keepListeningDeferred.await()
            }

        allLocalItems.value =
            (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
                .filter { it is Song || it is Album }

        // PERFORMANCE FIX: Parallelize ALL network operations for instant home screen loading
        coroutineScope {
            // 1. Account playlists (if logged in)
            val accountPlaylistsDeferred = if (YouTube.cookie != null) {
                async(Dispatchers.IO) {
                    YouTube.library("FEmusic_liked_playlists").completedLibraryPage().getOrNull()
                        ?.items?.filterIsInstance<PlaylistItem>()
                        ?.filterNot { it.id == "SE" }
                }
            } else null

            // 2. YouTube Home page
            val homePageDeferred = async(Dispatchers.IO) {
                android.util.Log.d("HomeViewModel", "=== API DEBUG ===")
                android.util.Log.d("HomeViewModel", "visitorData: ${YouTube.visitorData}")
                android.util.Log.d("HomeViewModel", "locale.gl: ${YouTube.locale.gl}")
                android.util.Log.d("HomeViewModel", "locale.hl: ${YouTube.locale.hl}")
                android.util.Log.d("HomeViewModel", "cookie: ${YouTube.cookie?.take(50) ?: "null"}")
                
                YouTube.home().onSuccess { 
                    android.util.Log.d("HomeViewModel", "YouTube.home() SUCCESS")
                }.onFailure {
                    android.util.Log.e("HomeViewModel", "YouTube.home() FAILED: ${it.message}")
                    reportException(it)
                }.getOrNull()
            }

            // 3. YouTube Explore page
            val explorePageDeferred = async(Dispatchers.IO) {
                YouTube.explore().onFailure { reportException(it) }.getOrNull()
            }

            // 4. Get artists for similar recommendations (parallel fetch)
            val mostPlayedArtistsDeferred = async(Dispatchers.IO) {
                database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                    .filter { it.artist.isYouTubeArtist }
                    .shuffled().take(3)
            }

            // 5. Get songs for similar recommendations (parallel fetch)
            val mostPlayedSongsDeferred = async(Dispatchers.IO) {
                database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                    .filter { it.album != null }
                    .shuffled().take(2)
            }

            // Await primary data first for faster initial render
            accountPlaylists.value = accountPlaylistsDeferred?.await()
            homePage.value = homePageDeferred.await()
            
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

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()

            isLoading.value = false
            hasInitialLoadCompleted.set(true)
        } finally {
            loadMutex.unlock()
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