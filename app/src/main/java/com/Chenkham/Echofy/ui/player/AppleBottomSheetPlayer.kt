package com.Chenkham.Echofy.ui.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.Chenkham.Echofy.db.getLyrics
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.EnableListenTogetherKey
import com.Chenkham.Echofy.constants.HapticBassBeatsKey
import com.Chenkham.Echofy.constants.LiveFluidColorPalette
import com.Chenkham.Echofy.constants.LiveFluidColorPaletteKey
import com.Chenkham.Echofy.constants.LiveFluidBackgroundKey
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.constants.PlayerBackgroundStyleKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.constants.RealTimeVisualizerKey
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.metadata
import com.Chenkham.Echofy.extensions.toggleRepeatMode
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.lyrics.LyricsUtils.findCurrentLineIndex
import com.Chenkham.Echofy.lyrics.LyricsUtils.parseLyrics
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.ui.component.BackpaperBackground
import com.Chenkham.Echofy.ui.component.BottomSheet
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.LocalAdManager
import com.Chenkham.Echofy.ui.component.LiveFluidBackground
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.component.MediaMetadataListItem
import com.Chenkham.Echofy.ui.component.RealTimeAudioVisualizer
import com.Chenkham.Echofy.ui.component.ResizableIconButton
import com.Chenkham.Echofy.ui.component.fallbackColors
import com.Chenkham.Echofy.ui.component.prefersArtworkColors
import com.Chenkham.Echofy.ui.menu.AddToPlaylistDialog
import com.Chenkham.Echofy.ui.menu.PlayerMenu
import com.Chenkham.Echofy.ui.screens.settings.DarkMode
import com.Chenkham.Echofy.ui.theme.extractGradientColors
import com.Chenkham.Echofy.utils.makeTimeString
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.extensions.move
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

enum class ApplePlayerState { MAIN, LYRICS, QUEUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleBottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // App theme & background extraction...
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val useBlackBackground = remember(useDarkTheme, pureBlack) { useDarkTheme && pureBlack }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onSurface
        else -> if (useDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    }

    // Playback state
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()


    // Haptic bass beats (now free for all)
    val hapticBassEnabled by rememberPreference(HapticBassBeatsKey, defaultValue = false)

