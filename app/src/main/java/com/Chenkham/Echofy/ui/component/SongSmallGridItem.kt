package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.Song

/**
 * Composable item for displaying a song in a small grid layout.
 */
@Composable
fun SongSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = SmallGridItem(
    title = song.song.title,
    thumbnailContent = {
        AsyncImage(
            model = song.song.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !(isActive && isPlaying),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    modifier = modifier,
)
