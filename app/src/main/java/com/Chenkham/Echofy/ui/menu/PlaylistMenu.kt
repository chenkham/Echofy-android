package com.Chenkham.Echofy.ui.menu

import android.content.Intent
import com.Chenkham.Echofy.ui.component.LocalAdManager
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.Chenkham.innertube.YouTube
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.innertube.utils.parseCookieString
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.PlaylistSong
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.playback.ExoDownloadService
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.DefaultDialog
import com.Chenkham.Echofy.ui.component.DownloadGridMenu
import com.Chenkham.Echofy.ui.component.GridMenu
import com.Chenkham.Echofy.ui.component.GridMenuItem
import com.Chenkham.Echofy.ui.component.TextFieldDialog
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime


@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dbPlaylist by database.playlist(playlist.id).collectAsState(initial = playlist)
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable == true

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_playlist)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue =
                TextFieldValue(
                    playlist.playlist.name,
                    TextRange(playlist.playlist.name.length),
                ),
            onDone = { name ->
                onDismiss()
                database.query {
                    update(
                        playlist.playlist.copy(
                            name = name,
                            lastUpdateTime = LocalDateTime.now()
                        )
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                }
            },
        )
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist.playlist.name
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            // First toggle the like using the same logic as the like button
                            if (playlist.playlist.bookmarkedAt != null) {
                                // Using the same toggleLike() method that's used in the like button
                                update(playlist.playlist.toggleLike())
                            }
                            // Then delete the playlist
                            delete(playlist.playlist)
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // New: Sync with YTM dialog state
    var showSyncWithYTMDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            coroutineScope.launch(Dispatchers.IO) {
                // add songs to playlist and push to ytm
                playlist.playlist.browseId?.let { playlistId ->
                    YouTube.addPlaylistToPlaylist(playlistId, playlist.id)
                }
            }
            songs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    // Sync with YTM Dialog
    SyncWithYTMDialog(
        isVisible = showSyncWithYTMDialog,
        playlist = playlist,
        songs = songs,
        onDismiss = { showSyncWithYTMDialog = false }
    )


    HorizontalDivider()

    val adManager = LocalAdManager.current
    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = playlist.playlist.name,
                    items = songs.map { it.toMediaItem() },
                ),
            )
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = playlist.playlist.name,
                    items = songs.shuffled().map { it.toMediaItem() },
                ),
            )
        }

        playlist.playlist.browseId?.let { browseId ->
            GridMenuItem(
                icon = R.drawable.radio,
                title = R.string.start_radio
            ) {
                coroutineScope.launch(Dispatchers.IO) {
                    YouTube.playlist(browseId).getOrNull()?.playlist?.let { playlistItem ->
                        playlistItem.radioEndpoint?.let { radioEndpoint ->
                            withContext(Dispatchers.Main) {
                                playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                            }
                        }
                    }
                }
                onDismiss()
            }
        }
        GridMenuItem(
            icon = R.drawable.playlist_play,
            title = R.string.play_next
        ) {
            coroutineScope.launch {
                playerConnection.playNext(songs.map { it.toMediaItem() })
            }
            onDismiss()
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(songs.map { it.toMediaItem() })
        }

        if (editable && autoPlaylist != true) {
            GridMenuItem(
                icon = R.drawable.edit,
                title = R.string.edit
            ) {
                showEditDialog = true
            }
        }

        // Show Sync with YT Music for local playlists when user is logged in
        if (autoPlaylist != true && isLoggedIn) {
            GridMenuItem(
                icon = R.drawable.sync,
                title = R.string.sync_with_ytm,
            ) {
                showSyncWithYTMDialog = true
            }
        }

        if (downloadPlaylist != true) {
            DownloadGridMenu(
                state = downloadState,
                onDownload = {
                    val isPremium = adManager?.isPremium?.value == true
                    if (isPremium) {
                        songs.forEach { song ->
                        val downloadRequest =
                            DownloadRequest
                                .Builder(song.id, song.id.toUri())
                                .setCustomCacheKey(song.id)
                                .setData(song.song.title.toByteArray())
                                .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    }
                    Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, R.string.premium_required, Toast.LENGTH_SHORT).show()
                }
            },
                onRemoveDownload = {
                    showRemoveDownloadDialog = true
                },
            )
        }

        if (autoPlaylist != true) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.delete,
            ) {
                showDeletePlaylistDialog = true
            }
        }

        playlist.playlist.shareLink?.let { shareLink ->
            GridMenuItem(
                icon = R.drawable.share,
                title = R.string.share
            ) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareLink)
                }
                context.startActivity(Intent.createChooser(intent, null))
                onDismiss()
            }
        }
    }
}