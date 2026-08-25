@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.Chenkham.Echofy.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.VideoQualityKey
import com.Chenkham.Echofy.utils.YTPlayerUtils
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val SEEK_STEP_MS = 10_000L
private const val CONTROLS_TIMEOUT_MS = 3_500L
private val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

/**
 * Video player that mirrors the YouTube player: inline 16:9 surface plus a fullscreen mode
 * that actually rotates the device into landscape.
 *
 * A single [PlayerView] is created once and re-parented between the inline container and the
 * fullscreen dialog. Using two separate PlayerViews races the surface handoff: the outgoing
 * view's surfaceDestroyed callback can land after the incoming view attached its own surface,
 * which clears the surface that is on screen and leaves fullscreen video black.
 */
@Composable
fun VideoPlayerView(
    exoPlayer: Player?,
    modifier: Modifier = Modifier,
    cornerRadius: Float = 16f,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isFullscreen by remember { mutableStateOf(false) }
    // After leaving fullscreen the device is still physically landscape, which would
    // immediately re-trigger the auto-enter below. Pin portrait until the user rotates back,
    // the same way YouTube does.
    var pinnedPortrait by remember { mutableStateOf(false) }

    val selectedQuality by remember(context) {
        context.dataStore.data.map { it[VideoQualityKey] ?: "Auto" }
    }.collectAsState(initial = "Auto")

    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            keepScreenOn = true
            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    DisposableEffect(exoPlayer) {
        playerView.player = exoPlayer
        onDispose { playerView.player = null }
    }

    // Nothing in the app ever touched requestedOrientation, which is exactly why
    // fullscreen never rotated: the screen kept whatever orientation the system rotation
    // lock allowed. SENSOR_LANDSCAPE overrides the user's rotation lock.
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val exitFullscreen: () -> Unit = {
        isFullscreen = false
        pinnedPortrait = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (!isFullscreen) {
            AndroidView(
                factory = { ctx: Context -> FrameLayout(ctx) },
                update = { container -> container.reparent(playerView) },
                modifier = Modifier.fillMaxSize(),
            )

            BarButton(
                icon = R.drawable.fullscreen,
                description = "Fullscreen",
                size = 36.dp,
                iconSize = 20.dp,
                background = Color.Black.copy(alpha = 0.6f),
                onClick = { isFullscreen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )

            if (selectedQuality != "Auto") {
                QualityBadge(
                    quality = selectedQuality,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
        }
    }

    if (isFullscreen) {
        FullscreenOverlay(
            exoPlayer = exoPlayer,
            playerView = playerView,
            quality = selectedQuality,
            onDismiss = exitFullscreen,
        )
    }
}
@Composable
private fun FullscreenOverlay(
    exoPlayer: Player?,
    playerView: PlayerView,
    quality: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentQuality by rememberUpdatedState(quality)

    // A Compose Dialog lives in its own Window added through WindowManager. Because
    // MainActivity declares configChanges="orientation|screenSize|..." it is never
    // recreated, and that detached dialog window keeps its original portrait size on
    // rotation, leaving a portrait-width strip on a landscape screen. Attaching the
    // overlay to the activity's own content view means the system relays it out with
    // the activity, so landscape works.
    DisposableEffect(activity) {
        val root = activity?.findViewById<ViewGroup>(android.R.id.content)
            ?: return@DisposableEffect onDispose { }

        val composeView = ComposeView(activity).apply {
            setContent {
                FullscreenContent(
                    exoPlayer = exoPlayer,
                    playerView = playerView,
                    quality = currentQuality,
                    onDismiss = { currentOnDismiss() },
                )
            }
        }

        root.addView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insets = WindowCompat.getInsetsController(window, window.decorView)
        val previousBehavior = insets.systemBarsBehavior
        insets.hide(WindowInsetsCompat.Type.systemBars())
        insets.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val previousCutoutMode = window.attributes.layoutInDisplayCutoutMode
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        onDispose {
            root.removeView(composeView)
            composeView.disposeComposition()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = previousCutoutMode
            }
            insets.systemBarsBehavior = previousBehavior
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun FullscreenContent(
    exoPlayer: Player?,
    playerView: PlayerView,
    quality: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isPlaying by remember { mutableStateOf(exoPlayer?.isPlaying == true) }
    var hasNext by remember { mutableStateOf(exoPlayer?.hasNextMediaItem() == true) }
    var hasPrevious by remember { mutableStateOf(exoPlayer?.hasPreviousMediaItem() == true) }
    var isBuffering by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var scrubPosition by remember { mutableStateOf<Long?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var interactionNonce by remember { mutableIntStateOf(0) }
    var isLocked by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(exoPlayer?.playbackParameters?.speed ?: 1f) }
    var sheet by remember { mutableStateOf(Sheet.NONE) }
    var isZoomed by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf(0) }

    BackHandler(enabled = true) {
        when {
            sheet != Sheet.NONE -> sheet = Sheet.NONE
            isLocked -> isLocked = false
            else -> onDismiss()
        }
    }

    // The play/pause icon previously read exoPlayer.isPlaying directly. That is not
    // observable state, so nothing recomposed and the icon stayed frozen.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }

            override fun onEvents(player: Player, events: Player.Events) {
                hasNext = player.hasNextMediaItem()
                hasPrevious = player.hasPreviousMediaItem()
            }
        }
        exoPlayer?.addListener(listener)
        isPlaying = exoPlayer?.isPlaying == true
        isBuffering = exoPlayer?.playbackState == Player.STATE_BUFFERING
        onDispose { exoPlayer?.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            exoPlayer?.let {
                position = it.currentPosition.coerceAtLeast(0L)
                bufferedPosition = it.bufferedPosition.coerceAtLeast(0L)
                duration = it.duration.takeIf { d -> d > 0L } ?: 0L
            }
            delay(250)
        }
    }

    // Controls stay up while paused, while a sheet is open, and while locked-out,
    // so they cannot vanish mid-interaction.
    LaunchedEffect(showControls, interactionNonce, isPlaying, sheet) {
        if (showControls && isPlaying && sheet == Sheet.NONE) {
            delay(CONTROLS_TIMEOUT_MS)
            showControls = false
        }
    }

    LaunchedEffect(seekFeedback) {
        if (seekFeedback != 0) {
            delay(600)
            seekFeedback = 0
        }
    }

    val seekBy: (Long) -> Unit = { deltaMs ->
        exoPlayer?.let {
            val target = (it.currentPosition + deltaMs).coerceIn(
                0L,
                it.duration.takeIf { d -> d > 0L } ?: Long.MAX_VALUE,
            )
            it.seekTo(target)
            position = target
        }
        interactionNonce++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        if (isLocked) return@detectTapGestures
                        if (offset.x < size.width / 2f) {
                            seekBy(-SEEK_STEP_MS)
                            seekFeedback = -1
                        } else {
                            seekBy(SEEK_STEP_MS)
                            seekFeedback = 1
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx: Context -> FrameLayout(ctx) },
            update = { container ->
                container.reparent(playerView)
                playerView.resizeMode = if (isZoomed) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (seekFeedback != 0) {
            SeekFeedback(
                forward = seekFeedback > 0,
                modifier = Modifier
                    .align(if (seekFeedback > 0) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 56.dp),
            )
        }

        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                if (isLocked) {
                    BarButton(
                        icon = R.drawable.lock,
                        description = "Unlock",
                        size = 52.dp,
                        iconSize = 26.dp,
                        background = Color.Black.copy(alpha = 0.5f),
                        onClick = { isLocked = false; interactionNonce++ },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp),
                    )
                } else {
                    TopBar(
                        quality = quality,
                        onCollapse = onDismiss,
                        onLock = { isLocked = true; interactionNonce++ },
                        onToggleFit = { isZoomed = !isZoomed; interactionNonce++ },
                        onSpeed = { sheet = Sheet.SPEED },
                        onQuality = { sheet = Sheet.QUALITY },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )

                    CenterControls(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        hasPrevious = hasPrevious,
                        hasNext = hasNext,
                        onPrevious = {
                            exoPlayer?.seekToPreviousMediaItem(); interactionNonce++
                        },
                        onReplay = { seekBy(-SEEK_STEP_MS) },
                        onPlayPause = {
                            exoPlayer?.let { if (it.isPlaying) it.pause() else it.play() }
                            interactionNonce++
                        },
                        onForward = { seekBy(SEEK_STEP_MS) },
                        onNext = { exoPlayer?.seekToNextMediaItem(); interactionNonce++ },
                        modifier = Modifier.align(Alignment.Center),
                    )

                    BottomBar(
                        position = scrubPosition ?: position,
                        bufferedPosition = bufferedPosition,
                        duration = duration,
                        onScrub = { scrubPosition = it; interactionNonce++ },
                        onScrubFinished = { target ->
                            exoPlayer?.seekTo(target)
                            position = target
                            scrubPosition = null
                            interactionNonce++
                        },
                        onExitFullscreen = onDismiss,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = if (isLandscape) 24.dp else 4.dp,
                                vertical = 8.dp,
                            ),
                    )
                }
            }
        }
    }

    when (sheet) {
        Sheet.SPEED -> OptionSheet(
            title = "Playback speed",
            options = PLAYBACK_SPEEDS.map { it.speedLabel() },
            selected = speed.speedLabel(),
            onSelect = { label ->
                PLAYBACK_SPEEDS.firstOrNull { it.speedLabel() == label }?.let { value ->
                    speed = value
                    exoPlayer?.setPlaybackSpeed(value)
                }
                sheet = Sheet.NONE
                interactionNonce++
            },
            onDismiss = { sheet = Sheet.NONE },
        )

        Sheet.QUALITY -> {
            val available by YTPlayerUtils.availableQualities.collectAsState()
            OptionSheet(
                title = "Quality",
                options = listOf("Auto") + available,
                selected = quality,
                onSelect = { value ->
                    CoroutineScope(Dispatchers.IO).launch {
                        context.dataStore.edit { it[VideoQualityKey] = value }
                    }
                    sheet = Sheet.NONE
                    interactionNonce++
                },
                onDismiss = { sheet = Sheet.NONE },
            )
        }

        Sheet.NONE -> Unit
    }
}

