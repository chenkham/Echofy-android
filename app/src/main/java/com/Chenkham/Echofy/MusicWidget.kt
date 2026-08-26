package com.Chenkham.Echofy

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver.PendingResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import com.Chenkham.Echofy.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.withContext

class MusicWidget : AppWidgetProvider() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var isUpdating = false

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        startProgressUpdater(context)
    }

    override fun onEnabled(context: Context) {
        startProgressUpdater(context)
    }

    override fun onDisabled(context: Context) {
        stopProgressUpdater()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        
        val player = PlayerConnection.instance?.player ?: com.Chenkham.Echofy.playback.MusicService.instance?.player
        if (player != null) {
            when (action) {
                ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) {
                        player.pause()
                    } else {
                        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                            player.prepare()
                        }
                        player.play()
                    }
                    updateAllWidgets(context)
                }

                ACTION_PREV -> {
                    if (player.hasPreviousMediaItem() || player.currentPosition > 3000) {
                        player.seekToPrevious()
                    }
                    updateAllWidgets(context)
                }

                ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                    }
                    updateAllWidgets(context)
                }

                ACTION_SHUFFLE -> {
                    PlayerConnection.instance?.toggleShuffle() ?: run {
                        player.shuffleModeEnabled = !player.shuffleModeEnabled
                    }
                    updateAllWidgets(context)
                }

                ACTION_LIKE -> {
                    PlayerConnection.instance?.toggleLike()
                    updateAllWidgets(context)
                }

                ACTION_REPLAY -> {
                    PlayerConnection.instance?.toggleReplayMode()
                    updateAllWidgets(context)
                }

                ACTION_OPEN_APP -> {
                    openApp(context)
                }

                ACTION_STATE_CHANGED, ACTION_UPDATE_PROGRESS -> {
                    updateAllWidgets(context)
                }
            }
        } else {
            handleActionWithMediaController(context, action, goAsync())
        }
    }

    private fun handleActionWithMediaController(context: Context, action: String, pendingResult: PendingResult? = null) {
        if (action == ACTION_OPEN_APP) {
            openApp(context)
            pendingResult?.finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val tokenContext = context.applicationContext
            val sessionToken = SessionToken(tokenContext, ComponentName(tokenContext, com.Chenkham.Echofy.playback.MusicService::class.java))
            val controllerFuture = MediaController.Builder(tokenContext, sessionToken).buildAsync()

            try {
                val controller = withContext(Dispatchers.IO) { controllerFuture.get() }

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
                    ACTION_PREV -> {
                        if (controller.hasPreviousMediaItem() || controller.currentPosition > 3000) {
                            controller.seekToPrevious()
                        }
                    }
                    ACTION_NEXT -> {
                        if (controller.hasNextMediaItem()) {
                            controller.seekToNext()
                        }
                    }
                    ACTION_SHUFFLE -> {
                        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
                    }
                    ACTION_LIKE -> {
                        PlayerConnection.instance?.toggleLike()
                    }
                    ACTION_STATE_CHANGED, ACTION_UPDATE_PROGRESS -> {
                    }
                }
                updateAllWidgets(context)
            } catch (_: Exception) {
            } finally {
                MediaController.releaseFuture(controllerFuture)
                pendingResult?.finish()
            }
        }
    }

    private fun startProgressUpdater(context: Context) {
        if (isUpdating) return
        isUpdating = true

        runnable = object : Runnable {
            override fun run() {
                try {
                    val appContext = context.applicationContext ?: context
                    val appWidgetManager = AppWidgetManager.getInstance(appContext)
                    val widgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(appContext, MusicWidget::class.java)
                    )

                    if (widgetIds.isNotEmpty()) {
                        val playerConnection = PlayerConnection.instance
                        if (playerConnection != null && playerConnection.player.isPlaying) {
                            val currentPos = playerConnection.player.currentPosition
                            val duration = playerConnection.player.duration

                            if (duration > 0 && duration != Long.MAX_VALUE) {
                                val progress = (currentPos * 100 / duration).toInt()
                                val showProgress = com.Chenkham.Echofy.widget.WidgetPreferences.cachedShowProgressBar

                                widgetIds.forEach { widgetId ->
                                    val views = RemoteViews(appContext.packageName, R.layout.widget_music)
                                    if (showProgress) {
                                        views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                                    }
                                    appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient exceptions
                }

                if (isUpdating) {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(runnable)
    }

    private fun stopProgressUpdater() {
        isUpdating = false
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.Chenkham.Echofy.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.Chenkham.Echofy.ACTION_PREV"
        const val ACTION_NEXT = "com.Chenkham.Echofy.ACTION_NEXT"
        const val ACTION_SHUFFLE = "com.Chenkham.Echofy.ACTION_SHUFFLE"
        const val ACTION_LIKE = "com.Chenkham.Echofy.ACTION_LIKE"
        const val ACTION_REPLAY = "com.Chenkham.Echofy.ACTION_REPLAY"
        const val ACTION_OPEN_APP = "com.Chenkham.Echofy.ACTION_OPEN_APP"
        const val ACTION_STATE_CHANGED = "com.Chenkham.Echofy.ACTION_STATE_CHANGED"
        const val ACTION_UPDATE_PROGRESS = "com.Chenkham.Echofy.ACTION_UPDATE_PROGRESS"

        @Volatile
        private var sharedImageLoader: ImageLoader? = null

        private fun widgetImageLoader(context: Context): ImageLoader =
            sharedImageLoader ?: synchronized(this) {
                sharedImageLoader ?: ImageLoader(context.applicationContext).also {
                    sharedImageLoader = it
                }
            }

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext ?: context
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(appContext, MusicWidget::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val activePlayer = PlayerConnection.instance?.player ?: com.Chenkham.Echofy.playback.MusicService.instance?.player
                val isLiked = PlayerConnection.instance?.isCurrentSongLiked() ?: false
                widgetIds.forEach { updateWidgetWithPlayer(appContext, appWidgetManager, it, activePlayer, isLiked) }
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val activePlayer = PlayerConnection.instance?.player ?: com.Chenkham.Echofy.playback.MusicService.instance?.player
            val isLiked = PlayerConnection.instance?.isCurrentSongLiked() ?: false
            updateWidgetWithPlayer(context, appWidgetManager, appWidgetId, activePlayer, isLiked)
        }

        private fun updateWidgetWithPlayer(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            player: Player?,
            isLiked: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            setPendingIntents(context, views)

            val bgMode = com.Chenkham.Echofy.widget.WidgetPreferences.cachedBackgroundMode
            val scrim = com.Chenkham.Echofy.widget.WidgetPreferences.cachedScrimOpacity
            val cornerRadius = com.Chenkham.Echofy.widget.WidgetPreferences.cachedCornerRadius
            val showProgress = com.Chenkham.Echofy.widget.WidgetPreferences.cachedShowProgressBar
            val density = context.resources.displayMetrics.density
            val cornerRadiusPx = cornerRadius * density

            views.setInt(
                R.id.widget_scrim_overlay,
                "setBackgroundColor",
                android.graphics.Color.argb((scrim * 255).toInt(), 0, 0, 0)
            )

            when (bgMode) {
                com.Chenkham.Echofy.constants.WidgetBackgroundMode.DOMINANT_COLOR -> {
                    views.setViewVisibility(R.id.widget_background_image, android.view.View.VISIBLE)
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background)
                }
                com.Chenkham.Echofy.constants.WidgetBackgroundMode.SOLID -> {
                    views.setViewVisibility(R.id.widget_background_image, android.view.View.GONE)
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background)
                }
                else -> {
                    views.setViewVisibility(R.id.widget_background_image, android.view.View.VISIBLE)
                    views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background)
                }
            }

            player?.let { player ->
                val songTitle = player.mediaMetadata.title?.toString()
                    ?: context.getString(R.string.song_title)
                val artist = player.mediaMetadata.artist?.toString()
                    ?: context.getString(R.string.artist_name)

                views.setTextViewText(R.id.widget_song_title, songTitle)
                views.setTextViewText(R.id.widget_artist, artist)

                val playPauseIcon = if (player.isPlaying) R.drawable.pause else R.drawable.play
                views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

                val shuffleIcon = if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.queue_music
                views.setImageViewResource(R.id.widget_shuffle, shuffleIcon)

                val likeIcon = if (isLiked) R.drawable.heart_fill else R.drawable.heart
                views.setImageViewResource(R.id.widget_like, likeIcon)

                val currentPos = player.currentPosition
                val duration = player.duration

                val progress = if (duration > 0 && duration != Long.MAX_VALUE) {
                    (currentPos * 100 / duration).toInt()
                } else 0

                if (showProgress && duration > 0 && duration != Long.MAX_VALUE) {
                    views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                }

                val thumbnailUrl = player.mediaMetadata.artworkUri?.toString()
                if (!thumbnailUrl.isNullOrEmpty() && bgMode != com.Chenkham.Echofy.constants.WidgetBackgroundMode.SOLID) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val appContext = context.applicationContext
                            val request = ImageRequest.Builder(appContext)
                                .data(thumbnailUrl)
                                .size(400, 300)
                                .build()
                            val drawable = widgetImageLoader(appContext).execute(request).drawable
                            drawable?.let {
                                val bmp = it.toBitmap()
                                val rounded = createRoundedBitmap(bmp, cornerRadiusPx)
                                views.setImageViewBitmap(R.id.widget_background_image, rounded)
                                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                            }
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                }
            } ?: run {
                views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_artist, context.getString(R.string.tap_to_open))
                views.setImageViewResource(R.id.widget_play_pause, R.drawable.play)
                views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            val playPausePendingIntent = getBroadcastPendingIntent(context, ACTION_PLAY_PAUSE)
            val prevPendingIntent = getBroadcastPendingIntent(context, ACTION_PREV)
            val nextPendingIntent = getBroadcastPendingIntent(context, ACTION_NEXT)
            val shufflePendingIntent = getBroadcastPendingIntent(context, ACTION_SHUFFLE)
            val likePendingIntent = getBroadcastPendingIntent(context, ACTION_LIKE)
            val openAppPendingIntent = getActivityPendingIntent(context)

            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_shuffle, shufflePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_like, likePendingIntent)

            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_waveform_icon, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_info_container, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_artist, openAppPendingIntent)
        }

        private fun getBroadcastPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidget::class.java).apply {
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

        fun createRoundedBitmap(source: Bitmap, cornerRadiusPx: Float): Bitmap {
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val rect = android.graphics.RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            val path = android.graphics.Path().apply {
                addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(path)
            canvas.drawBitmap(source, 0f, 0f, paint)
            return output
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
