package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.constants.OnboardingCompletedKey
import com.Chenkham.Echofy.constants.OnboardingSelectedArtistsKey
import com.Chenkham.Echofy.constants.OnboardingSelectedCountryKey
import com.Chenkham.Echofy.constants.OnboardingSelectedLanguageKey
import com.Chenkham.Echofy.utils.dataStore
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val application: android.app.Application
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry.asStateFlow()

    private val _selectedArtists = MutableStateFlow<Set<String>>(emptySet())
    val selectedArtists: StateFlow<Set<String>> = _selectedArtists.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _isNavigatingToLogin = MutableStateFlow(false)
    val isNavigatingToLogin: StateFlow<Boolean> = _isNavigatingToLogin.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < 3) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun skipAll() {
        // Skip all steps and mark onboarding as completed
        viewModelScope.launch {
            application.dataStore.edit { preferences ->
                preferences[OnboardingCompletedKey] = true
            }
        }
    }

    fun selectCountry(country: String) {
        _selectedCountry.value = country
    }

    fun toggleArtist(artistId: String) {
        val current = _selectedArtists.value.toMutableSet()
        if (current.contains(artistId)) {
            current.remove(artistId)
        } else {
            current.add(artistId)
        }
        _selectedArtists.value = current
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
    }

    fun navigateToLogin() {
        _isNavigatingToLogin.value = true
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            application.dataStore.edit { preferences ->
                preferences[OnboardingCompletedKey] = true
                _selectedCountry.value?.let { preferences[OnboardingSelectedCountryKey] = it }
                _selectedLanguage.value?.let { preferences[OnboardingSelectedLanguageKey] = it }
                
                // Store artists as comma-separated string
                val artistsString = _selectedArtists.value.joinToString(",")
                if (artistsString.isNotEmpty()) {
                    preferences[OnboardingSelectedArtistsKey] = artistsString
                }
            }
        }
    }

    fun canProceed(): Boolean {
        return when (_currentStep.value) {
            0 -> _selectedCountry.value != null
            1 -> _selectedArtists.value.isNotEmpty()
            2 -> _selectedLanguage.value != null
            3 -> true // Login step is always optional
            else -> false
        }
    }
}