private enum class Sheet { NONE, SPEED, QUALITY }

private fun Float.speedLabel(): String =
    if (this == 1f) "Normal" else "${toString().removeSuffix(".0")}x"

@Composable
private fun TopBar(
    quality: String,
    onCollapse: () -> Unit,
    onLock: () -> Unit,
    onToggleFit: () -> Unit,
    onSpeed: () -> Unit,
    onQuality: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(
            icon = R.drawable.expand_more,
            description = "Exit fullscreen",
            onClick = onCollapse,
        )
        Spacer(Modifier.weight(1f))
        BarButton(icon = R.drawable.lock, description = "Lock screen", onClick = onLock)
        BarButton(
            icon = R.drawable.aspect_ratio,
            description = "Change aspect ratio",
            onClick = onToggleFit,
        )
        BarButton(icon = R.drawable.speed, description = "Playback speed", onClick = onSpeed)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onQuality)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.hd),
                contentDescription = "Quality",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            if (quality != "Auto") {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = quality,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun CenterControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onReplay: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(
            icon = R.drawable.skip_previous,
            description = "Previous",
            size = 48.dp,
            iconSize = 30.dp,
            enabled = hasPrevious,
            onClick = onPrevious,
        )
        BarButton(
            icon = R.drawable.replay_10,
            description = "Rewind 10 seconds",
            size = 56.dp,
            iconSize = 34.dp,
            onClick = onReplay,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(76.dp),
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(52.dp),
                )
            } else {
                BarButton(
                    icon = if (isPlaying) R.drawable.pause else R.drawable.play,
                    description = if (isPlaying) "Pause" else "Play",
                    size = 76.dp,
                    iconSize = 52.dp,
                    onClick = onPlayPause,
                )
            }
        }
        BarButton(
            icon = R.drawable.forward_10,
            description = "Forward 10 seconds",
            size = 56.dp,
            iconSize = 34.dp,
            onClick = onForward,
        )
        BarButton(
            icon = R.drawable.skip_next,
            description = "Next",
            size = 48.dp,
            iconSize = 30.dp,
            enabled = hasNext,
            onClick = onNext,
        )
    }
}

