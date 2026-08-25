package com.Chenkham.Echofy.viewmodels

import android.content.Context
import com.Chenkham.Echofy.constants.FreesoundApiKeyKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.freesound.Freesound
import com.Chenkham.freesound.models.AmbientSound
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Ambient audio from Freesound: rain, white noise, field recordings.
 *
 * Requires a user-supplied API key, so an unconfigured state is a normal
 * outcome rather than an error and is surfaced separately via [needsApiKey].
 */
@HiltViewModel
class AmbientSoundsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _sounds = MutableStateFlow<List<AmbientSound>>(emptyList())
    val sounds: StateFlow<List<AmbientSound>> = _sounds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _needsApiKey = MutableStateFlow(false)
    val needsApiKey: StateFlow<Boolean> = _needsApiKey.asStateFlow()

    private val _selectedCategory = MutableStateFlow(Freesound.CATEGORIES.keys.first())
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories = Freesound.CATEGORIES

    init {
        loadSounds()
    }

    fun loadSounds() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val apiKey = withContext(Dispatchers.IO) {
                context.dataStore[FreesoundApiKeyKey]?.takeIf { it.isNotBlank() }
            }

            if (apiKey == null) {
                _needsApiKey.value = true
                _sounds.value = emptyList()
                _isLoading.value = false
                return@launch
            }

            _needsApiKey.value = false

            Freesound.getCategory(_selectedCategory.value, apiKey)
                .onSuccess { _sounds.value = it }
                .onFailure { _error.value = it.message ?: "Could not load sounds" }

            _isLoading.value = false
        }
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        loadSounds()
    }
}
