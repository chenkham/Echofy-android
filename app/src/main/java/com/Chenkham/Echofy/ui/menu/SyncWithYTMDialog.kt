package com.Chenkham.Echofy.ui.menu

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Chenkham.innertube.YouTube
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.addSongToPlaylist
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.Chenkham.Echofy.db.entities.PlaylistSong
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.ui.component.ListDialog
import com.Chenkham.Echofy.ui.component.PlaylistListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Dialog that lets users sync a local playlist with YT Music.
 * Two options:
 *  1. "Upload to YT Music" — creates a new synced YTM playlist from this local playlist.
 *  2. "Merge into YTM playlist" — shows existing YTM playlists to pick one, then uploads songs into it.
 *
 * @param playlist       The local playlist being synced.
 * @param songs          Songs that belong to this local playlist.
 * @param onDismiss      Called when dialog is closed.
 * @param isVisible      Whether the dialog is showing.
 */
@Composable
fun SyncWithYTMDialog(
    isVisible: Boolean,
    playlist: Playlist,
    songs: List<Song>,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    // Sub-screen states
    var showMergeList by remember { mutableStateOf(false) }
    var ytmPlaylists by remember { mutableStateOf(emptyList<Playlist>()) }

    LaunchedEffect(showMergeList) {
        if (showMergeList) {
            // Load all YTM synced playlists (those with a browseId)
            database.editablePlaylistsByCreateDateAsc().collect { all ->
                ytmPlaylists = all.filter { it.playlist.browseId != null }.asReversed()
            }
        }
    }

    if (showMergeList) {
        // Show list of existing YTM playlists to merge into
        ListDialog(onDismiss = {
            showMergeList = false
            onDismiss()
        }) {
            if (ytmPlaylists.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_ytm_playlists_to_merge),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(ytmPlaylists) { targetPlaylist ->
                PlaylistListItem(
                    playlist = targetPlaylist,
                    modifier = Modifier.clickable {
                        coroutineScope.launch(Dispatchers.IO) {
                            val songIds = songs.map { it.id }
                            // Add songs to target local playlist DB
                            database.addSongToPlaylist(targetPlaylist, songIds)
                            // Push songs to YTM playlist
                            targetPlaylist.playlist.browseId?.let { browseId ->
                                songIds.forEach { songId ->
                                    YouTube.addToPlaylist(browseId, songId)
                                }
                            }
                        }
                        showMergeList = false
                        onDismiss()
                        Toast.makeText(
                            context,
                            context.getString(R.string.songs_merged_into_ytm_playlist),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    } else {
        // Main dialog: choose action
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null
                )
            },
            title = {
                Text(text = stringResource(R.string.sync_with_ytm))
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.sync_with_ytm_description),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Option 1: Upload as new YTM playlist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch(Dispatchers.IO) {
                                    // 1. Create YTM playlist with the same name
                                    val browseIdResult = YouTube.createPlaylist(playlist.playlist.name)
                                    val browseId = browseIdResult.getOrNull()
                                    if (browseId == null) {
                                        launch(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.error_creating_ytm_playlist),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        return@launch
                                    }
                                    // 2. Update local playlist to be linked to YTM browseId
                                    database.query {
                                        update(
                                            playlist.playlist.copy(
                                                browseId = browseId.toString(),
                                                lastUpdateTime = LocalDateTime.now()
                                            )
                                        )
                                    }
                                    // 3. Add all local songs to the new YTM playlist
                                    songs.forEach { song ->
                                        YouTube.addToPlaylist(browseId.toString(), song.id)
                                    }
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.playlist_uploaded_to_ytm),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cloud_download),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.upload_to_ytm),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.upload_to_ytm_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Merge into an existing YTM playlist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMergeList = true
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.merge_into_ytm_playlist),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.merge_into_ytm_playlist_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
