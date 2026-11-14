package com.Chenkham.Echofy.ui.player

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.audio.SongChordTimeline
import com.Chenkham.Echofy.constants.SliderStyle
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.screens.settings.PlayerTextAlignment

@Composable
fun PlayerControlsSection(
    mediaMetadata: MediaMetadata,
    playerConnection: PlayerConnection,
    playerTextAlignment: PlayerTextAlignment,
    onBackgroundColor: Color,
    actionButtonColor: Color,
    smallButtonShape: Shape,
    currentPlayPauseShape: Shape,
    isLiked: Boolean,
    isPlaying: Boolean,
    playbackState: Int,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    sliderStyle: SliderStyle,
    realtimeChordsEnabled: Boolean,
    chordTimeline: SongChordTimeline?,
    livePlaybackPos: Long,
    songlinkEnabled: Boolean,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    downloadUtil: DownloadUtil,
    context: Context,
    navController: NavController,
    bottomSheetState: BottomSheetState,
    onOpenFullscreenLyrics: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenSonglink: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenChordDiagram: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PlayerMediaHeader(
            mediaMetadata = mediaMetadata,
            playerConnection = playerConnection,
            playerTextAlignment = playerTextAlignment,
            onBackgroundColor = onBackgroundColor,
            navController = navController,
            bottomSheetState = bottomSheetState,
        )

        Spacer(Modifier.height(8.dp))

        PlayerActionBar(
            mediaMetadata = mediaMetadata,
            isLiked = isLiked,
            actionButtonColor = actionButtonColor,
            onBackgroundColor = onBackgroundColor,
            buttonShape = smallButtonShape,
            songlinkEnabled = songlinkEnabled,
            sleepTimerEnabled = sleepTimerEnabled,
            sleepTimerTimeLeft = sleepTimerTimeLeft,
            playerConnection = playerConnection,
            downloadUtil = downloadUtil,
            context = context,
            onOpenFullscreenLyrics = onOpenFullscreenLyrics,
            onAddToPlaylist = onAddToPlaylist,
            onOpenSonglink = onOpenSonglink,
            onOpenSleepTimer = onOpenSleepTimer,
        )

        if (realtimeChordsEnabled && chordTimeline != null) {
            PlayerChordChip(
                chordTimeline = chordTimeline,
                livePlaybackPos = livePlaybackPos,
                onClick = onOpenChordDiagram,
            )
        }

        PlayerProgressSection(
            playerConnection = playerConnection,
            sliderStyle = sliderStyle,
            color = MaterialTheme.colorScheme.primary,
            isPlaying = isPlaying,
        )

        Spacer(Modifier.height(6.dp))

        PlayerPlaybackControls(
            playerConnection = playerConnection,
            isPlaying = isPlaying,
            playbackState = playbackState,
            repeatMode = repeatMode,
            shuffleModeEnabled = shuffleModeEnabled,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            onBackgroundColor = onBackgroundColor,
            playPauseShape = currentPlayPauseShape,
        )
    }
}
