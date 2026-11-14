package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun VintageCableCarThumb(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val armTop = Offset(w * 0.5f, h * 0.05f)
        val armBottom = Offset(w * 0.5f, h * 0.35f)

        drawCircle(
            color = tint,
            radius = w * 0.09f,
            center = armTop,
        )

        drawLine(
            color = tint,
            start = armTop,
            end = armBottom,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        val cabinLeft = w * 0.1f
        val cabinTop = h * 0.35f
        val cabinWidth = w * 0.8f
        val cabinHeight = h * 0.6f
        val cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())

        drawRoundRect(
            color = tint,
            topLeft = Offset(cabinLeft, cabinTop),
            size = Size(cabinWidth, cabinHeight),
            cornerRadius = cornerRadius,
        )

        val windowTop = cabinTop + cabinHeight * 0.18f
        val windowHeight = cabinHeight * 0.42f
        val windowWidth = cabinWidth * 0.22f
        val windowSpacing = cabinWidth * 0.07f
        val startWindowX = cabinLeft + cabinWidth * 0.10f
        val windowRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())

        for (i in 0..2) {
            val winX = startWindowX + i * (windowWidth + windowSpacing)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(winX, windowTop),
                size = Size(windowWidth, windowHeight),
                cornerRadius = windowRadius,
            )
        }

        drawLine(
            color = tint.copy(alpha = 0.8f),
            start = Offset(cabinLeft - 1.dp.toPx(), cabinTop + cabinHeight),
            end = Offset(cabinLeft + cabinWidth + 1.dp.toPx(), cabinTop + cabinHeight),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
