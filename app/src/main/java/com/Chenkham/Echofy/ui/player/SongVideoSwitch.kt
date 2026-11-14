package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.constants.PlaybackMode

@Composable
fun SongVideoSwitch(
    selectedMode: PlaybackMode,
    onModeChange: (PlaybackMode) -> Unit,
    isVideoAvailable: Boolean = true,
) {
    val effectiveMode =
        if (!isVideoAvailable && selectedMode == PlaybackMode.VIDEO) {
            PlaybackMode.AUDIO
        } else {
            selectedMode
        }
    LaunchedEffect(isVideoAvailable, selectedMode) {
        if (!isVideoAvailable && selectedMode == PlaybackMode.VIDEO) {
            onModeChange(PlaybackMode.AUDIO)
        }
    }

    Row(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape,
                )
                .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SwitchOption(
            text = "Song",
            isSelected = effectiveMode == PlaybackMode.AUDIO,
            onClick = { onModeChange(PlaybackMode.AUDIO) },
        )
        Spacer(modifier = Modifier.width(4.dp))
        SwitchOption(
            text = "Video",
            isSelected = effectiveMode == PlaybackMode.VIDEO,
            onClick = { onModeChange(PlaybackMode.VIDEO) },
            enabled = isVideoAvailable,
        )
    }
}

@Composable
fun SwitchOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                )
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}
