package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
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

/**
 * Composable grid containers for local songs, artists, and albums.
 */
@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges:
        @Composable()
        (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
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
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
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
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges:
        @Composable()
        (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
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
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges:
        @Composable()
        (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
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
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)
