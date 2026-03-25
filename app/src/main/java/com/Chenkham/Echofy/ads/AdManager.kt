package com.Chenkham.Echofy.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.Chenkham.Echofy.utils.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Manages AdMob ads throughout the app.
 * Shows ads only for non-premium users.
 * When subscription is active, ads are hidden.
 * When subscription is cancelled, ads are shown again.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: com.Chenkham.Echofy.auth.AuthRepository
) {
    companion object {
        // Test Ad Unit IDs - Use these for development/testing
        const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        
        // Production Ad Unit IDs - Use these for release builds
        const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-9643799722679113/9678058702"
        const val PROD_NATIVE_AD_UNIT_ID = "ca-app-pub-9643799722679113/4389416465"
        const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-9643799722679113/1644398275"
        
        // Set to true for testing, automated based on build type
        val USE_TEST_ADS: Boolean
            get() = com.Chenkham.Echofy.BuildConfig.DEBUG
        
        // Get the appropriate ad unit IDs based on build mode
        val BANNER_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID
            
        val NATIVE_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID
            
        val REWARDED_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    }
    
    // Mock Subscription Key matching SubscriptionManager
    private val MockSubscriptionKey = booleanPreferencesKey("mock_premium_status")
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Cache the premium status as a public StateFlow for reactive UI checks
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    private var isPremiumUser: Boolean
        get() = _isPremium.value
        set(value) { _isPremium.value = value }

    private var rewardedAd: RewardedAd? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    // Callback for when download ad is completed
    private var onDownloadAdComplete: (() -> Unit)? = null
    
    /**
     * Initialize the Mobile Ads SDK. Call this in Application.onCreate()
     */
    fun initialize() {
        MobileAds.initialize(context) { initializationStatus ->
            Timber.d("AdMob initialized: $initializationStatus")
            
            _isInitialized.value = true
            
            // Start monitoring subscription status - merges Real User Status OR Test/Mock Status
            scope.launch {
                val realPremiumFlow = authRepository.getActiveUser().map { it?.isPremium == true }
                val testPremiumFlow = context.dataStore.data.map { preferences ->
                    preferences[MockSubscriptionKey] ?: false
                }
                
                // Combine: Premium if either Real OR Test is true
                combine(realPremiumFlow, testPremiumFlow) { real, test ->
                    real || test
                }.collectLatest { isPremium ->
                    isPremiumUser = isPremium
                    Timber.d("AdManager: Premium status updated to $isPremium")
                }
            }
            
            // Preload rewarded ad for downloads if needed
            // We check logic inside loadRewardedAd anyway
            if (shouldShowAds()) {
                loadRewardedAd()
            }
        }
    }
    
    /**
     * Check if ads should be shown based on user premium/subscription status.
     * Returns true if user is NOT premium (should show ads).
     * Returns false if user IS premium (hide ads).
     * 
     * This integrates with the subscription workflow:
     * - When user subscribes, isPremium becomes true -> no ads
     * - When user cancels subscription, isPremium becomes false -> ads show again
     */
    fun shouldShowAds(): Boolean {
        // Disable all ads (including test ads) on debug builds
        if (com.Chenkham.Echofy.BuildConfig.DEBUG) return false
        
        // Return true only if user is NOT premium
        return !isPremiumUser
    }
    
    /**
     * Check premium status asynchronously via the user flow.
     * Use this when you need to reactively check premium status.
     */
    fun getUserPremiumFlow() = authRepository.getActiveUser()
    
    /**
     * Creates a banner ad view ready to display
     */
    fun createBannerAdView(activityContext: Context): AdView {
        return AdView(activityContext).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
        }
    }
    
    /**
     * Creates an adaptive banner ad view that fits the screen width
     */
    fun createAdaptiveBannerAdView(width: Int): AdView {
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width)
        return AdView(context).apply {
            setAdSize(adSize)
            adUnitId = BANNER_AD_UNIT_ID
            if (shouldShowAds()) {
                loadAd(AdRequest.Builder().build())
            }
        }
    }
    
    /**
     * Loads a rewarded ad for download triggers
     */
    fun loadRewardedAd() {
        if (!shouldShowAds()) return
        if (rewardedAd != null) return
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.d("Rewarded ad loaded")
                    rewardedAd = ad
                    setupRewardedCallback()
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Rewarded ad failed to load: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }
    
    private fun setupRewardedCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Timber.d("Rewarded ad dismissed")
                rewardedAd = null
                loadRewardedAd() // Preload next one
                // Trigger download after ad
                onDownloadAdComplete?.invoke()
                onDownloadAdComplete = null
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                // Still allow download if ad fails
                onDownloadAdComplete?.invoke()
                onDownloadAdComplete = null
            }
            
            override fun onAdShowedFullScreenContent() {
                Timber.d("Rewarded ad shown")
            }
        }
    }
    
    /**
     * Shows a rewarded video ad before allowing download.
     * @param activity The current activity
     * @param onComplete Callback invoked after ad is watched (or skipped if not ready)
     */
    fun showDownloadAd(activity: Activity, onComplete: () -> Unit) {
        if (!shouldShowAds()) {
            onComplete()
            return
        }
        
        rewardedAd?.let { ad ->
            onDownloadAdComplete = onComplete
            ad.show(activity) { rewardItem ->
                Timber.d("User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        } ?: run {
            // Ad not ready, allow download anyway
            Timber.d("Rewarded ad not ready, allowing download")
            loadRewardedAd()
            onComplete()
        }
    }

    /**
     * Shows a timed rewarded ad (e.g. every 30 mins)
     */
    fun showTimedAd(activity: Activity) {
        if (!shouldShowAds()) {
            Timber.d("Skipping timed ad: User is premium")
            return
        }
        
        rewardedAd?.let { ad ->
            // For timed ads, we don't necessarily have a "completion" action other than resuming
            // But we can reset the callback
            onDownloadAdComplete = null 
            ad.show(activity) { rewardItem ->
                Timber.d("User earned reward from timed ad")
            }
        } ?: run {
            Timber.d("Timed ad not ready, reloading")
            loadRewardedAd()
        }
    }
    
    /**
     * Check if download ad is ready
     */
    fun isDownloadAdReady(): Boolean = rewardedAd != null
    
    /**
     * Gets the native ad unit ID for loading native ads
     */
    fun getNativeAdUnitId(): String = NATIVE_AD_UNIT_ID
}
