package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.ytmusicapi.YTMusicApi
import com.Chenkham.ytmusicapi.models.PodcastLandingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.Chenkham.Echofy.constants.ContentCountryKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class PodcastsViewModel @Inject constructor(
    private val ytMusicApi: YTMusicApi,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _podcastPage = MutableStateFlow<PodcastLandingResult?>(null)
    val podcastPage: StateFlow<PodcastLandingResult?> = _podcastPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Observe country changes and reload podcasts
        viewModelScope.launch {
            dataStore.data
                .map { it[ContentCountryKey] ?: "ZZ" }
                .distinctUntilChanged()
                .collect {
                    loadPodcasts()
                }
        }
    }

    fun loadPodcasts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            ytMusicApi.getPodcastsLanding()
                .onSuccess { result ->
                    _podcastPage.value = result
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Unavailable"
                }
            
            _isLoading.value = false
        }
    }
}
