package com.Chenkham.Echofy.ui.screens.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.auth.AuthRepository
import com.Chenkham.Echofy.ads.SubscriptionManager
import com.Chenkham.Echofy.db.entities.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {
    
    val activeUser: StateFlow<UserEntity?> = authRepository.getActiveUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val isPremium: StateFlow<Boolean> = subscriptionManager.isSubscribed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val subscriptionPrice = subscriptionManager.subscriptionPrice
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    fun enablePremium() {
        // Only for internal testing/mocking if needed, but primarily use billing
         viewModelScope.launch {
             // For now, this might be a debug option or removed
             // authRepository.setPremiumStatus(true)
         }
    }
    
    fun disablePremium() {
        viewModelScope.launch {
            authRepository.setPremiumStatus(false)
        }
    }

    fun launchBillingFlow(activity: android.app.Activity) {
        subscriptionManager.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        subscriptionManager.restorePurchases()
    }
}
