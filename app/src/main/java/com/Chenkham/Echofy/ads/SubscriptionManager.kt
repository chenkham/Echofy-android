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
        const val PREMIUM_5_YEAR_ID = "echofy_premium_5_years" // Treat as INAPP
        const val PREMIUM_LIFETIME_ID = "echofy_premium_lifetime" // Treat as INAPP
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private lateinit var billingClient: BillingClient
    
    // Use AuthRepository as the source of truth, combined with Test Mode
    val isSubscribed: StateFlow<Boolean> = combine(
        authRepository.getActiveUser().map { it?.isPremium == true },
        context.dataStore.data.map { it[booleanPreferencesKey("mock_premium_status")] ?: false }
    ) { real, test ->
        real || test || com.Chenkham.Echofy.BuildConfig.DEBUG
    }.stateIn(mainScope, SharingStarted.WhileSubscribed(5000), false)
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _monthlyPrice = MutableStateFlow("₹15")
    val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _fiveYearPrice = MutableStateFlow("₹599")
    val fiveYearPrice: StateFlow<String> = _fiveYearPrice.asStateFlow()

    private val _lifetimePrice = MutableStateFlow("₹1999")
    val lifetimePrice: StateFlow<String> = _lifetimePrice.asStateFlow()
    
    private val productDetailsMap = mutableMapOf<String, ProductDetails>()
    
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
        // Query Monthly Subscription
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_MONTHLY_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
            
        billingClient.queryProductDetailsAsync(subsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                    if (details.productId == PREMIUM_MONTHLY_ID) {
                        val price = details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                        if (price != null) _monthlyPrice.value = price
                    }
                }
            }
        }

        // Query INAPP products (5 Year and Lifetime)
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_5_YEAR_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PREMIUM_LIFETIME_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(inAppParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList.forEach { details ->
                    productDetailsMap[details.productId] = details
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) {
                        when (details.productId) {
                            PREMIUM_5_YEAR_ID -> _fiveYearPrice.value = price
                            PREMIUM_LIFETIME_ID -> _lifetimePrice.value = price
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Launch the purchase flow
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = productDetailsMap[productId] ?: run {
            Timber.e("Cannot launch purchase flow: Product details not loaded for $productId")
            return
        }
        
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            
        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
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
        // Query Subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        billingClient.queryPurchasesAsync(subsParams) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremiumSub = purchases.any { purchase -> 
                    purchase.products.contains(PREMIUM_MONTHLY_ID) && 
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                if (hasPremiumSub) {
                    purchases.filter { it.products.contains(PREMIUM_MONTHLY_ID) }.forEach { handlePurchase(it) }
                    return@queryPurchasesAsync // User is premium, exit early
                }
            }
            
            // If not found in subs, check INAPP (5-year and lifetime)
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
                
            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasLifetimeOr5Year = inAppPurchases.any { purchase -> 
                        (purchase.products.contains(PREMIUM_5_YEAR_ID) || purchase.products.contains(PREMIUM_LIFETIME_ID)) && 
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    
                    if (hasLifetimeOr5Year) {
                        inAppPurchases.filter { 
                            it.products.contains(PREMIUM_5_YEAR_ID) || it.products.contains(PREMIUM_LIFETIME_ID) 
                        }.forEach { handlePurchase(it) }
                    } else {
                        // No active subscription or lifetime found -> Downgrade
                        Timber.d("No active premium products found. Downgrading premium status.")
                        updatePremiumStatus(false)
                    }
                }
            }
        }
    }
    
    fun endConnection() {
        billingClient.endConnection()
    }
}