    LaunchedEffect(isPlaying, hapticBassEnabled) {
        if (isPlaying && hapticBassEnabled) {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            while (isActive) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(30, 40))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(30)
                }
                kotlinx.coroutines.delay(480L)
            }
        }
    }

    // Dynamic Background logic
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val liveFluidBackground by rememberPreference(LiveFluidBackgroundKey, defaultValue = false)
    val realTimeVisualizer by rememberPreference(RealTimeVisualizerKey, defaultValue = false)
    val liveFluidPalette by rememberEnumPreference(
        LiveFluidColorPaletteKey,
        LiveFluidColorPalette.ALBUM
    )
    val useArtworkFluidColors = liveFluidBackground && liveFluidPalette.prefersArtworkColors()
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "bgAlpha"
    )
    val liveFluidAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && liveFluidBackground) {
            when (playerBackground) {
                PlayerBackgroundStyle.DEFAULT -> 0.92f
                PlayerBackgroundStyle.BLUR -> 0.42f
                PlayerBackgroundStyle.GRADIENT -> 0.58f
                else -> 0.58f
            }
        } else {
            0f
        },
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "liveFluidAlpha"
    )

    LaunchedEffect(mediaMetadata?.thumbnailUrl, playerBackground, useBlackBackground, useArtworkFluidColors) {
        if (useBlackBackground && !useArtworkFluidColors) {
            gradientColors = listOf(Color.Black, Color.Black)
            return@LaunchedEffect
        }
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || useArtworkFluidColors) {
            val url = mediaMetadata?.thumbnailUrl ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                try {
                    val result = (ImageLoader(context).execute(
                        ImageRequest.Builder(context).data(url).allowHardware(false)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED).build()
                    ).drawable as? BitmapDrawable)?.bitmap?.extractGradientColors()
                    result?.let { gradientColors = it }
                } catch (_: Exception) {}
            }
        } else gradientColors = emptyList()
    }

    val textColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        else -> {
            if (gradientColors.size >= 2) {
                val wc = ColorUtils.calculateContrast(gradientColors.first().toArgb(), Color.White.toArgb())
                val bc = ColorUtils.calculateContrast(gradientColors.last().toArgb(), Color.Black.toArgb())
                if (wc < 2f && bc > 2f) Color.Black else Color.White
            } else Color.White
        }
    }
    val safeTextColor = textColor 

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showEchofyJamSheet by rememberSaveable { mutableStateOf(false) }
    val enableListenTogether by rememberPreference(EnableListenTogetherKey, defaultValue = false)
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { _ -> mediaMetadata?.id?.let { listOf(it) } ?: emptyList() },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    if (showEchofyJamSheet) {
        com.Chenkham.Echofy.ui.component.EchofyJamSheet(
            onDismiss = { showEchofyJamSheet = false }
        )
    }

    val bottomSheetBg = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> MaterialTheme.colorScheme.surfaceContainer
        else -> if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    }

    // -- State tracking -- 
    var appleState by rememberSaveable { mutableStateOf(ApplePlayerState.MAIN) }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            val infiniteTransition = rememberInfiniteTransition()
            val fluidScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = if (liveFluidBackground) 1.5f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(15000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fluidScale"
            )
            val fluidRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = if (liveFluidBackground) 45f else 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(25000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "fluidRotation"
            )
            val fluidModifier = if (liveFluidBackground) Modifier.scale(fluidScale).rotate(fluidRotation) else Modifier

            Box(Modifier.fillMaxSize().background(bottomSheetBg)) {
                BackpaperBackground(screen = BackpaperScreen.PLAYER) {
                    when (playerBackground) {
                        PlayerBackgroundStyle.BLUR -> {
                            AnimatedContent(
                                targetState = mediaMetadata?.thumbnailUrl,
                                transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                                label = "blur"
                            ) { url ->
                                if (url != null) {
                                    Box(Modifier.alpha(backgroundAlpha)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context).data(url).size(100, 100).allowHardware(false).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().then(fluidModifier).graphicsLayer {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                    renderEffect = android.graphics.RenderEffect
                                                        .createBlurEffect(50f, 50f, android.graphics.Shader.TileMode.MIRROR)
                                                        .asComposeRenderEffect()
                                                }
                                            }
                                        )
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                                    }
                                }
                            }
                        }
                        PlayerBackgroundStyle.GRADIENT -> {
                            AnimatedContent(
                                targetState = gradientColors,
                                transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) },
                                label = "gradient"
                            ) { colors ->
                                if (colors.isNotEmpty()) {
                                    val stops = if (colors.size >= 3)
                                        arrayOf(0f to colors[0], 0.5f to colors[1], 1f to colors[2])
                                    else arrayOf(0f to colors[0], 0.6f to colors[0].copy(alpha = 0.7f), 1f to Color.Black)
                                    Box(Modifier.fillMaxSize().alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = stops))
                                        .background(Color.Black.copy(alpha = 0.2f)))
                                }
                            }
                        }
                        else -> {}
                    }
                    if (liveFluidAlpha > 0f) {
                        LiveFluidBackground(
                            colors = if (useArtworkFluidColors) gradientColors else emptyList(),
                            alpha = liveFluidAlpha,
                            fallbackColors = liveFluidPalette.fallbackColors(),
                        )
                    }
                }
            }
        },
        onDismiss = {
            // Un-set states when dismissed
            appleState = ApplePlayerState.MAIN
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = { MiniPlayer() },
    ) {
        val meta = mediaMetadata ?: return@BottomSheet

        // ── Single-screen fixed layout (SpaceBetween ensures NO scoll and pins bottom) ─────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── TOP DYNAMIC SECTION (Expands to fill) ─────────────────────────
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {

                // Top drag indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { state.collapseSoft() },
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier.width(36.dp).height(5.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.5f)))
                }

                // Sub-views based on active state
                AnimatedContent(
                    targetState = appleState,
                    transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(300))) },
                    label = "ApplePlayerState"
                ) { activeState ->
                    when (activeState) {
                        ApplePlayerState.MAIN -> AppleMainView(
                            meta = meta,
                            playerConnection = playerConnection,
                            currentSong = currentSong,
                            onBackgroundColor = safeTextColor,
                            navController = navController,
                            onCollapse = { state.collapseSoft() }
                        )
                        ApplePlayerState.LYRICS -> AppleLyricsView(
                            meta = meta,
                            playerConnection = playerConnection,
                            currentSong = currentSong,
                            onBackgroundColor = safeTextColor,
                            navController = navController,
                            onCollapse = { state.collapseSoft() }
                        )
                        ApplePlayerState.QUEUE -> AppleQueueView(
                            meta = meta,
                            playerConnection = playerConnection,
                            currentSong = currentSong,
                            onBackgroundColor = safeTextColor,
                            navController = navController,
                            onCollapse = { state.collapseSoft() }
                        )
                    }
                }
            }

            // ── FIXED BOTTOM CONTROLS (Always on screen) ──────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp, top = 8.dp)
            ) {
                if (realTimeVisualizer) {
                    RealTimeAudioVisualizer(
                        audioSessionId = playerConnection.player.audioSessionId,
                        color = safeTextColor,
                        isActive = isPlaying
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AppleProgressSection(
                    playerConnection = playerConnection,
                    color = safeTextColor,
                    isPlaying = isPlaying
                )

                Spacer(modifier = Modifier.height(12.dp))

                ApplePlaybackControls(
                    playerConnection = playerConnection,
                    isPlaying = isPlaying,
                    playbackState = playbackState,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    color = safeTextColor,
                    playPauseShape = androidx.compose.foundation.shape.CircleShape
                )

                Spacer(modifier = Modifier.height(24.dp))

                AppleFooter(
                    currentState = appleState,
                    color = safeTextColor,
                    onLyricsClick = { appleState = if (appleState == ApplePlayerState.LYRICS) ApplePlayerState.MAIN else ApplePlayerState.LYRICS },
                    onQueueClick = { appleState = if (appleState == ApplePlayerState.QUEUE) ApplePlayerState.MAIN else ApplePlayerState.QUEUE }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN VIEW COMPONENT
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMainView(
    meta: MediaMetadata,
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    currentSong: Song?,
    onBackgroundColor: Color,
    navController: NavController,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val enableListenTogether by rememberPreference(EnableListenTogetherKey, defaultValue = false)
    var showEchofyJamSheet by rememberSaveable { mutableStateOf(false) }

    if (showEchofyJamSheet) {
        com.Chenkham.Echofy.ui.component.EchofyJamSheet(
            onDismiss = { showEchofyJamSheet = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        // "Listening on" label above the cover art
        val deviceName = remember {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val preferred = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            }
            preferred?.productName?.toString() ?: android.os.Build.MODEL
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.volume_up),
                contentDescription = null,
                colorFilter = ColorFilter.tint(onBackgroundColor.copy(alpha = 0.65f)),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Listening on $deviceName",
                color = onBackgroundColor.copy(alpha = 0.65f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(meta.thumbnailUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = meta.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyLarge,
                    color = onBackgroundColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = meta.artists.firstOrNull()?.id != null) {
                        meta.artists.firstOrNull()?.id?.let { artistId ->
                            navController.navigate("artist/$artistId")
                            onCollapse()
                        }
                    }
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            playerConnection.toggleLike()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.primary else onBackgroundColor),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (enableListenTogether) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                                showEchofyJamSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                                        painter = painterResource(R.drawable.group),
                            contentDescription = "Start Together",
                            colorFilter = ColorFilter.tint(onBackgroundColor),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = meta,
                                    navController = navController,
                                    onShowDetailsDialog = {
                                        menuState.showDialog {
                                            com.Chenkham.Echofy.ui.utils.ShowMediaInfo(
                                                mediaMetadata = meta,
                                                onDismiss = menuState::dismissDialog
                                            )
                                        }
                                    },
                                    onDismiss = menuState::dismiss,
                                    onNavigateAway = onCollapse
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = stringResource(R.string.acc_more_options),
                        colorFilter = ColorFilter.tint(onBackgroundColor),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MINI HEADER (Shared by Lyrics & Queue)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniHeader(
    meta: MediaMetadata,
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    currentSong: Song?,
    onBackgroundColor: Color,
    navController: NavController,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current

    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(meta.thumbnailUrl).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = meta.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onBackgroundColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meta.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = onBackgroundColor.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        playerConnection.toggleLike()
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.primary else onBackgroundColor),
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = meta,
                                navController = navController,
                                onShowDetailsDialog = {
                                        menuState.showDialog {
                                            com.Chenkham.Echofy.ui.utils.ShowMediaInfo(
                                                mediaMetadata = meta,
                                                onDismiss = menuState::dismissDialog
                                            )
                                        }
                                    },
                                onDismiss = menuState::dismiss,
                                onNavigateAway = onCollapse
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LYRICS VIEW COMPONENT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppleLyricsView(
    meta: MediaMetadata,
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    currentSong: Song?,
    onBackgroundColor: Color,
    navController: NavController,
    onCollapse: () -> Unit
) {
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = lyricsEntity?.lyrics

    val lines = remember(lyrics) {
        if (lyrics.isNullOrEmpty() || lyrics == com.Chenkham.Echofy.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
            emptyList()
        } else if (lyrics.startsWith("[")) {
            listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parseLyrics(lyrics)
        } else {
            lyrics.lines().mapIndexed { index, line ->
                LyricsEntry(index * 100L, line)
            }
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = com.Chenkham.Echofy.LocalDatabase.current
    
    LaunchedEffect(meta.id) {
        if (lyrics.isNullOrEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val existing = database.getLyrics(meta.id)
                    if (existing == null || existing.lyrics == com.Chenkham.Echofy.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.Chenkham.Echofy.di.LyricsHelperEntryPoint::class.java
                        )
                        val lyricsHelper = entryPoint.lyricsHelper()
                        val fetchedLyrics = lyricsHelper.getLyrics(meta, false)
                        if (fetchedLyrics != com.Chenkham.Echofy.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                            val entity = com.Chenkham.Echofy.db.entities.LyricsEntity(meta.id, fetchedLyrics, "Auto")
                            database.query { upsert(entity) }
                        }
                    }
                } catch(e: Exception) {}
            }
        }
    }

    val isSynced = lines.size > 1 && lyrics?.startsWith("[") == true
    var currentLineIndex by remember { mutableStateOf(-1) }
    var position by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(isPlaying, playerConnection.player.currentPosition) {
        while (isActive) {
            position = playerConnection.player.currentPosition
            if (isSynced) {
                currentLineIndex = findCurrentLineIndex(lines, position)
            }
            kotlinx.coroutines.delay(100)
        }
    }

    LaunchedEffect(currentLineIndex) {
        if (isSynced && currentLineIndex >= 0) {
            lazyListState.animateScrollToItem(kotlin.math.max(0, currentLineIndex))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MiniHeader(meta, playerConnection, currentSong, onBackgroundColor, navController, onCollapse)
        Spacer(modifier = Modifier.height(24.dp))

        if (lines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No lyrics found",
                    color = onBackgroundColor.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(lines) { index, entry ->
                    val isCurrent = index == currentLineIndex
                    val alpha = if (isSynced) (if (isCurrent) 1f else 0.4f) else 0.8f
                    val weight = if (isSynced && isCurrent) FontWeight.Bold else FontWeight.Medium

                    Text(
                        text = entry.text,
                        color = onBackgroundColor.copy(alpha = alpha),
                        style = MaterialTheme.typography.titleLarge.copy(lineHeight = 32.sp),
                        fontWeight = weight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable(enabled = isSynced) {
                                playerConnection.player.seekTo(entry.time)
                            }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUEUE VIEW COMPONENT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AppleQueueView(
    meta: MediaMetadata,
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    currentSong: Song?,
    onBackgroundColor: Color,
    navController: NavController,
    onCollapse: () -> Unit
) {
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val automix by playerConnection.service.automixItems.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val lazyListState = rememberLazyListState()
    val mutableQueueWindows = remember { androidx.compose.runtime.mutableStateListOf<Timeline.Window>() }

    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            mutableQueueWindows.move(from.index, to.index)
            val safeFrom = from.index.coerceIn(0, mutableQueueWindows.lastIndex)
            val safeTo = to.index.coerceIn(0, mutableQueueWindows.lastIndex)

            if (!playerConnection.player.shuffleModeEnabled) {
                playerConnection.player.moveMediaItem(safeFrom, safeTo)
            } else {
                playerConnection.player.setShuffleOrder(
                    DefaultShuffleOrder(
                        queueWindows
                            .map { it.firstPeriodIndex }
                            .toMutableList()
                            .move(safeFrom, safeTo)
                            .toIntArray(),
                        System.currentTimeMillis()
                    ),
                )
            }
        }
    )

    LaunchedEffect(currentWindowIndex) {
        if (currentWindowIndex >= 0 && currentWindowIndex < queueWindows.size) {
            lazyListState.scrollToItem(currentWindowIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MiniHeader(meta, playerConnection, currentSong, onBackgroundColor, navController, onCollapse)
        
        Spacer(modifier = Modifier.height(20.dp))

        // Pill Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Shuffle
            val shuffleBg = if (shuffleModeEnabled) {
                onBackgroundColor.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(shuffleBg)
                    .clickable { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Repeat
            val repeatBg = if (repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) {
                onBackgroundColor.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(repeatBg)
                    .clickable { playerConnection.player.toggleRepeatMode() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Continue Playing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = onBackgroundColor)

        Spacer(modifier = Modifier.height(8.dp))

        // Tracks
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items = mutableQueueWindows, key = { _, item -> item.uid.hashCode() }) { index, window ->
                ReorderableItem(state = reorderableState, key = window.uid.hashCode()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                playerConnection.player.playWhenReady = true
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small art
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(window.mediaItem.metadata?.thumbnailUrl).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = window.mediaItem.metadata?.title.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = onBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = window.mediaItem.metadata?.artists?.joinToString { it.name }.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = onBackgroundColor.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        androidx.compose.material3.IconButton(
                            onClick = { },
                            modifier = Modifier.draggableHandle()
                        ) {
                            Image(
                                painter = painterResource(R.drawable.drag_handle),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(onBackgroundColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM SECTION COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleProgressSection(
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    color: Color,
    isPlaying: Boolean
) {
    var position by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by remember { mutableLongStateOf(playerConnection.player.duration) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isPlaying, playerConnection) {
        if (isPlaying) {
            while (isActive) {
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
                kotlinx.coroutines.delay(100L)
            }
        } else {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { sliderPosition = it.toLong() },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    sliderPosition = null
                },
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                colors = SliderDefaults.colors(
                    activeTrackColor = color.copy(alpha = 0.8f),
                    inactiveTrackColor = color.copy(alpha = 0.2f),
                    thumbColor = Color.Transparent
                )
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(sliderPosition ?: position),
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium
            )

            val remaining = if (duration != C.TIME_UNSET) {
                val rem = duration - (sliderPosition ?: position)
                if (rem > 0) "-${makeTimeString(rem)}" else "0:00"
            } else ""

            Text(
                text = remaining,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.65f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ApplePlaybackControls(
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    isPlaying: Boolean,
    playbackState: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    color: Color,
    playPauseShape: androidx.compose.ui.graphics.Shape
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .clickable(enabled = canSkipPrevious, indication = ripple(bounded = false), interactionSource = remember { MutableInteractionSource() }) {
                    playerConnection.seekToPrevious()
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.apple_skip_previous),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color.copy(alpha = if (canSkipPrevious) 1f else 0.4f)),
                modifier = Modifier.size(44.dp)
            )
        }

        Box(
            modifier = Modifier.size(76.dp).clip(playPauseShape)
                .background(color.copy(alpha = 0.1f))
                .clickable(indication = ripple(bounded = false), interactionSource = remember { MutableInteractionSource() }) {
                    if (playbackState == STATE_ENDED) {
                        playerConnection.player.seekTo(0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.playWhenReady = !isPlaying
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(if (isPlaying) R.drawable.apple_pause else R.drawable.apple_play),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier.size(56.dp)
            )
        }

        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .clickable(enabled = canSkipNext, indication = ripple(bounded = false), interactionSource = remember { MutableInteractionSource() }) {
                    playerConnection.seekToNext()
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.apple_skip_next),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color.copy(alpha = if (canSkipNext) 1f else 0.4f)),
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun AppleFooter(
    currentState: ApplePlayerState,
    color: Color,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val lyricsBg = if (currentState == ApplePlayerState.LYRICS) color.copy(alpha = 0.2f) else Color.Transparent
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(lyricsBg).clickable { onLyricsClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.apple_lyrics),
                contentDescription = "Lyrics",
                colorFilter = ColorFilter.tint(color.copy(alpha = if (currentState == ApplePlayerState.LYRICS) 1f else 0.65f)),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val queueBg = if (currentState == ApplePlayerState.QUEUE) color.copy(alpha = 0.2f) else Color.Transparent
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(queueBg).clickable { onQueueClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.apple_queue),
                contentDescription = "Queue",
                colorFilter = ColorFilter.tint(color.copy(alpha = if (currentState == ApplePlayerState.QUEUE) 1f else 0.65f)),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MAIN VIEW COMPONENT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ApplePinnedPlayerControls(
    playerConnection: com.Chenkham.Echofy.playback.PlayerConnection,
    isPlaying: Boolean,
    playbackState: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    currentState: ApplePlayerState,
    color: Color,
    showVisualizer: Boolean = false,
    playPauseShape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showVisualizer) {
            RealTimeAudioVisualizer(
                audioSessionId = playerConnection.player.audioSessionId,
                color = color,
                isActive = isPlaying
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        AppleProgressSection(
            playerConnection = playerConnection,
            color = color,
            isPlaying = isPlaying
        )

        Spacer(modifier = Modifier.height(12.dp))

        ApplePlaybackControls(
            playerConnection = playerConnection,
            isPlaying = isPlaying,
            playbackState = playbackState,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            color = color,
            playPauseShape = playPauseShape
        )

        Spacer(modifier = Modifier.height(24.dp))

        AppleFooter(
            currentState = currentState,
            color = color,
            onLyricsClick = onLyricsClick,
            onQueueClick = onQueueClick
        )
    }
}
