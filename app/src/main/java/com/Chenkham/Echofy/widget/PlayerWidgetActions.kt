package com.Chenkham.Echofy.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.playback.MusicService
import com.Chenkham.Echofy.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object PlayerWidgetActions {
    const val ACTION_PLAY_PAUSE = "com.Chenkham.Echofy.widget.action.PLAY_PAUSE"
    const val ACTION_NEXT = "com.Chenkham.Echofy.widget.action.NEXT"
    const val ACTION_PREVIOUS = "com.Chenkham.Echofy.widget.action.PREVIOUS"
    const val ACTION_REFRESH = "com.Chenkham.Echofy.widget.action.REFRESH"

    fun serviceIntent(
        context: Context,
        action: String,
    ): Intent = Intent(context, MusicService::class.java).setAction(action)

    fun openAppIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    suspend fun executeAction(
        context: Context,
        action: String,
    ) {
        val activePlayer = PlayerConnection.instance?.player ?: MusicService.instance?.player
        if (activePlayer != null) {
            withContext(Dispatchers.Main) {
                when (action) {
                    ACTION_PLAY_PAUSE -> {
                        if (activePlayer.isPlaying) {
                            activePlayer.pause()
                        } else {
                            if (activePlayer.playbackState == Player.STATE_IDLE || activePlayer.playbackState == Player.STATE_ENDED) {
                                activePlayer.prepare()
                            }
                            activePlayer.play()
                        }
                    }
                    ACTION_NEXT -> if (activePlayer.hasNextMediaItem()) activePlayer.seekToNext()
                    ACTION_PREVIOUS -> {
                        if (activePlayer.hasPreviousMediaItem() || activePlayer.currentPosition > 3000) {
                            activePlayer.seekToPrevious()
                        }
                    }
                }
            }
            return
        }

        // Out-of-process / inactive fallback: Control via MediaSession MediaController
        try {
            val appContext = context.applicationContext
            val sessionToken =
                SessionToken(
                    appContext,
                    ComponentName(appContext, MusicService::class.java),
                )
            val controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
            val controller = withContext(Dispatchers.IO) { controllerFuture.get() }
            withContext(Dispatchers.Main) {
                when (action) {
                    ACTION_PLAY_PAUSE -> {
                        if (controller.playWhenReady) {
                            controller.pause()
                        } else {
                            if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                                controller.prepare()
                            }
                            controller.play()
                        }
                    }
                    ACTION_NEXT -> if (controller.hasNextMediaItem()) controller.seekToNext()
                    ACTION_PREVIOUS -> {
                        if (controller.hasPreviousMediaItem() || controller.currentPosition > 3000) {
                            controller.seekToPrevious()
                        }
                    }
                }
            }
            MediaController.releaseFuture(controllerFuture)
        } catch (e: Throwable) {
            Timber.e(e, "Failed to execute widget action via MediaController")
        }
    }

    fun sendServiceAction(
        context: Context,
        action: String,
    ) {
        try {
            context.startService(serviceIntent(context, action))
        } catch (e: Throwable) {
            Timber.w(e, "sendServiceAction fallback suppressed exception")
        }
    }
}

class PlayPauseWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerWidgetActions.executeAction(context, PlayerWidgetActions.ACTION_PLAY_PAUSE)
    }
}

class NextWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerWidgetActions.executeAction(context, PlayerWidgetActions.ACTION_NEXT)
    }
}

class PreviousWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        PlayerWidgetActions.executeAction(context, PlayerWidgetActions.ACTION_PREVIOUS)
    }
}
