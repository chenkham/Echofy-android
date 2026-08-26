package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.arturo254.opentune.spotify.SpotifyMapper
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import com.arturo254.opentune.spotify.models.SpotifyTrack
import com.Chenkham.Echofy.ui.utils.resize
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString

@Composable
fun SpotifyLibraryPlaylistListItem(
    playlist: SpotifyPlaylist,
    navController: NavController,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
) {
    val libraryPlaylist = remember(playlist) { playlist.toLibraryPlaylist() }
    val openPlaylist = {
        navController.navigate("spotify_playlist/${playlist.id}")
    }

    PlaylistListItem(
        playlist = libraryPlaylist,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = openPlaylist),
    )
}

@Composable
fun SpotifyLibraryPlaylistGridItem(
    playlist: SpotifyPlaylist,
    navController: NavController,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = false,
) {
    val openPlaylist = {
        navController.navigate("spotify_playlist/${playlist.id}")
    }

    GridItem(
        modifier = modifier.clickable(onClick = openPlaylist),
        title = playlist.name,
        subtitle = playlist.tracks?.total?.let { "${it} songs" } ?: "Spotify",
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        thumbnailContent = {
            AsyncImage(
                model = SpotifyMapper.getPlaylistThumbnail(playlist)?.resize(400, 400),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
        },
        fillMaxWidth = fillMaxWidth,
    )
}

@Composable
fun SpotifyTrackListItem(
    track: SpotifyTrack,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    badges: @Composable RowScope.() -> Unit = {
        if (track.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    },
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showSongIconPlaceholder: Boolean = true,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val duration =
        track.durationMs
            .takeIf { it > 0 }
            ?.toLong()
            ?.let(::makeTimeString)
    val subtitle =
        joinByBullet(
            track.artists.joinToString { it.name },
            duration,
        )

    ListItem(
        title = track.name,
        subtitle = subtitle,
        badges = badges,
        thumbnailContent = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ListThumbnailSize),
            ) {
                if (albumIndex != null) {
                    AnimatedVisibility(
                        visible = !isActive,
                        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                        exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                modifier = Modifier.align(Alignment.Center),
                                contentDescription = null,
                            )
                        } else {
                            Text(
                                text = albumIndex.toString(),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                } else {
                    if (isSelected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1000f)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                modifier = Modifier.align(Alignment.Center),
                                contentDescription = null,
                            )
                        }
                    }
                    AsyncImage(
                        model = SpotifyMapper.getTrackThumbnail(track)?.resize(200, 200),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    )
                }

                PlayingIndicatorBox(
                    isActive = isActive,
                    playWhenReady = isPlaying,
                    color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = if (albumIndex != null) Color.Transparent else Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                )
            }
        },
        trailingContent = trailingContent,
        modifier = modifier,
        isActive = isActive,
    )
}

private fun SpotifyPlaylist.toLibraryPlaylist(): Playlist =
    Playlist(
        playlist =
            PlaylistEntity(
                id = "SPOTIFY_PLAYLIST_$id",
                name = name,
                thumbnailUrl = SpotifyMapper.getPlaylistThumbnail(this),
                remoteSongCount = tracks?.total ?: 0,
                isEditable = false,
            ),
        songCount = tracks?.total ?: 0,
        thumbnails = images.map { it.url },
    )
