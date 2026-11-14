package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString

/**
 * Composable item for displaying generic MediaMetadata in a list layout.
 */
@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = mediaMetadata.title,
    subtitle =
        joinByBullet(
            mediaMetadata.artists.joinToString { it.name },
            makeTimeString(mediaMetadata.duration * 1000L),
        ),
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ListThumbnailSize),
        ) {
            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(1000f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.done),
                        modifier = Modifier.align(Alignment.Center),
                        contentDescription = null,
                    )
                }
            }

            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = Color.White,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)
