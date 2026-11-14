package com.Chenkham.Echofy.ui.main

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Rational
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.playback.PlayerConnection
import timber.log.Timber

/**
 * Manages Picture-in-Picture (PiP) parameters, controls, and system callbacks.
 */
class PictureInPictureHelper(
    private val activity: Activity,
    private val playerConnectionProvider: () -> PlayerConnection?,
) {
    var isInPipMode = false
        private set

    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        isInPipMode = isInPictureInPictureMode
    }

    private val pipBroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                if (intent?.action == ACTION_PIP_CONTROL) {
                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        CONTROL_PLAY_PAUSE -> {
                            playerConnectionProvider()?.player?.let { player ->
                                if (player.isPlaying) player.pause() else player.play()
                            }
                        }
                        CONTROL_NEXT -> playerConnectionProvider()?.player?.seekToNext()
                        CONTROL_PREVIOUS -> playerConnectionProvider()?.player?.seekToPrevious()
                    }
                    updatePipParams()
                }
            }
        }

    fun register(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                pipBroadcastReceiver,
                IntentFilter(ACTION_PIP_CONTROL),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(pipBroadcastReceiver, IntentFilter(ACTION_PIP_CONTROL))
        }
    }

    fun unregister(context: Context) {
        try {
            context.unregisterReceiver(pipBroadcastReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    fun enterPipMode() {
        try {
            val params = buildPipParams()
            activity.enterPictureInPictureMode(params)
        } catch (e: Exception) {
            Timber.e(e, "Failed to enter PiP mode")
        }
    }

    fun updatePipParams() {
        if (isInPipMode) {
            try {
                activity.setPictureInPictureParams(buildPipParams())
            } catch (e: Exception) {
                Timber.e(e, "Failed to update PiP params")
            }
        }
    }

    fun buildPipParams(): PictureInPictureParams {
        val isPlaying = playerConnectionProvider()?.player?.isPlaying == true

        val actions = mutableListOf<RemoteAction>()

        val prevIntent =
            PendingIntent.getBroadcast(
                activity,
                CONTROL_PREVIOUS,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_PREVIOUS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        actions.add(
            RemoteAction(
                android.graphics.drawable.Icon.createWithResource(activity, R.drawable.skip_previous),
                "Previous",
                "Previous track",
                prevIntent,
            ),
        )

        val playPauseIntent =
            PendingIntent.getBroadcast(
                activity,
                CONTROL_PLAY_PAUSE,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"
        actions.add(
            RemoteAction(
                android.graphics.drawable.Icon.createWithResource(activity, playPauseIcon),
                playPauseTitle,
                playPauseTitle,
                playPauseIntent,
            ),
        )

        val nextIntent =
            PendingIntent.getBroadcast(
                activity,
                CONTROL_NEXT,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_CONTROL_TYPE, CONTROL_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        actions.add(
            RemoteAction(
                android.graphics.drawable.Icon.createWithResource(activity, R.drawable.skip_next),
                "Next",
                "Next track",
                nextIntent,
            ),
        )

        val builder =
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }

        return builder.build()
    }

    companion object {
        const val ACTION_PIP_CONTROL = "com.Chenkham.Echofy.PIP_CONTROL"
        private const val EXTRA_CONTROL_TYPE = "control_type"
        private const val CONTROL_PLAY_PAUSE = 1
        private const val CONTROL_NEXT = 2
        private const val CONTROL_PREVIOUS = 3
    }
}
