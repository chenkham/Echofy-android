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
import androidx.media3.common.Player
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

    /**
     * Reorder the media buttons so the notification looks like Spotify:
     * [Repeat] [Previous] [Play/Pause] [Next] [Heart/Like]
     *
     * Media3's DefaultMediaNotificationProvider returns custom + player buttons.
     * We separate them by checking sessionCommand (custom) vs playerCommand (player),
     * then reassemble in the desired order.
     */
    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        val defaultButtons = super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)

        // Separate: custom buttons have sessionCommand set, player buttons have playerCommand set
        val customButtons = defaultButtons.filter { it.sessionCommand != null }
        val playerButtons = defaultButtons.filter { it.sessionCommand == null }

        // Find specific custom buttons by their action string
        val repeatButton = customButtons.find {
            it.sessionCommand?.customAction == com.Chenkham.Echofy.constants.MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE
        }
        val likeButton = customButtons.find {
            it.sessionCommand?.customAction == com.Chenkham.Echofy.constants.MediaSessionConstants.ACTION_TOGGLE_LIKE
        }

        // Find player buttons using ALL possible command variants
        val prevButton = playerButtons.find {
            it.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
            it.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        }
        val playPauseButton = playerButtons.find {
            it.playerCommand == Player.COMMAND_PLAY_PAUSE
        }
        val nextButton = playerButtons.find {
            it.playerCommand == Player.COMMAND_SEEK_TO_NEXT ||
            it.playerCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
        }

        // Rebuild with notification-specific larger icons for transport controls
        val reordered = mutableListOf<CommandButton>()

        repeatButton?.let { reordered.add(it) }

        // Previous with bigger icon
        if (prevButton != null) {
            reordered.add(
                prevButton.copyWithIcon(R.drawable.notif_skip_previous)
            )
        }

        // Play/Pause with bigger icon
        if (playPauseButton != null) {
            val icon = if (showPauseButton) R.drawable.notif_pause else R.drawable.notif_play
            reordered.add(
                playPauseButton.copyWithIcon(icon)
            )
        }

        // Next with bigger icon
        if (nextButton != null) {
            reordered.add(
                nextButton.copyWithIcon(R.drawable.notif_skip_next)
            )
        }

        likeButton?.let { reordered.add(it) }

        return ImmutableList.copyOf(reordered)
    }

    /**
     * Helper to clone a CommandButton with a different icon resource.
     */
    private fun CommandButton.copyWithIcon(iconResId: Int): CommandButton {
        val builder = CommandButton.Builder()
            .setDisplayName(displayName)
            .setIconResId(iconResId)
            .setEnabled(isEnabled)
        if (sessionCommand != null) {
            builder.setSessionCommand(sessionCommand!!)
        } else {
            builder.setPlayerCommand(playerCommand)
        }
        return builder.build()
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
            setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            priority = NotificationCompat.PRIORITY_DEFAULT
            
            // Modern styling
            setColorized(true)
            setColor(0xFF7C4DFF.toInt()) // Brand Purple
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setAllowSystemGeneratedContextualActions(true)
            }

            // Show Previous, Play/Pause, Next in the compact notification view
            // Indices: 0=Repeat, 1=Previous, 2=Play/Pause, 3=Next, 4=Like
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(1, 2, 3) // Prev, Play/Pause, Next
                    .setMediaSession(
                        mediaSession.sessionCompatToken
                    )
            )
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
                    NotificationManager.IMPORTANCE_DEFAULT
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
