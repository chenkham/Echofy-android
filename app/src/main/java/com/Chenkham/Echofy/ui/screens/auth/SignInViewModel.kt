package com.Chenkham.Echofy.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.auth.AuthRepository
import com.Chenkham.Echofy.ads.SubscriptionManager
import com.Chenkham.Echofy.db.entities.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SignInState {
    object Idle : SignInState()
    object Loading : SignInState()
    data class Success(val user: UserEntity) : SignInState()
    data class Error(val message: String) : SignInState()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {
    
    private val _signInState = MutableStateFlow<SignInState>(SignInState.Idle)
    val signInState: StateFlow<SignInState> = _signInState
    
    // TODO: Replace with your actual Web Client ID from Firebase Console
    private val WEB_CLIENT_ID = "1033902318161-c6qh9nvnnrvc5nql7031b4bcjotp07v6.apps.googleusercontent.com"
    
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _signInState.value = SignInState.Loading
            
            try {
                val result = authRepository.signInWithGoogle(WEB_CLIENT_ID, context)
                
                _signInState.value = result.fold(
                    onSuccess = { user -> 
                        // Restore purchases after successful login
                        // This checks Google Play for existing subscriptions
                        subscriptionManager.restorePurchases()
                        SignInState.Success(user) 
                    },
                    onFailure = { error -> 
                        android.util.Log.e("SignInViewModel", "Sign in failed", error)
                        SignInState.Error(error.message ?: "Sign in failed. Check network or configuration.") 
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("SignInViewModel", "Sign in exception", e)
                _signInState.value = SignInState.Error(e.message ?: "Unexpected error during sign in")
            }
        }
    }
}

