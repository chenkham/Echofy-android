package com.Chenkham.Echofy.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Chenkham.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.Chenkham.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.Chenkham.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.Chenkham.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.Chenkham.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.Chenkham.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.ArtistItem
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.innertube.models.YTItem
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.SearchHistory
import com.Chenkham.Echofy.constants.AppBarHeight
import com.Chenkham.Echofy.constants.SearchFilterHeight
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.ChipsRow
import com.Chenkham.Echofy.ui.component.EmptyPlaceholder
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.NavigationTitle
import com.Chenkham.Echofy.ui.component.YouTubeListItem
import com.Chenkham.Echofy.ui.component.NativeAdCard
import com.Chenkham.Echofy.ui.component.shimmer.ListItemPlaceHolder
import com.Chenkham.Echofy.ui.component.shimmer.ShimmerHost
import com.Chenkham.Echofy.ui.menu.YouTubeAlbumMenu
import com.Chenkham.Echofy.ui.menu.YouTubeArtistMenu
import com.Chenkham.Echofy.ui.menu.YouTubePlaylistMenu
import com.Chenkham.Echofy.ui.menu.YouTubeSongMenu
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.launch
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.ui.component.BackpaperBackground

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
    adManager: AdManager? = null,
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    BackpaperBackground(screen = BackpaperScreen.SEARCH) {
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            menuState.show {
                when (item) {
                    is SongItem ->
                        YouTubeSongMenu(
                            song = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is AlbumItem ->
                        YouTubeAlbumMenu(
                            albumItem = item,
                            navController = navController,
                            onDismiss = menuState::dismiss,
                        )

                    is ArtistItem ->
                        YouTubeArtistMenu(
                            artist = item,
                            onDismiss = menuState::dismiss,
                        )

                    is PlaylistItem ->
                        YouTubePlaylistMenu(
                            playlist = item,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive =
                when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(
                    onClick = longClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
                Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata()
                                            ),
                                        )
                                    }
                                    menuState.dismiss()
                                }

                                is AlbumItem -> {
                                    // Add to history
                                    val query = item.title
                                    coroutineScope.launch {
                                        database.query {
                                            insert(SearchHistory(query = query))
                                        }
                                    }
                                    navController.navigate("album/${item.id}")
                                    menuState.dismiss()
                                }

                                is ArtistItem -> {
                                    // Add to history
                                    val query = item.title
                                    coroutineScope.launch {
                                        database.query {
                                            insert(SearchHistory(query = query))
                                        }
                                    }
                                    navController.navigate("artist/${item.id}")
                                    menuState.dismiss()
                                }

                                is PlaylistItem -> {
                                    // Add to history
                                    val query = item.title
                                    coroutineScope.launch {
                                        database.query {
                                            insert(SearchHistory(query = query))
                                        }
                                    }
                                    navController.navigate("online_playlist/${item.id}")
                                    menuState.dismiss()
                                }
                            }
                        },
                        onLongClick = longClick,
                    )
                    .animateItem(),
        )
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .add(WindowInsets(top = SearchFilterHeight))
                .add(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .asPaddingValues(),
    ) {
        if (searchFilter == null) {
            searchSummary?.summaries?.forEach { summary ->
                item {
                    NavigationTitle(summary.title)
                }

                items(
                    items = summary.items,
                    key = { "${summary.title}/${it.id}" },
                    itemContent = ytItemContent,
                )
                
                // Ad after each category
                adManager?.let {
                    item(key = "ad_${summary.title}") {
                        NativeAdCard(adManager = it)
                    }
                }
            }

            if (searchSummary?.summaries?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        } else {
            val distinctItems = itemsPage?.items.orEmpty().distinctBy { it.id }
            itemsIndexed(
                items = distinctItems,
                key = { _, item -> item.id },
            ) { index, item ->
                ytItemContent(item)
                
                // Show ad every 5 items
                if ((index + 1) % 5 == 0 && adManager != null) {
                    NativeAdCard(adManager = adManager)
                }
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
                }
            }
        }
    }

    ChipsRow(
        chips =
            listOf(
                null to stringResource(R.string.filter_all),
                FILTER_SONG to stringResource(R.string.filter_songs),
                FILTER_VIDEO to stringResource(R.string.filter_videos),
                FILTER_ALBUM to stringResource(R.string.filter_albums),
                FILTER_ARTIST to stringResource(R.string.filter_artists),
                FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
            ),
        currentValue = searchFilter,
        onValueUpdate = {
            if (viewModel.filter.value != it) {
                viewModel.filter.value = it
            }
            coroutineScope.launch {
                lazyListState.animateScrollToItem(0)
            }
        },
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                        .add(WindowInsets(top = AppBarHeight))
                )
                .fillMaxWidth()
    )
    }
}