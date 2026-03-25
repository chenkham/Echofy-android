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

    private fun loadPodcast(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Depending on ID format, it might be a Playlist (New Episodes) or Channel or Podcast
            // Try getPodcast first as it covers most
            ytMusicApi.getPodcast(id)
                .onSuccess { result ->
                    _podcast.value = result
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Available offline"
                }

            _isLoading.value = false
        }
    }
}
