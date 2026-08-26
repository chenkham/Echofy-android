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
        val action = intent.action ?: return

        val player = PlayerConnection.instance?.player ?: com.Chenkham.Echofy.playback.MusicService.instance?.player
        if (player != null) {
            when (action) {
                MusicWidget.ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                            player.prepare()
                        }
                        player.play()
                    }
                }
                MusicWidget.ACTION_PREV -> {
                    if (player.hasPreviousMediaItem() || player.currentPosition > 3000) {
                        player.seekToPrevious()
                    }
                }
                MusicWidget.ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                    }
                }
                MusicWidget.ACTION_OPEN_APP -> openApp(context)
            }
            updateAllWidgets(context)
            MusicWidget.updateAllWidgets(context)
            return
        }

        if (action == MusicWidget.ACTION_OPEN_APP) {
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
                when (action) {
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
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(appContext, CompactMusicWidget::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val activePlayer = PlayerConnection.instance?.player ?: com.Chenkham.Echofy.playback.MusicService.instance?.player
                widgetIds.forEach {
                    updateWidgetWithPlayer(appContext, appWidgetManager, it, activePlayer)
                }
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val activePlayer = PlayerConnection.instance?.player ?: com.Chenkham.Echofy.playback.MusicService.instance?.player
            updateWidgetWithPlayer(context, appWidgetManager, appWidgetId, activePlayer)
        }

        private fun updateWidgetWithPlayer(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            player: Player?,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_compact)
            setPendingIntents(context, views)

            val bgMode = com.Chenkham.Echofy.widget.WidgetPreferences.cachedBackgroundMode
            val scrim = com.Chenkham.Echofy.widget.WidgetPreferences.cachedScrimOpacity
            val showProgress = com.Chenkham.Echofy.widget.WidgetPreferences.cachedShowProgressBar

            views.setInt(
                R.id.widget_compact_scrim_overlay,
                "setBackgroundColor",
                android.graphics.Color.argb((scrim * 255).toInt(), 0, 0, 0)
            )

            when (bgMode) {
                com.Chenkham.Echofy.constants.WidgetBackgroundMode.DOMINANT_COLOR -> {
                    views.setViewVisibility(R.id.widget_compact_background_image, android.view.View.VISIBLE)
                    views.setInt(R.id.widget_compact_root, "setBackgroundColor", android.graphics.Color.argb(255, 20, 20, 26))
                }
                com.Chenkham.Echofy.constants.WidgetBackgroundMode.SOLID -> {
                    views.setViewVisibility(R.id.widget_compact_background_image, android.view.View.GONE)
                    views.setInt(R.id.widget_compact_root, "setBackgroundColor", android.graphics.Color.argb(255, 22, 22, 28))
                }
                else -> {
                    views.setViewVisibility(R.id.widget_compact_background_image, android.view.View.VISIBLE)
                    views.setInt(R.id.widget_compact_root, "setBackgroundResource", R.drawable.widget_background)
                }
            }

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

            val currentPos = player?.currentPosition ?: 0
            val duration = player?.duration ?: 0
            val progress = if (duration > 0 && duration != Long.MAX_VALUE) {
                (currentPos * 100 / duration).toInt()
            } else 0

            if (showProgress && duration > 0 && duration != Long.MAX_VALUE) {
                views.setProgressBar(R.id.widget_compact_progress_bar, 100, progress, false)
                views.setViewVisibility(R.id.widget_compact_progress_bar, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_compact_progress_bar, android.view.View.GONE)
            }

            val thumbnailUrl = player?.mediaMetadata?.artworkUri?.toString()
            if (!thumbnailUrl.isNullOrEmpty() && bgMode != com.Chenkham.Echofy.constants.WidgetBackgroundMode.SOLID) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val appContext = context.applicationContext
                        val drawable = widgetImageLoader(appContext).execute(
                            ImageRequest.Builder(appContext)
                                .data(thumbnailUrl)
                                .size(300, 300)
                                .build()
                        ).drawable
                        drawable?.let {
                            views.setImageViewBitmap(R.id.widget_compact_background_image, it.toBitmap())
                            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        }
                    } catch (_: Exception) {
                    }
                }
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

            val openAppIntent = getActivityPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_compact_root, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_waveform_icon, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_info_container, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_song_title, openAppIntent)
            views.setOnClickPendingIntent(R.id.widget_compact_artist, openAppIntent)
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

        private fun getActivityPendingIntent(context: Context): PendingIntent {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java)
            launchIntent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getActivity(context, 0, launchIntent, flags)
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
