package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.models.MediaMetadata

@Composable
fun LyricsFullscreenBackground(
    mediaMetadata: MediaMetadata?,
    playerBackground: PlayerBackgroundStyle,
    rotateBackground: Boolean,
    rotation: Float,
    lyricsBackdropBrush: Brush,
    lyricsGlowBrush: Brush,
) {
    BackpaperBackground(screen = BackpaperScreen.LYRICS) {}
    Box(modifier = Modifier.fillMaxSize().background(lyricsBackdropBrush).background(lyricsGlowBrush).background(Color.Black.copy(0.22f)))
    mediaMetadata?.let { metadata ->
        if (playerBackground == PlayerBackgroundStyle.BLUR) {
            AsyncImage(
                model = metadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.fillMaxSize().blur(15.dp).graphicsLayer {
                        scaleX = 2.5f
                        scaleY = 2.5f
                        if (rotateBackground) {
                            rotationZ = rotation
                            transformOrigin = TransformOrigin.Center
                        }
                    },
            )
            Box(
                modifier =
                    Modifier.fillMaxSize().background(
                        lyricsBackdropBrush,
                    ).background(lyricsGlowBrush).background(Color.Black.copy(0.38f)),
            )
        }
    }
}
