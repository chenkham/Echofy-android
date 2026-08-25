package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.innertube.YouTube
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.utils.dataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onCreated: suspend (Playlist) -> Unit = {},
) {
    val database = LocalDatabase.current
    val workerScope = remember { CoroutineScope(Dispatchers.IO) }
    var syncedPlaylist by remember { mutableStateOf(false) }
    
    
    val context = LocalContext.current
    val innerTubeCookie by context.dataStore.data
        .map { it[InnerTubeCookieKey] }
        .collectAsState(initial = null)
    
    val isLoggedIn = !innerTubeCookie.isNullOrEmpty()
    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = stringResource(R.string.create_playlist)) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            workerScope.launch {
                val browseId = if (syncedPlaylist && isLoggedIn) {
                    val result = YouTube.createPlaylist(playlistName)
                    val createdBrowseId = result.getOrNull()?.removePrefix("VL")

                    if (createdBrowseId == null) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "Error creating playlist", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    createdBrowseId
                } else null

                val now = LocalDateTime.now()
                val existingPlaylist =
                    browseId?.let {
                        database.playlistByBrowseId(it).firstOrNull()
                            ?: database.playlistByBrowseId("VL$it").firstOrNull()
                    }

                val localPlaylist =
                    if (existingPlaylist != null) {
                        val updatedPlaylist =
                            existingPlaylist.playlist.copy(
                                name = playlistName,
                                browseId = browseId,
                                bookmarkedAt = existingPlaylist.playlist.bookmarkedAt ?: now,
                                lastUpdateTime = now,
                                isEditable = true,
                            )
                        database.update(updatedPlaylist)
                        database.playlist(updatedPlaylist.id).firstOrNull()
                    } else {
                        val playlistEntity =
                            PlaylistEntity(
                                name = playlistName,
                                browseId = browseId,
                                bookmarkedAt = now,
                                lastUpdateTime = now,
                                isEditable = true,
                            )
                        database.insert(playlistEntity)
                        database.playlist(playlistEntity.id).firstOrNull()
                    }

                localPlaylist?.let {
                    onCreated(it)
                }
            }
        },
        extraContent = {
            if (allowSyncing) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 40.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sync_playlist),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.allows_for_sync_witch_youtube),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Switch(
                            checked = syncedPlaylist && isLoggedIn,
                            onCheckedChange = {
                                if (isLoggedIn) {
                                    syncedPlaylist = !syncedPlaylist
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.login_to_youtube_music_first),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        )
                    }
                }
            }
        }
    )
}
