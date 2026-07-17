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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.db.update
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.PrefetchOnVisible
import com.Chenkham.Echofy.ui.utils.OptimizedAsyncImage
import com.Chenkham.Echofy.ui.utils.ThumbnailSizes
import com.Chenkham.Echofy.ui.menu.YouTubeSongMenu
import com.Chenkham.Echofy.viewmodels.OnlineSearchSuggestionViewModel
import com.Chenkham.Echofy.db.entities.RecentSearchSong
import kotlinx.coroutines.Dispatchers
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
    val isSearching = query.isNotBlank()
    val songSuggestions = viewState.items.filterIsInstance<SongItem>().take(3)

    fun rememberRecentSong(song: SongItem) {
        coroutineScope.launch(Dispatchers.IO) {
            val metadata = song.toMediaMetadata()
            val existingSong = database.getSongById(song.id)
            if (existingSong == null) {
                database.insert(metadata)
            } else {
                database.update(existingSong, metadata)
            }
            database.insertRecentSearchSong(RecentSearchSong(songId = song.id))
        }
    }

    fun showSongMenu(song: SongItem) {
        menuState.show {
            YouTubeSongMenu(
                song = song,
                navController = navController,
                onDismiss = menuState::dismiss,
            )
        }
    }

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

    // Keep the first few playable suggestions warm without flooding the UI thread/network.
    LaunchedEffect(songSuggestions) {
        songSuggestions.forEach { song ->
            playerConnection.prefetch(song.id)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding =
            WindowInsets.systemBars
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
    ) {
        if (!isSearching) {
            if (viewState.recentSongs.isNotEmpty() || viewState.history.isNotEmpty()) {
                item(key = "recentSearchesHeader") {
                    SearchSectionHeader(
                        text = stringResource(R.string.recent_searches),
                        modifier = Modifier.padding(top = 18.dp)
                    )
                }
            }

            if (viewState.recentSongs.isNotEmpty()) {
                item(key = "recentSongsRow") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        items(
                            items = viewState.recentSongs,
                            key = { it.id }
                        ) { song ->
                            PrefetchOnVisible(mediaId = song.id)

                            RecentSongItem(
                                song = song,
                                isActive = mediaMetadata?.id == song.id,
                                isPlaying = isPlaying,
                                onClick = {
                                    if (song.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                WatchEndpoint(videoId = song.id),
                                                song.toMediaMetadata()
                                            ),
                                        )
                                        onDismiss()
                                    }
                                },
                                onRemove = {
                                    database.query {
                                        deleteRecentSearchSong(song.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else if (viewState.history.isNotEmpty()) {
            item(key = "historyMatchesHeader") {
                SearchSectionHeader(text = stringResource(R.string.SearchHistory))
            }
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

        if (isSearching && viewState.suggestions.isNotEmpty()) {
            item(key = "suggestionsHeader") {
                SearchSectionHeader(text = stringResource(R.string.Sujestions))
            }
        }

        items(
            items = if (isSearching) viewState.suggestions else emptyList(),
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

        if (isSearching && songSuggestions.isNotEmpty()) {
            item(key = "songSuggestionsDivider") {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )
            }
        }

        items(
            items = if (isSearching) songSuggestions else emptyList(),
            key = { it.id },
            contentType = { "top_song_suggestion" },
        ) { item ->
            PrefetchOnVisible(mediaId = item.id)

            CompactSongSuggestionItem(
                song = item,
                isActive = mediaMetadata?.id == item.id,
                isPlaying = isPlaying,
                onMenuClick = { showSongMenu(item) },
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            rememberRecentSong(item)
                            if (item.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = item.id),
                                        item.toMediaMetadata()
                                    ),
                                )
                                onDismiss()
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showSongMenu(item)
                        },
                    )
            )
        }
    }
}

@Composable
private fun SearchSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
    )
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
                .height(48.dp)
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(20.dp)
                    .alpha(0.7f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = query,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun CompactSongSuggestionItem(
    song: SongItem,
    isActive: Boolean,
    isPlaying: Boolean,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                }
            )
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
            OptimizedAsyncImage(
                url = song.thumbnail,
                contentDescription = song.title,
                size = ThumbnailSizes.SMALL,
                modifier = Modifier.matchParentSize()
            )

            if (isActive) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Recent song item with album cover for horizontal display (YouTube Music style)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentSongItem(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemoveOption by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = modifier
            .width(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showRemoveOption = true }
            ),
        horizontalAlignment = Alignment.Start
    ) {
        androidx.compose.material3.DropdownMenu(
            expanded = showRemoveOption,
            onDismissRequest = { showRemoveOption = false }
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Remove") },
                onClick = {
                    showRemoveOption = false
                    onRemove()
                }
            )
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            OptimizedAsyncImage(
                url = song.song.thumbnailUrl,
                contentDescription = song.song.title,
                size = ThumbnailSizes.SMALL,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        Text(
            text = song.song.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
