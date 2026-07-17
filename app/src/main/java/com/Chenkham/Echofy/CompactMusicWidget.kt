package com.Chenkham.Echofy

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import coil.request.ImageRequest
import com.Chenkham.Echofy.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompactMusicWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val playerConnection = PlayerConnection.instance
        if (playerConnection != null) {
            when (intent.action) {
                MusicWidget.ACTION_PLAY_PAUSE -> playerConnection.togglePlayPause()
                MusicWidget.ACTION_PREV -> playerConnection.seekToPrevious()
                MusicWidget.ACTION_NEXT -> playerConnection.seekToNext()
                MusicWidget.ACTION_OPEN_APP -> openApp(context)
            }
            updateAllWidgets(context)
            MusicWidget.updateAllWidgets(context)
            return
        }

        if (intent.action == MusicWidget.ACTION_OPEN_APP) {
            openApp(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            val sessionToken = SessionToken(
                context.applicationContext,
                ComponentName(context.applicationContext, com.Chenkham.Echofy.playback.MusicService::class.java)
            )
            val controllerFuture = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
            try {
                val controller = withContext(Dispatchers.IO) { controllerFuture.get() }
                when (intent.action) {
                    MusicWidget.ACTION_PLAY_PAUSE -> {
                        if (controller.playWhenReady) controller.pause() else {
                            if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                                controller.prepare()
                            }
                            controller.play()
                        }
                    }
                    MusicWidget.ACTION_PREV -> controller.seekToPrevious()
                    MusicWidget.ACTION_NEXT -> controller.seekToNext()
                }
                updateAllWidgets(context)
                MusicWidget.updateAllWidgets(context)
            } catch (_: Exception) {
            } finally {
                MediaController.releaseFuture(controllerFuture)
                pendingResult.finish()
            }
        }
    }

    companion object {
        @Volatile
        private var sharedImageLoader: ImageLoader? = null

        private fun widgetImageLoader(context: Context): ImageLoader =
            sharedImageLoader ?: synchronized(this) {
                sharedImageLoader ?: ImageLoader(context.applicationContext).also {
                    sharedImageLoader = it
                }
            }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, CompactMusicWidget::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val playerConnection = PlayerConnection.instance
                if (playerConnection != null) {
                    widgetIds.forEach {
                        updateWidgetWithPlayer(context, appWidgetManager, it, playerConnection.player)
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        val sessionToken = SessionToken(
                            context,
                            ComponentName(context, com.Chenkham.Echofy.playback.MusicService::class.java)
                        )
                        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                        try {
                            val controller = withContext(Dispatchers.IO) { controllerFuture.get() }
                            widgetIds.forEach {
                                updateWidgetWithPlayer(context, appWidgetManager, it, controller)
                            }
                        } catch (_: Exception) {
                            widgetIds.forEach {
                                updateWidgetWithPlayer(context, appWidgetManager, it, null)
                            }
                        } finally {
                            MediaController.releaseFuture(controllerFuture)
                        }
                    }
                }
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val playerConnection = PlayerConnection.instance
            updateWidgetWithPlayer(context, appWidgetManager, appWidgetId, playerConnection?.player)
        }

        private fun updateWidgetWithPlayer(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            player: Player?,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_compact)
            setPendingIntents(context, views)

            val songTitle = player?.mediaMetadata?.title?.toString()
                ?: context.getString(R.string.app_name)
            val artist = player?.mediaMetadata?.artist?.toString()
                ?: context.getString(R.string.tap_to_open)

            views.setTextViewText(R.id.widget_compact_song_title, songTitle)
            views.setTextViewText(R.id.widget_compact_artist, artist)
            views.setImageViewResource(
                R.id.widget_compact_play_pause,
                if (player?.isPlaying == true) R.drawable.pause else R.drawable.play
            )

            val thumbnailUrl = player?.mediaMetadata?.artworkUri?.toString()
            if (!thumbnailUrl.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        val drawable = widgetImageLoader(appContext).execute(
                            ImageRequest.Builder(appContext)
                                .data(thumbnailUrl)
                                .size(120, 120)
                                .build()
                        ).drawable
                        drawable?.let {
                            views.setImageViewBitmap(R.id.widget_compact_album_art, it.toBitmap())
                            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        }
                    } catch (_: Exception) {
                        views.setImageViewResource(R.id.widget_compact_album_art, R.drawable.music_note)
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                    }
                }
            } else {
                views.setImageViewResource(R.id.widget_compact_album_art, R.drawable.music_note)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            views.setOnClickPendingIntent(
                R.id.widget_compact_prev,
                getBroadcastPendingIntent(context, MusicWidget.ACTION_PREV)
            )
            views.setOnClickPendingIntent(
                R.id.widget_compact_play_pause,
                getBroadcastPendingIntent(context, MusicWidget.ACTION_PLAY_PAUSE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_compact_next,
                getBroadcastPendingIntent(context, MusicWidget.ACTION_NEXT)
            )

            val openAppIntent = getBroadcastPendingIntent(context, MusicWidget.ACTION_OPEN_APP)
            views.setOnClickPendingIntent(R.id.widget_compact_root, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_album_art, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_song_title, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_artist, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_logo, openAppIntent)
        }

        private fun getBroadcastPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, CompactMusicWidget::class.java).apply {
                this.action = action
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
        }

        private fun openApp(context: Context) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(this)
            }
        }
    }
}
