package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.jam.JamRoomCode

@Composable
internal fun JoinJamScreen(
    roomCode: String,
    guestDisplayName: String,
    isWorking: Boolean,
    lastError: String?,
    onBack: () -> Unit,
    onRoomCodeChange: (String) -> Unit,
    onGuestNameChange: (String) -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isJoinEnabled = JamRoomCode.parse(roomCode) != null && !isWorking

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        SheetHeader(
            title = "Join Together",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = roomCode,
            onValueChange = onRoomCodeChange,
            singleLine = true,
            label = { Text("Room code") },
            placeholder = { Text("01-AB92FQ") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = guestDisplayName,
            onValueChange = onGuestNameChange,
            singleLine = true,
            label = { Text("Your name") },
            placeholder = { Text("Listener") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (!lastError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color(0xFF442B2E),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = lastError,
                    color = Color(0xFFFFC7CE),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onJoin,
            enabled = isJoinEnabled,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            shape = CircleShape,
        ) {
            Text(if (isWorking) "Joining..." else "Join Together", fontWeight = FontWeight.Bold)
        }
    }
}
