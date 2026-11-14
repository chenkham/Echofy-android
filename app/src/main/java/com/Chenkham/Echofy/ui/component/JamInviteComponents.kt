package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.jam.JamRegistry
import com.Chenkham.Echofy.utils.ShareUtils

@Composable
internal fun InviteJamSheetContent(
    roomCode: String,
    inviteLink: String?,
    onCopyCode: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Color(0xFF121212))
                .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Invite friends",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "Share the room code now. If your control plane has a public invite URL, the same share message will include it too.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.68f),
        )

        val targetInviteUrl =
            inviteLink?.takeIf { it.isNotBlank() }
                ?: "${ShareUtils.DEFAULT_SHARE_DOMAIN}/?jam=${roomCode.trim().uppercase()}"

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Room code",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                    Text(
                        text = roomCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                QrCodeView(
                    content = targetInviteUrl,
                    sizeDp = 180.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                Text(
                    text = "Scan with camera to join immediately",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCopyCode,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text("Copy code")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text("Share")
            }
        }
    }
}

internal fun buildInviteLink(
    registry: JamRegistry,
    roomCode: String,
): String? {
    if (roomCode.isBlank()) return null
    val base = registry.inviteBaseUrl.trim().removeSuffix("/")
    return if (base.isNotBlank()) {
        if (base.endsWith("/r") || base.endsWith("/jam")) {
            "$base/$roomCode"
        } else {
            "$base/?jam=${roomCode.trim().uppercase()}"
        }
    } else {
        "${ShareUtils.DEFAULT_SHARE_DOMAIN}/?jam=${roomCode.trim().uppercase()}"
    }
}

internal fun buildInviteShareText(
    roomCode: String,
    inviteLink: String?,
): String =
    buildString {
        append("Join my Echofy Jam Together session! 🎧")
        appendLine()
        append("Room code: $roomCode")
        inviteLink?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            append("Tap to listen together: $it")
        }
    }
