package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding
import com.Chenkham.Echofy.extensions.toggleRepeatMode
import com.Chenkham.Echofy.playback.PlayerConnection

@Composable
fun PlayerPlaybackControls(
    playerConnection: PlayerConnection,
    isPlaying: Boolean,
    playbackState: Int,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onBackgroundColor: Color,
    playPauseShape: Shape,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding),
    ) {
        // Shuffle
        Box(modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .clickable { playerConnection.toggleShuffle() },
            ) {
                Image(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = stringResource(R.string.acc_shuffle),
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier =
                        Modifier
                            .size(32.dp)
                            .alpha(if (shuffleModeEnabled) 1f else 0.72f),
                )
            }
        }

        // Skip Previous
        Box(modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(38.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .alpha(if (canSkipPrevious) 1f else 0.38f)
                        .clickable(enabled = canSkipPrevious) { playerConnection.seekToPrevious() },
            ) {
                Image(
                    painter = painterResource(R.drawable.player_skip_previous),
                    contentDescription = stringResource(R.string.acc_skip_prev),
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier = Modifier.size(50.dp),
                )
            }
        }

        // Play/Pause
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(86.dp)
                    .clip(playPauseShape)
                    .background(onBackgroundColor)
                    .clickable {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.playWhenReady = !isPlaying
                        }
                    },
        ) {
            if (playbackState == Player.STATE_BUFFERING) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                    strokeWidth = 3.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent,
                )
            }
            Image(
                painter =
                    painterResource(
                        if (isPlaying && playbackState != STATE_ENDED) {
                            R.drawable.apple_pause
                        } else {
                            R.drawable.apple_play
                        },
                    ),
                contentDescription =
                    stringResource(
                        if (isPlaying && playbackState != STATE_ENDED) R.string.acc_pause else R.string.acc_play,
                    ),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.surface),
                modifier = Modifier.size(44.dp),
            )
        }

        // Skip Next
        Box(modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(38.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .alpha(if (canSkipNext) 1f else 0.38f)
                        .clickable(enabled = canSkipNext) { playerConnection.seekToNext() },
            ) {
                Image(
                    painter = painterResource(R.drawable.player_skip_next),
                    contentDescription = stringResource(R.string.acc_skip_next),
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier = Modifier.size(50.dp),
                )
            }
        }

        // Repeat Mode
        Box(modifier = Modifier.weight(1f)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .clickable { playerConnection.player.toggleRepeatMode() },
            ) {
                Image(
                    painter =
                        painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> R.drawable.repeat
                            },
                        ),
                    contentDescription = stringResource(R.string.acc_repeat),
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier =
                        Modifier
                            .size(32.dp)
                            .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.72f else 1f),
                )
            }
        }
    }
}
