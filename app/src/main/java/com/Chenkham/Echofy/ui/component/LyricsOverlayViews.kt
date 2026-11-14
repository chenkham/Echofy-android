package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.models.MediaMetadata

@Composable
fun BoxScope.LyricsOverlayViews(
    showImageOverlay: Boolean,
    onDismissImageOverlay: () -> Unit,
    mediaMetadata: MediaMetadata?,
    imageScale: Float,
    cornerRadius: Float,
    rotateBackground: Boolean,
    rotation: Float,
    isSelectionModeActive: Boolean,
    selectedCount: Int,
    showControls: Boolean,
) {
    AnimatedVisibility(
        visible = showImageOverlay,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
        modifier = Modifier.fillMaxSize().zIndex(1f),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismissImageOverlay() })
                    },
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                mediaMetadata?.let { metadata ->
                    AsyncImage(
                        model = metadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier =
                            Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .scale(imageScale)
                                .clip(RoundedCornerShape(cornerRadius))
                                .then(
                                    if (rotateBackground) {
                                        Modifier.graphicsLayer {
                                            rotationZ = rotation
                                            transformOrigin = TransformOrigin.Center
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isSelectionModeActive,
        enter =
            slideInVertically(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) { it } + fadeIn(),
        exit =
            slideOutVertically(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).zIndex(3f),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 6.dp,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(bottom = if (showControls) 180.dp else 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(18.dp),
                )

                Text(
                    text = stringResource(R.string.selection_mode_active, selectedCount),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
