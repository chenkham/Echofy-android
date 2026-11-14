package com.Chenkham.Echofy.ui.main

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.util.fastAny
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.LocalSyncUtils
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ai.EchofyAiManager
import com.Chenkham.Echofy.constants.AodAutoActivationKey
import com.Chenkham.Echofy.constants.DefaultOpenTabKey
import com.Chenkham.Echofy.constants.PauseSearchHistoryKey
import com.Chenkham.Echofy.constants.SearchSource
import com.Chenkham.Echofy.constants.SearchSourceKey
import com.Chenkham.Echofy.constants.SlimNavBarKey
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.SearchHistory
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.BottomSheetMenu
import com.Chenkham.Echofy.ui.component.LocalAdManager
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.MenuState
import com.Chenkham.Echofy.ui.screens.Screens
import com.Chenkham.Echofy.ui.screens.settings.NavigationTab
import com.Chenkham.Echofy.ui.utils.resetHeightOffset
import com.Chenkham.Echofy.utils.SyncUtils
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    activity: Activity,
    playerConnection: PlayerConnection?,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    syncUtils: SyncUtils,
    adManager: AdManager,
    latestVersionName: String,
    pureBlack: Boolean,
    pendingJamRoomCode: String?,
    onClearJamRoomCode: () -> Unit,
    getLastInteractionTime: () -> Long,
    onResetInteractionTime: () -> Unit,
    searchActionTriggered: Boolean,
    shortcutDestinationTab: NavigationTab?,
    wakeWordAiManager: EchofyAiManager,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val coroutineScope = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current
        val navController = rememberNavController()
        val navigationItems = remember { Screens.MainScreens }
        val slimNav by rememberPreference(SlimNavBarKey, defaultValue = false)
        val defaultOpenTab by rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
        val topLevelScreens = remember { listOf(Screens.Home.route, Screens.Explore.route, Screens.Library.route, "settings") }

        var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
        var active by rememberSaveable { mutableStateOf(false) }
        var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
        val searchBarFocusRequester = remember { FocusRequester() }
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        val state = rememberMainScaffoldState(
            maxHeight = maxHeight,
            navBackStackEntry = navBackStackEntry,
            active = active,
            navigationItems = navigationItems,
            playerConnection = playerConnection,
            onQuerySync = { query = it },
        )

        val onActiveChange: (Boolean) -> Unit = { newActive ->
            active = newActive
            if (!newActive) {
                focusManager.clearFocus()
                if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                    query = TextFieldValue()
                }
            }
        }

        val onSearch: (String) -> Unit = {
            if (it.isNotEmpty()) {
                onActiveChange(false)
                navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                if (activity.dataStore[PauseSearchHistoryKey] != true) {
                    database.query { insert(SearchHistory(query = it)) }
                }
            }
        }

        val aodTimeoutSecs by activity.dataStore.data.collectAsState(initial = null).value?.get(AodAutoActivationKey)?.let { mutableStateOf(it) } ?: rememberPreference(AodAutoActivationKey, 0)
        val aodIsPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
        MainAodAutoController(
            aodTimeoutSecs = aodTimeoutSecs,
            isPlaying = aodIsPlaying,
            navBackStackEntry = navBackStackEntry,
            navController = navController,
            getLastInteractionTime = getLastInteractionTime,
            onResetInteractionTime = onResetInteractionTime,
        )

        val shouldShowTopBar = !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
        val menuState = remember { MenuState() }

        val navigateToScreenAction: (Screens) -> Unit = { screen ->
            navController.navigate(screen.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        CompositionLocalProvider(
            LocalPlayerConnection provides playerConnection,
            LocalDownloadUtil provides downloadUtil,
            LocalDatabase provides database,
            LocalSyncUtils provides syncUtils,
            LocalAdManager provides adManager,
            LocalPlayerAwareWindowInsets provides state.playerAwareWindowInsets,
            LocalMenuState provides menuState,
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (state.shouldShowNavigationRail) {
                    MainNavigationRail(
                        navController = navController,
                        navigationItems = navigationItems,
                        navBackStackEntry = navBackStackEntry,
                        pureBlack = pureBlack,
                        slimNav = slimNav,
                        onNavigateToScreen = navigateToScreenAction,
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            MainScaffoldTopBar(
                                activity = activity,
                                navController = navController,
                                navBackStackEntry = navBackStackEntry,
                                shouldShowTopBar = shouldShowTopBar,
                                active = active,
                                onActiveChange = onActiveChange,
                                searchBarScrollBehavior = state.searchBarScrollBehavior,
                                latestVersionName = latestVersionName,
                                query = query,
                                onQueryChange = { query = it },
                                onSearch = onSearch,
                                navigationItems = navigationItems,
                                searchSource = searchSource,
                                onSearchSourceChange = { searchSource = it },
                                searchBarFocusRequester = searchBarFocusRequester,
                                pureBlack = pureBlack,
                                playerBottomSheetDismissed = state.playerBottomSheetState.isDismissed,
                            )
                        },
                        bottomBar = {
                            MainScaffoldBottomBar(
                                state = state,
                                navController = navController,
                                wakeWordAiManager = wakeWordAiManager,
                                pendingJamRoomCode = pendingJamRoomCode,
                                onDismissJamSheet = onClearJamRoomCode,
                                navigationItems = navigationItems,
                                navBackStackEntry = navBackStackEntry,
                                pureBlack = pureBlack,
                                slimNav = slimNav,
                                onActiveChange = onActiveChange,
                                onResetScroll = { coroutineScope.launch { runCatching { state.searchBarScrollBehavior.state.resetHeightOffset() } } },
                                onNavigateToScreen = navigateToScreenAction,
                            )
                        },
                        modifier = Modifier.fillMaxSize().nestedScroll(state.searchBarScrollBehavior.nestedScrollConnection),
                    ) {
                        MainNavHost(
                            navController = navController,
                            startDestinationRoute = when (shortcutDestinationTab ?: defaultOpenTab) {
                                NavigationTab.HOME -> Screens.Home
                                NavigationTab.EXPLORE -> Screens.Explore
                                NavigationTab.LIBRARY -> Screens.Library
                            }.route,
                            topLevelScreens = topLevelScreens,
                            navigationItems = navigationItems,
                            navBackStackEntry = navBackStackEntry,
                            searchBarScrollBehavior = state.searchBarScrollBehavior,
                            topAppBarScrollBehavior = state.topAppBarScrollBehavior,
                            latestVersionName = latestVersionName,
                            adManager = adManager,
                        )
                        BottomSheetMenu(state = menuState)
                    }
                }
            }

            LaunchedEffect(state.shouldShowBottomNav, searchActionTriggered) {
                if (state.shouldShowBottomNav && searchActionTriggered) {
                    onActiveChange(true)
                    delay(100)
                    runCatching { searchBarFocusRequester.requestFocus() }
                }
            }
        }
    }
}
