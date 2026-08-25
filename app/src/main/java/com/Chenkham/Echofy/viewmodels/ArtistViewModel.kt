package com.Chenkham.Echofy.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.pages.ArtistPage
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.enrichment.ArtistEnrichment
import com.Chenkham.Echofy.enrichment.ArtistEnrichmentRepository
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    private val enrichmentRepository: ArtistEnrichmentRepository,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = database.artistSongsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /**
     * Metadata from external catalogues: MusicBrainz facts, TheAudioDB biography
     * and artwork, Bandsintown tour dates, TasteDive similar artists.
     * Loaded separately from the YouTube page so an outage in any of them never
     * blocks the main artist screen from rendering.
     */
    private val _enrichment = MutableStateFlow(ArtistEnrichment())
    val enrichment: StateFlow<ArtistEnrichment> = _enrichment.asStateFlow()

    init {
        fetchArtistsFromYTM()
    }

    private fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            YouTube.artist(artistId)
                .onSuccess {
                    artistPage = it
                    // The YouTube page has no MusicBrainz id, so the artist name is
                    // the only key available to bridge the two catalogues.
                    fetchEnrichment(it.artist.title)
                }.onFailure {
                    reportException(it)
                }
        }
    }

    private fun fetchEnrichment(artistName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _enrichment.value = enrichmentRepository.enrich(artistName)
        }
    }
}
