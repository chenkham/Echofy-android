package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.db.update
import com.Chenkham.Echofy.enrichment.ReleaseEnrichmentRepository
import com.Chenkham.Echofy.utils.reportException
import com.Chenkham.discogs.models.ReleaseInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val releaseEnrichmentRepository: ReleaseEnrichmentRepository,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    /**
     * Physical release details from Discogs. Loaded after the album resolves,
     * separately from the main content so it never delays the track list.
     */
    private val _releaseInfo = MutableStateFlow<ReleaseInfo?>(null)
    val releaseInfo: StateFlow<ReleaseInfo?> = _releaseInfo.asStateFlow()

    init {
        viewModelScope.launch {
            val album = database.album(albumId).first()
            YouTube
                .album(albumId)
                .onSuccess {
                    playlistId.value = it.album.playlistId
                    otherVersions.value = it.otherVersions
                    database.transaction {
                        if (album == null) {
                            insert(it)
                        } else {
                            update(album.album, it, album.artists)
                        }
                    }
                    fetchReleaseInfo(
                        artist = it.album.artists?.joinToString { artist -> artist.name }.orEmpty(),
                        album = it.album.title,
                    )
                }.onFailure {
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }
        }
    }

    private fun fetchReleaseInfo(artist: String, album: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _releaseInfo.value = releaseEnrichmentRepository.findRelease(artist, album)
        }
    }
}
