package com.Chenkham.Echofy.ui.player

import android.content.res.Configuration
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import android.widget.Toast
import com.Chenkham.Echofy.utils.makeTimeString
import com.Chenkham.Echofy.utils.toShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.Chenkham.Echofy.LocalDownloadUtil
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.Chenkham.Echofy.ui.component.LocalAdManager
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.constants.PlaybackModeKey
import com.Chenkham.Echofy.utils.rememberEnumPreference
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import com.Chenkham.Echofy.playback.ExoDownloadService
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.DefaultPlayPauseButtonShape
import com.Chenkham.Echofy.constants.DefaultSmallButtonsShape
import com.Chenkham.Echofy.constants.PlayPauseButtonShapeKey
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.constants.PlayerBackgroundStyleKey
import com.Chenkham.Echofy.constants.PlayerButtonsStyle
import com.Chenkham.Echofy.constants.PlayerButtonsStyleKey
import com.Chenkham.Echofy.constants.PlayerHorizontalPadding
import com.Chenkham.Echofy.constants.PlayerTextAlignmentKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.constants.QueuePeekHeight
import com.Chenkham.Echofy.constants.ShowLyricsKey
import com.Chenkham.Echofy.constants.SliderStyle
import com.Chenkham.Echofy.constants.SliderStyleKey
import com.Chenkham.Echofy.constants.SmallButtonsShapeKey
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.extensions.toggleRepeatMode
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.ui.component.BottomSheet
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.PlayerSliderTrack
import com.Chenkham.Echofy.ui.component.ResizableIconButton
import com.Chenkham.Echofy.ui.component.rememberBottomSheetState
import com.Chenkham.Echofy.ui.menu.PlayerMenu
import com.Chenkham.Echofy.ui.menu.AddToPlaylistDialog
import com.Chenkham.Echofy.ui.screens.settings.DarkMode
import com.Chenkham.Echofy.ui.screens.settings.PlayerTextAlignment
import com.Chenkham.Echofy.ui.theme.extractGradientColors
import com.Chenkham.Echofy.utils.getPlayPauseShape
import com.Chenkham.Echofy.utils.getSmallButtonShape
import com.Chenkham.Echofy.utils.makeTimeString
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ui.component.StandardBannerAdView

import com.Chenkham.Echofy.ui.component.MediumRectangleAdView
import android.app.Activity
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.ui.component.BackpaperBackground

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit, // NEW PARAMETER
    adManager: AdManager? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current

    val clipboardManager = LocalClipboardManager.current

    var showFullscreenLyrics by remember { mutableStateOf(false) }

    val playerConnection = LocalPlayerConnection.current ?: return






    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.CENTER
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val (playbackMode, onPlaybackModeChange) = rememberEnumPreference(
        key = PlaybackModeKey,
        defaultValue = PlaybackMode.AUDIO
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)

// Position state removed for performance - moved to PlayerProgressSection

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var changeColor by remember {
        mutableStateOf(false)
    }



    // Animations for background effects
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (state.isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 20.dp else 0.dp, // Optimized: reduced from 150dp
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "blurRadius"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            !state.isExpanded -> 0f
            playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
            playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )


    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.SECONDARY
    )
    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    LaunchedEffect(mediaMetadata?.thumbnailUrl, playerBackground, useBlackBackground) {
        // Update image URL for smooth transitions
        backgroundImageUrl = mediaMetadata?.thumbnailUrl

        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
            return@LaunchedEffect
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT) {
            gradientColors = listOf(Color.Black, Color.Black)
            return@LaunchedEffect
        }

        if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            // PERFORMANCE FIX: Only extract colors if URL actually changed
            val currentUrl = mediaMetadata?.thumbnailUrl
            if (currentUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val result =
                            (
                                    ImageLoader(context)
                                        .execute(
                                            ImageRequest
                                                .Builder(context)
                                                .data(currentUrl)
                                                .allowHardware(false)
                                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                                .build(),
                                        ).drawable as? BitmapDrawable
                                    )?.bitmap?.extractGradientColors()

                        result?.let {
                            gradientColors = it
                        }
                    } catch (e: Exception) {
                        // Fail silently on image extraction errors
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val changeBound = state.expandedBound / 3

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.White
                } else {
                    changeColor = false
                    Color.White
                }
            }
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.White
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.Black
                } else {
                    changeColor = false
                    Color.Black
                }
            }
        }

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }


    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // Add to Playlist Dialog
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { _ ->
            mediaMetadata?.id?.let { listOf(it) } ?: emptyList()
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )


    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val smallButtonShape = remember(smallButtonsShapeState.value) {
        getSmallButtonShape(smallButtonsShapeState.value)
    }

    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )

    val playPauseShape = remember(playPauseShapeState.value) {
        getPlayPauseShape(playPauseShapeState.value)
    }



    val infiniteTransition = rememberInfiniteTransition(label = "play_pause_rotation")
    val playPauseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000, // 9 seconds for a full rotation
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

