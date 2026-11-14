package com.Chenkham.Echofy.ui.main

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.player.PipPlayerView
import com.Chenkham.Echofy.ui.screens.settings.NavigationTab
import com.Chenkham.Echofy.ui.theme.EchofyTheme
import com.Chenkham.Echofy.utils.SyncUtils

/**
 * Top-level application composable hosting themes, PiP mode switching, wake-word, and scaffolding.
 */
@Composable
fun MainAppContent(
    activity: Activity,
    playerConnection: PlayerConnection?,
    isInPipMode: Boolean,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    syncUtils: SyncUtils,
    adManager: AdManager,
    latestVersionName: String,
    pendingJamRoomCode: String?,
    onClearJamRoomCode: () -> Unit,
    getLastInteractionTime: () -> Long,
    onResetInteractionTime: () -> Unit,
    searchActionTriggered: Boolean,
    shortcutDestinationTab: NavigationTab?,
) {
    val themeConfig = rememberMainThemeConfig(activity, playerConnection)
    val wakeWordAiManager = rememberMainWakeWordManager(activity, playerConnection)
    MainAppUpdaterAndAds(activity, adManager, playerConnection)

    EchofyTheme(
        darkTheme = themeConfig.useDarkTheme,
        pureBlack = themeConfig.pureBlack,
        themeColor = themeConfig.themeColor,
        useSystemFont = themeConfig.useSystemFont,
        appFont = themeConfig.appFont,
    ) {
        if (isInPipMode) {
            PipPlayerView(playerConnection = playerConnection, modifier = Modifier.fillMaxSize())
        } else {
            MainAppScaffold(
                activity = activity,
                playerConnection = playerConnection,
                database = database,
                downloadUtil = downloadUtil,
                syncUtils = syncUtils,
                adManager = adManager,
                latestVersionName = latestVersionName,
                pureBlack = themeConfig.pureBlack,
                pendingJamRoomCode = pendingJamRoomCode,
                onClearJamRoomCode = onClearJamRoomCode,
                getLastInteractionTime = getLastInteractionTime,
                onResetInteractionTime = onResetInteractionTime,
                searchActionTriggered = searchActionTriggered,
                shortcutDestinationTab = shortcutDestinationTab,
                wakeWordAiManager = wakeWordAiManager,
            )
        }
    }
}
