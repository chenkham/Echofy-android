package com.Chenkham.Echofy.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding
import com.Chenkham.Echofy.constants.ShowAudioQualityBadgeKey
import com.Chenkham.Echofy.db.entities.FormatEntity
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.screens.settings.PlayerTextAlignment
import com.Chenkham.Echofy.utils.rememberPreference

@Composable
fun PlayerMediaHeader(
    mediaMetadata: MediaMetadata,
    playerConnection: PlayerConnection,
    playerTextAlignment: PlayerTextAlignment,
    onBackgroundColor: Color,
    navController: NavController,
    bottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
) {
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val showAudioQualityBadge by rememberPreference(ShowAudioQualityBadgeKey, defaultValue = false)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement =
                when (playerTextAlignment) {
                    PlayerTextAlignment.SIDED -> Arrangement.Start
                    PlayerTextAlignment.CENTER -> Arrangement.Center
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
        ) {
            AnimatedContent(
                targetState = mediaMetadata.title,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "mediaTitle",
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor,
                    modifier =
                        Modifier
                            .basicMarquee()
                            .clickable(enabled = mediaMetadata.album != null) {
                                mediaMetadata.album?.let { album ->
                                    navController.navigate("album/${album.id}")
                                    bottomSheetState.collapseSoft()
                                }
                            },
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            horizontalArrangement =
                when (playerTextAlignment) {
                    PlayerTextAlignment.SIDED -> Arrangement.Start
                    PlayerTextAlignment.CENTER -> Arrangement.Center
                },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
        ) {
            mediaMetadata.artists.fastForEachIndexed { index, artist ->
                AnimatedContent(
                    targetState = artist.name,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "artistName",
                ) { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = onBackgroundColor,
                        maxLines = 1,
                        modifier =
                            Modifier.clickable(enabled = artist.id != null) {
                                artist.id?.let { id ->
                                    navController.navigate("artist/$id")
                                    bottomSheetState.collapseSoft()
                                }
                            },
                    )
                }

                if (index != mediaMetadata.artists.lastIndex) {
                    AnimatedContent(
                        targetState = ", ",
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "artistSeparator",
                    ) { comma ->
                        Text(
                            text = comma,
                            style = MaterialTheme.typography.titleMedium,
                            color = onBackgroundColor,
                        )
                    }
                }
            }
        }

        if (showAudioQualityBadge) {
            AudioQualityBadge(
                currentFormat = currentFormat,
                playerConnection = playerConnection,
                onBackgroundColor = onBackgroundColor,
            )
        }
    }
}

@Composable
private fun AudioQualityBadge(
    currentFormat: FormatEntity?,
    playerConnection: PlayerConnection,
    onBackgroundColor: Color,
) {
    val liveAudioFormat = playerConnection.player.audioFormat
    val rawCodec = currentFormat?.mimeType ?: liveAudioFormat?.sampleMimeType ?: "audio/opus"
    val codec = rawCodec.substringAfter("/").substringBefore(";").uppercase()
    val bitrate =
        currentFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" }
            ?: liveAudioFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" }
            ?: "160 kbps"
    val sampleRate =
        currentFormat?.sampleRate?.takeIf { it > 0 }?.let { "${it / 1000} kHz" }
            ?: liveAudioFormat?.sampleRate?.takeIf { it > 0 }?.let { "${it / 1000} kHz" }
            ?: "48 kHz"

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = onBackgroundColor.copy(alpha = 0.85f),
            )
            Text(
                text = "$codec • $bitrate • $sampleRate",
                style = MaterialTheme.typography.labelSmall,
                color = onBackgroundColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
