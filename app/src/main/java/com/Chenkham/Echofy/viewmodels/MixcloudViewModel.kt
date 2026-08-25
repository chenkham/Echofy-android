package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.mixcloud.Mixcloud
import com.Chenkham.mixcloud.models.DjMix
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DJ mixes and radio shows from Mixcloud, shown alongside podcasts since both
 * are long-form spoken/curated audio.
 */
@HiltViewModel
class MixcloudViewModel
@Inject
constructor() : ViewModel() {
    private val _mixes = MutableStateFlow<List<DjMix>>(emptyList())
    val mixes: StateFlow<List<DjMix>> = _mixes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    val availableTags = Mixcloud.TAGS

    init {
        loadMixes()
    }

    /**
     * Loads popular mixes, or mixes for [selectedTag] when a genre chip is active.
     */
    fun loadMixes() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val tag = _selectedTag.value
            val result = if (tag == null) {
                Mixcloud.getPopular()
            } else {
                Mixcloud.getByTag(tag)
            }

            result
                .onSuccess { _mixes.value = it }
                .onFailure { _error.value = it.message ?: "Could not load mixes" }

            _isLoading.value = false
        }
    }

    fun selectTag(tag: String?) {
        if (_selectedTag.value == tag) return
        _selectedTag.value = tag
        loadMixes()
    }
}
