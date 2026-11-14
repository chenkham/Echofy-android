package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

val LyricsPreviewTime = 2.seconds
const val ANIMATE_SCROLL_DURATION = 300L

/**
 * Lets the user nudge lyric timing when it drifts against the audio.
 * Range is +/-10s in 0.5s steps; positive values make lyrics appear earlier.
 */
@Composable
fun LyricsSyncDialog(
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val step = 500L
    val range = 10_000L

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(R.string.lyrics_sync_title)) },
        buttons = {
            TextButton(onClick = { onOffsetChange(0L) }) {
                Text(stringResource(R.string.reset))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    ) {
        Text(
            text =
                when {
                    offsetMs > 0 -> stringResource(R.string.lyrics_sync_earlier, offsetMs / 1000f)
                    offsetMs < 0 -> stringResource(R.string.lyrics_sync_later, -offsetMs / 1000f)
                    else -> stringResource(R.string.lyrics_sync_none)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButton(
                onClick = { onOffsetChange((offsetMs - step).coerceAtLeast(-range)) },
                enabled = offsetMs > -range,
            ) {
                Icon(
                    painter = painterResource(R.drawable.remove),
                    contentDescription = stringResource(R.string.lyrics_sync_decrease),
                )
            }

            Text(
                text = String.format(Locale.US, "%+.1fs", offsetMs / 1000f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            IconButton(
                onClick = { onOffsetChange((offsetMs + step).coerceAtMost(range)) },
                enabled = offsetMs < range,
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = stringResource(R.string.lyrics_sync_increase),
                )
            }
        }
    }
}