// Forma dinÃ¡mica: cuando estÃ¡ reproduciendo usa la forma seleccionada
// Cuando estÃ¡ en pausa usa Square
    val currentPlayPauseShape = remember(isPlaying, playPauseShape) {
        if (isPlaying) {
            playPauseShape
        } else {
            RectangleShape
        }
    }


    // Function to create the modifier for small buttons
    val smallButtonModifier = @Composable {
        Modifier
            .size(42.dp)
            .clip(smallButtonShape)
            .background(textButtonColor)
    }

// Loop removed for performance - moved to PlayerProgressSection

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    
    val actionButtonColor = MaterialTheme.colorScheme.surfaceVariant
    val downloadUtil = LocalDownloadUtil.current

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                                currentFormat?.contentLength?.let {
                                    Formatter.formatShortFileSize(
                                        context,
                                        it
                                    )
                                },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
                .calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT ->
            MaterialTheme.colorScheme.surfaceContainer
        else ->
            if (useBlackBackground) Color.Black
            else MaterialTheme.colorScheme.surfaceContainer
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                BackpaperBackground(screen = BackpaperScreen.PLAYER) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(100, 100)
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                                        50f, 50f, android.graphics.Shader.TileMode.MIRROR
                                                    ).asComposeRenderEffect()
                                                }
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops = if (colors.size >= 3) {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.5f to colors[1],
                                        1.0f to colors[2]
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.6f to colors[0].copy(alpha = 0.7f),
                                        1.0f to Color.Black
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer()
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )


            Row(
                horizontalArrangement =
                    when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Arrangement.Start
                        PlayerTextAlignment.CENTER -> Arrangement.Center
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                AnimatedContent(
                    targetState = mediaMetadata.title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = onBackgroundColor,
                        modifier =
                            Modifier
                                .basicMarquee()
                                .clickable(enabled = mediaMetadata.album != null) {
                                    navController.navigate("album/${mediaMetadata.album!!.id}")
                                    state.collapseSoft()
                                },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))


            Row(
                horizontalArrangement =
                    when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Arrangement.Start
                        PlayerTextAlignment.CENTER -> Arrangement.Center
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                mediaMetadata.artists.fastForEachIndexed { index, artist ->
                    AnimatedContent(
                        targetState = artist.name,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = onBackgroundColor,
                            maxLines = 1,
                            modifier =
                                Modifier.clickable(enabled = artist.id != null) {
                                    navController.navigate("artist/${artist.id}")
                                    state.collapseSoft()
                                },
                        )
                    }

                    if (index != mediaMetadata.artists.lastIndex) {
                        AnimatedContent(
                            targetState = ", ",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { comma ->
                            Text(
                                text = comma,
                                style = MaterialTheme.typography.titleMedium,
                                color = onBackgroundColor,
                            )
                }
            }
        }
    }

            Spacer(Modifier.height(12.dp))

            // Unified action bar with round icon buttons (YTM-style)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .horizontalScroll(rememberScrollState()),
            ) {
                // Like button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(smallButtonShape.toShape())
                        .background(
                            if (currentSong?.song?.liked == true) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else actionButtonColor
                        )
                        .clickable { playerConnection.toggleLike() },
                ) {
                    Image(
                        painter = painterResource(
                            if (currentSong?.song?.liked == true) R.drawable.heart_fill 
                            else R.drawable.heart
                        ),
                        contentDescription = stringResource(if (currentSong?.song?.liked == true) R.string.acc_unfavorite else R.string.acc_favorite),
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Save button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(smallButtonShape.toShape())
                        .background(actionButtonColor)
                        .clickable { showChoosePlaylistDialog = true },
                ) {
                    Image(
                        painter = painterResource(R.drawable.playlist_add),
                        contentDescription = stringResource(R.string.acc_add_to_playlist),
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Share button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(smallButtonShape.toShape())
                        .background(actionButtonColor)
                        .clickable {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        },
                ) {
                    Image(
                        painter = painterResource(R.drawable.share),
                        contentDescription = stringResource(R.string.acc_share),
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(24.dp),
                    )
                }

                // Radio button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(smallButtonShape.toShape())
                        .background(actionButtonColor)
                        .clickable { playerConnection.service.startRadioSeamlessly() },
                ) {
                    Image(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(24.dp),
                    )
                }



                // Download button (Unified style)
                Box(modifier = Modifier) { 
                    val download by remember(mediaMetadata.id) { 
                        downloadUtil.getDownload(mediaMetadata.id) 
                    }.collectAsState(initial = null)

                    val iconResource = when (download?.state) {
                        Download.STATE_COMPLETED -> R.drawable.offline
                        Download.STATE_DOWNLOADING, Download.STATE_QUEUED -> R.drawable.downloading
                        else -> R.drawable.download
                    }

                    val adManager = LocalAdManager.current

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(smallButtonShape.toShape())
                            .background(actionButtonColor)
                            .clickable {
                                // PREMIUM CHECK
                                if (adManager?.isPremium?.value != true) {
                                    Toast.makeText(context, R.string.premium_required, Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                if (download?.state == Download.STATE_COMPLETED || download?.state == Download.STATE_DOWNLOADING || download?.state == Download.STATE_QUEUED) {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                } else {
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            },
                    ) {
                         // ... (keep existing download icon/progress logic but ensure color contrast) ...
                         if (download?.state == Download.STATE_DOWNLOADING || download?.state == Download.STATE_QUEUED) {
                             CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = onBackgroundColor
                             )
                         } else {
                             Image(
                                painter = painterResource(iconResource),
                                contentDescription = stringResource(R.string.acc_download),
                                colorFilter = ColorFilter.tint(onBackgroundColor),
                                modifier = Modifier.size(24.dp)
                             )
                         }
                    }
                }


                // Sleep Timer
                Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(smallButtonShape.toShape())
                            .background(actionButtonColor),
                    ) {
                         AnimatedContent(
                            label = "sleepTimer",
                            targetState = sleepTimerEnabled,
                        ) { sleepTimerEnabled ->
                            if (sleepTimerEnabled) {
                                Text(
                                    text = makeTimeString(sleepTimerTimeLeft),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = onBackgroundColor,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable(onClick = playerConnection.service.sleepTimer::clear)
                                        .basicMarquee(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().clickable { showSleepTimerDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.bedtime),
                                        colorFilter = ColorFilter.tint(onBackgroundColor),
                                        contentDescription = stringResource(R.string.acc_sleep_timer),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
            }


            Spacer(Modifier.height(6.dp))

            PlayerProgressSection(
                playerConnection = playerConnection,
                sliderStyle = sliderStyle,
                color = TextBackgroundColor,
                isPlaying = isPlaying
            )

            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        },
                        contentDescription = stringResource(R.string.acc_repeat),
                        color = TextBackgroundColor,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                        onClick = {
                            playerConnection.player.toggleRepeatMode()
                        },
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_previous,
                        contentDescription = stringResource(R.string.acc_skip_prev),
                        enabled = canSkipPrevious,
                        color = TextBackgroundColor,
                        modifier =
                            Modifier
                                .size(56.dp)
                                .align(Alignment.Center),
                        onClick = playerConnection::seekToPrevious,
                    )
                }

                Spacer(Modifier.width(8.dp))

            // Play/Pause Button Area
             Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(playPauseShape.toShape())
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFFF12711), Color(0xFFF5AF19)) // Vibrant orange/red gradient
                        )
                    )
                    .clickable {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.playWhenReady = !isPlaying
                        }
                    },
            ) {
                Image(
                    painter =
                        painterResource(
                            if (playbackState ==
                                STATE_ENDED
                            ) {
                                R.drawable.replay
                            } else if (isPlaying) {
                                R.drawable.pause
                            } else {
                                R.drawable.play
                            },
                        ),
                    contentDescription = stringResource(if (playbackState == STATE_ENDED) R.string.acc_repeat else if (isPlaying) R.string.acc_pause else R.string.acc_play),
                    colorFilter = ColorFilter.tint(Color.White), // White icon on gradient bg
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .rotate(if (isPlaying) -playPauseRotation else 0f),
                )
            }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_next,
                        contentDescription = stringResource(R.string.acc_skip_next),
                        enabled = canSkipNext,
                        color = TextBackgroundColor,
                        modifier =
                            Modifier
                                .size(56.dp)
                                .align(Alignment.Center),
                        onClick = playerConnection::seekToNext,
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.lyrics,
                        contentDescription = stringResource(R.string.acc_lyrics),
                        color = TextBackgroundColor,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .padding(4.dp)
                                .align(Alignment.Center),
                        onClick = onOpenFullscreenLyrics,
                    )
                }
            }
        }

        // Animated background effects
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background with blurred image
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.BLUR && backgroundImageUrl != null,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(400))
            ) {
                AsyncImage(
                    model = backgroundImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius)
                        .alpha(backgroundAlpha)
                )
            }

            // Animated gradient background
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2,
                enter = fadeIn(tween(800)),
                exit = fadeOut(tween(600))
            ) {
                val animatedGradientColors = gradientColors.map { color ->
                    androidx.compose.animation.animateColorAsState(
                        targetValue = color,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "gradientColor"
                    ).value
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backgroundAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = if (animatedGradientColors.isNotEmpty()) animatedGradientColors else gradientColors
                            )
                        )
                )
            }

            // Animated dark overlay
            AnimatedVisibility(
                visible = overlayAlpha > 0f,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                )
            }

            // Additional overlay for lyrics
            if (playerBackground != PlayerBackgroundStyle.DEFAULT && showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = animateFloatAsState(
                                    targetValue = if (state.isExpanded) 0.4f else 0f,
                                    animationSpec = tween(durationMillis = 500),
                                    label = "lyricsOverlay"
                                ).value
                            )
                        )
                )
            }
        }
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Fixed top bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Minimize button (chevron down)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { state.collapseSoft() },
                        ) {
                            Image(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = stringResource(R.string.acc_minimize),
                                colorFilter = ColorFilter.tint(onBackgroundColor),
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        // More options (3-dot menu)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    menuState.show {
                                        PlayerMenu(
                                            mediaMetadata = mediaMetadata ?: return@show,
                                            navController = navController,
                                            onShowDetailsDialog = { showDetailsDialog = true },
                                            onDismiss = menuState::dismiss,
                                            onNavigateAway = { state.collapseSoft() },
                                        )
                                    }
                                },
                        ) {
                            Image(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = stringResource(R.string.acc_more_options),
                                colorFilter = ColorFilter.tint(onBackgroundColor),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            val screenWidth = LocalConfiguration.current.screenWidthDp
                            val thumbnailSize = (screenWidth * 0.35).dp

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Song/Video Switch
                                val (playbackMode, onPlaybackModeChange) = rememberEnumPreference(
                                    key = PlaybackModeKey,
                                    defaultValue = PlaybackMode.AUDIO
                                )
                                
                                SongVideoSwitch(
                                    selectedMode = playbackMode,
                                    onModeChange = onPlaybackModeChange
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val queueTitle by playerConnection.queueTitle.collectAsState()
                                AnimatedVisibility(
                                    visible = !queueTitle.isNullOrEmpty(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.playing_from),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = onBackgroundColor.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            text = queueTitle.orEmpty(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = onBackgroundColor,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .basicMarquee()
                                        )
                                    }
                                }

                                Thumbnail(
                                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                    modifier = Modifier
                                        .size(thumbnailSize)
                                )

                                adManager?.let { manager ->
                                    Spacer(Modifier.height(12.dp))
                                    MediumRectangleAdView(
                                        adManager = manager
                                    )
                                }
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                        ) {
                            Spacer(Modifier.weight(1f))

                            mediaMetadata?.let {
                                controlsContent(it)
                            }

                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            else -> {
                val configuration = LocalConfiguration.current
                val isSmallScreen = configuration.screenHeightDp < 750
                // Pull top bar down (original was 24.dp)
                val topBarTopPadding = if (isSmallScreen) 24.dp else 36.dp
                // Reduce thumbnail width (and height) on small screens to prevent overlap
                val thumbnailPadding = if (isSmallScreen) 32.dp else 16.dp

                // Custom nested scroll connection to consume post-scroll overflow and prevent vibration
                val consumeOverflowNestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            // Consume all remaining velocity to prevent propagation
                            return available
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // Consume all remaining scroll delta to prevent propagation
                            return available
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    // Fixed top bar with minimize and more options
                    // Top Bar with Toggle and Options
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = topBarTopPadding, bottom = 0.dp)
                    ) {
                        // Switch Centered
                        val (playbackMode, onPlaybackModeChange) = rememberEnumPreference(
                            key = PlaybackModeKey,
                            defaultValue = PlaybackMode.AUDIO
                        )
                        Box(modifier = Modifier.align(Alignment.Center)) {
                            SongVideoSwitch(
                                selectedMode = playbackMode,
                                onModeChange = onPlaybackModeChange
                            )
                        }

                        // Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Minimize button (chevron down)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { state.collapseSoft() },
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.expand_more),
                                    contentDescription = stringResource(R.string.acc_minimize),
                                    colorFilter = ColorFilter.tint(onBackgroundColor),
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            // More options (3-dot menu)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = mediaMetadata ?: return@show,
                                                navController = navController,
                                                onShowDetailsDialog = { showDetailsDialog = true },
                                                onDismiss = menuState::dismiss,
                                                onNavigateAway = { state.collapseSoft() },
                                            )
                                        }
                                    },
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = stringResource(R.string.acc_more_options),
                                    colorFilter = ColorFilter.tint(onBackgroundColor),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }

                    // Content area with thumbnail
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .nestedScroll(consumeOverflowNestedScrollConnection)
                                .nestedScroll(state.preUpPostDownNestedScrollConnection)
                        ) {
                            Thumbnail(
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (playbackMode == PlaybackMode.VIDEO) Modifier
                                        else Modifier.padding(horizontal = thumbnailPadding)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp)) // Pushes title down

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            onBackgroundColor = onBackgroundColor,
            textBackgroundColor = TextBackgroundColor,
        )
    }
}

