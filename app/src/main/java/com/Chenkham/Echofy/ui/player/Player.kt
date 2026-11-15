package com.Chenkham.Echofy.ui.player

import android.content.Context
import android.content.res.Configuration
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.audio.ChordsManager
import com.Chenkham.Echofy.constants.DefaultPlayPauseButtonShape
import com.Chenkham.Echofy.constants.DefaultSmallButtonsShape
import com.Chenkham.Echofy.constants.EnableListenTogetherKey
import com.Chenkham.Echofy.constants.HapticBassBeatsKey
import com.Chenkham.Echofy.constants.PlayPauseButtonShapeKey
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.constants.PlaybackModeKey
import com.Chenkham.Echofy.constants.PlayerTextAlignmentKey
import com.Chenkham.Echofy.constants.QueuePeekHeight
import com.Chenkham.Echofy.constants.RealtimeChordsEnabledKey
import com.Chenkham.Echofy.constants.SliderStyle
import com.Chenkham.Echofy.constants.SliderStyleKey
import com.Chenkham.Echofy.constants.SmallButtonsShapeKey
import com.Chenkham.Echofy.constants.SonglinkEnabledKey
import com.Chenkham.Echofy.ui.component.BottomSheet
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.rememberBottomSheetState
import com.Chenkham.Echofy.ui.screens.settings.PlayerTextAlignment
import com.Chenkham.Echofy.utils.getPlayPauseShape
import com.Chenkham.Echofy.utils.getSmallButtonShape
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    adManager: AdManager? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val downloadUtil = LocalDownloadUtil.current

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val visualState = rememberPlayerVisualState(mediaMetadata = mediaMetadata, isExpanded = state.isExpanded)
    val dialogState = rememberPlayerDialogState()
    val sleepTimerState = rememberSleepTimerState(playerConnection)

    val hapticBassEnabled by rememberPreference(HapticBassBeatsKey, defaultValue = false)
    LaunchedEffect(isPlaying, hapticBassEnabled) {
        if (isPlaying && hapticBassEnabled) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            while (true) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, 40))
                delay(480L)
            }
        }
    }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val smallButtonsShapeState = rememberPreference(SmallButtonsShapeKey, DefaultSmallButtonsShape)
    val smallButtonShape = remember(smallButtonsShapeState.value) { getSmallButtonShape(smallButtonsShapeState.value) }
    val playPauseShapeState = rememberPreference(PlayPauseButtonShapeKey, DefaultPlayPauseButtonShape)
    val playPauseShape = remember(playPauseShapeState.value) { getPlayPauseShape(playPauseShapeState.value) }
    val playerTextAlignment by rememberEnumPreference(PlayerTextAlignmentKey, PlayerTextAlignment.SIDED)
    val (playbackMode, onPlaybackModeChange) = rememberEnumPreference(PlaybackModeKey, PlaybackMode.AUDIO)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.VINTAGE_CABLE)
    val enableListenTogether by rememberPreference(EnableListenTogetherKey, defaultValue = false)
    val songlinkEnabled by rememberPreference(SonglinkEnabledKey, defaultValue = false)
    val realtimeChordsEnabled by rememberPreference(RealtimeChordsEnabledKey, false)

    val chordTimeline =
        remember(mediaMetadata?.id) {
            mediaMetadata?.let { ChordsManager.generateTimeline(it.id, it.title, it.artists.joinToString(), it.duration.toLong() * 1000) }
        }

    var livePlaybackPos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying, mediaMetadata?.id) {
        while (isActive) {
            livePlaybackPos = playerConnection.player.currentPosition
            delay(500)
        }
    }

    PlayerDialogs(
        dialogState = dialogState,
        chordTimeline = chordTimeline,
        currentMedia = mediaMetadata,
        playerConnection = playerConnection,
    )

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

    val isVideoAvailable = remember(mediaMetadata, currentSong) { isVideoAvailableFor(mediaMetadata, currentSong?.song?.id) }
    val playerConfig = LocalConfiguration.current
    val isLandscape = playerConfig.orientation == Configuration.ORIENTATION_LANDSCAPE || playerConfig.screenWidthDp > playerConfig.screenHeightDp

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            PlayerBackgroundLayer(
                playerBackground = visualState.playerBackground,
                backgroundImageUrl = visualState.backgroundImageUrl,
                blurRadius = visualState.blurRadius,
                backgroundAlpha = visualState.backgroundAlpha,
                gradientColors = visualState.gradientColors,
                overlayAlpha = visualState.overlayAlpha,
                liveFluidAlpha = visualState.liveFluidAlpha,
                useArtworkFluidColors = visualState.useArtworkFluidColors,
                liveFluidPalette = visualState.liveFluidPalette,
                showLyrics = visualState.showLyrics,
                isExpanded = state.isExpanded,
                useBlackBackground = visualState.useBlackBackground,
            )
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = { MiniPlayer() },
    ) {
        val controlsContent: @Composable () -> Unit = {
            mediaMetadata?.let { metadata ->
                PlayerControlsSection(
                    mediaMetadata = metadata,
                    playerConnection = playerConnection,
                    playerTextAlignment = playerTextAlignment,
                    onBackgroundColor = visualState.onBackgroundColor,
                    actionButtonColor = MaterialTheme.colorScheme.surfaceVariant,
                    smallButtonShape = smallButtonShape,
                    currentPlayPauseShape = playPauseShape,
                    isLiked = currentSong?.song?.liked == true,
                    isPlaying = isPlaying,
                    playbackState = playbackState,
                    repeatMode = repeatMode,
                    shuffleModeEnabled = shuffleModeEnabled,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    sliderStyle = sliderStyle,
                    realtimeChordsEnabled = realtimeChordsEnabled,
                    chordTimeline = chordTimeline,
                    livePlaybackPos = livePlaybackPos,
                    songlinkEnabled = songlinkEnabled,
                    sleepTimerEnabled = sleepTimerState.enabled,
                    sleepTimerTimeLeft = sleepTimerState.timeLeft,
                    downloadUtil = downloadUtil,
                    context = context,
                    navController = navController,
                    bottomSheetState = state,
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    onAddToPlaylist = { dialogState.showChoosePlaylist = true },
                    onOpenSonglink = { dialogState.showSonglink = true },
                    onOpenSleepTimer = { dialogState.showSleepTimer = true },
                    onOpenChordDiagram = { dialogState.showChordDiagram = true },
                )
            }
        }

        if (isLandscape) {
            PlayerLandscapeView(
                mediaMetadata = mediaMetadata,
                playbackMode = playbackMode,
                onPlaybackModeChange = onPlaybackModeChange,
                isVideoAvailable = isVideoAvailable,
                enableListenTogether = enableListenTogether,
                onBackgroundColor = visualState.onBackgroundColor,
                state = state,
                navController = navController,
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                onOpenJamSheet = { dialogState.showJamSheet = true },
                onShowDetailsDialog = { dialogState.showDetails = true },
                controlsContent = controlsContent,
            )
        } else {
            PlayerPortraitView(
                mediaMetadata = mediaMetadata,
                playbackMode = playbackMode,
                onPlaybackModeChange = onPlaybackModeChange,
                isVideoAvailable = isVideoAvailable,
                enableListenTogether = enableListenTogether,
                onBackgroundColor = visualState.onBackgroundColor,
                state = state,
                navController = navController,
                queueSheetState = queueSheetState,
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                onOpenJamSheet = { dialogState.showJamSheet = true },
                onShowDetailsDialog = { dialogState.showDetails = true },
                controlsContent = controlsContent,
            )
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor = if (visualState.useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
            onBackgroundColor = visualState.onBackgroundColor,
            textBackgroundColor = visualState.textBackgroundColor,
        )
    }
}
