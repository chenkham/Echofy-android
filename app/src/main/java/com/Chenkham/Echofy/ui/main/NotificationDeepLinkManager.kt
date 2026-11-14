package com.Chenkham.Echofy.ui.main

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles push notification deep links and in-app notification history.
 */
class NotificationDeepLinkManager(private val context: Context) {
    private var pendingNotificationAlbumId: String? = null

    /**
     * Inspect incoming intent for push notification payloads and deep links.
     */
    fun handleNotificationDeepLink(intent: Intent?) {
        intent ?: return

        val navigateTo = intent.getStringExtra("navigate_to")
        val albumId = intent.getStringExtra("albumId")

        // Check if this is from a notification (FCM adds google.message_id)
        val messageId = intent.getStringExtra("google.message_id")
        val fcmTitle = intent.getStringExtra("gcm.notification.title")
        val fcmBody = intent.getStringExtra("gcm.notification.body")

        if (messageId != null || fcmTitle != null) {
            val title = fcmTitle ?: intent.getStringExtra("title") ?: "Notification"
            val body = fcmBody ?: intent.getStringExtra("body") ?: ""
            val type = intent.getStringExtra("type") ?: "general"
            saveNotificationToHistory(title, body, type)
        }

        if (navigateTo == "album" && albumId != null) {
            Log.d(TAG, "Deep link to album: $albumId")
            pendingNotificationAlbumId = albumId
        }
    }

    /**
     * Persist received notification in local SharedPreferences for notification center display.
     */
    private fun saveNotificationToHistory(
        title: String,
        body: String,
        type: String,
    ) {
        try {
            val prefs = context.getSharedPreferences("echofy_notifications", Context.MODE_PRIVATE)
            val notifications = prefs.getStringSet("notifications", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

            val exists = notifications.any { it.contains("|$title|$body|") }
            if (exists) return

            notifications.add("${System.currentTimeMillis()}|$title|$body|$type|false")
            prefs.edit().putStringSet("notifications", notifications.take(50).toSet()).apply()
            Log.d(TAG, "Saved notification to history: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save notification: ${e.message}")
        }
    }

    /**
     * Consumes and clears the pending album navigation target when the NavHost is ready.
     */
    fun consumePendingNotificationNavigation(): String? {
        val albumId = pendingNotificationAlbumId
        pendingNotificationAlbumId = null
        return albumId
    }

    companion object {
        private const val TAG = "NotificationDeepLink"
    }
}
