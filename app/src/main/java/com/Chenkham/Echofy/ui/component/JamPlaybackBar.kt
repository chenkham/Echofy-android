package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.Chenkham.Echofy.R

@Composable
internal fun JamPlaybackBar(
    canControlPlayback: Boolean,
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D0D))
                .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onToggleShuffle,
                enabled = canControlPlayback,
            ) {
                Icon(
                    painter = painterResource(if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint =
                        when {
                            !canControlPlayback -> Color.White.copy(alpha = 0.36f)
                            shuffleEnabled -> Color(0xFF1DB954)
                            else -> Color.White.copy(alpha = 0.72f)
                        },
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = onSkipPrevious,
                enabled = canControlPlayback && canSkipPrevious,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    tint = if (canControlPlayback && canSkipPrevious) Color.White else Color.White.copy(alpha = 0.36f),
                    modifier = Modifier.size(28.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(if (canControlPlayback) Color.White else Color(0xFF2A2A2A))
                        .clickable(enabled = canControlPlayback, onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (canControlPlayback) Color.Black else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(
                onClick = onSkipNext,
                enabled = canControlPlayback && canSkipNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    tint = if (canControlPlayback && canSkipNext) Color.White else Color.White.copy(alpha = 0.36f),
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(
                onClick = onToggleRepeat,
                enabled = canControlPlayback,
            ) {
                val repeatIcon =
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                        else -> R.drawable.repeat
                    }
                Icon(
                    painter = painterResource(repeatIcon),
                    contentDescription = "Repeat",
                    tint =
                        when {
                            !canControlPlayback -> Color.White.copy(alpha = 0.36f)
                            repeatMode != Player.REPEAT_MODE_OFF -> Color(0xFF1DB954)
                            else -> Color.White.copy(alpha = 0.72f)
                        },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
