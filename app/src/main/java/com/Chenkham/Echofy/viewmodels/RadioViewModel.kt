package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.radiobrowser.RadioBrowser
import com.Chenkham.radiobrowser.models.RadioStation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor() : ViewModel() {

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    init {
        loadDefaultTags()
        loadTopStations()
    }

    private fun loadDefaultTags() {
        _tags.value = listOf(
            "pop", "rock", "jazz", "classical", "news", "talk",
            "electronic", "hip-hop", "country", "80s", "90s", "sports"
        )
    }

    fun loadTopStations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedTag.value = null

            RadioBrowser.getTopStations(limit = 50)
                .onSuccess { result ->
                    _stations.value = result
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load stations"
                }

            _isLoading.value = false
        }
    }

    fun selectTag(tag: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedTag.value = tag

            RadioBrowser.getByTag(tag, limit = 50)
                .onSuccess { result ->
                    _stations.value = result
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load stations"
                }

            _isLoading.value = false
        }
    }

    fun searchByName(query: String) {
        if (query.isBlank()) {
            loadTopStations()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedTag.value = null

            RadioBrowser.searchByName(query, limit = 50)
                .onSuccess { result ->
                    _stations.value = result
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Search failed"
                }

            _isLoading.value = false
        }
    }
}
