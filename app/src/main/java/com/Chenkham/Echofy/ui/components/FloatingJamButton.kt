package com.Chenkham.Echofy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.data.repository.ListenTogetherRepository
import com.Chenkham.Echofy.utils.JamButtonPositionManager
import kotlin.math.roundToInt

/**
 * Floating action button for accessing Jam session chat.
 * 
 * Features:
 * - Visible when user is in an active Jam session
 * - Draggable and repositionable anywhere on screen
 * - Remembers position across app restarts
 * - Pulsing animation to indicate active session
 * - Single tap toggles chat overlay
 */
@Composable
fun FloatingJamButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Screen dimensions
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Button size
    val buttonSize = 56.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }
    
    // Load saved position or use default (bottom-right, above navbar)
    val savedPosition = remember { JamButtonPositionManager.getPosition(context) }
    var offsetX by remember { 
        mutableFloatStateOf(
            if (savedPosition.first >= 0f) savedPosition.first 
            else screenWidth - buttonSizePx - with(density) { 16.dp.toPx() }
        ) 
    }
    var offsetY by remember { 
        mutableFloatStateOf(
            if (savedPosition.second >= 0f) savedPosition.second 
            else screenHeight - buttonSizePx - with(density) { 180.dp.toPx() } // Above navbar + mini player
        ) 
    }
    
    // Track if dragging
    var isDragging by remember { mutableStateOf(false) }
    
    // Pulsing animation for active session indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    // Content scale animation when expanded
    val contentScale by animateFloatAsState(
        targetValue = if (isExpanded) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "contentScale"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Pulsing glow behind button
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(buttonSize)
                    .scale(pulseScale)
                    .alpha(pulseAlpha * 0.4f)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            )
                        )
                    )
            )
        }
        
        // Main floating button
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(buttonSize)
                .scale(contentScale)
                .shadow(
                    elevation = if (isDragging) 12.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.primary,
                    spotColor = MaterialTheme.colorScheme.primary
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // Save position when drag ends
                            JamButtonPositionManager.savePosition(context, offsetX, offsetY)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Clamp to screen bounds
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidth - buttonSizePx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeight - buttonSizePx)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { 
                    if (!isDragging) onToggle() 
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(
                        if (isExpanded) R.drawable.close else R.drawable.chat
                    ),
                    contentDescription = if (isExpanded) "Close chat" else "Open chat",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Unread indicator dot (optional - shows when there are new messages while collapsed)
        val messages by ListenTogetherRepository.messages.collectAsState()
        var lastSeenCount by remember { mutableIntStateOf(messages.size) }
        val hasUnread = !isExpanded && messages.size > lastSeenCount
        
        // Update seen count when expanded
        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                lastSeenCount = messages.size
            }
        }
        
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .offset { 
                        IntOffset(
                            (offsetX + buttonSizePx - with(density) { 14.dp.toPx() }).roundToInt(), 
                            (offsetY - with(density) { 2.dp.toPx() }).roundToInt()
                        ) 
                    }
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
            )
        }
    }
}
