package com.Chenkham.Echofy.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.ArtistItem
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.SuggestionItemHeight
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.PrefetchOnVisible
import com.Chenkham.Echofy.ui.component.YouTubeListItem
import com.Chenkham.Echofy.ui.menu.YouTubeAlbumMenu
import com.Chenkham.Echofy.ui.menu.YouTubeArtistMenu
import com.Chenkham.Echofy.ui.menu.YouTubePlaylistMenu
import com.Chenkham.Echofy.ui.menu.YouTubeSongMenu
import com.Chenkham.Echofy.viewmodels.OnlineSearchSuggestionViewModel
import com.Chenkham.Echofy.db.entities.SearchHistory
import com.Chenkham.Echofy.db.entities.RecentSearchSong
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    // INSTANT PLAYBACK: Bulk prefetch all song results when they load
    // This ensures URLs are cached before user can tap
    LaunchedEffect(viewState.items) {
        val songs = viewState.items.filterIsInstance<SongItem>().take(10) // First 10 songs
        songs.forEach { song ->
            playerConnection.prefetch(song.id)
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            WindowInsets.systemBars
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
    ) {
        // Recent Songs - Horizontal Row with Album Covers
        if (viewState.recentSongs.isNotEmpty()) {
            item(key = "recentSongsHeader") {
                Text(
                    text = "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            item(key = "recentSongsRow") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(
                        items = viewState.recentSongs,
                        key = { it.id }
                    ) { song ->
                        // INSTANT PLAYBACK: Prefetch URL when song becomes visible
                        PrefetchOnVisible(mediaId = song.id)
                        
                        RecentSongItem(
                            song = song,
                            isActive = mediaMetadata?.id == song.id,
                            isPlaying = isPlaying,
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = song.id),
                                            song.toMediaMetadata()
                                        ),
                                    )
                                    onDismiss()
                                }
                            }
                        )
                    }
                }
            }
        }
        
        item(key = "historyHeader") {
            Text(
                text = (stringResource(R.string.SearchHistory)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        items(
            items = viewState.history,
            key = { it.query },
        ) { history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query {
                        delete(history)
                    }
                },
                onFillTextField = {
                    onQueryChange(
                        TextFieldValue(
                            text = history.query,
                            selection = TextRange(history.query.length),
                        ),
                    )
                },
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            )
        }
        if (viewState.suggestions.isNotEmpty()) {
            item(key = "suggestionsHeader") {
                Text(
                    text = (stringResource(R.string.Sujestions)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }


        items(
            items = viewState.suggestions,
            key = { it },
        ) { query ->
            SuggestionItem(
                query = query,
                online = true,
                onClick = {
                    onSearch(query)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(
                        TextFieldValue(
                            text = query,
                            selection = TextRange(query.length),
                        ),
                    )
                },
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            )
        }

        if (viewState.items.isNotEmpty()) {
            item(key = "resultsHeader") {
                Text(
                    text = (stringResource(R.string.SearchResutls)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(
            items = viewState.items,
            key = { it.id },
        ) { item ->
            // INSTANT PLAYBACK: Prefetch URL for songs when they become visible
            if (item is SongItem) {
                PrefetchOnVisible(mediaId = item.id)
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
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                            is SongItem -> {
                                    // Add to recent search songs
                                    coroutineScope.launch {
                                        database.insertRecentSearchSong(RecentSearchSong(songId = item.id))
                                    }
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = item.id),
                                                item.toMediaMetadata()
                                            ),
                                        )
                                        onDismiss()
                                    }
                                }

                                is AlbumItem -> {
                                    navController.navigate("album/${item.id}")
                                    onDismiss()
                                }

                                is ArtistItem -> {
                                    navController.navigate("artist/${item.id}")
                                    onDismiss()
                                }

                                is PlaylistItem -> {
                                    navController.navigate("online_playlist/${item.id}")
                                    onDismiss()
                                }
                            }
                        },
                        onLongClick = {
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
                        },
                    )
                    .animateItem()
            )
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(SuggestionItemHeight)
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
                    .alpha(0.7f),
                tint = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = query,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!online) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.alpha(0.7f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onFillTextField,
                modifier = Modifier.alpha(0.7f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_top_left),
                    contentDescription = "Fill",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Recent song item with album cover for horizontal display (YouTube Music style)
 */
@Composable
fun RecentSongItem(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album cover
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = song.song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            
            // Play indicator overlay when active
            if (isActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Song title
        Text(
            text = song.song.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
        )
        
        // Artist name
        Text(
            text = song.artists.firstOrNull()?.name ?: "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
