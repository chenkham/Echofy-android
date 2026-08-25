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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.Chenkham.Echofy.ads.AdManager
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Native ad card that blends with the app's design.
 * HIDDEN until ad is fully loaded to prevent blank spaces.
 *
 * [slotId] identifies the placement so each one keeps its own cached ad. Give every call site
 * a distinct, stable id.
 */
@Composable
fun NativeAdCard(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    slotId: String = "default"
) {
    if (!adManager.shouldShowAds()) return

    // The ad is owned by AdManager rather than this composable. Scrolling the item out of a
    // LazyColumn used to destroy the ad and scrolling back requested a new one, which spent
    // requests without producing impressions. Now the cached ad survives scrolling and only
    // refreshes on AdManager's own interval, and it is destroyed with the Activity instead.
    val adVersion by adManager.nativeAdVersion.collectAsState()
    val nativeAd = remember(slotId, adVersion) { adManager.getCachedNativeAd(slotId) }

    LaunchedEffect(slotId) {
        adManager.ensureNativeAdLoaded(slotId)
    }

    // ONLY show content if ad is successfully loaded
    if (nativeAd != null) {
        // The ad body is an Android View, so pull the theme colours out of Compose and
        // apply them manually — otherwise the text stays white and vanishes in light mode.
        val headlineColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

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
                            setTextColor(bodyColor)
                        }
                        adView.addView(adLabel)
                        
                        val headline = TextView(ctx).apply {
                            textSize = 16f
                            setTextColor(headlineColor)
                        }
                        headlineView = headline
                        adView.addView(headline)
                        
                        val body = TextView(ctx).apply {
                            textSize = 14f
                            setTextColor(bodyColor)
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
                    // factory only runs once, so re-apply colours here to follow theme changes.
                    (adView.headlineView as? TextView)?.setTextColor(headlineColor)
                    (adView.bodyView as? TextView)?.setTextColor(bodyColor)
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
