package com.Chenkham.Echofy.ui.player

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.audio.SongChordTimeline
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.ChordDiagramDialog
import com.Chenkham.Echofy.ui.component.EchofyJamSheet
import com.Chenkham.Echofy.ui.component.SonglinkShareDialog
import com.Chenkham.Echofy.ui.menu.AddToPlaylistDialog
import com.Chenkham.Echofy.ui.utils.ShowMediaInfo
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDialogs(
    dialogState: PlayerDialogState,
    chordTimeline: SongChordTimeline?,
    currentMedia: MediaMetadata?,
    playerConnection: PlayerConnection,
) {
    if (dialogState.showSleepTimer) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { dialogState.showSleepTimer = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogState.showSleepTimer = false
                        playerConnection.service.sleepTimer.start(dialogState.sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogState.showSleepTimer = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.minute,
                                dialogState.sleepTimerValue.roundToInt(),
                                dialogState.sleepTimerValue.roundToInt(),
                            ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = dialogState.sleepTimerValue,
                        onValueChange = { dialogState.sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            dialogState.showSleepTimer = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    AddToPlaylistDialog(
        isVisible = dialogState.showChoosePlaylist,
        onGetSong = { currentMedia?.id?.let { listOf(it) } ?: emptyList() },
        onDismiss = { dialogState.showChoosePlaylist = false },
    )

    if (dialogState.showJamSheet) {
        EchofyJamSheet(onDismiss = { dialogState.showJamSheet = false })
    }

    if (dialogState.showSonglink && currentMedia != null) {
        SonglinkShareDialog(
            videoId = currentMedia.id,
            songTitle = currentMedia.title,
            artistName = currentMedia.artists.joinToString { it.name },
            onDismiss = { dialogState.showSonglink = false },
        )
    }

    if (dialogState.showDetails && currentMedia != null) {
        ShowMediaInfo(
            mediaMetadata = currentMedia,
            onDismiss = { dialogState.showDetails = false },
        )
    }

    if (dialogState.showChordDiagram && chordTimeline != null) {
        val currentPos = playerConnection.player.currentPosition
        val activeChord = chordTimeline.chords.find { currentPos in it.startMs..it.endMs }?.chord ?: chordTimeline.key
        ChordDiagramDialog(
            initialChord = activeChord,
            progression = chordTimeline.chords.map { it.chord },
            onDismiss = { dialogState.showChordDiagram = false },
        )
    }
}
