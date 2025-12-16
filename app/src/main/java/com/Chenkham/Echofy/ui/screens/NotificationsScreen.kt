package com.Chenkham.Echofy.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import java.util.concurrent.TimeUnit

/**
 * Data class representing a locally stored notification.
 */
data class LocalNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: String,
    val isRead: Boolean,
    val imageUrl: String? = null
)

/**
 * Professional Notifications screen with expandable cards and modern UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf<List<LocalNotification>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    fun loadNotifications() {
        isRefreshing = true
        val prefs = context.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
        val storedNotifications = prefs.getStringSet("notifications", emptySet()) ?: emptySet()
        
        notifications = storedNotifications.mapNotNull { entry ->
            try {
                // Format: timestamp|title|body|type|isRead|imageUrl (6 parts)
                val parts = entry.split("|", limit = 6)
                if (parts.size >= 5) {
                    LocalNotification(
                        id = parts[0],
                        title = parts[1],
                        message = parts[2],
                        type = parts[3],
                        isRead = parts[4].toBoolean(),
                        timestamp = parts[0].toLongOrNull() ?: 0L,
                        imageUrl = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
                    )
                } else if (parts.size >= 4) {
                    LocalNotification(
                        id = parts[0],
                        title = parts[1],
                        message = parts[2],
                        type = parts[3],
                        isRead = false,
                        timestamp = parts[0].toLongOrNull() ?: 0L,
                        imageUrl = null
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.timestamp }
        
        isRefreshing = false
    }
    
    fun clearAllNotifications() {
        val prefs = context.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
        prefs.edit().remove("notifications").apply()
        notifications = emptyList()
    }
    
    fun markAsRead(notificationId: String) {
        val prefs = context.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
        val storedNotifications = prefs.getStringSet("notifications", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        val updated = storedNotifications.map { entry ->
            if (entry.startsWith(notificationId)) {
                entry.replace("|false", "|true")
            } else entry
        }.toSet()
        
        prefs.edit().putStringSet("notifications", updated).apply()
        loadNotifications()
    }
    
    LaunchedEffect(Unit) {
        loadNotifications()
    }
    
    val unreadCount = notifications.count { !it.isRead }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            stringResource(R.string.notifications),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Text(
                                "$unreadCount unread",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { clearAllNotifications() }) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = "Clear all"
                            )
                        }
                    }
                    IconButton(onClick = { loadNotifications() }) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isRefreshing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.notification_on),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.no_notifications),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "You're all caught up!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onRead = { markAsRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: LocalNotification,
    onRead: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val accentColor = when (notification.type) {
        "welcome" -> Color(0xFF4CAF50)
        "listen_together" -> Color(0xFF2196F3)
        "update" -> Color(0xFFFF9800)
        "promotion" -> Color(0xFFE91E63)
        "album" -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { 
                isExpanded = !isExpanded
                if (!notification.isRead) onRead()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (notification.isRead) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Type icon with gradient background
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.7f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            when (notification.type) {
                                "welcome" -> R.drawable.favorite
                                "listen_together" -> R.drawable.people_filled
                                "update" -> R.drawable.update
                                "promotion" -> R.drawable.open_in_new
                                "album" -> R.drawable.album
                                else -> R.drawable.notification_on
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    // Title row with unread indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (!notification.isRead) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    // Relative timestamp
                    Text(
                        text = getRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Message - expandable
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f
            )
            
            // Tap to expand hint
            if (!isExpanded && notification.message.length > 80) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to read more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Returns human-readable relative time like "2 hours ago", "Yesterday", etc.
 */
private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            "$mins min${if (mins > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "$hours hour${if (hours > 1) "s" else ""} ago"
        }
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "$days days ago"
        }
        else -> {
            val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
            "$weeks week${if (weeks > 1) "s" else ""} ago"
        }
    }
}
