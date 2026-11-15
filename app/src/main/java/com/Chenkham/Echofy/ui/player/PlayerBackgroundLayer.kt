package com.Chenkham.Echofy.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.LiveFluidColorPalette
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.ui.component.BackpaperBackground
import com.Chenkham.Echofy.ui.component.LiveFluidBackground
import com.Chenkham.Echofy.ui.component.fallbackColors

@Composable
fun PlayerBackgroundLayer(
    playerBackground: PlayerBackgroundStyle,
    backgroundImageUrl: String?,
    blurRadius: Dp,
    backgroundAlpha: Float,
    gradientColors: List<Color>,
    overlayAlpha: Float,
    liveFluidAlpha: Float,
    useArtworkFluidColors: Boolean,
    liveFluidPalette: LiveFluidColorPalette,
    showLyrics: Boolean,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    BackpaperBackground(screen = BackpaperScreen.PLAYER) {
        Box(modifier = modifier.fillMaxSize()) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    AnimatedContent(
                        targetState = backgroundImageUrl,
                        transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                        label = "blurBackground",
                    ) { url ->
                        if (url != null) {
                            Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .blur(blurRadius),
                                )
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                )
                            }
                        }
                    }
                }
                PlayerBackgroundStyle.GRADIENT,
                PlayerBackgroundStyle.COLORING,
                PlayerBackgroundStyle.BLUR_GRADIENT,
                PlayerBackgroundStyle.GLOW,
                PlayerBackgroundStyle.GLOW_ANIMATED,
                PlayerBackgroundStyle.CUSTOM,
                -> {
                    AnimatedContent(
                        targetState = gradientColors,
                        transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                        label = "gradientBackground",
                    ) { colors ->
                        if (colors.isNotEmpty()) {
                            val gradientColorStops =
                                if (colors.size >= 3) {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.5f to colors[1],
                                        1.0f to colors[2],
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.6f to colors[0].copy(alpha = 0.7f),
                                        1.0f to Color.Black,
                                    )
                                }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .alpha(backgroundAlpha)
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                    .background(Color.Black.copy(alpha = 0.2f)),
                            )
                        }
                    }
                }
                else -> {
                    // DEFAULT
                }
            }

            if (liveFluidAlpha > 0f) {
                LiveFluidBackground(
                    colors = if (useArtworkFluidColors) gradientColors else emptyList(),
                    alpha = liveFluidAlpha,
                    fallbackColors = liveFluidPalette.fallbackColors(),
                )
            }

            AnimatedVisibility(
                visible = overlayAlpha > 0f,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayAlpha)),
                )
            }

            if (playerBackground != PlayerBackgroundStyle.DEFAULT && showLyrics) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Color.Black.copy(
                                    alpha =
                                        animateFloatAsState(
                                            targetValue = if (isExpanded) 0.4f else 0f,
                                            animationSpec = tween(durationMillis = 500),
                                            label = "lyricsOverlay",
                                        ).value,
                                ),
                            ),
                )
            }
        }
    }
}
