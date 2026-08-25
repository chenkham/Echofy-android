package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ai.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

/**
 * Individual chat bubble composable for AI Assistant screen.
 * Renders user messages with gradient background (right-aligned)
 * and AI responses with dark background + avatar (left-aligned).
 */
@Composable
fun ChatBubble(msg: ChatMessage) {
    val time = remember(msg.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
    }
    val isVoiceTriggered = remember(msg.text) {
        msg.text.lowercase(Locale.getDefault()).let {
            it.startsWith("hey ") || it.startsWith("playing ") || it.startsWith("paused ") ||
            it.startsWith("resuming ") || it.startsWith("skipping ") || it.startsWith("going back") ||
            it.startsWith("setting volume")
        }
    }

    if (msg.isUser) {
        // User bubble — right aligned, gradient
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
                color = Color.Transparent,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(UserBubbleGradientStart, UserBubbleGradientEnd)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        msg.text,
                        style = TextStyle(fontSize = 15.sp, color = Color.White, lineHeight = 22.sp)
                    )
                }
            }
            Text(
                time,
                style = TextStyle(fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.padding(top = 3.dp, end = 6.dp)
            )
        }
    } else {
        // AI bubble — left aligned, with Echofy avatar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(R.drawable.echofy),
                contentDescription = "Echofy",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp),
                    color = AiBubbleBg,
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            msg.text,
                            style = TextStyle(fontSize = 15.sp, color = OnSurface, lineHeight = 22.sp)
                        )

                        // Voice icon for voice-triggered responses
                        if (!msg.isUser && isVoiceTriggered) {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.graphic_eq),
                                    contentDescription = "Voice Response",
                                    tint = Tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Voice",
                                    style = TextStyle(fontSize = 10.sp, color = Tertiary)
                                )
                            }
                        }
                    }
                }
                Text(
                    time,
                    style = TextStyle(fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 3.dp, start = 6.dp)
                )
            }
        }
    }
}

/**
 * Animated thinking indicator with pulsing dots, shown while AI is processing.
 */
@Composable
fun ThinkingIndicator() {
    val transition = rememberInfiniteTransition(label = "think")
    val dot1 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "d1")
    val dot2 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(600, delayMillis = 150), RepeatMode.Reverse), label = "d2")
    val dot3 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(600, delayMillis = 300), RepeatMode.Reverse), label = "d3")

    Row(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.echofy),
            contentDescription = "Echofy",
            modifier = Modifier.size(24.dp).clip(CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Surface(
            color = AiBubbleBg,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf<Float>(dot1, dot2, dot3).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Primary.copy(alpha = 0.3f + alpha * 0.7f))
                    )
                }
            }
        }
    }
}
