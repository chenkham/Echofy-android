package com.Chenkham.Echofy.ui.menu

import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.db.update
import com.Chenkham.Echofy.db.artistSongs

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ArtistSongSortType
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.ui.component.ArtistListItem
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.Chenkham.Echofy.ui.component.GridMenuItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ArtistMenu(
    originalArtist: Artist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val artistState = database.artist(originalArtist.id).collectAsState(initial = originalArtist)
    val artist = artistState.value ?: originalArtist


    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 8.dp
        ),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                ArtistListItem(
                    artist = artist,
                    badges = {},
                    trailingContent = {
                        IconButton(
                            onClick = {
                                database.transaction {
                                    update(artist.artist.toggleLike())
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(if (artist.artist.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                                tint = if (artist.artist.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                contentDescription = null,
                            )
                        }
                    },
                )
                HorizontalDivider()
            }
        }

        if (artist.songCount > 0) {
            GridMenuItem(
                icon = R.drawable.play,
                title = R.string.play,
            ) {
                coroutineScope.launch {
                    val songs =
                        withContext(Dispatchers.IO) {
                            database
                                .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                .first()
                                .map { it.toMediaItem() }
                        }
                    playerConnection.playQueue(
                        ListQueue(
                            title = artist.artist.name,
                            items = songs,
                        ),
                    )
                }
                onDismiss()
            }
            GridMenuItem(
                icon = R.drawable.shuffle,
                title = R.string.shuffle,
            ) {
                coroutineScope.launch {
                    val songs =
                        withContext(Dispatchers.IO) {
                            database
                                .artistSongs(artist.id, ArtistSongSortType.CREATE_DATE, true)
                                .first()
                                .map { it.toMediaItem() }
                                .shuffled()
                        }
                    playerConnection.playQueue(
                        ListQueue(
                            title = artist.artist.name,
                            items = songs,
                        ),
                    )
                }
                onDismiss()
            }
        }
        if (artist.artist.isYouTubeArtist) {
            GridMenuItem(
                icon = R.drawable.share,
                title = R.string.share,
            ) {
                onDismiss()
                val intent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "https://music.youtube.com/channel/${artist.id}"
                        )
                    }
                context.startActivity(Intent.createChooser(intent, null))
            }
        }
    }
}
