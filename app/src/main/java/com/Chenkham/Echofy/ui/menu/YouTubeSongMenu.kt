package com.Chenkham.Echofy.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListItemHeight
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.SongEntity
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.ExoDownloadService
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.ListDialog
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString
import com.Chenkham.Echofy.ui.component.LocalAdManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.Chenkham.Echofy.ui.component.ListItem as EchofyListItem
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,

) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val adManager = LocalAdManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val artists =
        remember {
            song.artists.mapNotNull {
                it.id?.let { artistId ->
                    MediaMetadata.Artist(id = artistId, name = it.name)
                }
            }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(song.toMediaMetadata())
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = artists,
                key = { it.id ?: it.name }
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillParentMaxWidth()
                                .height(ListItemHeight)
                                .clickable {
                                    navController.navigate("artist/${artist.id}")
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                                .padding(horizontal = 24.dp),
                    ) {
                        Text(
                            text = artist.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // LazyListState to track scroll position
    val lazyListState = rememberLazyListState()

    // Nested scroll connection to properly handle scroll gestures
    // Prevents vibration/bounce when over-scrolling at the top of the menu
    val nestedScrollConnection = remember(lazyListState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val isAtTop = !lazyListState.canScrollBackward
                val isAtBottom = !lazyListState.canScrollForward
                val isScrollingUp = available.y > 0
                val isScrollingDown = available.y < 0

                // When at top and trying to scroll up (pull down gesture), consume the scroll
                // to prevent bounce/vibration, but allow small amount for dismiss gesture
                if (isAtTop && isScrollingUp && source == NestedScrollSource.UserInput) {
                    // Only pass through if it's a significant gesture (for dismiss)
                    return if (available.y > 50f) {
                        androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        // Consume small scroll to prevent vibration
                        available.copy(x = 0f)
                    }
                }

                // When at bottom and trying to scroll down, consume to prevent bounce
                if (isAtBottom && isScrollingDown) {
                    return available.copy(x = 0f)
                }

                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // Consume any leftover scroll to prevent vibration
                val isAtTop = !lazyListState.canScrollBackward
                val isAtBottom = !lazyListState.canScrollForward

                if ((isAtTop && available.y > 0) || (isAtBottom && available.y < 0)) {
                    return available.copy(x = 0f)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 8.dp),
        userScrollEnabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection)
    ) {
            item {
                EchofyListItem(
                    title = song.title,
                    subtitle =
                    joinByBullet(
                        song.artists.joinToString { it.name },
                        song.duration?.let { makeTimeString(it * 1000L) },
                    ),
                    thumbnailContent = {
                        AsyncImage(
                            model = song.thumbnail,
                            contentDescription = null,
                            modifier =
                            Modifier
                                .size(ListThumbnailSize)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                database.transaction {
                                    librarySong.let { librarySong ->
                                        if (librarySong == null) {
                                            insert(song.toMediaMetadata(), SongEntity::toggleLike)
                                        } else {
                                            update(librarySong.song.toggleLike())
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(if (librarySong?.song?.liked == true) R.drawable.heart_fill else R.drawable.heart),
                                tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                contentDescription = null,
                            )
                        }
                    },
                )

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                // Quick action buttons row (4 buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Start radio button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDismiss()
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.radio),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.start_radio),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }

                    // Play next button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDismiss()
                                playerConnection.playNext(song.toMediaItem())
                            }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_play),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.play_next),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }

                    // Add to playlist button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showChoosePlaylistDialog = true
                            }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.add_to_playlist),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }

                    // Share button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val intent =
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, song.shareLink)
                                    }
                                context.startActivity(Intent.createChooser(intent, null))
                                onDismiss()
                            }
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.share),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }

            // Menu items list
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        playerConnection.addToQueue(song.toMediaItem())
                    }
                )
            }

            if (artists.isNotEmpty()) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.artist),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            if (artists.size == 1) {
                                navController.navigate("artist/${artists[0].id}")
                                onDismiss()
                            } else {
                                showSelectArtistDialog = true
                            }
                        }
                    )
                }
            }

            song.album?.let { album ->
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.view_album)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.album),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            navController.navigate("album/${album.id}")
                            onDismiss()
                        }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(
                                if (librarySong?.song?.inLibrary != null) R.string.remove_from_library
                                else R.string.add_to_library
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                if (librarySong?.song?.inLibrary != null) R.drawable.library_add_check
                                else R.drawable.library_add
                            ),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        if (librarySong?.song?.inLibrary != null) {
                            database.query {
                                inLibrary(song.id, null)
                            }
                        } else {
                            database.transaction {
                                insert(song.toMediaMetadata())
                                inLibrary(song.id, LocalDateTime.now())
                            }
                        }
                        onDismiss()
                    }
                )
            }

            item {
                val performDownload = {
                    database.transaction {
                        insert(song.toMediaMetadata())
                    }
                    val downloadRequest =
                        DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
                            .build()
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false,
                    )
                }

                val downloadState = download?.state
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(
                                when (downloadState) {
                                    androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> R.string.remove_download
                                    androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING,
                                    androidx.media3.exoplayer.offline.Download.STATE_QUEUED -> R.string.downloading
                                    else -> R.string.download
                                }
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                when (downloadState) {
                                    androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> R.drawable.offline
                                    androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING,
                                    androidx.media3.exoplayer.offline.Download.STATE_QUEUED -> R.drawable.download
                                    else -> R.drawable.download
                                }
                            ),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        when (downloadState) {
                            androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                            androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING,
                            androidx.media3.exoplayer.offline.Download.STATE_QUEUED -> {
                                // Do nothing while downloading
                            }
                            else -> {
                                if (adManager?.isPremium?.value != true) {
                                    android.widget.Toast.makeText(context, R.string.premium_required, android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    performDownload()
                                }
                            }
                        }
                        onDismiss()
                    }
                )
            }
        }
    }
