@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.Chenkham.Echofy.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.Chenkham.Echofy.R
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.constants.VideoQualityKey

/**
 * Video player composable that displays the music video.
 * Uses ExoPlayer's PlayerView to render video content.
 * Fullscreen uses a Dialog overlay - only the video goes fullscreen.
 */
@Composable
fun VideoPlayerView(
    exoPlayer: Player?,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 16f,
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val selectedQuality by remember(context) {
        context.dataStore.data
            .map { it[VideoQualityKey] ?: "Auto" }
    }.collectAsState(initial = "Auto")

    // Normal (non-fullscreen) video view
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (exoPlayer != null && !isFullscreen) {
            AndroidView(
                factory = { ctx: Context ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        keepScreenOn = true
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.keepScreenOn = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fullscreen button (bottom-right)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { isFullscreen = true }
        ) {
            Icon(
                painter = painterResource(R.drawable.fullscreen),
                contentDescription = "Fullscreen",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Quality indicator (top-right)
        if (selectedQuality != "Auto") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = selectedQuality,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        }
    }

    // Fullscreen Dialog
    if (isFullscreen) {
        FullscreenVideoDialog(
            exoPlayer = exoPlayer,
            onDismiss = { isFullscreen = false }
        )
    }
}

@Composable
private fun FullscreenVideoDialog(
    exoPlayer: Player?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    // Handle system UI for fullscreen
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        // Enter fullscreen — don't force landscape orientation.
        // Android 16 ignores orientation restrictions on large screens (foldables, tablets),
        // and the video player uses RESIZE_MODE_FIT to adapt to any orientation.
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            // Exit fullscreen - restore system bars
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Video player
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx: Context ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            keepScreenOn = true
                        }
                    },
                    update = { playerView ->
                        playerView.player = exoPlayer
                        playerView.keepScreenOn = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Controls overlay
            if (showControls) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    // Center controls: Skip back, Play/Pause, Skip forward
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip back 15 seconds
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { exoPlayer?.seekBack() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.fast_forward),
                                contentDescription = "Skip back 15s",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(scaleX = -1f, scaleY = 1f)
                            )
                        }

                        // Play/Pause
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                                .clickable {
                                    exoPlayer?.let {
                                        it.playWhenReady = !it.playWhenReady
                                    }
                                }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (exoPlayer?.isPlaying == true) R.drawable.pause else R.drawable.play
                                ),
                                contentDescription = if (exoPlayer?.isPlaying == true) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Skip forward 15 seconds
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { exoPlayer?.seekForward() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.fast_forward),
                                contentDescription = "Skip forward 15s",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Exit fullscreen button (top-left)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onDismiss() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.fullscreen_exit),
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
