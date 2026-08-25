package com.Chenkham.Echofy.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Google Assistant style interactive visual indicator.
 *
 * Shows 4 glowing animated dots while listening, and replaces the placeholder hint
 * with the live transcript as the user speaks so they can see what was understood.
 */
@Composable
fun GoogleAssistantVisualOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    statusText: String = "Listening for command...",
    transcript: String = ""
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = Color(0xEE1E1E2C),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google Assistant 4-Color Glowing Wave Dots
                AssistantDot(color = Color(0xFF4285F4), delayMs = 0)
                Spacer(Modifier.width(8.dp))
                AssistantDot(color = Color(0xFFEA4335), delayMs = 150)
                Spacer(Modifier.width(8.dp))
                AssistantDot(color = Color(0xFFFBBC05), delayMs = 300)
                Spacer(Modifier.width(8.dp))
                AssistantDot(color = Color(0xFF34A853), delayMs = 450)

                Spacer(Modifier.width(14.dp))

                val hasTranscript = transcript.isNotBlank()
                Text(
                    text = if (hasTranscript) transcript else statusText,
                    // Recognised speech is emphasised; the hint stays muted.
                    color = if (hasTranscript) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = if (hasTranscript) 15.sp else 13.sp,
                    fontWeight = if (hasTranscript) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
private fun AssistantDot(
    color: Color,
    delayMs: Int
) {
    val transition = rememberInfiniteTransition(label = "assistantDot")

    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, delayMillis = delayMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .offset(y = offsetY.dp)
            .size((9 * scale).dp)
            .clip(CircleShape)
            .background(color)
    )
}
