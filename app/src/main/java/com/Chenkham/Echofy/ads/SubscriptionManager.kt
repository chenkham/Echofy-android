package com.Chenkham.Echofy.ads

import android.app.Activity
import android.content.Context
import com.Chenkham.Echofy.auth.AuthRepository
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.Chenkham.Echofy.utils.dataStore

/**
 * Manages Google Play Billing for premium subscription.
 * Handles purchase flow, subscription status, and restoration.
 */
@Singleton
class SubscriptionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) : PurchasesUpdatedListener {
    
    companion object {
        // Subscription product ID - must match Google Play Console
        const val PREMIUM_SUBSCRIPTION_ID = "echofy_premium_monthly"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var billingClient: BillingClient
    
    // Use AuthRepository as the source of truth, combined with Test Mode
    val isSubscribed: StateFlow<Boolean> = combine(
        authRepository.getActiveUser().map { it?.isPremium == true },
        context.dataStore.data.map { it[booleanPreferencesKey("mock_premium_status")] ?: false }
    ) { real, test ->
        real || test
    }.stateIn(mainScope, SharingStarted.WhileSubscribed(5000), false)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _subscriptionPrice = MutableStateFlow("Loading...")
    val subscriptionPrice: StateFlow<String> = _subscriptionPrice.asStateFlow()
    
    private var productDetails: ProductDetails? = null
    
    /**
     * Initialize subscription manager.
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
            
        startConnection()
    }
    
    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Billing Setup Finished")
                    queryProductDetails()
                    queryPurchases() // Check for existing purchases
                } else {
                    Timber.e("Billing Setup Failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Timber.w("Billing Service Disconnected. Retrying...")
                // Retry logic could be added here
            }
        })
    }
    
    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_SUBSCRIPTION_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
            
        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    productDetails = productDetailsList[0]
                    productDetails?.subscriptionOfferDetails?.let { offers ->
                        // Find the base plan or offer (checking for first available for now)
                        val offer = offers.firstOrNull()
                        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        _subscriptionPrice.value = price ?: "Unavailable"
                    }
                } else {
                    Timber.w("No product details found for $PREMIUM_SUBSCRIPTION_ID")
                    _subscriptionPrice.value = "Unavailable"
                }
            }
        }
    }
    
    /**
     * Launch the purchase flow
     */
    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails ?: run {
            Timber.e("Cannot launch purchase flow: Product details not loaded")
            return
        }
        
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
            
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("Failed to launch billing flow: ${billingResult.debugMessage}")
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Timber.d("User canceled purchase")
        } else {
            Timber.e("Purchase error: ${billingResult.debugMessage}")
        }
    }
    
    private fun handlePurchase(purchase: Purchase) {
        // Here you would normally verify the purchase with your backend
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                    
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Timber.d("Purchase acknowledged successfully")
                        // Update app state
                        updatePremiumStatus(true)
                    }
                }
            } else {
                updatePremiumStatus(true)
            }
        }
    }
    
    private fun updatePremiumStatus(isPremium: Boolean) {
        scope.launch {
            authRepository.setPremiumStatus(isPremium)
        }
    }
    
    /**
     * Restore purchases
     */
    fun restorePurchases() {
        queryPurchases()
    }
    
    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                // Check if our subscription is in the list
                val hasPremium = purchases.any { purchase -> 
                    purchase.products.contains(PREMIUM_SUBSCRIPTION_ID) && 
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                // If found, ensure it's acknowledged and update intent
                if (hasPremium) {
                    purchases.filter { it.products.contains(PREMIUM_SUBSCRIPTION_ID) }.forEach { handlePurchase(it) }
                } else {
                    // No active subscription found on Google Play for this user -> Downgrade status
                    Timber.d("No active subscription found. Downgrading premium status.")
                    updatePremiumStatus(false)
                }
            }
        }
    }
    
    fun endConnection() {
        billingClient.endConnection()
    }
}


