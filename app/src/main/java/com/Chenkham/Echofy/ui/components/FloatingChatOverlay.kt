package com.Chenkham.Echofy.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.data.models.ChatMessage
import com.Chenkham.Echofy.data.repository.ListenTogetherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Live-stream style floating chat overlay for Jam sessions.
 * 
 * Features:
 * - Fully transparent background - messages float over content
 * - Ephemeral messages that fade out after ~6 seconds
 * - White text with soft shadows for readability
 * - No metrics (listener count, message count)
 * - Rapid scroll-up animation for new messages
 * - Input field fixed at bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingChatOverlay(
    modifier: Modifier = Modifier,
    bottomPadding: Float = 140f // Space for navbar + mini player
) {
    val currentRoom by ListenTogetherRepository.currentRoom.collectAsState()
    val allMessages by ListenTogetherRepository.messages.collectAsState()
    
    // Only show if user is in an active session
    if (currentRoom == null) return
    
    // Track visible messages with their display time for ephemeral fading
    var visibleMessages by remember { mutableStateOf<List<EphemeralMessage>>(emptyList()) }
    
    // Add new messages and manage ephemeral lifecycle
    LaunchedEffect(allMessages) {
        val existing = visibleMessages.map { it.message.id }.toSet()
        val newMessages = allMessages.filter { it.id !in existing }
        
        if (newMessages.isNotEmpty()) {
            // Add new messages with current timestamp
            visibleMessages = visibleMessages + newMessages.map { 
                EphemeralMessage(it, System.currentTimeMillis()) 
            }
        }
    }
    
    // Cleanup old messages (older than 6 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            delay(500L) // Check every 500ms
            val cutoff = System.currentTimeMillis() - 6000L
            visibleMessages = visibleMessages.filter { it.addedTime > cutoff }
        }
    }
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentUserId = ListenTogetherRepository.currentUserId ?: ""
    val currentUserName = ListenTogetherRepository.savedUserName ?: "Guest"
    
    // Auto-scroll to latest message
    LaunchedEffect(visibleMessages.size) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomPadding.dp)
        ) {
            // Message stream area - fully transparent
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 250.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = visibleMessages,
                    key = { it.message.id }
                ) { ephemeralMsg ->
                    val isOwnMessage = ephemeralMsg.message.senderId == currentUserId
                    
                    // Calculate fade based on age (fade out in last 2 seconds)
                    val age = System.currentTimeMillis() - ephemeralMsg.addedTime
                    val fadeStart = 4000L // Start fading at 4 seconds
                    val fadeDuration = 2000L // Fade over 2 seconds
                    val alpha = when {
                        age < fadeStart -> 1f
                        else -> 1f - ((age - fadeStart).toFloat() / fadeDuration).coerceIn(0f, 1f)
                    }
                    
                    // Trigger recomposition for fade animation
                    var currentAlpha by remember { mutableFloatStateOf(1f) }
                    LaunchedEffect(ephemeralMsg.message.id) {
                        while (true) {
                            val currentAge = System.currentTimeMillis() - ephemeralMsg.addedTime
                            currentAlpha = when {
                                currentAge < fadeStart -> 1f
                                else -> 1f - ((currentAge - fadeStart).toFloat() / fadeDuration).coerceIn(0f, 1f)
                            }
                            delay(50) // Update every 50ms for smooth fade
                        }
                    }
                    
                    LiveStreamMessageBubble(
                        message = ephemeralMsg.message,
                        isOwnMessage = isOwnMessage,
                        alpha = currentAlpha
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Input field - semi-transparent background
            LiveStreamInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        scope.launch {
                            ListenTogetherRepository.sendMessage(
                                roomCode = currentRoom?.roomCode ?: "",
                                senderId = currentUserId,
                                senderName = currentUserName,
                                content = messageText.trim()
                            )
                            messageText = ""
                        }
                    }
                }
            )
        }
    }
}

/**
 * Data class to track message display time for ephemeral behavior
 */
private data class EphemeralMessage(
    val message: ChatMessage,
    val addedTime: Long
)

/**
 * Format timestamp to readable time format
 */
private fun formatMessageTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Live-stream style message bubble with text shadow.
 * "Username: message" format with drop shadow for readability.
 */
@Composable
private fun LiveStreamMessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    alpha: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer {
                // Text shadow using renderEffect would be ideal but we'll use layered approach
                shadowElevation = 4f
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Small avatar circle
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (isOwnMessage) 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else 
                        Color.White.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.senderName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Message content with shadow
        Column {
            // Shadow layer (behind main text)
            Box {
                // Shadow text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOwnMessage) "You" else message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                    )
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Transparent
                    )
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.5f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                    )
                }
                
                // Main text on top
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOwnMessage) "You" else message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOwnMessage) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            Color(0xFF64B5F6), // Light blue for others
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = " ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Timestamp
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 2.dp, top = 1.dp)
            )
        }
    }
}

/**
 * Compact input field with semi-transparent styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveStreamInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            placeholder = { 
                Text(
                    "Say something...",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                ) 
            },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .padding(start = 8.dp),
            shape = RoundedCornerShape(22.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f)
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White,
                fontSize = 14.sp
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )
        
        // Send button
        FilledIconButton(
            onClick = onSend,
            modifier = Modifier.size(38.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = "Send",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
