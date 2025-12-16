package com.Chenkham.Echofy.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.Chenkham.Echofy.R
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
class EchofyMediaNotificationProvider(
    private val context: Context
) : DefaultMediaNotificationProvider(context) {

    init {
        setSmallIcon(R.drawable.echofy_monochrome)
    }

    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {
        val actions = super.addNotificationActions(
            mediaSession,
            mediaButtons,
            builder,
            actionFactory
        )

        builder.apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setShowWhen(false)
            setOngoing(true)
            setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            priority = NotificationCompat.PRIORITY_MAX
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowSystemGeneratedContextualActions(true)
            }
        }

        return actions
    }

    companion object {
        fun createNotificationChannel(
            context: Context,
            channelId: String,
            @StringRes nameResId: Int
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    context.getString(nameResId),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = context.getString(R.string.music_player_description)
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}
