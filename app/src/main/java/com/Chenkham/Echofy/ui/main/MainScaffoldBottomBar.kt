package com.Chenkham.Echofy.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.Chenkham.Echofy.ai.EchofyAiManager
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.constants.PlayerLayoutStyleKey
import com.Chenkham.Echofy.ui.screens.Screens
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.arturo254.opentune.innertube.models.SongItem

@Composable
fun MainScaffoldBottomBar(
    state: MainScaffoldState,
    navController: NavHostController,
    wakeWordAiManager: EchofyAiManager,
    pendingJamRoomCode: String?,
    onDismissJamSheet: () -> Unit,
    navigationItems: List<Screens>,
    navBackStackEntry: NavBackStackEntry?,
    pureBlack: Boolean,
    slimNav: Boolean,
    onActiveChange: (Boolean) -> Unit,
    onResetScroll: () -> Unit,
    onNavigateToScreen: (Screens) -> Unit,
) {
    var showFullscreenLyrics by rememberSaveable { mutableStateOf(false) }
    var sharedSong by remember { mutableStateOf<SongItem?>(null) }
    val playerLayoutStyle by rememberEnumPreference(PlayerLayoutStyleKey, defaultValue = PlayerLayoutStyle.CLASSIC)
    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val insetBg = if (state.playerBottomSheetState.progress > 0f) Color.Transparent else baseBg

    MainPlayerAndAiOverlays(
        playerBottomSheetState = state.playerBottomSheetState,
        playerLayoutStyle = playerLayoutStyle,
        navController = navController,
        showFullscreenLyrics = showFullscreenLyrics,
        onShowFullscreenLyricsChange = { showFullscreenLyrics = it },
        wakeWordAiManager = wakeWordAiManager,
        shouldShowBottomNav = state.shouldShowBottomNav,
        bottomInset = state.bottomInset,
        sharedSong = sharedSong,
        onDismissSharedSong = { sharedSong = null },
        pendingJamRoomCode = pendingJamRoomCode,
        onDismissJamSheet = onDismissJamSheet,
    )
    if (state.shouldShowBottomNav) {
        MainBottomNavigationBar(
            navController = navController,
            navigationItems = navigationItems,
            navBackStackEntry = navBackStackEntry,
            navigationBarHeight = state.navigationBarHeight,
            bottomInset = state.bottomInset,
            bottomInsetDp = state.bottomInsetDp,
            playerBottomSheetProgress = state.playerBottomSheetState.progress,
            pureBlack = pureBlack,
            insetBg = insetBg,
            slimNav = slimNav,
            onActiveChange = onActiveChange,
            onResetScroll = onResetScroll,
            onNavigateToScreen = onNavigateToScreen,
        )
    }
}
