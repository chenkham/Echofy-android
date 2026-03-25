package com.Chenkham.Echofy.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.auth.AuthRepository
import com.Chenkham.Echofy.db.entities.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val user: StateFlow<UserEntity?> = authRepository.getActiveUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
