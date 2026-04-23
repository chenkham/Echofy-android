package com.Chenkham.Echofy.ui.menu

import com.Chenkham.Echofy.db.addSongToPlaylist

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.utils.parseCookieString
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.ui.component.CreatePlaylistDialog
import com.Chenkham.Echofy.ui.component.DefaultDialog
import com.Chenkham.Echofy.ui.component.ListDialog
import com.Chenkham.Echofy.ui.component.ListItem
import com.Chenkham.Echofy.ui.component.PlaylistListItem
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend (Playlist) -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var playlists by remember {
        mutableStateOf(emptyList<Playlist>())
    }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var showCreatePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }
    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }
    var songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }
    var duplicates by remember {
        mutableStateOf(emptyList<String>())
    }

    suspend fun syncSongIdsToYouTube(playlist: Playlist, selectedSongIds: List<String>) {
        val browseId = playlist.playlist.browseId ?: return
        selectedSongIds.forEach { songId ->
            YouTube.addToPlaylist(browseId, songId)
        }
    }

    suspend fun handlePlaylistSelection(playlist: Playlist) {
        val selectedSongIds = songIds ?: onGetSong(playlist).also { songIds = it }

        if (selectedSongIds.isEmpty()) {
            withContext(Dispatchers.Main) {
                onDismiss()
            }
            return
        }

        val duplicateSongIds = database.playlistDuplicates(playlist.id, selectedSongIds)
        if (duplicateSongIds.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                selectedPlaylist = playlist
                duplicates = duplicateSongIds
                showDuplicateDialog = true
            }
            return
        }

        database.addSongToPlaylist(playlist, selectedSongIds)
        syncSongIdsToYouTube(playlist, selectedSongIds)

        withContext(Dispatchers.Main) {
            onDismiss()
        }
    }

    // Reset cached song IDs whenever the dialog is reopened so we always
    // use the *currently playing* song instead of a stale previous one.
    LaunchedEffect(isVisible) {
        if (isVisible) {
            songIds = null
            duplicates = emptyList()
            selectedPlaylist = null
        }
    }

    LaunchedEffect(Unit) {
        database.editablePlaylistsByCreateDateAsc().collect {
            playlists = it.asReversed()
        }
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss,
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.create_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            items(playlists) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier.clickable {
                        selectedPlaylist = playlist
                        coroutineScope.launch(Dispatchers.IO) {
                            handlePlaylistSelection(playlist)
                        }
                    }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing,
            onCreated = { createdPlaylist ->
                withContext(Dispatchers.Main) {
                    showCreatePlaylistDialog = false
                }
                handlePlaylistSelection(createdPlaylist)
            }
        )
    }

    // duplicate songs warning
    if (showDuplicateDialog) {
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(
                    onClick = {
                        val playlist = selectedPlaylist ?: return@TextButton
                        val selectedSongIds = songIds.orEmpty().filterNot { duplicates.contains(it) }
                        showDuplicateDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            if (selectedSongIds.isNotEmpty()) {
                                database.transaction {
                                    addSongToPlaylist(playlist, selectedSongIds)
                                }
                                syncSongIdsToYouTube(playlist, selectedSongIds)
                            }
                            withContext(Dispatchers.Main) {
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.skip_duplicates))
                }

                TextButton(
                    onClick = {
                        val playlist = selectedPlaylist ?: return@TextButton
                        val selectedSongIds = songIds.orEmpty()
                        showDuplicateDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            if (selectedSongIds.isNotEmpty()) {
                                database.transaction {
                                    addSongToPlaylist(playlist, selectedSongIds)
                                }
                                syncSongIdsToYouTube(playlist, selectedSongIds)
                            }
                            withContext(Dispatchers.Main) {
                                onDismiss()
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_anyway))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showDuplicateDialog = false
            }
        ) {
            Text(
                text = if (duplicates.size == 1) {
                    stringResource(R.string.duplicates_description_single)
                } else {
                    stringResource(R.string.duplicates_description_multiple, duplicates.size)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}
