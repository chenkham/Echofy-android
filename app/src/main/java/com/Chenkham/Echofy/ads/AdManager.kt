package com.Chenkham.Echofy.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AdMob ads throughout the app.
 * Shows ads for non-premium users.
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

        // AdMob asks that a placement refreshes at most once per 60 seconds. Anything faster
        // spends inventory without giving each ad time to register a viewable impression.
        private const val MIN_REFRESH_INTERVAL_MS = 60_000L
        
        val USE_TEST_ADS: Boolean
            get() = com.Chenkham.Echofy.BuildConfig.DEBUG
        
        val BANNER_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID
            
        val NATIVE_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_NATIVE_AD_UNIT_ID else PROD_NATIVE_AD_UNIT_ID
            
        val REWARDED_AD_UNIT_ID: String
            get() = if (USE_TEST_ADS) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Default is false so ads are enabled for standard users
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private var onDownloadAdComplete: (() -> Unit)? = null
    
    /**
     * Initialize the Mobile Ads SDK. Called in Application.onCreate()
     */
    fun initialize() {
        try {
            MobileAds.initialize(context) { initializationStatus ->
                Timber.d("AdMob Mobile Ads SDK Initialized successfully")
            }
            // Video ad creatives request audio focus when they play with sound, which
            // pauses whatever the user is listening to. Muting the SDK keeps every ad
            // silent so playback is never interrupted by an ad.
            MobileAds.setAppMuted(true)
            MobileAds.setAppVolume(0f)
        } catch (e: Exception) {
            Timber.e(e, "Error initializing MobileAds SDK")
        }
        
        _isInitialized.value = true

        loadRewardedAd()
    }
    
    /**
     * Ads are shown to all users.
     */
    fun shouldShowAds(): Boolean = true
    
    fun getUserPremiumFlow() = authRepository.getActiveUser()
    
    fun createBannerAdView(activityContext: Context): AdView {
        return AdView(activityContext).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
        }
    }
    
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
                loadRewardedAd()
                onDownloadAdComplete?.invoke()
                onDownloadAdComplete = null
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Timber.w("Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                onDownloadAdComplete?.invoke()
                onDownloadAdComplete = null
            }
            
            override fun onAdShowedFullScreenContent() {
                Timber.d("Rewarded ad shown")
            }
        }
    }
    
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
            Timber.d("Rewarded ad not ready, allowing download")
            loadRewardedAd()
            onComplete()
        }
    }

    fun showTimedAd(activity: Activity) {
        if (!shouldShowAds()) {
            Timber.d("Skipping timed ad: User is premium")
            return
        }
        
        rewardedAd?.let { ad ->
            onDownloadAdComplete = null 
            ad.show(activity) { rewardItem ->
                Timber.d("User earned reward from timed ad")
            }
        } ?: run {
            Timber.d("Timed ad not ready, reloading")
            loadRewardedAd()
        }
    }
    
    fun isDownloadAdReady(): Boolean = rewardedAd != null
    
    fun getNativeAdUnitId(): String = NATIVE_AD_UNIT_ID

    /**
     * Native ads were previously loaded inside the composable and destroyed by onDispose as
     * soon as the item scrolled out of the list. Scrolling back re-ran the effect and fired a
     * fresh request, so a single placement could burn many requests without ever producing a
     * countable impression. That wastes inventory and looks like invalid traffic, which drags
     * match rate and eCPM down.
     *
     * Caching the loaded ad here (this class is a @Singleton, so it outlives scrolling) means
     * one request serves every scroll pass. A slot only refreshes after MIN_REFRESH_INTERVAL_MS,
     * which respects AdMob's guidance of at most one refresh per 60 seconds.
     */
    private val nativeAdCache = mutableMapOf<String, CachedNativeAd>()
    private val nativeAdRequestsInFlight = mutableSetOf<String>()

    private data class CachedNativeAd(
        val ad: NativeAd,
        val loadedAtMs: Long,
    )

    private val _nativeAdVersion = MutableStateFlow(0)
    val nativeAdVersion: StateFlow<Int> = _nativeAdVersion.asStateFlow()

    fun getCachedNativeAd(slotId: String): NativeAd? = nativeAdCache[slotId]?.ad

    /**
     * Returns the cached ad for [slotId], requesting one only when the slot is empty or its ad
     * is older than [MIN_REFRESH_INTERVAL_MS]. Safe to call on every recomposition.
     */
    fun ensureNativeAdLoaded(slotId: String) {
        if (!shouldShowAds()) return

        val cached = nativeAdCache[slotId]
        val isStale = cached == null ||
            System.currentTimeMillis() - cached.loadedAtMs > MIN_REFRESH_INTERVAL_MS
        if (!isStale) return
        if (!nativeAdRequestsInFlight.add(slotId)) return

        AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                // Release the ad this slot was showing before replacing it, otherwise the old
                // native ad object leaks for the lifetime of the process.
                nativeAdCache[slotId]?.ad?.destroy()
                nativeAdCache[slotId] = CachedNativeAd(ad, System.currentTimeMillis())
                nativeAdRequestsInFlight.remove(slotId)
                _nativeAdVersion.value = _nativeAdVersion.value + 1
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Native ad failed to load for %s: %s", slotId, error.message)
                    nativeAdRequestsInFlight.remove(slotId)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /**
     * Destroys every cached native ad. Call when the host Activity is finishing so the ads are
     * released with it rather than on every scroll.
     */
    fun releaseNativeAds() {
        nativeAdCache.values.forEach { it.ad.destroy() }
        nativeAdCache.clear()
        nativeAdRequestsInFlight.clear()
    }
}