@Composable
fun SongVideoSwitch(
    selectedMode: PlaybackMode,
    onModeChange: (PlaybackMode) -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SwitchOption(
            text = "Song",
            isSelected = selectedMode == PlaybackMode.AUDIO,
            onClick = { onModeChange(PlaybackMode.AUDIO) }
        )
        Spacer(modifier = Modifier.width(4.dp))
        SwitchOption(
            text = "Video",
            isSelected = selectedMode == PlaybackMode.VIDEO,
            onClick = { onModeChange(PlaybackMode.VIDEO) }
        )
    }
}

@Composable
fun SwitchOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerProgressSection(
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    sliderStyle: SliderStyle,
    color: Color,
    isPlaying: Boolean,
) {
    var position by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by remember { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(playerConnection, isPlaying) {
        if (isPlaying) {
            while (isActive) {
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                delay(100L)
            }
        } else {
             // Update once when paused to ensure valid state
             position = playerConnection.player.currentPosition
             duration = playerConnection.player.duration
        }
    }

    when (sliderStyle) {
        SliderStyle.DEFAULT -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    sliderPosition = null
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = color,
                    inactiveTrackColor = Color.Gray,
                    activeTickColor = color,
                    inactiveTickColor = Color.Gray,
                    thumbColor = color
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
            )
        }

        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    sliderPosition = null
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = color,
                    inactiveTrackColor = Color.Gray,
                    activeTickColor = color,
                    inactiveTickColor = Color.Gray,
                    thumbColor = color
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                squigglesSpec =
                    SquigglySlider.SquigglesSpec(
                        amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                        strokeWidth = 3.dp,
                    ),
            )
        }

        SliderStyle.SLIM -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    sliderPosition = null
                },
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors(
                            activeTrackColor = color,
                            inactiveTrackColor = Color.Gray,
                            activeTickColor = color,
                            inactiveTickColor = Color.Gray
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
