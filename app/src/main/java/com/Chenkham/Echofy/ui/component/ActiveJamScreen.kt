package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.jam.JamParticipant
import com.Chenkham.Echofy.models.MediaMetadata

@Composable
internal fun ActiveJamScreen(
    jamTitle: String,
    roomCode: String,
    isHost: Boolean,
    canControlPlayback: Boolean,
    participants: List<JamParticipant>,
    allowGuestControls: Boolean,
    currentSong: MediaMetadata?,
    isPlaying: Boolean,
    requireApproval: Boolean,
    onRequireApprovalChange: (Boolean) -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onDismiss: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onToggleGuestControls: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showParticipantsDialog by remember { mutableStateOf(false) }

    if (showParticipantsDialog) {
        ParticipantsDialog(
            participants = participants,
            isHost = isHost,
            requireApproval = requireApproval,
            onRequireApprovalChange = onRequireApprovalChange,
            onDismiss = { showParticipantsDialog = false },
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
    ) {
        if (!currentSong?.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentSong?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    Color(0xFF1DB954).copy(alpha = 0.18f),
                                    Color(0xFF121212),
                                    Color(0xFF121212),
                                ),
                        ),
                    ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(painter = painterResource(R.drawable.close), contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHost) "You're hosting" else "Listening on this phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1DB954),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = jamTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = roomCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f),
                )
                if (participants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${participants.size} ${if (participants.size == 1) "listener" else "listeners"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1DB954),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ParticipantRow(
                        participants = participants,
                        onClick = { showParticipantsDialog = true },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onInviteClick, shape = CircleShape) {
                            Text("Invite")
                        }
                        OutlinedButton(onClick = onLeaveClick, shape = CircleShape) {
                            Text(if (isHost) "End" else "Leave")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Let others change what's playing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Switch(
                        checked = allowGuestControls,
                        enabled = isHost,
                        onCheckedChange = onToggleGuestControls,
                    )
                }
            }

            // Big now-playing card
            JamNowPlayingCard(
                currentSong = currentSong,
                isHost = isHost,
                canControlPlayback = canControlPlayback,
                modifier = Modifier.weight(1f),
            )

            // Playback bar
            JamPlaybackBar(
                canControlPlayback = canControlPlayback,
                isPlaying = isPlaying,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onTogglePlayPause = onTogglePlayPause,
                onSkipPrevious = onSkipPrevious,
                onSkipNext = onSkipNext,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
            )
        }
    }
}
