package com.Chenkham.Echofy.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.filterExplicit
import com.Chenkham.Echofy.constants.HideExplicitKey
import com.Chenkham.Echofy.constants.LastNewReleaseCheckKey
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewReleaseViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val database: MusicDatabase,
) : ViewModel() {
    private val _newReleaseAlbums = MutableStateFlow<List<AlbumItem>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    private val _hasNewReleases = MutableStateFlow(false)
    val hasNewReleases = _hasNewReleases.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadNewReleases()
    }

    fun loadNewReleases() {
        viewModelScope.launch {
            _isLoading.value = true
            YouTube
                .newReleaseAlbums()
                .onSuccess { albums ->
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }

                    val sortedAlbums = albums
                        .sortedBy { album ->
                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                            val firstArtistKey =
                                artistIds.firstNotNullOfOrNull { artistId ->
                                    if (artistId in favouriteArtists.values) {
                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                    } else {
                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                    }
                                } ?: Int.MAX_VALUE
                            firstArtistKey
                        }.filterExplicit(context.dataStore.get(HideExplicitKey, false))

                    _newReleaseAlbums.value = sortedAlbums
                    _isLoading.value = false

                    // Check for new releases
                    checkForNewReleases()

                }.onFailure {
                    _isLoading.value = false
                    reportException(it)
                }
        }
    }



    private suspend fun checkForNewReleases() {
        try {
            val lastCheckTime = context.dataStore.get(LastNewReleaseCheckKey, 0L)
            val currentTime = System.currentTimeMillis()

            // If first time checking, don't show notification
            if (lastCheckTime == 0L) {
                context.dataStore.updateData { it.toMutablePreferences().apply {
                    set(LastNewReleaseCheckKey, currentTime)
                }}
                _hasNewReleases.value = false
                return
            }

            // If there are albums and enough time has passed since last check
            val hasNewReleases = _newReleaseAlbums.value.isNotEmpty() &&
                    (currentTime - lastCheckTime) > (24 * 60 * 60 * 1000) // 24 hours

            _hasNewReleases.value = hasNewReleases

        } catch (e: Exception) {
            reportException(e)
            _hasNewReleases.value = false
        }
    }

    fun markNewReleasesAsSeen() {
        viewModelScope.launch {
            try {
                context.dataStore.updateData { it.toMutablePreferences().apply {
                    set(LastNewReleaseCheckKey, System.currentTimeMillis())
                }}
                _hasNewReleases.value = false
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }
}