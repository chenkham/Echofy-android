package com.Chenkham.Echofy.ui.main

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.playback.PlayerConnection
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.minutes

/**
 * Handles periodic full-screen timed ads and Google Play In-App Updates.
 */
@Composable
fun MainAppUpdaterAndAds(
    activity: Activity,
    adManager: AdManager,
    playerConnection: PlayerConnection?,
) {
    // 30-minute Interval Ad Timer
    LaunchedEffect(activity, adManager, playerConnection) {
        while (true) {
            delay(30.minutes)
            withContext(Dispatchers.Main) {
                try {
                    if (!adManager.shouldShowAds()) {
                        Timber.d("Timer fired but user is premium, skipping ad")
                    } else if (playerConnection?.player?.isPlaying == true) {
                        Timber.d("Timer fired while playing, skipping ad to protect playback")
                    } else {
                        Timber.d("30-minute ad timer fired, showing ad")
                        adManager.showTimedAd(activity)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error showing timed ad")
                }
            }
        }
    }

    // Google Play In-App Updates check
    val appUpdateManager = remember(activity) { AppUpdateManagerFactory.create(activity) }
    LaunchedEffect(activity) {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        activity,
                        1001,
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for Google Play updates")
        }
    }
}
