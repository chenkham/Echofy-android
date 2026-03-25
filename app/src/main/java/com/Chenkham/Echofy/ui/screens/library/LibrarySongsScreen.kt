package com.Chenkham.Echofy.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.CONTENT_TYPE_HEADER
import com.Chenkham.Echofy.constants.CONTENT_TYPE_SONG
import com.Chenkham.Echofy.constants.SongFilter
import com.Chenkham.Echofy.constants.SongFilterKey
import com.Chenkham.Echofy.constants.SongSortDescendingKey
import com.Chenkham.Echofy.constants.SongSortType
import com.Chenkham.Echofy.constants.SongSortTypeKey
import com.Chenkham.Echofy.constants.YtmSyncKey
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.ui.component.ChipsRow
import com.Chenkham.Echofy.ui.component.HideOnScrollFAB
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.PrefetchOnVisible
import com.Chenkham.Echofy.ui.component.SongListItem
import com.Chenkham.Echofy.ui.component.SortHeader
import com.Chenkham.Echofy.ui.component.VerticalFastScroller
import com.Chenkham.Echofy.ui.menu.SelectionSongMenu
import com.Chenkham.Echofy.ui.menu.SongMenu
import com.Chenkham.Echofy.ui.utils.ItemWrapper
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import com.Chenkham.Echofy.viewmodels.LibrarySongsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val songs by viewModel.allSongs.collectAsState()

    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                else -> return@LaunchedEffect
            }
        }
    }

    val wrappedSongs = remember(songs) {
        songs.map { item -> ItemWrapper(item) }.toMutableStateList()
    }
    var selection by remember {
        mutableStateOf(false)
    }

    val lazyListState = rememberLazyListState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
            item(
                key = "filter",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        label = { Text(stringResource(R.string.songs)) },
                        selected = true,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = onDeselect,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = ""
                            )
                        },
                    )
                    ChipsRow(
                        chips =
                            listOf(
                                SongFilter.LIKED to stringResource(R.string.filter_liked),
                                SongFilter.LIBRARY to stringResource(R.string.filter_library),
                                SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
                            ),
                        currentValue = filter,
                        onValueUpdate = {
                            filter = it
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item(
                key = "header",
                contentType = CONTENT_TYPE_HEADER,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selection) {
                        val count = wrappedSongs.count { it.isSelected }
                        IconButton(
                            onClick = { selection = false },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (count == wrappedSongs.size) {
                                    wrappedSongs.forEach { it.isSelected = false }
                                } else {
                                    wrappedSongs.forEach { it.isSelected = true }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all),
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }
                                            .map { it.item },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false },
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        SongSortType.NAME -> R.string.sort_by_name
                                        SongSortType.ARTIST -> R.string.sort_by_artist
                                        SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                },
                            )

                            Spacer(Modifier.weight(1f))

                            Text(
                                text = pluralStringResource(
                                    R.plurals.n_song,
                                    songs.size,
                                    songs.size
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = wrappedSongs,
                key = { _, item -> item.item.song.id },
                contentType = { _, _ -> CONTENT_TYPE_SONG },
            ) { index, songWrapper ->
                // INSTANT PLAYBACK: Prefetch URL when song becomes visible
                PrefetchOnVisible(mediaId = songWrapper.item.song.id)
                
                SongListItem(
                    song = songWrapper.item,
                    showInLibraryIcon = true,
                    isActive = songWrapper.item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,

                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    isSelected = songWrapper.isSelected && selection,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.queue_all_songs),
                                                    items = songs.map { it.toMediaItem() },
                                                    startIndex = index,
                                                ),
                                            )
                                        }
                                    } else {
                                        songWrapper.isSelected = !songWrapper.isSelected
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!selection) {
                                        selection = true
                                    }
                                    wrappedSongs.forEach {
                                        it.isSelected = false
                                    } // Clear previous selections
                                    songWrapper.isSelected = true // Select current item
                                },
                            )
                            .animateItem(),
                )
            }
        }
    }

        HideOnScrollFAB(
            visible = songs.isNotEmpty() == true,
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.queue_all_songs),
                        items = songs.shuffled().map { it.toMediaItem() },
                    ),
                )
            },
        )
    }
}