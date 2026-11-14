package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.audio.SongChordTimeline
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding

@Composable
fun PlayerChordChip(
    chordTimeline: SongChordTimeline?,
    livePlaybackPos: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chordTimeline == null) return
    val activeChord = chordTimeline.chords.find { livePlaybackPos in it.startMs..it.endMs } ?: return

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier.clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "🎸", fontSize = 14.sp)
                Text(
                    text = "${activeChord.section}:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
                Text(
                    text = activeChord.chord,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                val nextChords =
                    chordTimeline.chords
                        .filter { it.startMs > activeChord.endMs }
                        .take(2)
                        .map { it.chord }
                if (nextChords.isNotEmpty()) {
                    Text(
                        text = "→ ${nextChords.joinToString(" → ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}
