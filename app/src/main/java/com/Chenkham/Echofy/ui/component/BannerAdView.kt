package com.Chenkham.Echofy.ui.component

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Chenkham.Echofy.ads.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import timber.log.Timber

/**
 * Composable wrapper for AdMob banner ads.
 * Utilizes View visibility effectively to prevent blank spaces.
 */
@Composable
fun BannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    if (!adManager.shouldShowAds()) return
    
    val context = LocalContext.current
    
    // Create AdView once and remember it
    val adView = remember {
        try {
            adManager.createBannerAdView(context).apply {
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        visibility = View.VISIBLE
                        Timber.d("Banner ad loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        super.onAdFailedToLoad(error)
                        visibility = View.GONE
                        Timber.w("Banner ad failed to load: ${error.message}")
                    }
                }
                // Defer loadAd to avoid blocking Compose's main thread instantiation
                post {
                    loadAd(AdRequest.Builder().build())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create Banner AdView")
            null
        }
    }

    // Cleanup when Composable is disposed
    DisposableEffect(adView) {
        onDispose {
            try {
                adView?.destroy()
                Timber.d("Banner AdView destroyed")
            } catch (e: Exception) {
                Timber.e(e, "Error destroying Banner AdView")
            }
        }
    }

    if (adView != null) {
        // Always render AndroidView but manage visibility internally
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                // Create a FrameLayout container
                val container = FrameLayout(ctx)
                
                // Remove from parent if needed (in case of reuse, though unlikely with remember)
                (adView.parent as? ViewGroup)?.removeView(adView)
                
                // Add AdView to container
                container.addView(adView)
                container
            }
        )
    }
}

/**
 * Standard fixed-size banner ad (320x50)
 */
@Composable
fun StandardBannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    BannerAdView(adManager, modifier)
}

/**
 * Medium Rectangle ad (300x250) - Square-ish ad for player album area
 */
@Composable
fun MediumRectangleAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    if (!adManager.shouldShowAds()) return

    AndroidView(
        modifier = modifier
            .width(300.dp), // Height not forced to allow GONE to collapse
        factory = { ctx ->
            val container = FrameLayout(ctx)
            
            val adView = AdView(ctx).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                adUnitId = AdManager.BANNER_AD_UNIT_ID // Using banner unit ID as fallback/test
                visibility = View.VISIBLE // VISIBLE by default
                
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        visibility = View.VISIBLE
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        visibility = View.GONE
                    }
                }
                post {
                    loadAd(AdRequest.Builder().build())
                }
            }
            
            container.addView(adView)
            container
        }
    )
}

/**
 * Large Banner ad (320x100)
 */
@Composable
fun LargeBannerAdView(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    if (!adManager.shouldShowAds()) return
    
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            val container = FrameLayout(ctx)
            val adView = AdView(ctx).apply {
                setAdSize(AdSize.LARGE_BANNER)
                adUnitId = AdManager.BANNER_AD_UNIT_ID
                visibility = View.VISIBLE
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                         visibility = View.VISIBLE
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                         visibility = View.GONE
                    }
                }
                post {
                    loadAd(AdRequest.Builder().build())
                }
            }
            container.addView(adView)
            container
        }
    )
}
