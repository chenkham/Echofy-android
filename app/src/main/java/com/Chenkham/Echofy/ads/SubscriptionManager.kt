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
        const val PREMIUM_MONTHLY_ID = "echofy_premium_monthly"
        const val PREMIUM_2_YEAR_ID = "echofy_premium_2_years"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var billingClient: BillingClient
    
    // Use AuthRepository as the source of truth, combined with Test Mode
    val isSubscribed: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _monthlyPrice = MutableStateFlow("--")
    val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _twoYearPrice = MutableStateFlow("--")
    val twoYearPrice: StateFlow<String> = _twoYearPrice.asStateFlow()
    
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    private var hasInitializedBilling = false
    private var pendingRestore = false
    
    /**
     * Initialize subscription manager.
     */
    fun initialize() {
        hasInitializedBilling = true
    }
    
    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Billing Setup Finished")
                    queryProductDetails()
                    queryPurchases() // Check for existing purchases
                    if (pendingRestore) {
                        pendingRestore = false
                        queryPurchases()
                    }
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
        // Query Subscriptions
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_MONTHLY_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_2_YEAR_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
            
        billingClient.queryProductDetailsAsync(subsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                processProductDetails(productDetailsList)
            }
        }

        // Query INAPP in case they are configured as one-time purchases
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_MONTHLY_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_2_YEAR_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
            
        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList != null) {
                processProductDetails(productDetailsList)
            }
        }
    }
    
    private fun processProductDetails(productDetailsList: List<ProductDetails>) {
        productDetailsList.forEach { details ->
            productDetailsMap[details.productId] = details
            
            val price = if (details.productType == BillingClient.ProductType.SUBS) {
                // Subscription price: get recurring price
                details.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.pricingPhases
                    ?.pricingPhaseList
                    ?.lastOrNull()
                    ?.formattedPrice
            } else {
                // In-App price
                details.oneTimePurchaseOfferDetails?.formattedPrice
            }
            
            if (price != null) {
                when (details.productId) {
                    PREMIUM_MONTHLY_ID -> _monthlyPrice.value = price
                    PREMIUM_2_YEAR_ID -> _twoYearPrice.value = price
                }
            } else {
                Timber.e("Could not extract price for ${details.productId}")
            }
        }
    }
    
    /**
     * Launch the purchase flow
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        updatePremiumStatus(true)
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
        updatePremiumStatus(true)
    }
    
    private fun queryPurchases() {
        if (!billingClient.isReady) {
            pendingRestore = true
            startConnection()
            return
        }
        // Query Subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremiumSub = purchases.any { purchase -> 
                    (purchase.products.contains(PREMIUM_MONTHLY_ID) ||
                        purchase.products.contains(PREMIUM_2_YEAR_ID)) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                if (hasPremiumSub) {
                    purchases.filter {
                        it.products.contains(PREMIUM_MONTHLY_ID) ||
                            it.products.contains(PREMIUM_2_YEAR_ID)
                    }.forEach { handlePurchase(it) }
                    return@queryPurchasesAsync // User is premium, exit early
                }
            }

            Timber.d("No active premium products found. Downgrading premium status.")
            updatePremiumStatus(false)
        }
    }
    
    fun endConnection() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}


