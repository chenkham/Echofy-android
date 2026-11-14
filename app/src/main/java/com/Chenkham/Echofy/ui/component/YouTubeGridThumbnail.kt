package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.playback.queues.LocalAlbumRadio
import com.Chenkham.Echofy.utils.reportException
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.YTItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Thumbnail container with quick-play and active playing state overlays for YouTube grid items.
 */
@Composable
fun YouTubeGridThumbnail(
    item: YTItem,
    thumbnailShape: Shape,
    thumbnailRatio: Float,
    isActive: Boolean,
    isPlaying: Boolean,
    coroutineScope: CoroutineScope?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxSize()
                .aspectRatio(thumbnailRatio)
                .clip(thumbnailShape),
    ) {
        val context = LocalContext.current
        val imageRequest =
            remember(item.thumbnail) {
                ImageRequest.Builder(context)
                    .data(item.thumbnail)
                    .size(544)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .allowHardware(true)
                    .build()
            }
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        AnimatedVisibility(
            visible = item is AlbumItem && !isActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        ) {
            val database = LocalDatabase.current
            val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable {
                            var playlistId = ""
                            coroutineScope?.launch(Dispatchers.IO) {
                                var albumWithSongs = database.albumWithSongs(item.id).first()
                                if (albumWithSongs?.songs.isNullOrEmpty()) {
                                    YouTube.album(item.id).onSuccess { albumPage ->
                                        playlistId = albumPage.album.playlistId
                                        database.transaction { insert(albumPage) }
                                        albumWithSongs = database.albumWithSongs(item.id).first()
                                    }.onFailure {
                                        reportException(it)
                                    }
                                }
                                albumWithSongs?.let {
                                    withContext(Dispatchers.Main) {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(LocalAlbumRadio(it))
                                    }
                                }
                            }
                        },
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                            shape = thumbnailShape,
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = item is SongItem && !(isActive && isPlaying),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}
