package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.utils.completed
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    init {
        viewModelScope.launch {
            YouTube.library("FEmusic_liked_playlists").completed().onSuccess { page ->
                playlists.value = page.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_liked_albums").completed().onSuccess { page ->
                albums.value = page.items.filterIsInstance<AlbumItem>()
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess { page ->
                artists.value = page.items.filterIsInstance<ArtistItem>()
            }.onFailure {
                reportException(it)
            }
        }
    }
}