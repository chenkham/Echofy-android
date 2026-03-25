package com.Chenkham.Echofy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.auth.AuthRepository
import com.Chenkham.Echofy.constants.IsGuestModeKey
import com.Chenkham.Echofy.db.entities.UserEntity
import com.Chenkham.Echofy.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: UserEntity) : AuthState()
    object Guest : AuthState()
    object Unauthenticated : AuthState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val isGuestMode = context.dataStore.data
        .map { it[IsGuestModeKey] ?: false }

    val authState: StateFlow<AuthState> = combine(
        authRepository.getActiveUser(),
        isGuestMode
    ) { user, isGuest ->
        when {
            user != null -> AuthState.Authenticated(user)
            isGuest -> AuthState.Guest
            else -> AuthState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading
    )
    
    val activeUser: StateFlow<UserEntity?> = authRepository.getActiveUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    suspend fun enableGuestMode() {
        context.dataStore.edit { preferences ->
            preferences[IsGuestModeKey] = true
        }
    }
    
    fun disableGuestMode() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[IsGuestModeKey] = false
            }
        }
    }
}
