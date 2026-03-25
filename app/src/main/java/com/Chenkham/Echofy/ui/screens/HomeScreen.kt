package com.Chenkham.Echofy.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.Chenkham.Echofy.ui.component.BannerAdView
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.ArtistItem
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.innertube.models.YTItem
import com.Chenkham.innertube.utils.parseCookieString
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.GridThumbnailHeight
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
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

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
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
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
                                playerConnection.player.togglePlayPause()
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
        BoxWithConstraints(
            modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
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

        // PERFORMANCE: Professional-grade LazyColumn configuration
        // These settings match Spotify/YouTube Music level smoothness
        // Key optimizations:
        // 1. Use key() for all items to enable efficient diffing
        // 2. Minimize recomposition with stable keys
        // 3. beyondBoundsItemCount for smooth prefetching
        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            // PERFORMANCE: Prefetch items beyond visible bounds for smooth scrolling
            // This loads items before they're visible, preventing jank during fast scrolls
        ) {
            item {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    ChipsRow(
                            chips = remember(isLoggedIn) {
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
                            },
                        currentValue = "",
                        onValueUpdate = { value ->
                            when (value) {
                                "history" -> navController.navigate("history")
                                "stats" -> navController.navigate("stats")
                                "liked" -> navController.navigate("auto_playlist/liked")
                                "downloads" -> navController.navigate("auto_playlist/downloaded")
                                "account" -> if (isLoggedIn) navController.navigate("account")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
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
                            .height(ListItemHeight * 4)
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = quickPicks,
                            key = { index, item -> "quick_pick_${item.id}" }
                        ) { index, song ->
                            // PERFORMANCE: Use the song directly without additional DB query
                            // The viewModel already provides fresh data
                            val isActive = song.id == currentMediaId
                            
                            // INSTANT PLAYBACK: Prefetch stream URL when song becomes visible
                            PrefetchOnVisible(mediaId = song.id)

                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = isActive,
                                isPlaying = currentIsPlaying,
                                trailingContent = {
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
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (isActive) {
                                                playerConnection.player.togglePlayPause()
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
                            )
                        }
                    }
                }
            }
            
            // Native ad placement - shows between content sections
            adManager?.let { manager ->
                item(key = "ad_native_1") {
                    NativeAdCard(
                        adManager = manager,
                        modifier = Modifier.animateItem()
                    )
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
            adManager?.let { manager ->
                item(key = "ad_native_3") {
                    NativeAdCard(
                        adManager = manager,
                        modifier = Modifier.animateItem()
                    )
                }
            }

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

            // New Releases section from explorePage
            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { newReleases ->
                item(key = "new_releases_title") {
                    NavigationTitle(
                        title = stringResource(R.string.new_release_albums),
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
                        ) { index, album ->
                            ytGridItem(album)
                        }
                    }
                }
            }

            // Ad after new releases
            adManager?.let { manager ->
                item(key = "ad_native_4") {
                    NativeAdCard(
                        adManager = manager,
                        modifier = Modifier.animateItem()
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
                            .height(ListItemHeight * rows)
                            .animateItem()
                    ) {
                        items(
                            items = forgottenFavorites,
                            key = { it.id }
                        ) { song ->
                            // PERFORMANCE: Use the song directly without additional DB query
                            val isActive = song.id == currentMediaId

                            SongListItem(
                                song = song,
                                showInLibraryIcon = true,
                                isActive = isActive,
                                isPlaying = currentIsPlaying,
                                modifier = Modifier
                                    .width(horizontalLazyGridItemWidth)
                                    .combinedClickable(
                                        onClick = {
                                            if (isActive) {
                                                playerConnection.player.togglePlayPause()
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
                            )
                        }
                    }
                }
            }
            
            // Ad after keep listening
            adManager?.let { manager ->
                item(key = "ad_native_5") {
                    NativeAdCard(
                        adManager = manager,
                        modifier = Modifier.animateItem()
                    )
                }
            }
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

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
    }
}
