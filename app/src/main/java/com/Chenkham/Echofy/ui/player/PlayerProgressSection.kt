package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding
import com.Chenkham.Echofy.constants.SliderStyle
import com.Chenkham.Echofy.constants.WaveformHeatmapScrubberEnabledKey
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.PlayerSliderTrack
import com.Chenkham.Echofy.utils.makeTimeString
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.squiggles.SquigglySlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressSection(
    playerConnection: PlayerConnection,
    sliderStyle: SliderStyle,
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }

    LaunchedEffect(isPlaying) {
        while (isActive) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
            delay(500L)
        }
    }

    val waveformHeatmapScrubberEnabled by rememberPreference(WaveformHeatmapScrubberEnabledKey, defaultValue = false)

    val ytMusicProgress =
        remember(sliderPosition, position, duration) {
            val current = (sliderPosition ?: position).toFloat()
            val total = if (duration == C.TIME_UNSET || duration <= 0) 1f else duration.toFloat()
            (current / total).coerceIn(0f, 1f)
        }

    val currentVal = (sliderPosition ?: position).toFloat()
    val maxVal = if (duration == C.TIME_UNSET) 0f else duration.toFloat()
    val onSeek: (Float) -> Unit = { sliderPosition = it.toLong() }
    val onSeekFinished: () -> Unit = {
        sliderPosition?.let {
            playerConnection.player.seekTo(it)
            position = it
        }
        sliderPosition = null
    }

    val defaultSliderColors =
        SliderDefaults.colors(
            activeTrackColor = color,
            inactiveTrackColor = Color.Gray,
            activeTickColor = color,
            inactiveTickColor = Color.Gray,
            thumbColor = color,
        )

    Column(modifier = modifier.fillMaxWidth()) {
        if (waveformHeatmapScrubberEnabled) {
            PlayerWaveformScrubber(
                currentMediaId = playerConnection.player.currentMediaItem?.mediaId.orEmpty(),
                progressFraction = ytMusicProgress,
                color = color,
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding, vertical = 2.dp),
            )
        }

        when (sliderStyle) {
            SliderStyle.DEFAULT -> {
                Slider(
                    value = currentVal,
                    valueRange = 0f..maxVal,
                    onValueChange = onSeek,
                    onValueChangeFinished = onSeekFinished,
                    colors = defaultSliderColors,
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                )
            }

            SliderStyle.SQUIGGLY -> {
                SquigglySlider(
                    value = currentVal,
                    valueRange = 0f..maxVal,
                    onValueChange = onSeek,
                    onValueChangeFinished = onSeekFinished,
                    colors = defaultSliderColors,
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    squigglesSpec =
                        SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                            strokeWidth = 3.dp,
                        ),
                )
            }

            SliderStyle.SLIM -> {
                Slider(
                    value = currentVal,
                    valueRange = 0f..maxVal,
                    onValueChange = onSeek,
                    onValueChangeFinished = onSeekFinished,
                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                    track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = defaultSliderColors) },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                )
            }

            SliderStyle.YOUTUBE_MUSIC -> {
                Slider(
                    value = currentVal,
                    valueRange = 0f..maxVal,
                    onValueChange = onSeek,
                    onValueChangeFinished = onSeekFinished,
                    thumb = {
                        Box(
                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color),
                        )
                    },
                    track = {
                        Box(
                            modifier =
                                Modifier
                                    .height(3.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(color.copy(alpha = 0.2f)),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = ytMusicProgress)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(color),
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                )
            }

            SliderStyle.VINTAGE_CABLE -> {
                Slider(
                    value = currentVal,
                    valueRange = 0f..maxVal,
                    onValueChange = onSeek,
                    onValueChangeFinished = onSeekFinished,
                    thumb = {
                        VintageCableCarThumb(
                            tint = color,
                            modifier = Modifier.size(width = 30.dp, height = 24.dp),
                        )
                    },
                    track = {
                        Box(
                            modifier =
                                Modifier
                                    .height(2.5.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(color.copy(alpha = 0.25f)),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = ytMusicProgress)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(color),
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp),
        ) {
            Text(
                text = makeTimeString(sliderPosition ?: position),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = if (duration != C.TIME_UNSET && duration > 0) makeTimeString(duration) else "LIVE",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
