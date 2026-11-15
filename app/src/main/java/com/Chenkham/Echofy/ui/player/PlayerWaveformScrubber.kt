package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlayerWaveformScrubber(
    currentMediaId: String,
    progressFraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val hash = remember(currentMediaId) { currentMediaId.hashCode().let { if (it < 0) -it else it } }

    Box(
        modifier = modifier.fillMaxWidth().height(22.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = 44
            val barWidth = (size.width / barCount) * 0.7f
            val spacing = size.width / barCount

            for (i in 0 until barCount) {
                val barHash = ((hash + i * 37) % 100) / 100f
                val barHeight = (0.25f + barHash * 0.75f) * size.height
                val x = i * spacing + (spacing - barWidth) / 2
                val y = (size.height - barHeight) / 2
                val isPast = (i.toFloat() / barCount) <= progressFraction

                drawRoundRect(
                    color = if (isPast) color else Color.Gray.copy(alpha = 0.35f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}
