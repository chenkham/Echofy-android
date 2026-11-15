package com.Chenkham.Echofy.ui.main

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavBackStackEntry
import com.Chenkham.Echofy.constants.AppBarHeight
import com.Chenkham.Echofy.constants.MiniPlayerHeight
import com.Chenkham.Echofy.constants.NavigationBarAnimationSpec
import com.Chenkham.Echofy.constants.NavigationBarHeight
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.rememberBottomSheetState
import com.Chenkham.Echofy.ui.screens.Screens
import com.Chenkham.Echofy.ui.utils.appBarScrollBehavior
import com.Chenkham.Echofy.ui.utils.resetHeightOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder

class MainScaffoldState
    @OptIn(ExperimentalMaterial3Api::class)
    constructor(
        val isLandscape: Boolean,
        val shouldShowBottomNav: Boolean,
        val shouldShowNavigationRail: Boolean,
        val navigationBarHeight: Dp,
        val bottomInset: Dp,
        val bottomInsetDp: Dp,
        val playerBottomSheetState: BottomSheetState,
        val playerAwareWindowInsets: WindowInsets,
        val searchBarScrollBehavior: TopAppBarScrollBehavior,
        val topAppBarScrollBehavior: TopAppBarScrollBehavior,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberMainScaffoldState(
    maxHeight: Dp,
    navBackStackEntry: NavBackStackEntry?,
    active: Boolean,
    navigationItems: List<Screens>,
    playerConnection: PlayerConnection?,
    onQuerySync: (TextFieldValue) -> Unit,
): MainScaffoldState {
    val density = LocalDensity.current
    val windowsInsets = WindowInsets.systemBars
    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp > configuration.screenHeightDp
    val shouldShowNavigationBar =
        navBackStackEntry?.destination?.route == null ||
            (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } && !active)
    val shouldShowBottomNav = !isLandscape && shouldShowNavigationBar
    val shouldShowNavigationRail = isLandscape && shouldShowNavigationBar

    val navigationBarHeight by animateDpAsState(
        targetValue = if (shouldShowBottomNav) NavigationBarHeight else 0.dp,
        animationSpec = NavigationBarAnimationSpec,
        label = "nav_bar_height",
    )

    val playerBottomSheetState =
        rememberBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = bottomInset + (if (shouldShowBottomNav) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
            expandedBound = maxHeight,
        )

    val playerAwareWindowInsets =
        remember(bottomInset, shouldShowBottomNav, playerBottomSheetState.isDismissed) {
            var bottom = bottomInset
            if (shouldShowBottomNav) bottom += NavigationBarHeight
            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
            windowsInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top).add(WindowInsets(top = AppBarHeight, bottom = bottom))
        }

    val searchBarScrollBehavior =
        appBarScrollBehavior(
            canScroll = {
                navBackStackEntry?.destination?.route?.startsWith("search/") == false && (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
            },
        )
    val topAppBarScrollBehavior = appBarScrollBehavior(canScroll = { false })

    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
            val searchQuery =
                withContext(Dispatchers.IO) {
                    val raw = navBackStackEntry?.arguments?.getString("query").orEmpty()
                    if (raw.contains("%")) raw else URLDecoder.decode(raw, "UTF-8")
                }
            onQuerySync(TextFieldValue(searchQuery, TextRange(searchQuery.length)))
        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
            onQuerySync(TextFieldValue())
        }
        searchBarScrollBehavior.state.resetHeightOffset()
        topAppBarScrollBehavior.state.resetHeightOffset()
    }

    LaunchedEffect(playerConnection) {
        val player = playerConnection?.player ?: return@LaunchedEffect
        if (player.currentMediaItem == null && !playerBottomSheetState.isDismissed) {
            playerBottomSheetState.dismiss()
        } else if (player.currentMediaItem != null && playerBottomSheetState.isDismissed) {
            playerBottomSheetState.collapseSoft()
        }
    }

    DisposableEffect(playerConnection, playerBottomSheetState) {
        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
        val listener =
            object : Player.Listener {
                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null && playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    return MainScaffoldState(
        isLandscape = isLandscape,
        shouldShowBottomNav = shouldShowBottomNav,
        shouldShowNavigationRail = shouldShowNavigationRail,
        navigationBarHeight = navigationBarHeight,
        bottomInset = bottomInset,
        bottomInsetDp = bottomInsetDp,
        playerBottomSheetState = playerBottomSheetState,
        playerAwareWindowInsets = playerAwareWindowInsets,
        searchBarScrollBehavior = searchBarScrollBehavior,
        topAppBarScrollBehavior = topAppBarScrollBehavior,
    )
}
