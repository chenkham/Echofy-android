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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.Chenkham.Echofy.constants.DoubleTapSeekKey
import com.Chenkham.Echofy.constants.DoubleTapSeekSecondsKey
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
    val doubleTapSeek by rememberPreference(DoubleTapSeekKey, false)
    val doubleTapSeekSeconds by rememberPreference(DoubleTapSeekSecondsKey, 10)
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

    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val isLand = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || config.screenWidthDp > config.screenHeightDp

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
                        .then(
                            if (isLand) Modifier.fillMaxHeight()
                            else Modifier.fillMaxWidth().then(
                                if (playbackMode == PlaybackMode.VIDEO || isWideThumbnail) Modifier
                                else Modifier.padding(horizontal = PlayerHorizontalPadding)
                            )
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
                        }
                        .pointerInput(doubleTapSeek, doubleTapSeekSeconds) {
                            if (!doubleTapSeek) return@pointerInput
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    // Left half rewinds, right half fast-forwards, like a video player.
                                    val step = doubleTapSeekSeconds * 1000L
                                    val player = playerConnection.player
                                    val target = if (offset.x < size.width / 2f) {
                                        (player.currentPosition - step).coerceAtLeast(0L)
                                    } else {
                                        val duration = player.duration
                                        val raw = player.currentPosition + step
                                        if (duration > 0) raw.coerceAtMost(duration) else raw
                                    }
                                    player.seekTo(target)
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

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE || configuration.screenWidthDp > configuration.screenHeightDp

                // Show Video or Thumbnail based on playback mode
                if (playbackMode == PlaybackMode.VIDEO) {
                    VideoPlayerView(
                        exoPlayer = playerConnection.player,
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .then(
                                if (isLandscape) Modifier.fillMaxHeight().aspectRatio(16f / 9f, matchHeightConstraintsFirst = true)
                                else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                            ),
                        cornerRadius = if (isLandscape) 12f else 0f
                    )
                } else if (mediaMetadata?.thumbnailUrl.isNullOrBlank()) {
                    val isRadio = mediaMetadata?.id?.startsWith(com.Chenkham.Echofy.playback.MusicService.RADIO_MEDIA_ID_PREFIX) == true
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.roundToInt(), 0) }
                            .then(
                                if (isLandscape) Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true)
                                else Modifier.fillMaxWidth().aspectRatio(1f)
                            )
                            .clip(RoundedCornerShape(cornerRadius * 2))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(if (isRadio) R.drawable.radio else R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier.size(if (isLandscape) 48.dp else 72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
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
                            .then(
                                if (isLandscape) Modifier.fillMaxHeight().aspectRatio(thumbnailAspectRatio, matchHeightConstraintsFirst = true)
                                else Modifier.fillMaxWidth().aspectRatio(thumbnailAspectRatio)
                            )
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
