package com.Chenkham.Echofy.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.constants.RadioDefaultCountryKey
import com.Chenkham.Echofy.constants.RadioHideBrokenKey
import com.Chenkham.Echofy.constants.RadioMinBitrateKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.radiobrowser.RadioBrowser
import com.Chenkham.radiobrowser.models.RadioStation
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RadioViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _stations = MutableStateFlow<List<RadioStation>>(emptyList())
    val stations: StateFlow<List<RadioStation>> = _stations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String>("all")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    init {
        loadDefaultTags()
        val defaultCountry = context.dataStore[RadioDefaultCountryKey] ?: "all"
        _selectedCountry.value = defaultCountry
        loadStations()
    }

    private fun loadDefaultTags() {
        _tags.value = listOf(
            "pop", "rock", "jazz", "classical", "news", "talk",
            "electronic", "hip-hop", "country", "80s", "90s", "sports"
        )
    }

    fun selectCountry(countryCode: String) {
        _selectedCountry.value = countryCode
        _selectedTag.value = null
        loadStations()
    }

    fun loadStations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val country = _selectedCountry.value
            val tag = _selectedTag.value

            val result = when {
                tag != null -> RadioBrowser.getByTag(tag, limit = 50)
                country.isNotBlank() && country != "all" -> RadioBrowser.getByCountry(country, limit = 50)
                else -> RadioBrowser.getTopStations(limit = 50)
            }

            result
                .onSuccess { rawStations ->
                    val hideBroken = context.dataStore[RadioHideBrokenKey] ?: false
                    val minBitrateStr = context.dataStore[RadioMinBitrateKey] ?: "any"
                    val minBitrate = minBitrateStr.toIntOrNull() ?: 0

                    val filtered = rawStations.filter { station ->
                        if (hideBroken && station.lastCheckOk == 0) return@filter false
                        if (minBitrate > 0 && station.bitrate < minBitrate) return@filter false
                        true
                    }
                    _stations.value = filtered
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load stations"
                }

            _isLoading.value = false
        }
    }

    fun loadTopStations() {
        _selectedTag.value = null
        loadStations()
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
        loadStations()
    }

    fun searchByName(query: String) {
        if (query.isBlank()) {
            loadStations()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedTag.value = null

            RadioBrowser.searchByName(query, limit = 50)
                .onSuccess { result ->
                    val hideBroken = context.dataStore[RadioHideBrokenKey] ?: false
                    val minBitrateStr = context.dataStore[RadioMinBitrateKey] ?: "any"
                    val minBitrate = minBitrateStr.toIntOrNull() ?: 0

                    val filtered = result.filter { station ->
                        if (hideBroken && station.lastCheckOk == 0) return@filter false
                        if (minBitrate > 0 && station.bitrate < minBitrate) return@filter false
                        true
                    }
                    _stations.value = filtered
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Search failed"
                }

            _isLoading.value = false
        }
    }
}
