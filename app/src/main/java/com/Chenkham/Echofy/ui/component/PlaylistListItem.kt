package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastForEachIndexed
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.Playlist

/**
 * Composable item for displaying a playlist in a list layout.
 */
@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
    autoPlaylist: Boolean = false,
) = ListItem(
    title = playlist.playlist.name,
    subtitle =
        if (autoPlaylist) {
            ""
        } else {
            if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
                pluralStringResource(
                    R.plurals.n_song,
                    playlist.playlist.remoteSongCount,
                    playlist.playlist.remoteSongCount,
                )
            } else {
                pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
            }
        },
    thumbnailContent = {
        val painter =
            when (playlist.playlist.name) {
                stringResource(R.string.liked) -> R.drawable.heart
                stringResource(R.string.offline) -> R.drawable.offline
                stringResource(R.string.cached_playlist) -> R.drawable.cached
                else -> {
                    if (autoPlaylist) {
                        R.drawable.trending_up
                    } else {
                        R.drawable.queue_music
                    }
                }
            }
        when (playlist.thumbnails.size) {
            0 ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(ListThumbnailSize / 2)
                                .align(Alignment.Center),
                    )
                }

            1 ->
                AsyncImage(
                    model = playlist.thumbnails[0],
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

            else ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd,
                    ).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .align(alignment)
                                    .size(ListThumbnailSize / 2),
                        )
                    }
                }
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
)
