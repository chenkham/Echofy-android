package com.Chenkham.Echofy.ui.player

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.ExoDownloadService
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.utils.ShareUtils
import com.Chenkham.Echofy.utils.makeTimeString

@Composable
fun PlayerActionBar(
    mediaMetadata: MediaMetadata,
    isLiked: Boolean,
    actionButtonColor: Color,
    onBackgroundColor: Color,
    buttonShape: Shape,
    songlinkEnabled: Boolean,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    playerConnection: PlayerConnection,
    downloadUtil: DownloadUtil,
    context: Context,
    onOpenFullscreenLyrics: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenSonglink: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
                .horizontalScroll(rememberScrollState()),
    ) {
        // Like button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(buttonShape)
                    .background(if (isLiked) MaterialTheme.colorScheme.primaryContainer else actionButtonColor)
                    .clickable { playerConnection.toggleLike() },
        ) {
            Image(
                painter = painterResource(if (isLiked) R.drawable.heart_fill else R.drawable.heart),
                contentDescription = stringResource(if (isLiked) R.string.acc_unfavorite else R.string.acc_favorite),
                colorFilter = ColorFilter.tint(onBackgroundColor),
                modifier = Modifier.size(24.dp),
            )
        }

        // Lyrics button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(buttonShape)
                    .background(actionButtonColor)
                    .clickable(onClick = onOpenFullscreenLyrics),
        ) {
            Image(
                painter = painterResource(R.drawable.apple_lyrics),
                contentDescription = stringResource(R.string.acc_lyrics),
                colorFilter = ColorFilter.tint(onBackgroundColor),
                modifier = Modifier.size(24.dp),
            )
        }

        // Save button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(buttonShape)
                    .background(actionButtonColor)
                    .clickable(onClick = onAddToPlaylist),
        ) {
            Image(
                painter = painterResource(R.drawable.playlist_add),
                contentDescription = stringResource(R.string.acc_add_to_playlist),
                colorFilter = ColorFilter.tint(onBackgroundColor),
                modifier = Modifier.size(24.dp),
            )
        }

        // Share button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(buttonShape)
                    .background(actionButtonColor)
                    .clickable {
                        if (songlinkEnabled) {
                            onOpenSonglink()
                        } else {
                            val shareText =
                                ShareUtils.buildTrackShareText(
                                    context = context,
                                    songId = mediaMetadata.id,
                                    title = mediaMetadata.title,
                                    artist = mediaMetadata.artists.joinToString { it.name },
                                )
                            val intent =
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    },
        ) {
            Image(
                painter = painterResource(R.drawable.share),
                contentDescription = stringResource(R.string.acc_share),
                colorFilter = ColorFilter.tint(onBackgroundColor),
                modifier = Modifier.size(24.dp),
            )
        }

        // Radio button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(buttonShape)
                    .background(actionButtonColor)
                    .clickable { playerConnection.service.startRadioSeamlessly() },
        ) {
            Image(
                painter = painterResource(R.drawable.radio),
                contentDescription = null,
                colorFilter = ColorFilter.tint(onBackgroundColor),
                modifier = Modifier.size(24.dp),
            )
        }

        // Download button
        Box(modifier = Modifier) {
            val download by remember(mediaMetadata.id) {
                downloadUtil.getDownload(mediaMetadata.id)
            }.collectAsState(initial = null)

            val iconResource =
                when (download?.state) {
                    Download.STATE_COMPLETED -> R.drawable.offline
                    Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> R.drawable.downloading
                    else -> R.drawable.download
                }

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(buttonShape)
                        .background(actionButtonColor)
                        .clickable {
                            if (download?.state == Download.STATE_COMPLETED || download?.state == Download.STATE_DOWNLOADING || download?.state == Download.STATE_QUEUED) {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    mediaMetadata.id,
                                    false,
                                )
                            } else {
                                val downloadRequest =
                                    DownloadRequest
                                        .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                        .setCustomCacheKey(mediaMetadata.id)
                                        .setData(mediaMetadata.title.toByteArray())
                                        .build()
                                DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false,
                                )
                            }
                        },
            ) {
                if (download?.state == Download.STATE_DOWNLOADING || download?.state == Download.STATE_QUEUED) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = onBackgroundColor,
                    )
                } else {
                    Image(
                        painter = painterResource(iconResource),
                        contentDescription = stringResource(R.string.acc_download),
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        // Sleep Timer
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(42.dp)
                    .clip(buttonShape)
                    .background(actionButtonColor),
        ) {
            AnimatedContent(
                label = "sleepTimer",
                targetState = sleepTimerEnabled,
            ) { enabled ->
                if (enabled) {
                    Text(
                        text = makeTimeString(sleepTimerTimeLeft),
                        style = MaterialTheme.typography.labelLarge,
                        color = onBackgroundColor,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = playerConnection.service.sleepTimer::clear)
                                .basicMarquee(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(onClick = onOpenSleepTimer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.bedtime),
                            colorFilter = ColorFilter.tint(onBackgroundColor),
                            contentDescription = stringResource(R.string.acc_sleep_timer),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
