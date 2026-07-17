package com.Chenkham.Echofy.ui.player

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.constants.PlaybackModeKey
import com.Chenkham.Echofy.constants.VideoPlaybackEnabledKey
import com.Chenkham.Echofy.constants.ShowLyricsKey
import com.Chenkham.Echofy.constants.SwipeThumbnailKey
import com.Chenkham.Echofy.ui.component.AppConfig
import com.Chenkham.Echofy.ui.component.Lyrics
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.Chenkham.Echofy.R

private fun guessedThumbnailAspectRatio(url: String?): Float {
    if (url.isNullOrBlank()) return 1f
    return if (
        url.contains("i.ytimg.com", ignoreCase = true) ||
        url.contains("ytimg.com/vi", ignoreCase = true) ||
        url.contains("vi_webp", ignoreCase = true)
    ) {
        16f / 9f
    } else {
        1f
    }
}

@Composable
fun Thumbnail(
    onOpenFullscreenLyrics: () -> Unit, // NUEVO PARÁMETRO
    modifier: Modifier = Modifier,
    changeColor: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentView = LocalView.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()

    var showLyrics by rememberPreference(ShowLyricsKey, false)
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val playbackModeSelected by rememberEnumPreference(PlaybackModeKey, PlaybackMode.AUDIO)
    val videoPlaybackEnabled by rememberPreference(VideoPlaybackEnabledKey, true)
    
    // Only use video mode if both: user selected VIDEO mode AND video playback is enabled
    val playbackMode = if (videoPlaybackEnabled) playbackModeSelected else PlaybackMode.AUDIO

    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var thumbnailAspectRatio by remember(mediaMetadata?.thumbnailUrl) {
        mutableFloatStateOf(guessedThumbnailAspectRatio(mediaMetadata?.thumbnailUrl))
    }
    val isWideThumbnail = thumbnailAspectRatio > 1.18f

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier, // QUITADO: .fillMaxSize() y .statusBarsPadding()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxWidth() // SOLO ancho completo, no altura
                        .then(
                            // Wide video-style covers should use the available width so no artwork is cropped.
                            if (playbackMode == PlaybackMode.VIDEO || isWideThumbnail) Modifier
                            else Modifier.padding(horizontal = PlayerHorizontalPadding)
                        )
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragCancel = {
                                    offsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    if (swipeThumbnail) {
                                        offsetX += dragAmount
                                    }
                                },
                                onDragEnd = {
                                    if (offsetX > 300) {
                                        if (playerConnection.player.previousMediaItemIndex != -1) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        }
                                    } else if (offsetX < -300) {
                                        if (playerConnection.player.nextMediaItemIndex != -1) {
                                            playerConnection.seekToNext()
                                        }
                                    }
                                    offsetX = 0f
                                },
                            )
                        },
            ) {
                var cornerRadius by remember { mutableFloatStateOf(16f) } // Valor por defecto
                val context = LocalContext.current

                // Recuperar el valor de DataStore de manera segura
                LaunchedEffect(Unit) {
                    cornerRadius = AppConfig.getThumbnailCornerRadius(context)
                }

                // Show Video or Thumbnail based on playback mode
                if (playbackMode == PlaybackMode.VIDEO) {
                    VideoPlayerView(
                        exoPlayer = playerConnection.player,
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f), // Standard video aspect ratio
                        cornerRadius = 0f // Edge-to-edge, no rounded corners
                    )
                } else {
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            val width = drawable.intrinsicWidth
                            val height = drawable.intrinsicHeight
                            if (width > 0 && height > 0) {
                                thumbnailAspectRatio = (width.toFloat() / height.toFloat()).coerceIn(0.75f, 1.9f)
                            }
                        },
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .fillMaxWidth()
                            .aspectRatio(thumbnailAspectRatio)
                            .clip(RoundedCornerShape(cornerRadius * 2))
                            .background(Color.Black.copy(alpha = 0.10f))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { offset ->
                                        if (offset.x < size.width / 2) {
                                            playerConnection.player.seekBack()
                                        } else {
                                            playerConnection.player.seekForward()
                                        }
                                    },
                                )
                            },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Lyrics()
            }
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .padding(32.dp)
                    .align(Alignment.Center),
        ) {
            error?.let { error ->
                PlaybackError(
                    error = error,
                    retry = playerConnection.player::prepare,
                )
            }
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    var thumbnailCornerRadiusV2: Float
        get() = prefs.getFloat("THUMBNAIL_CORNER_RADIUS", 16f)
        set(value) = prefs.edit() { putFloat("THUMBNAIL_CORNER_RADIUS", value) }
}
