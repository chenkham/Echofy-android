package com.Chenkham.Echofy.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.utils.backToMain
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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
 * Professional Notifications screen with day grouping, polished cards, and modern UI.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    
    fun deleteNotification(notificationId: String) {
        val prefs = context.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
        val storedNotifications = prefs.getStringSet("notifications", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        val updated = storedNotifications.filterNot { entry ->
            entry.startsWith(notificationId)
        }.toSet()
        
        prefs.edit().putStringSet("notifications", updated).apply()
        loadNotifications()
    }
    
    LaunchedEffect(Unit) {
        loadNotifications()
    }
    
    val unreadCount = notifications.count { !it.isRead }
    val groupedNotifications = remember(notifications) {
        notifications.groupBy { getDayCategory(it.timestamp) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.notifications),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { loadNotifications() },
                        onLongClick = {},
                    ) {
                        Icon(
                            painterResource(R.drawable.sync),
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { clearAllNotifications() },
                            onLongClick = {},
                        ) {
                            Icon(
                                painterResource(R.drawable.delete),
                                contentDescription = "Clear all",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                NotificationsEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(
                            bottom = LocalPlayerAwareWindowInsets.current
                                .asPaddingValues()
                                .calculateBottomPadding()
                        )
                )
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedNotifications.forEach { (dayCategory, notificationsInDay) ->
                        stickyHeader(key = "header_$dayCategory") {
                            NotificationsDayHeader(title = dayCategory)
                        }
                        items(
                            items = notificationsInDay,
                            key = { it.id }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onRead = { markAsRead(notification.id) },
                                onDelete = { deleteNotification(notification.id) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centered, intentional empty state shown when there is nothing in the inbox.
 */
@Composable
private fun NotificationsEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.notification_on),
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "You're all caught up",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "New updates, releases and announcements will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Sticky day separator ("Today" / "Yesterday" / "Earlier").
 */
@Composable
private fun NotificationsDayHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 8.dp, bottom = 6.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationCard(
    notification: LocalNotification,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    
    val accentColor = when (notification.type) {
        "welcome" -> Color(0xFF4CAF50)
        "listen_together" -> Color(0xFF2196F3)
        "update" -> Color(0xFFFF9800)
        "promotion" -> Color(0xFFE91E63)
        "album" -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = modifier
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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // No shadow
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
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
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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
                    
                    Spacer(Modifier.height(3.dp))
                    
                    // Relative timestamp
                    Text(
                        text = getRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // 3-dot menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        onLongClick = {},
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "Options",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as read") },
                            onClick = {
                                onRead()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.visibility),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(10.dp))
            
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
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Returns the day category for grouping notifications.
 */
private fun getDayCategory(timestamp: Long): String {
    val notificationCal = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }
    val todayCal = Calendar.getInstance()
    
    val notificationDay = notificationCal.get(Calendar.DAY_OF_YEAR)
    val notificationYear = notificationCal.get(Calendar.YEAR)
    val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)
    val todayYear = todayCal.get(Calendar.YEAR)
    
    return when {
        notificationYear == todayYear && notificationDay == todayDay -> "Today"
        notificationYear == todayYear && notificationDay == todayDay - 1 -> "Yesterday"
        else -> "Earlier"
    }
}

/**
 * Returns human-readable relative time like "2m ago", "3h ago", etc.
 */
private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> {
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
            "${mins}m ago"
        }
        diff < TimeUnit.DAYS.toMillis(1) -> {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            "${hours}h ago"
        }
        diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
        diff < TimeUnit.DAYS.toMillis(7) -> {
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            "${days}d ago"
        }
        else -> {
            val notificationCal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val pattern = if (notificationCal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)) {
                "MMM d"
            } else {
                "MMM d, yyyy"
            }
            SimpleDateFormat(pattern, Locale.getDefault()).format(timestamp)
        }
    }
}
