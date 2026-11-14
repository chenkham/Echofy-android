package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString

/**
 * Composable item for displaying a song in a vertical list.
 */
@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    isSelected: Boolean = false,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = "Liked",
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = "In library",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            when (download?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = song.song.title,
    subtitle =
        joinByBullet(
            song.artists.joinToString { it.name },
            makeTimeString(song.song.duration * 1000L),
        ),
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
                        modifier =
                            Modifier
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
                // PERFORMANCE: Optimized image loading with caching
                val context = LocalContext.current
                val imageRequest =
                    remember(song.song.thumbnailUrl) {
                        ImageRequest.Builder(context)
                            .data(song.song.thumbnailUrl)
                            .size(256) // Fixed size for consistent caching
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .crossfade(false) // Instant display from cache
                            .allowHardware(true)
                            .build()
                    }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )
            }

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color =
                                if (albumIndex != null) {
                                    Color.Transparent
                                } else {
                                    Color.Black.copy(
                                        alpha = 0.4f,
                                    )
                                },
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)
