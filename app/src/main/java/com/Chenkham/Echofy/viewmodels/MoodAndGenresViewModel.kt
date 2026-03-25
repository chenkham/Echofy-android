package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.pages.MoodAndGenres
import com.Chenkham.Echofy.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoodAndGenresViewModel
@Inject
constructor() : ViewModel() {
    private val _moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)
    val moodAndGenres = _moodAndGenres.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadMoodAndGenres()
    }

    fun retry() {
        loadMoodAndGenres()
    }

    fun loadMoodAndGenres() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            YouTube
                .moodAndGenres()
                .onSuccess {
                    _moodAndGenres.value = it
                    _isLoading.value = false
                }.onFailure {
                    _isLoading.value = false
                    _error.value = it.message ?: "Unknown error"
                    reportException(it)
                }
        }
    }
}
