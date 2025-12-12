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

    init {
        loadMoodAndGenres()
    }

    fun loadMoodAndGenres() {
        viewModelScope.launch {
            _isLoading.value = true
            YouTube
                .moodAndGenres()
                .onSuccess {
                    _moodAndGenres.value = it
                    _isLoading.value = false
                }.onFailure {
                    _isLoading.value = false
                    reportException(it)
                }
        }
    }
}
