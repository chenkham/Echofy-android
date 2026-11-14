package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.models.MediaMetadata

@Composable
internal fun JamStartSetupScreen(
    currentSong: MediaMetadata?,
    hostDisplayName: String,
    selectedSeedOption: JamSeedOption,
    playlistsAvailable: Boolean,
    isWorking: Boolean,
    onBack: () -> Unit,
    onHostNameChange: (String) -> Unit,
    onSeedOptionChange: (JamSeedOption) -> Unit,
    onPickPlaylist: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        SheetHeader(
            title = "Start Together",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = hostDisplayName,
            onValueChange = onHostNameChange,
            singleLine = true,
            label = { Text("Your name") },
            placeholder = { Text("Host") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Choose what starts the room",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SourceOptionCard(
            title = "Current song",
            subtitle =
                currentSong?.let {
                    "Everyone will hear \"${it.title}\" — manage what plays next from your main player."
                } ?: "Play a song first, then start the session.",
            selected = selectedSeedOption == JamSeedOption.CURRENT_QUEUE,
            enabled = currentSong != null,
            onClick = { onSeedOptionChange(JamSeedOption.CURRENT_QUEUE) },
        )

        Spacer(modifier = Modifier.height(10.dp))

        SourceOptionCard(
            title = "From a playlist",
            subtitle =
                if (playlistsAvailable) {
                    "Pick a playlist — the first song plays for everyone. Manage what's next from your main player."
                } else {
                    "No playlists found yet. Add or bookmark one first."
                },
            selected = selectedSeedOption == JamSeedOption.PLAYLIST,
            enabled = playlistsAvailable,
            onClick = {
                onSeedOptionChange(JamSeedOption.PLAYLIST)
                onPickPlaylist()
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedSeedOption == JamSeedOption.CURRENT_QUEUE) {
            CurrentSourcePreview(currentSong = currentSong)
            Spacer(modifier = Modifier.height(20.dp))
        }

        Button(
            onClick = onStart,
            enabled = !isWorking,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            shape = CircleShape,
        ) {
            Text(if (isWorking) "Starting..." else "Start Together", fontWeight = FontWeight.Bold)
        }
    }
}
