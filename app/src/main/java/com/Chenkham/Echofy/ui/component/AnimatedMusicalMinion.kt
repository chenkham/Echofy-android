package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedMusicalMinion(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minionDance")

    // Head/Body bobbing animation
    val bodyBob by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bodyBob"
    )

    // Eye looking around animation
    val eyePupilOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eyePupilOffset"
    )

    // Equalizer wave bar heights
    val eq1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val eq2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val eq3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    // Floating musical note animations
    val note1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -28f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "note1Y"
    )
    val note1Alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "note1Alpha"
    )

    val note2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -32f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 300, easing = LinearEasing), RepeatMode.Restart),
        label = "note2Y"
    )
    val note2Alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 300, easing = LinearEasing), RepeatMode.Restart),
        label = "note2Alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(105.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width(220.dp)
                .height(100.dp)
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f + bodyBob

            // 1. Equalizer bars on left and right
            val eqPrimary = Color(0xFF64B5F6)
            val eqSecondary = Color(0xFFFF4081)

            // Left EQ
            drawRoundRect(
                color = eqPrimary.copy(alpha = 0.85f),
                topLeft = Offset(centerX - 80f, centerY - 20f * eq1),
                size = Size(6f, 40f * eq1),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = eqSecondary.copy(alpha = 0.85f),
                topLeft = Offset(centerX - 68f, centerY - 25f * eq2),
                size = Size(6f, 50f * eq2),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = eqPrimary.copy(alpha = 0.85f),
                topLeft = Offset(centerX - 56f, centerY - 20f * eq3),
                size = Size(6f, 40f * eq3),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // Right EQ
            drawRoundRect(
                color = eqPrimary.copy(alpha = 0.85f),
                topLeft = Offset(centerX + 50f, centerY - 20f * eq3),
                size = Size(6f, 40f * eq3),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = eqSecondary.copy(alpha = 0.85f),
                topLeft = Offset(centerX + 62f, centerY - 25f * eq1),
                size = Size(6f, 50f * eq1),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = eqPrimary.copy(alpha = 0.85f),
                topLeft = Offset(centerX + 74f, centerY - 20f * eq2),
                size = Size(6f, 40f * eq2),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 2. Minion Body (Yellow Capsule)
            val minionYellow = Color(0xFFFFD54F)
            val minionShadow = Color(0xFFFFB300)
            val bodyWidth = 56f
            val bodyHeight = 76f
            val bodyLeft = centerX - bodyWidth / 2f
            val bodyTop = centerY - bodyHeight / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(minionYellow, minionShadow),
                    startY = bodyTop,
                    endY = bodyTop + bodyHeight
                ),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(bodyWidth / 2f, bodyWidth / 2f)
            )

            // 3. Blue Overalls
            val denimBlue = Color(0xFF1E88E5)
            val denimDark = Color(0xFF1565C0)
            val pantsHeight = 28f
            val pantsTop = bodyTop + bodyHeight - pantsHeight

            // Main overalls base
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(denimBlue, denimDark),
                    startY = pantsTop,
                    endY = pantsTop + pantsHeight
                ),
                topLeft = Offset(bodyLeft, pantsTop),
                size = Size(bodyWidth, pantsHeight),
                cornerRadius = CornerRadius(0f, 0f)
            )
            // Bottom rounded corners
            drawRoundRect(
                color = denimDark,
                topLeft = Offset(bodyLeft, bodyTop + bodyHeight - 12f),
                size = Size(bodyWidth, 12f),
                cornerRadius = CornerRadius(bodyWidth / 2f, bodyWidth / 2f)
            )

            // Overalls Bib (Center Chest)
            drawRoundRect(
                color = denimBlue,
                topLeft = Offset(centerX - 16f, pantsTop - 10f),
                size = Size(32f, 14f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Overalls Straps
            drawLine(
                color = denimDark,
                start = Offset(bodyLeft + 4f, pantsTop - 16f),
                end = Offset(centerX - 12f, pantsTop),
                strokeWidth = 5f
            )
            drawLine(
                color = denimDark,
                start = Offset(bodyLeft + bodyWidth - 4f, pantsTop - 16f),
                end = Offset(centerX + 12f, pantsTop),
                strokeWidth = 5f
            )

            // 4. Goggle Strap
            drawLine(
                color = Color(0xFF212121),
                start = Offset(bodyLeft, bodyTop + 24f),
                end = Offset(bodyLeft + bodyWidth, bodyTop + 24f),
                strokeWidth = 7f
            )

            // 5. Goggle Ring (Silver)
            val goggleRadius = 13f
            drawCircle(
                color = Color(0xFF90A4AE),
                radius = goggleRadius + 2.5f,
                center = Offset(centerX, bodyTop + 24f)
            )
            drawCircle(
                color = Color(0xFFCFD8DC),
                radius = goggleRadius,
                center = Offset(centerX, bodyTop + 24f)
            )

            // Eye White
            drawCircle(
                color = Color.White,
                radius = goggleRadius - 2f,
                center = Offset(centerX, bodyTop + 24f)
            )

            // Iris (Brown)
            val irisCenter = Offset(centerX + eyePupilOffset, bodyTop + 24f)
            drawCircle(
                color = Color(0xFF795548),
                radius = 5.5f,
                center = irisCenter
            )

            // Pupil (Black) + Glint
            drawCircle(
                color = Color.Black,
                radius = 3.5f,
                center = irisCenter
            )
            drawCircle(
                color = Color.White,
                radius = 1.2f,
                center = Offset(irisCenter.x - 1f, irisCenter.y - 1f)
            )

            // 6. Cute Singing Mouth (Smile)
            val mouthPath = Path().apply {
                moveTo(centerX - 7f, bodyTop + 42f)
                quadraticBezierTo(
                    centerX, bodyTop + 48f,
                    centerX + 7f, bodyTop + 42f
                )
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF5D4037),
                style = Stroke(width = 2.5f)
            )

            // 7. Headphones
            val headphoneColor = Color(0xFFFF1744) // Vibrant red/pink headphone
            // Headband Arc
            val headbandPath = Path().apply {
                moveTo(bodyLeft - 3f, bodyTop + 24f)
                cubicTo(
                    bodyLeft - 2f, bodyTop - 12f,
                    bodyLeft + bodyWidth + 2f, bodyTop - 12f,
                    bodyLeft + bodyWidth + 3f, bodyTop + 24f
                )
            }
            drawPath(
                path = headbandPath,
                color = Color(0xFF37474F),
                style = Stroke(width = 4.5f)
            )

            // Left Earcup
            drawRoundRect(
                color = headphoneColor,
                topLeft = Offset(bodyLeft - 7f, bodyTop + 14f),
                size = Size(8f, 20f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Right Earcup
            drawRoundRect(
                color = headphoneColor,
                topLeft = Offset(bodyLeft + bodyWidth - 1f, bodyTop + 14f),
                size = Size(8f, 20f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 8. Floating Musical Notes
            val noteColor1 = Color(0xFFFFD54F).copy(alpha = note1Alpha)
            val noteColor2 = Color(0xFF80D8FF).copy(alpha = note2Alpha)

            // Note 1 (Left floating ♪)
            val n1X = centerX - 36f
            val n1Y = bodyTop + note1Y
            drawCircle(color = noteColor1, radius = 3.5f, center = Offset(n1X, n1Y))
            drawLine(color = noteColor1, start = Offset(n1X + 3.5f, n1Y), end = Offset(n1X + 3.5f, n1Y - 10f), strokeWidth = 2f)
            drawLine(color = noteColor1, start = Offset(n1X + 3.5f, n1Y - 10f), end = Offset(n1X + 8f, n1Y - 7f), strokeWidth = 2f)

            // Note 2 (Right floating ♫)
            val n2X = centerX + 36f
            val n2Y = bodyTop + note2Y
            drawCircle(color = noteColor2, radius = 3f, center = Offset(n2X, n2Y))
            drawCircle(color = noteColor2, radius = 3f, center = Offset(n2X + 8f, n2Y - 2f))
            drawLine(color = noteColor2, start = Offset(n2X + 3f, n2Y), end = Offset(n2X + 3f, n2Y - 10f), strokeWidth = 2f)
            drawLine(color = noteColor2, start = Offset(n2X + 11f, n2Y - 2f), end = Offset(n2X + 11f, n2Y - 12f), strokeWidth = 2f)
            drawLine(color = noteColor2, start = Offset(n2X + 3f, n2Y - 10f), end = Offset(n2X + 11f, n2Y - 12f), strokeWidth = 2.5f)
        }
    }
}
