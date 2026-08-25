package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ai.ChatSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat history sessions drawer dialog.
 * Extracted from AiAssistantScreen for clean modular architecture.
 */
@Composable
fun ChatHistoryDrawer(
    sessions: List<ChatSession>,
    activeSessionId: String,
    onDismiss: () -> Unit,
    onSelectSession: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteSession: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Chat Sessions",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OnSurface),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onNewChat()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+ New", fontSize = 12.sp, color = OnPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    val isActive = session.id == activeSessionId
                    val dateStr = remember(session.timestamp) {
                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(session.timestamp))
                    }

                    Surface(
                        color = if (isActive) PrimaryContainer else SurfaceContainerHighest,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSession(session.id)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        session.title.ifEmpty { "Chat Session" },
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isActive) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("ACTIVE", color = Tertiary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                                Text("$dateStr · ${session.messages.size} msgs", color = OnSurfaceVariant, fontSize = 10.sp)
                            }

                            if (sessions.size > 1) {
                                IconButton(
                                    onClick = { onDeleteSession(session.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.delete),
                                        contentDescription = "Delete",
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Close", color = OnSurfaceVariant)
            }
        }
    )
}
