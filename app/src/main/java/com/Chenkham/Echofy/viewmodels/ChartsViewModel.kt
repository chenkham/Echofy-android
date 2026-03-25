package com.Chenkham.Echofy.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.constants.ChartCountryKey
import com.Chenkham.Echofy.constants.CountryCodeToName
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.ytmusicapi.YTMusicApi
import com.Chenkham.ytmusicapi.models.ChartsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.preferences.core.edit

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val application: Application,
    private val ytMusicApi: YTMusicApi
) : AndroidViewModel(application) {

    private val _charts = MutableStateFlow<ChartsResult?>(null)
    val charts: StateFlow<ChartsResult?> = _charts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedCountry = MutableStateFlow("ZZ")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    // Only countries provided by the YouTube Music API
    private val _countryList = MutableStateFlow<Map<String, String>>(mapOf("ZZ" to "Global"))
    val countryList: StateFlow<Map<String, String>> = _countryList.asStateFlow()

    init {
        // Load saved country preference
        val savedCountry = application.dataStore.get(ChartCountryKey, "ZZ")
        _selectedCountry.value = savedCountry
        loadCharts(savedCountry)
    }

    fun loadCharts(country: String = _selectedCountry.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _selectedCountry.value = country
            
            // Persist the selection
            application.dataStore.edit { preferences ->
                preferences[ChartCountryKey] = country
            }
            
            ytMusicApi.getCharts(country)
                .onSuccess { result ->
                    _charts.value = result
                    
                    // Build country list ONLY from API-provided options
                    val apiCountries = mutableMapOf<String, String>()
                    
                    // Always include Global
                    apiCountries["ZZ"] = "Global"
                    
                    // Add only countries provided by the API
                    result.countries.options.forEach { code ->
                        // Use our predefined country names, fallback to code if unknown
                        val name = CountryCodeToName[code] ?: code
                        apiCountries[code] = name
                    }
                    
                    // Sort by name (Global first, then alphabetically)
                    val sortedCountries = apiCountries.entries
                        .sortedWith(compareBy { 
                            if (it.key == "ZZ") "" else it.value 
                        })
                        .associate { it.key to it.value }
                    
                    _countryList.value = sortedCountries
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Available offline"
                }
            
            _isLoading.value = false
        }
    }
}
