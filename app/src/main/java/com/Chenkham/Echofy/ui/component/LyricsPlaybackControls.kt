package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.utils.makeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.LyricsPlaybackControls(
    visible: Boolean,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    playbackState: Int,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isLiked: Boolean,
    onSeekTo: (Long) -> Unit,
    onToggleRepeatMode: () -> Unit,
    onSeekToPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekToNext: () -> Unit,
    onToggleLike: () -> Unit,
) {
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .zIndex(2f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val sliderFraction =
                if (duration > 0) {
                    ((sliderPosition ?: position).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            val sliderBrush = Brush.horizontalGradient(listOf(Color(0xFF81D4FA), Color(0xFFCE93D8)))

            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { sliderPosition = it.toLong() },
                onValueChangeFinished = {
                    sliderPosition?.let { onSeekTo(it) }
                    sliderPosition = null
                },
                colors = SliderDefaults.colors(thumbColor = Color.White),
                thumb = {
                    Box(modifier = Modifier.size(12.dp).background(Color.White, CircleShape))
                },
                track = {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth(fraction = sliderFraction.coerceAtLeast(0.001f))
                                    .fillMaxHeight()
                                    .background(brush = sliderBrush, shape = CircleShape),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(24.dp),
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = onToggleRepeatMode,
                    modifier = Modifier.size(48.dp).alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                },
                            ),
                        contentDescription = "Repeat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = onSeekToPrevious,
                    enabled = canSkipPrevious,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(64.dp).border(width = 1.dp, brush = sliderBrush, shape = CircleShape),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                when {
                                    playbackState == STATE_ENDED -> R.drawable.replay
                                    isPlaying -> R.drawable.pause
                                    else -> R.drawable.play
                                },
                            ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(
                    onClick = onSeekToNext,
                    enabled = canSkipNext,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (isLiked) R.drawable.heart_fill else R.drawable.heart,
                            ),
                        contentDescription = "Like",
                        tint = if (isLiked) Color(0xFFFF4081) else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
