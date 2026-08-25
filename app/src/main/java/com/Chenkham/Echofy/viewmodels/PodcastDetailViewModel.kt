package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.ytmusicapi.YTMusicApi
import com.Chenkham.ytmusicapi.models.PodcastResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PodcastDetailViewModel @Inject constructor(
    private val ytMusicApi: YTMusicApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val podcastId: String? = savedStateHandle["podcastId"]

    private val _podcast = MutableStateFlow<PodcastResult?>(null)
    val podcast: StateFlow<PodcastResult?> = _podcast.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        if (podcastId != null) {
            loadPodcast(podcastId)
        } else {
            _error.value = "Podcast ID missing"
        }
    }

    private fun loadPodcast(rawId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val cleanId = rawId.removePrefix("VL")

            var success = false
            ytMusicApi.getPodcast(cleanId).onSuccess { result ->
                if (result.episodes.isNotEmpty() || result.title.isNotBlank()) {
                    _podcast.value = result
                    success = true
                }
            }

            if (!success) {
                com.arturo254.opentune.innertube.YouTube.playlist(cleanId).onSuccess { page ->
                    val episodes = page.songs.mapIndexed { index, song ->
                        com.Chenkham.ytmusicapi.models.Episode(
                            index = index + 1,
                            title = song.title,
                            description = song.artists.joinToString(", ") { it.name },
                            duration = song.duration?.let { sec -> "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}" },
                            videoId = song.id,
                            browseId = song.id,
                            videoType = "MUSIC_VIDEO_TYPE_OMV",
                            date = null,
                            thumbnails = listOf(com.Chenkham.ytmusicapi.models.Thumbnail(url = song.thumbnail.orEmpty(), width = 500, height = 500))
                        )
                    }
                    _podcast.value = com.Chenkham.ytmusicapi.models.PodcastResult(
                        author = page.playlist.author?.let { com.Chenkham.ytmusicapi.models.Author(name = it.name, id = it.id) },
                        title = page.playlist.title,
                        description = page.playlist.songCountText ?: "${episodes.size} episodes",
                        saved = false,
                        thumbnails = listOf(com.Chenkham.ytmusicapi.models.Thumbnail(url = page.playlist.thumbnail.orEmpty(), width = 500, height = 500)),
                        episodes = episodes
                    )
                    success = true
                }
            }

            if (_podcast.value == null || (_podcast.value?.title.isNullOrBlank() && _podcast.value?.episodes.isNullOrEmpty())) {
                _error.value = "Unable to load podcast episodes"
            }

            _isLoading.value = false
        }
    }
}
