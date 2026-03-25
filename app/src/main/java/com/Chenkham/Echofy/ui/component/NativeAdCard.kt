package com.Chenkham.Echofy.ui.component

import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Chenkham.Echofy.ads.AdManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import timber.log.Timber

/**
 * Native ad card that blends with the app's design.
 * HIDDEN until ad is fully loaded to prevent blank spaces.
 */
@Composable
fun NativeAdCard(
    adManager: AdManager,
    modifier: Modifier = Modifier
) {
    if (!adManager.shouldShowAds()) return
    
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }
    
    // Load native ad
    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adManager.getNativeAdUnitId())
            .forNativeAd { ad ->
                nativeAd = ad
                isAdLoaded = true
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Native ad failed to load: ${error.message}")
                    isAdLoaded = false
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        
        adLoader.loadAd(AdRequest.Builder().build())
        
        onDispose {
            nativeAd?.destroy()
        }
    }
    
    // ONLY show content if ad is successfully loaded
    if (isAdLoaded && nativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                factory = { ctx ->
                    NativeAdView(ctx).apply {
                        val adView = android.widget.LinearLayout(ctx).apply {
                            orientation = android.widget.LinearLayout.VERTICAL
                            setPadding(16, 8, 16, 8)
                        }
                        
                        val adLabel = TextView(ctx).apply {
                            text = "Ad"
                            textSize = 10f
                            setTextColor(android.graphics.Color.GRAY)
                        }
                        adView.addView(adLabel)
                        
                        val headline = TextView(ctx).apply {
                            textSize = 16f
                            setTextColor(android.graphics.Color.WHITE)
                        }
                        headlineView = headline
                        adView.addView(headline)
                        
                        val body = TextView(ctx).apply {
                            textSize = 14f
                            setTextColor(android.graphics.Color.LTGRAY)
                            maxLines = 2
                        }
                        bodyView = body
                        adView.addView(body)
                        
                        val cta = Button(ctx).apply {
                            textSize = 12f
                        }
                        callToActionView = cta
                        adView.addView(cta)
                        
                        addView(adView)
                    }
                },
                update = { adView ->
                    nativeAd?.let { ad ->
                        (adView.headlineView as? TextView)?.text = ad.headline
                        (adView.bodyView as? TextView)?.text = ad.body
                        (adView.callToActionView as? Button)?.text = ad.callToAction
                        adView.setNativeAd(ad)
                    }
                }
            )
        }
    }
}