@Composable
private fun BottomBar(
    position: Long,
    bufferedPosition: Long,
    duration: Long,
    onScrub: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "${makeTimeString(position)} / ${makeTimeString(duration)}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
            Spacer(Modifier.weight(1f))
            BarButton(
                icon = R.drawable.fullscreen_exit,
                description = "Exit fullscreen",
                onClick = onExitFullscreen,
            )
        }
        YouTubeSeekBar(
            position = position,
            bufferedPosition = bufferedPosition,
            duration = duration,
            onScrub = onScrub,
            onScrubFinished = onScrubFinished,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        )
    }
}

/**
 * YouTube-style scrubber: thin red played track over a lighter buffered track, with a small
 * round thumb that grows while dragging. Built on raw pointer input rather than [Slider] so
 * the buffered track can be drawn underneath and the thumb size can react to the drag.
 */
@Composable
private fun YouTubeSeekBar(
    position: Long,
    bufferedPosition: Long,
    duration: Long,
    onScrub: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }

    val total = duration.coerceAtLeast(1L)
    val playedFraction = (position.toFloat() / total).coerceIn(0f, 1f)
    val bufferedFraction = (bufferedPosition.toFloat() / total).coerceIn(0f, 1f)

    val trackHeight = with(density) { 3.dp.toPx() }
    val thumbRadius = with(density) { (if (isDragging) 9.dp else 6.dp).toPx() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(28.dp)
            .pointerInput(duration) {
                if (duration <= 0L) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val width = size.width.toFloat()
                        isDragging = true
                        var lastX = down.position.x
                        onScrub(positionFor(lastX, width, duration))
                        down.consume()
                        horizontalDrag(down.id) { change ->
                            lastX = change.position.x
                            onScrub(positionFor(lastX, width, duration))
                            change.consume()
                        }
                        isDragging = false
                        onScrubFinished(positionFor(lastX, width, duration))
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val usableWidth = size.width

            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(0f, centerY),
                end = Offset(usableWidth, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(usableWidth * bufferedFraction, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.Red,
                start = Offset(0f, centerY),
                end = Offset(usableWidth * playedFraction, centerY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.Red,
                radius = thumbRadius,
                center = Offset(usableWidth * playedFraction, centerY),
            )
        }
    }
}

private fun positionFor(x: Float, widthPx: Float, duration: Long): Long =
    if (widthPx <= 0f) 0L else ((x / widthPx).coerceIn(0f, 1f) * duration).toLong()

@Composable
private fun SeekFeedback(
    forward: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(
                if (forward) R.drawable.forward_10 else R.drawable.replay_10,
            ),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(34.dp),
        )
        Text(
            text = "10 seconds",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun OptionSheet(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxSheetHeight = (configuration.screenHeightDp * 0.8f).dp

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF212121),
        ) {
            // In landscape the screen is only ~1080px tall, so a plain Column pushed the
            // last options (144p/240p) off-screen with no way to reach them. Cap the sheet
            // to a fraction of the window and scroll the options inside it.
            Column(
                modifier = Modifier
                    .fillMaxWidth(if (isLandscape) 0.5f else 0.9f)
                    .heightIn(max = maxSheetHeight)
                    .padding(vertical = 16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(options) { option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                        ) {
                            if (option == selected) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(16.dp))
                            } else {
                                Spacer(Modifier.width(36.dp))
                            }
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    background: Color = Color.Transparent,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun QualityBadge(
    quality: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = quality,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

/** Moves [view] into this container, detaching it from any previous parent first. */
private fun FrameLayout.reparent(view: View) {
    if (view.parent === this) return
    (view.parent as? ViewGroup)?.removeView(view)
    addView(
        view,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ),
    )
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

