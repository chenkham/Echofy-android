package com.Chenkham.Echofy.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Firebase Cloud Messaging service with FULL notification features:
 * 
 * SUPPORTED FEATURES:
 * ==================
 * 1. TEXT NOTIFICATIONS - Basic title/body
 * 2. IMAGE NOTIFICATIONS - BigPictureStyle with image
 * 3. ACTION BUTTONS - Up to 3 clickable actions
 * 4. BIG TEXT - Expandable long text
 * 5. INBOX STYLE - Multiple lines of text
 * 6. DEEP LINKS - Navigate to specific screens/URLs
 * 7. CUSTOM SOUNDS - Different sounds per notification type
 * 8. PRIORITY - Normal, High, Max priority levels
 * 9. GROUPED NOTIFICATIONS - Bundle related notifications
 * 
 * HOW TO SEND FROM FIREBASE CONSOLE:
 * ==================================
 * 1. Go to Cloud Messaging → Compose notification
 * 2. Fill in title and body
 * 3. Under "Additional options" → "Custom data", add these keys:
 * 
 *    IMAGES:
 *    - image: URL of image to display (e.g., https://example.com/image.jpg)
 *    
 *    ACTION BUTTONS (up to 3):
 *    - action1_title: "Listen Now"
 *    - action1_action: "play_album"
 *    - action1_data: "album_id_here"
 *    - action2_title: "View"
 *    - action2_action: "open_url"
 *    - action2_data: "https://example.com"
 *    
 *    NOTIFICATION TYPE:
 *    - type: "album" | "promotion" | "update" | "listen_together" | "welcome"
 *    
 *    DEEP LINK DATA:
 *    - albumId: "album_id" (for album notifications)
 *    - url: "https://..." (for promotion/update notifications)
 *    
 *    STYLING:
 *    - style: "big_text" | "inbox" | "image" | "normal"
 *    - priority: "high" | "max" | "normal"
 *    - group: "group_name" (for bundled notifications)
 */
class EchofyMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "EchofyMessaging"
        private const val CHANNEL_ID = "echofy_notifications"
        private const val CHANNEL_NAME = "Echofy Notifications"
        private const val GROUP_KEY = "com.Chenkham.Echofy.NOTIFICATIONS"
        
        /**
         * Helper to save notification externally (e.g., from MainActivity).
         */
        fun saveNotification(
            context: Context, 
            title: String, 
            body: String, 
            type: String,
            imageUrl: String? = null
        ) {
            val prefs = context.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
            val notifications = prefs.getStringSet("notifications", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            
            // Check for duplicates
            val exists = notifications.any { it.contains("|$title|$body|") }
            if (exists) return
            
            // Format: timestamp|title|body|type|isRead|imageUrl
            val imageUrlPart = imageUrl ?: ""
            notifications.add("${System.currentTimeMillis()}|$title|$body|$type|false|$imageUrlPart")
            prefs.edit().putStringSet("notifications", notifications.take(50).toSet()).apply()
            Log.d(TAG, "Saved notification to history: $title (image: ${imageUrl != null})")
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        saveTokenToPrefs(token)
        subscribeToTopics()
    }
    
    private fun subscribeToTopics() {
        listOf("all_users", "updates", "listen_together", "promotions").forEach { topic ->
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    Log.d(TAG, if (task.isSuccessful) "Subscribed to $topic" else "Failed: $topic")
                }
        }
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Data payload: ${message.data}")
        
        // Extract all data from payload
        val notificationData = NotificationData(
            title = message.notification?.title ?: message.data["title"] ?: "Echofy",
            body = message.notification?.body ?: message.data["body"] ?: "",
            type = message.data["type"] ?: "general",
            imageUrl = message.notification?.imageUrl?.toString() ?: message.data["image"],
            albumId = message.data["albumId"],
            url = message.data["url"],
            style = message.data["style"] ?: "normal",
            priority = message.data["priority"] ?: "high",
            group = message.data["group"],
            // Action buttons
            action1Title = message.data["action1_title"],
            action1Action = message.data["action1_action"],
            action1Data = message.data["action1_data"],
            action2Title = message.data["action2_title"],
            action2Action = message.data["action2_action"],
            action2Data = message.data["action2_data"],
            action3Title = message.data["action3_title"],
            action3Action = message.data["action3_action"],
            action3Data = message.data["action3_data"]
        )
        
        // Show system notification
        CoroutineScope(Dispatchers.Main).launch {
            showRichNotification(notificationData)
        }
        
        // Save to bell icon history
        saveNotificationLocally(notificationData)
    }
    
    /**
     * Data class holding all notification parameters.
     */
    data class NotificationData(
        val title: String,
        val body: String,
        val type: String,
        val imageUrl: String?,
        val albumId: String?,
        val url: String?,
        val style: String,
        val priority: String,
        val group: String?,
        val action1Title: String?,
        val action1Action: String?,
        val action1Data: String?,
        val action2Title: String?,
        val action2Action: String?,
        val action2Data: String?,
        val action3Title: String?,
        val action3Action: String?,
        val action3Data: String?
    )
    
    private suspend fun showRichNotification(data: NotificationData) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(manager)
        
        // Load image if URL provided
        val imageBitmap = data.imageUrl?.let { loadImageFromUrl(it) }
        
        // Build notification
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_on)
            .setContentTitle(data.title)
            .setContentText(data.body)
            .setPriority(when (data.priority) {
                "max" -> NotificationCompat.PRIORITY_MAX
                "high" -> NotificationCompat.PRIORITY_HIGH
                else -> NotificationCompat.PRIORITY_DEFAULT
            })
            .setContentIntent(createContentIntent(data))
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 250, 250, 250))
        
        // Apply style based on content
        when {
            imageBitmap != null || data.style == "image" -> {
                // BigPictureStyle for images
                val style = NotificationCompat.BigPictureStyle()
                    .bigPicture(imageBitmap)
                    .setBigContentTitle(data.title)
                    .setSummaryText(data.body)
                if (imageBitmap != null) {
                    builder.setLargeIcon(imageBitmap)
                    style.bigLargeIcon(null as Bitmap?) // Hide large icon when expanded
                }
                builder.setStyle(style)
            }
            data.style == "inbox" -> {
                // InboxStyle for multiple lines
                val lines = data.body.split("\\n")
                val style = NotificationCompat.InboxStyle()
                    .setBigContentTitle(data.title)
                lines.forEach { style.addLine(it) }
                builder.setStyle(style)
            }
            data.style == "big_text" || data.body.length > 50 -> {
                // BigTextStyle for long text
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(data.body))
            }
        }
        
        // Add action buttons
        addActionButtons(builder, data)
        
        // Add grouping
        if (data.group != null) {
            builder.setGroup(data.group)
        } else {
            builder.setGroup(GROUP_KEY)
        }
        
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
        Log.d(TAG, "Showed rich notification: ${data.title}")
    }
    
    private fun addActionButtons(builder: NotificationCompat.Builder, data: NotificationData) {
        // Action 1
        if (data.action1Title != null && data.action1Action != null) {
            builder.addAction(
                getActionIcon(data.action1Action),
                data.action1Title,
                createActionIntent(data.action1Action, data.action1Data)
            )
        }
        
        // Action 2
        if (data.action2Title != null && data.action2Action != null) {
            builder.addAction(
                getActionIcon(data.action2Action),
                data.action2Title,
                createActionIntent(data.action2Action, data.action2Data)
            )
        }
        
        // Action 3
        if (data.action3Title != null && data.action3Action != null) {
            builder.addAction(
                getActionIcon(data.action3Action),
                data.action3Title,
                createActionIntent(data.action3Action, data.action3Data)
            )
        }
    }
    
    private fun getActionIcon(action: String): Int {
        return when (action) {
            "play_album", "play" -> R.drawable.play
            "open_url", "view" -> R.drawable.open_in_new
            "dismiss" -> R.drawable.close
            "download" -> R.drawable.download
            "share" -> R.drawable.share
            else -> R.drawable.arrow_forward
        }
    }
    
    private fun createActionIntent(action: String, data: String?): PendingIntent {
        val intent = when (action) {
            "open_url", "view" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(data ?: "https://github.com/chenkham/Echofy-android"))
            }
            "play_album" -> {
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "album")
                    putExtra("albumId", data)
                    putExtra("autoplay", true)
                }
            }
            "download" -> {
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("navigate_to", "downloads")
                }
            }
            else -> defaultIntent()
        }
        
        return PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    private fun createContentIntent(data: NotificationData): PendingIntent {
        val intent = when (data.type) {
            "album" -> data.albumId?.let {
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", "album")
                    putExtra("albumId", it)
                }
            } ?: defaultIntent()
            "promotion", "update" -> data.url?.let {
                Intent(Intent.ACTION_VIEW, Uri.parse(it))
            } ?: defaultIntent()
            "listen_together" -> Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("navigate_to", "listen_together")
            }
            else -> defaultIntent()
        }
        
        return PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
    
    private suspend fun loadImageFromUrl(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            val inputStream = connection.getInputStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image from $url: ${e.message}")
            null
        }
    }
    
    private fun createNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Echofy push notifications"
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun defaultIntent() = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    
    private fun saveTokenToPrefs(token: String) {
        getSharedPreferences("echofy_messaging", Context.MODE_PRIVATE)
            .edit().putString("fcm_token", token).apply()
    }
    
    private fun saveNotificationLocally(data: NotificationData) {
        saveNotification(this, data.title, data.body, data.type, data.imageUrl)
    }
}
