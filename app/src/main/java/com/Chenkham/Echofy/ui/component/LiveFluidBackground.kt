package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.Chenkham.Echofy.constants.LiveFluidColorPalette
import kotlin.math.min

fun LiveFluidColorPalette.displayName(): String =
    when (this) {
        LiveFluidColorPalette.ALBUM -> "Album Colors"
        LiveFluidColorPalette.ECHOFY -> "Echofy Glow"
        LiveFluidColorPalette.OCEAN -> "Ocean Blue"
        LiveFluidColorPalette.SUNSET -> "Sunset Heat"
        LiveFluidColorPalette.ROSE -> "Rose Neon"
        LiveFluidColorPalette.EMERALD -> "Emerald Wave"
        LiveFluidColorPalette.MONO -> "Monochrome"
    }

fun LiveFluidColorPalette.prefersArtworkColors(): Boolean = this == LiveFluidColorPalette.ALBUM

fun LiveFluidColorPalette.fallbackColors(): List<Color> =
    when (this) {
        LiveFluidColorPalette.ALBUM -> listOf(
            Color(0xFF7C4DFF),
            Color(0xFF00C2FF),
            Color(0xFFFF7A59),
        )

        LiveFluidColorPalette.ECHOFY -> listOf(
            Color(0xFF7C4DFF),
            Color(0xFF00C2FF),
            Color(0xFFFF7A59),
        )

        LiveFluidColorPalette.OCEAN -> listOf(
            Color(0xFF0057FF),
            Color(0xFF00B8D9),
            Color(0xFF82F7FF),
        )

        LiveFluidColorPalette.SUNSET -> listOf(
            Color(0xFFFF5A36),
            Color(0xFFFF8A00),
            Color(0xFFFFD166),
        )

        LiveFluidColorPalette.ROSE -> listOf(
            Color(0xFFFF4D8D),
            Color(0xFFC44DFF),
            Color(0xFFFFB3C7),
        )

        LiveFluidColorPalette.EMERALD -> listOf(
            Color(0xFF00A86B),
            Color(0xFF00C897),
            Color(0xFFB7FFDA),
        )

        LiveFluidColorPalette.MONO -> listOf(
            Color(0xFFF2F2F2),
            Color(0xFF8A8A8A),
            Color(0xFF1E1E1E),
        )
    }

@Composable
fun LiveFluidBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    fallbackColors: List<Color> = listOf(
        Color(0xFF7C4DFF),
        Color(0xFF00C2FF),
        Color(0xFFFF7A59),
    ),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveFluidBackground")

    val blob1X = infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob1X",
    ).value
    val blob1Y = infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob1Y",
    ).value
    val blob2X = infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob2X",
    ).value
    val blob2Y = infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.84f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob2Y",
    ).value
    val blob3X = infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob3X",
    ).value
    val blob3Y = infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob3Y",
    ).value
    val blob4X = infiniteTransition.animateFloat(
        initialValue = 0.58f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob4X",
    ).value
    val blob4Y = infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blob4Y",
    ).value

    val palette = remember(colors, fallbackColors) {
        buildList {
            addAll(colors.filter { it.alpha > 0f }.take(4))
            fallbackColors.forEach { color ->
                if (size >= 4) return@forEach
                add(color)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .alpha(alpha),
    ) {
        val width = size.width
        val height = size.height
        val baseRadius = min(width, height) * 0.8f

        fun fluidColor(index: Int, tintAlpha: Float): Color =
            palette.getOrElse(index) { fallbackColors[index % fallbackColors.size] }.copy(alpha = tintAlpha)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    fluidColor(0, 0.12f),
                    fluidColor(1, 0.08f),
                    Color.Transparent,
                ),
            ),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fluidColor(0, 0.72f), Color.Transparent),
                center = Offset(width * blob1X, height * blob1Y),
                radius = baseRadius,
            ),
            radius = baseRadius,
            center = Offset(width * blob1X, height * blob1Y),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fluidColor(1, 0.58f), Color.Transparent),
                center = Offset(width * blob2X, height * blob2Y),
                radius = baseRadius * 0.92f,
            ),
            radius = baseRadius * 0.92f,
            center = Offset(width * blob2X, height * blob2Y),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fluidColor(2, 0.62f), Color.Transparent),
                center = Offset(width * blob3X, height * blob3Y),
                radius = baseRadius * 0.74f,
            ),
            radius = baseRadius * 0.74f,
            center = Offset(width * blob3X, height * blob3Y),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fluidColor(3, 0.45f), Color.Transparent),
                center = Offset(width * blob4X, height * blob4Y),
                radius = baseRadius * 0.62f,
            ),
            radius = baseRadius * 0.62f,
            center = Offset(width * blob4X, height * blob4Y),
        )
    }
}
