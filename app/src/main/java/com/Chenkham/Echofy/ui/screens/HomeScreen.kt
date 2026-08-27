package com.Chenkham.Echofy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.Chenkham.Echofy.ui.component.PlayingIndicator


import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.Chenkham.Echofy.ui.component.ExpressivePullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.Chenkham.Echofy.ui.component.BannerAdView
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.ArtistItem
import com.arturo254.opentune.innertube.models.PlaylistItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.innertube.models.YTItem
import com.arturo254.opentune.innertube.utils.parseCookieString
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.GridThumbnailHeight
import com.Chenkham.Echofy.constants.HomeRow
import com.Chenkham.Echofy.constants.HomeRowOrderKey
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.constants.ListItemHeight
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.Album
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.db.entities.LocalItem
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.LocalAlbumRadio
import com.Chenkham.Echofy.playback.queues.YouTubeAlbumRadio
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.AlbumGridItem
import com.Chenkham.Echofy.ui.component.ArtistGridItem
import com.Chenkham.Echofy.ui.component.ChipsRow
import com.Chenkham.Echofy.ui.component.HideOnScrollFAB
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.NavigationTitle
import com.Chenkham.Echofy.ui.component.SongGridItem
import com.Chenkham.Echofy.ui.component.SongListItem
import com.Chenkham.Echofy.ui.component.YouTubeGridItem
import com.Chenkham.Echofy.ui.component.PrefetchOnVisible
import com.Chenkham.Echofy.ui.component.shimmer.GridItemPlaceHolder
import com.Chenkham.Echofy.ui.component.shimmer.ShimmerHost
import com.Chenkham.Echofy.ui.component.shimmer.TextPlaceholder
import com.Chenkham.Echofy.ui.menu.AlbumMenu
import com.Chenkham.Echofy.ui.menu.ArtistMenu
import com.Chenkham.Echofy.ui.menu.SongMenu
import com.Chenkham.Echofy.ui.menu.YouTubeAlbumMenu
import com.Chenkham.Echofy.ui.menu.YouTubeArtistMenu
import com.Chenkham.Echofy.ui.menu.YouTubePlaylistMenu
import com.Chenkham.Echofy.ui.menu.YouTubeSongMenu
import com.Chenkham.Echofy.ui.utils.SnapLayoutInfoProvider
import com.Chenkham.Echofy.ui.utils.ImmutablePlaybackInfo
import com.Chenkham.Echofy.ui.utils.ImmutableLibraryInfo
import com.Chenkham.Echofy.utils.rememberPreference
import com.Chenkham.Echofy.viewmodels.HomeViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ui.component.NativeAdCard
import com.Chenkham.Echofy.ui.component.BackpaperBackground
import com.Chenkham.Echofy.constants.BackpaperScreen

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    adManager: AdManager? = null,
) {
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    // PERFORMANCE: Collect state with proper lifecycle awareness
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    // PERFORMANCE: Create immutable state objects to minimize recomposition
    // Using derivedStateOf ensures this only recomputes when actual values change
    val playbackInfo by remember {
        derivedStateOf {
            ImmutablePlaybackInfo(
                currentMediaId = mediaMetadata?.id,
                currentAlbumId = mediaMetadata?.album?.id,
                isPlaying = isPlaying
            )
        }
    }

    // PERFORMANCE: Collect ViewModel state efficiently
    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val hiddenGems by viewModel.hiddenGems.collectAsState()
    val becauseYouListenedArtist by viewModel.becauseYouListenedArtist.collectAsState()
    val becauseYouListenedSongs by viewModel.becauseYouListenedSongs.collectAsState()
    val moodPlaylist by viewModel.moodPlaylist.collectAsState()
    val moodSongs by viewModel.moodSongs.collectAsState()
    val timeMachine by viewModel.timeMachine.collectAsState()
    val timeMachineYear by viewModel.timeMachineYear.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val topCharts by viewModel.topCharts.collectAsState()
    val viral50 by viewModel.viral50.collectAsState()
    val showTopChartsHome by rememberPreference(com.Chenkham.Echofy.constants.ShowTopChartsHomeKey, defaultValue = true)
    val showViral50Home by rememberPreference(com.Chenkham.Echofy.constants.ShowViral50HomeKey, defaultValue = true)

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    // PERFORMANCE: These are already optimized in ViewModel with stateIn(SharingStarted.Eagerly)
    val likedSongIds by viewModel.likedSongIds.collectAsState()
    val librarySongIds by viewModel.librarySongIds.collectAsState()
    val bookmarkedAlbumIds by viewModel.bookmarkedAlbumIds.collectAsState()

    // PERFORMANCE: Create immutable library info to pass to child composables
    val libraryInfo by remember {
        derivedStateOf {
            ImmutableLibraryInfo(
                likedSongIds = likedSongIds,
                librarySongIds = librarySongIds,
                bookmarkedAlbumIds = bookmarkedAlbumIds
            )
        }
    }

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
    val homeRowOrderRaw by rememberPreference(HomeRowOrderKey, "")
    // Stored rows first, in the user's order; anything unknown (a row added in a later
    // release, or one the user hid) is appended so it is never silently lost.
    val homeRowOrder = remember(homeRowOrderRaw) {
        val stored = homeRowOrderRaw.split(",")
            .mapNotNull { name -> HomeRow.entries.find { it.name == name.trim() } }
        stored + HomeRow.entries.filterNot { it in stored }
    }
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")

    // PERFORMANCE: Cache computed values with remember
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = remember(isLoggedIn, accountImageUrl) {
        if (isLoggedIn) accountImageUrl else null
    }

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = lazylistState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreSections()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    // PERFORMANCE: Use playbackInfo and libraryInfo for minimal remember keys
    // These immutable objects bundle related state, reducing recomposition when only one value changes
    val localGridItem: @Composable (LocalItem) -> Unit = remember(playbackInfo) {
        { item ->
        when (item) {
            is Song -> SongGridItem(
                song = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (item.id == playbackInfo.currentMediaId) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(item.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = item.id == playbackInfo.currentMediaId,
                isPlaying = playbackInfo.isPlaying,
            )

            is Album -> AlbumGridItem(
                album = item,
                isActive = item.id == playbackInfo.currentAlbumId,
                isPlaying = playbackInfo.isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${item.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${item.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }
    }

    // PERFORMANCE: Use libraryInfo and playbackInfo immutable objects - only 2 keys instead of 6
    val ytGridItem: @Composable (YTItem) -> Unit = remember(libraryInfo, playbackInfo) {
        { item ->
        YouTubeGridItem(
            item = item,
            isLiked = libraryInfo.isLiked(item.id),
            inLibrary = libraryInfo.inLibrary(item.id),
            isBookmarked = libraryInfo.isBookmarked(item.id),
            isActive = playbackInfo.isActiveItem(item.id),
            isPlaying = playbackInfo.isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
        }
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }


    BackpaperBackground(screen = BackpaperScreen.HOME) {
        ExpressivePullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    val staticChips = remember(isLoggedIn) {
                        listOfNotNull(
                            Pair("history", context.getString(R.string.history)),
                            Pair("stats", context.getString(R.string.stats)),
                            Pair("liked", context.getString(R.string.liked)),
                            Pair("downloads", context.getString(R.string.offline)),
                            if (isLoggedIn) Pair(
                                "account",
                                context.getString(R.string.account)
                            ) else null
                        )
                    }

                    val dynamicChips = remember(homePage?.chips) {
                        homePage?.chips.orEmpty().mapNotNull { chip ->
                            val endpoint = chip.endpoint ?: return@mapNotNull null
                            val browseId = endpoint.browseId ?: return@mapNotNull null
                            val params = endpoint.params ?: ""
                            Pair("browse_$browseId?$params", chip.title)
                        }
                    }

                    ChipsRow(
                        chips = staticChips + dynamicChips,
                        currentValue = "",
                        onValueUpdate = { value ->
                            when {
                                value == "history" -> navController.navigate("history")
                                value == "stats" -> navController.navigate("stats")
                                value == "liked" -> navController.navigate("auto_playlist/liked")
                                value == "downloads" -> navController.navigate("auto_playlist/downloaded")
                                value == "account" -> if (isLoggedIn) navController.navigate("account")
                                value.startsWith("browse_") -> {
                                    val rest = value.removePrefix("browse_")
                                    val browseId = rest.substringBefore("?")
                                    val params = rest.substringAfter("?", "")
                                    navController.navigate("youtube_browse/$browseId?params=$params")
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )

                    com.Chenkham.Echofy.utils.EchofyHomeSharePill()
                }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    com.Chenkham.Echofy.utils.ShareStatsTracker.loadTotalShares(context)
                }
            }

            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item(key = "quick_picks_title") {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "quick_picks_grid") {
                    // PERFORMANCE: Pre-cache song IDs for comparison to avoid repeat queries
                    val currentMediaId = playbackInfo.currentMediaId
                    val currentIsPlaying = playbackInfo.isPlaying

                    LazyHorizontalGrid(
                        state = quickPicksLazyGridState,
                        rows = GridCells.Fixed(4),
                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp * 4)
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = quickPicks,
                            key = { index, item -> "quick_pick_${item.id}" }
                        ) { index, song ->
                            val isActive = song.id == currentMediaId
                            PrefetchOnVisible(mediaId = song.id)

                            Box(
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .height(64.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isActive) 1.dp else 0.dp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (isActive) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .padding(start = 6.dp, end = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Thumbnail with active playing indicator
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(song.song.thumbnailUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (currentIsPlaying) {
                                                    PlayingIndicator(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.height(20.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(R.drawable.play),
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Title + Subtitle
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = song.song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (song.song.explicit) {
                                                Icon(
                                                    painter = painterResource(R.drawable.explicit),
                                                    contentDescription = "Explicit",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .padding(end = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = song.artists.joinToString { it.name } + if (song.song.albumName != null) " • ${song.song.albumName}" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // 3-dot action button
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            

            // Native ad placement - shows between content sections
            adManager?.let { manager ->
                item(key = "ad_native_1") {
                    NativeAdCard(
                        adManager = manager,
                        modifier = Modifier.animateItem(),
                        slotId = "home_1"
                    )
                }
            }

            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { newReleases ->
                item(key = "new_releases_title") {
                    NavigationTitle(
                        title = stringResource(R.string.new_releases),
                        onClick = {
                            navController.navigate("new_release")
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "new_releases_row") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = newReleases,
                            key = { index, item -> "new_release_${item.id}" }
                        ) { index, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            if (showTopChartsHome) {
                topCharts?.takeIf { it.isNotEmpty() }?.let { charts ->
                    item(key = "top_charts_title") {
                        NavigationTitle(
                            title = "🏆 Top 10 Charts",
                            onClick = {
                                navController.navigate("explore")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "top_charts_row") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            itemsIndexed(
                                items = charts.take(10),
                                key = { index, item -> "top_chart_${item.id}" }
                            ) { index, item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            if (showViral50Home) {
                viral50?.takeIf { it.isNotEmpty() }?.let { viral ->
                    item(key = "viral_50_title") {
                        NavigationTitle(
                            title = "🔥 Echofy Viral 50",
                            onClick = {
                                navController.navigate("explore")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "viral_50_row") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            itemsIndexed(
                                items = viral.take(50),
                                key = { index, item -> "viral_50_${item.id}" }
                            ) { index, item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item(key = "keep_listening_title") {
                    NavigationTitle(
                        title = stringResource(R.string.keep_listening),
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "keep_listening_grid") {
                    val rows = if (keepListening.size > 6) 2 else 1
                    LazyHorizontalGrid(
                        state = rememberLazyGridState(),
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((GridThumbnailHeight + with(LocalDensity.current) {
                                MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                        MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                            }) * rows)
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = keepListening,
                            key = { index, item -> "keep_listening_${item.id}" }
                        ) { index, it ->
                            localGridItem(it)
                        }
                    }
                }
            }



            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item(key = "account_playlists_title") {
                    NavigationTitle(
                        label = stringResource(R.string.your_ytb_playlists),
                        title = accountName,
                        thumbnail = {
                            if (url != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(url)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .diskCacheKey(url)
                                        .crossfade(true)
                                        .build(),
                                    placeholder = painterResource(id = R.drawable.person),
                                    error = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.person),
                                    contentDescription = null,
                                    modifier = Modifier.size(ListThumbnailSize)
                                )
                            }
                        },
                        onClick = {
                            navController.navigate("account")
                        },
                        modifier = Modifier.animateItem()
                    )
                }


                item(key = "account_playlists_row") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = accountPlaylists,
                            key = { index, item -> "account_playlist_${item.id}" },
                        ) { index, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }



            similarRecommendations?.forEachIndexed { _, recommendation ->
                item(key = "similar_${recommendation.title.id}_title") {
                    NavigationTitle(
                        label = stringResource(R.string.similar_to),
                        title = recommendation.title.title,
                        thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (recommendation.title is Artist) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = {
                            when (recommendation.title) {
                                is Song -> navController.navigate("album/${recommendation.title.album!!.id}")
                                is Album -> navController.navigate("album/${recommendation.title.id}")
                                is Artist -> navController.navigate("artist/${recommendation.title.id}")
                                is Playlist -> {}
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "similar_${recommendation.title.id}_row") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = recommendation.items,
                            key = { index, item -> "similar_${recommendation.title.id}_${item.id}" }
                        ) { index, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // Ad after similar recommendations
            // Home used to carry four native ads. Two is enough to keep the feed
            // readable; the remaining slots moved to Library / Explore / Support.

            homePage?.sections?.forEachIndexed { sectionIndex, section ->
                item(key = "home_section_${sectionIndex}_title") {
                    NavigationTitle(
                        title = section.title,
                        label = section.label,
                        thumbnail = section.thumbnail?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "home_section_${sectionIndex}_row") {
                    LazyRow(
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier.animateItem()
                    ) {
                        itemsIndexed(
                            items = section.items,
                            key = { index, item -> "home_section_${sectionIndex}_${item.id}" }
                        ) { index, item ->
                            ytGridItem(item)
                        }
                    }
                }
            }

            // Ad after new releases
            adManager?.let { manager ->
                item(key = "ad_native_4") {
                    NativeAdCard(
                        adManager = manager,
                        modifier = Modifier.animateItem(),
                        slotId = "home_4"
                    )
                }
            }

            if (isLoading) {
                item(key = "loading_shimmer") {
                    ShimmerHost(
                        modifier = Modifier.animateItem()
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .width(250.dp),
                        )
                        LazyRow {
                            items(4) {
                                GridItemPlaceHolder()
                            }
                        }
                    }
                }
            }

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item(key = "forgotten_favorites_title") {
                    NavigationTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        modifier = Modifier.animateItem()
                    )
                }

                item(key = "forgotten_favorites_grid") {
                    // PERFORMANCE: Pre-cache for comparison to avoid repeat queries
                    val currentMediaId = playbackInfo.currentMediaId
                    val currentIsPlaying = playbackInfo.isPlaying

                    // take min in case list size is less than 4
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                        state = forgottenFavoritesLazyGridState,
                        rows = GridCells.Fixed(rows),
                        flingBehavior = rememberSnapFlingBehavior(
                            forgottenFavoritesSnapLayoutInfoProvider
                        ),
                        contentPadding = WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal)
                            .asPaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp * rows)
                            .animateItem()
                    ) {
                        items(
                            items = forgottenFavorites,
                            key = { it.id }
                        ) { song ->
                            val isActive = song.id == currentMediaId

                            Box(
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .height(64.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isActive) 1.dp else 0.dp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (isActive) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .padding(start = 6.dp, end = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(song.song.thumbnailUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.45f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (currentIsPlaying) {
                                                    PlayingIndicator(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.height(20.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(R.drawable.play),
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = song.song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (song.song.explicit) {
                                                Icon(
                                                    painter = painterResource(R.drawable.explicit),
                                                    contentDescription = "Explicit",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .padding(end = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = song.artists.joinToString { it.name } + if (song.song.albumName != null) " • ${song.song.albumName}" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Discovery rows are emitted in the user's configured order. Each entry is a
            // lambda so the LazyListScope calls stay lazy and keep their stable item keys.
            val discoveryRows = mapOf<HomeRow, () -> Unit>(
                HomeRow.HIDDEN_GEMS to {
                    hiddenGems?.takeIf { it.isNotEmpty() }?.let { gems ->
                        item(key = "hidden_gems_title") {
                            NavigationTitle(
                                title = stringResource(R.string.hidden_gems),
                                modifier = Modifier.animateItem()
                            )
                        }

                        item(key = "hidden_gems_row") {
                            DiscoverySongRow(
                                songs = gems,
                                currentMediaId = playbackInfo.currentMediaId,
                                isPlaying = playbackInfo.isPlaying,
                                itemWidth = horizontalLazyGridItemWidth,
                                onPlay = { song ->
                                    if (song.id == playbackInfo.currentMediaId) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                    }
                                },
                                onLongClick = { song ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                },

                HomeRow.TIME_MACHINE to {
                    timeMachine?.takeIf { it.isNotEmpty() }?.let { throwbacks ->
                        item(key = "time_machine_title") {
                            NavigationTitle(
                                title = stringResource(R.string.time_machine_title, timeMachineYear),
                                modifier = Modifier.animateItem()
                            )
                        }

                        item(key = "time_machine_row") {
                            DiscoverySongRow(
                                songs = throwbacks,
                                currentMediaId = playbackInfo.currentMediaId,
                                isPlaying = playbackInfo.isPlaying,
                                itemWidth = horizontalLazyGridItemWidth,
                                onPlay = { song ->
                                    if (song.id == playbackInfo.currentMediaId) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                    }
                                },
                                onLongClick = { song ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                },

                HomeRow.BECAUSE_YOU_LISTENED to {
                    becauseYouListenedSongs?.takeIf { it.isNotEmpty() }?.let { picks ->
                        val seedArtist = becauseYouListenedArtist
                        if (seedArtist != null) {
                            item(key = "because_you_listened_title") {
                                NavigationTitle(
                                    title = stringResource(R.string.because_you_listened_to, seedArtist),
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item(key = "because_you_listened_row") {
                                DiscoverySongRow(
                                    songs = picks,
                                    currentMediaId = playbackInfo.currentMediaId,
                                    isPlaying = playbackInfo.isPlaying,
                                    itemWidth = horizontalLazyGridItemWidth,
                                    onPlay = { song ->
                                        if (song.id == playbackInfo.currentMediaId) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                        }
                                    },
                                    onLongClick = { song ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                },
                HomeRow.MOOD to {
                    moodSongs?.takeIf { it.isNotEmpty() }?.let { picks ->
                        val bucket = moodPlaylist
                        if (bucket != null) {
                            item(key = "mood_title") {
                                NavigationTitle(
                                    title = stringResource(bucket.titleRes),
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item(key = "mood_row") {
                                DiscoverySongRow(
                                    songs = picks,
                                    currentMediaId = playbackInfo.currentMediaId,
                                    isPlaying = playbackInfo.isPlaying,
                                    itemWidth = horizontalLazyGridItemWidth,
                                    onPlay = { song ->
                                        if (song.id == playbackInfo.currentMediaId) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                        }
                                    },
                                    onLongClick = { song ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                },
            )

            homeRowOrder.forEach { row -> discoveryRows[row]?.invoke() }

        }

        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
            lazyListState = lazylistState,
            icon = R.drawable.shuffle,
            onClick = {
                val local = when {
                    allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                    allLocalItems.isNotEmpty() -> true
                    else -> false
                }
                scope.launch(Dispatchers.Main) {
                    if (local) {
                        when (val luckyItem = allLocalItems.random()) {
                            is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is Album -> {
                                val albumWithSongs = withContext(Dispatchers.IO) {
                                    database.albumWithSongs(luckyItem.id).first()
                                }
                                albumWithSongs?.let {
                                    playerConnection.playQueue(LocalAlbumRadio(it))
                                }
                            }

                            is Artist -> {}
                            is Playlist -> {}
                        }
                    } else {
                        when (val luckyItem = allYtItems.random()) {
                            is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                            is ArtistItem -> luckyItem.radioEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }

                            is PlaylistItem -> luckyItem.playEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }
                        }
                    }
                }
            }
        )
    }
    }
    }
}

/**
 * A single horizontal strip of songs, shared by the opt-in discovery rows on the home
 * screen so they look and behave the same as the existing sections.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiscoverySongRow(
    songs: List<Song>,
    currentMediaId: String?,
    isPlaying: Boolean,
    itemWidth: Dp,
    onPlay: (Song) -> Unit,
    onLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Horizontal)
            .asPaddingValues(),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(
            count = songs.size,
            key = { songs[it].id },
        ) { index ->
            val song = songs[index]
            SongListItem(
                song = song,
                showInLibraryIcon = true,
                isActive = song.id == currentMediaId,
                isPlaying = isPlaying,
                modifier = Modifier
                    .width(itemWidth)
                    .combinedClickable(
                        onClick = { onPlay(song) },
                        onLongClick = { onLongClick(song) },
                    ),
            )
        }
    }
}
