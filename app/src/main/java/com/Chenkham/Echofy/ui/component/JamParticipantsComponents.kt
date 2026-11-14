package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.jam.JamParticipant
import com.Chenkham.Echofy.jam.JamParticipantRole

@Composable
internal fun ParticipantRow(
    participants: List<JamParticipant>,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        participants.take(4).forEach { participant ->
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (participant.role == JamParticipantRole.HOST) {
                                Color(0xFF1DB954)
                            } else {
                                Color(0xFF2B2B2B)
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = participant.nameInitial,
                    color = if (participant.role == JamParticipantRole.HOST) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (participants.size > 4) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF181818)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+${participants.size - 4}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
        ) {
            Text(
                text = "${participants.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
internal fun ParticipantsDialog(
    participants: List<JamParticipant>,
    isHost: Boolean,
    requireApproval: Boolean,
    onRequireApprovalChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = {
            Column {
                Text(
                    "Room Participants",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    "${participants.size} ${if (participants.size == 1) "person" else "people"} listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1DB954),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isHost) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Require Approval to Join",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                        Switch(
                            checked = requireApproval,
                            onCheckedChange = onRequireApprovalChange,
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(participants, key = { it.participantId }) { participant ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (participant.role == JamParticipantRole.HOST) {
                                                Color(0xFF1DB954)
                                            } else {
                                                Color(0xFF2B2B2B)
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = participant.nameInitial,
                                    color = if (participant.role == JamParticipantRole.HOST) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = participant.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                )
                                Text(
                                    text = if (participant.role == JamParticipantRole.HOST) "Host" else "Listener",
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (participant.role == JamParticipantRole.HOST) {
                                            Color(0xFF1DB954)
                                        } else {
                                            Color.White.copy(alpha = 0.5f)
                                        },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF1DB954))
            }
        },
    )
}
