package com.Chenkham.Echofy.ui.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshSupporters()
    }

    fun refreshSupporters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val topSupporters = donationRepository.loadTopSupporters()
                val latestSupporters = donationRepository.loadLatestSupporters()
                _uiState.update {
                    it.copy(
                        topSupporters = topSupporters,
                        latestSupporters = latestSupporters,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load supporters",
                    )
                }
            }
        }
    }
}

data class SupportUiState(
    val topSupporters: List<DonationSupporter> = emptyList(),
    val latestSupporters: List<DonationSupporter> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
