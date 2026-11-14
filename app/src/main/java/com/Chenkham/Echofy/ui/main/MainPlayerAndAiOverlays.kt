package com.Chenkham.Echofy.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.ai.AiCallState
import com.Chenkham.Echofy.ai.EchofyAiManager
import com.Chenkham.Echofy.constants.NavigationBarHeight
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.EchofyJamSheet
import com.Chenkham.Echofy.ui.component.Lyrics
import com.Chenkham.Echofy.ui.menu.YouTubeSongMenu
import com.Chenkham.Echofy.ui.player.AppleBottomSheetPlayer
import com.Chenkham.Echofy.ui.player.BottomSheetPlayer
import com.Chenkham.Echofy.ui.screens.ai.GoogleAssistantVisualOverlay
import com.Chenkham.Echofy.ui.screens.ai.DrivingModeBanner
import com.arturo254.opentune.innertube.models.SongItem

/**
 * Hosts BottomSheetPlayer, Lyrics, Google Assistant overlay, DrivingModeBanner, and dialogs.
 */
@Composable
fun MainPlayerAndAiOverlays(
    playerBottomSheetState: BottomSheetState,
    playerLayoutStyle: PlayerLayoutStyle,
    navController: NavHostController,
    showFullscreenLyrics: Boolean,
    onShowFullscreenLyricsChange: (Boolean) -> Unit,
    wakeWordAiManager: EchofyAiManager,
    shouldShowBottomNav: Boolean,
    bottomInset: Dp,
    sharedSong: SongItem?,
    onDismissSharedSong: () -> Unit,
    pendingJamRoomCode: String?,
    onDismissJamSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (playerLayoutStyle == PlayerLayoutStyle.APPLE_MUSIC) {
            AppleBottomSheetPlayer(
                state = playerBottomSheetState,
                navController = navController,
                onOpenFullscreenLyrics = { onShowFullscreenLyricsChange(true) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            BottomSheetPlayer(
                state = playerBottomSheetState,
                navController = navController,
                onOpenFullscreenLyrics = { onShowFullscreenLyricsChange(true) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = showFullscreenLyrics,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
        ) {
            Lyrics(
                onNavigateBack = { onShowFullscreenLyricsChange(false) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        val globalIsAiActiveVisual by wakeWordAiManager.isAiActiveVisual.collectAsState()
        val globalCallState by wakeWordAiManager.callState.collectAsState()
        val globalAwaitingCommand by wakeWordAiManager.isAwaitingCommand.collectAsState()
        val globalTranscript by wakeWordAiManager.liveTranscript.collectAsState()

        GoogleAssistantVisualOverlay(
            visible = globalIsAiActiveVisual || globalAwaitingCommand,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = bottomInset + (if (shouldShowBottomNav) NavigationBarHeight else 0.dp) + (if (!playerBottomSheetState.isDismissed) 80.dp else 0.dp) + 8.dp,
                    ),
            statusText =
                when {
                    globalAwaitingCommand -> "Speak now..."
                    globalCallState == AiCallState.SPEAKING -> "Echofy AI Speaking..."
                    globalCallState == AiCallState.THINKING -> "Processing command..."
                    else -> "Listening for command..."
                },
            transcript = globalTranscript,
        )

        val globalDrivingMode by wakeWordAiManager.isDrivingMode.collectAsState()
        DrivingModeBanner(
            visible = globalDrivingMode,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
        )

        val playerConnection = LocalPlayerConnection.current
        sharedSong?.let { song ->
            if (playerConnection != null) {
                Dialog(
                    onDismissRequest = onDismissSharedSong,
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    Surface(
                        modifier = Modifier.padding(24.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = AlertDialogDefaults.containerColor,
                        tonalElevation = AlertDialogDefaults.TonalElevation,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            YouTubeSongMenu(
                                song = song,
                                navController = navController,
                                onDismiss = onDismissSharedSong,
                            )
                        }
                    }
                }
            }
        }

        pendingJamRoomCode?.let { roomCode ->
            EchofyJamSheet(
                onDismiss = onDismissJamSheet,
                initialRoomCode = roomCode,
            )
        }
    }
}
