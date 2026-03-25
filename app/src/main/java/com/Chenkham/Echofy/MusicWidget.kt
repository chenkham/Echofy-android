package com.Chenkham.Echofy

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        
        val playerConnection = PlayerConnection.instance
        if (playerConnection != null) {
            handleActionWithPlayerConnection(context, intent.action, playerConnection)
        } else {
            handleActionWithMediaController(context, intent.action)
        }
    }

    private fun handleActionWithPlayerConnection(context: Context, action: String?, playerConnection: PlayerConnection) {
        when (action) {
            ACTION_PLAY_PAUSE -> {
                playerConnection.togglePlayPause()
                updateAllWidgets(context)
            }

            ACTION_PREV -> {
                playerConnection.seekToPrevious()
                updateAllWidgets(context)
            }

            ACTION_NEXT -> {
                playerConnection.seekToNext()
                updateAllWidgets(context)
            }

            ACTION_SHUFFLE -> {
                PlayerConnection.instance?.toggleShuffle()
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
    }

    private fun handleActionWithMediaController(context: Context, action: String?) {
        if (action == ACTION_OPEN_APP) {
            openApp(context)
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val tokenContext = context.applicationContext
            val sessionToken = SessionToken(tokenContext, ComponentName(tokenContext, com.Chenkham.Echofy.playback.MusicService::class.java))
            val controllerFuture = MediaController.Builder(tokenContext, sessionToken).buildAsync()
            
            try {
                // Wait for controller
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
                            
                            // If the service process was completely killed, the controller connection restores the Service, 
                            // but the persistent queue loads asynchronously. play() is ignored if the queue is empty.
                            // We wait for the queue to load and retry play.
                            if (controller.mediaItemCount == 0) {
                                withContext(Dispatchers.IO) {
                                    var retries = 0
                                    while (controller.mediaItemCount == 0 && retries < 20) {
                                        kotlinx.coroutines.delay(100)
                                        retries++
                                    }
                                }
                                controller.prepare()
                                controller.play()
                            }
                        }
                    }
                    ACTION_PREV -> {
                        if (controller.hasPreviousMediaItem() || controller.currentPosition > 3000) {
                            controller.seekToPrevious()
                            controller.prepare()
                            controller.play()
                        }
                    }
                    ACTION_NEXT -> {
                        if (controller.hasNextMediaItem()) {
                            controller.seekToNext()
                            controller.prepare()
                            controller.play()
                        }
                    }
                    ACTION_SHUFFLE -> {
                        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
                    }
                    ACTION_REPLAY -> {
                        controller.repeatMode = if (controller.repeatMode == Player.REPEAT_MODE_ONE) {
                            Player.REPEAT_MODE_OFF
                        } else {
                            Player.REPEAT_MODE_ONE
                        }
                    }
                    ACTION_LIKE -> {
                        // MediaController does not natively support like directly without custom session commands
                        // We will skip this or send a custom command if supported, for now skip for background widget clicks
                    }
                }
                
                // Redraw with current state from controller
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, MusicWidget::class.java)
                )
                if (widgetIds.isNotEmpty()) {
                    widgetIds.forEach { updateWidgetWithPlayer(context, appWidgetManager, it, controller, false) }
                }
                
                // Release controller
                MediaController.releaseFuture(controllerFuture)
                
            } catch (e: Exception) {
                // Ignore, unable to connect
            }
        }
    }

    private fun openApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressUpdater(context: Context) {
        if (isUpdating) return

        isUpdating = true
        runnable = Runnable {
            val playerConnection = PlayerConnection.instance
            val player = playerConnection?.player

            // Solo actualizar si hay mÃºsica reproduciÃ©ndose o pausada
            if (player != null && (player.isPlaying || player.playbackState == Player.STATE_READY)) {
                updateAllWidgets(context)
                handler.postDelayed(runnable, 1000)
            } else {
                // Si no hay mÃºsica, reducir la frecuencia de actualizaciÃ³n
                updateAllWidgets(context)
                handler.postDelayed(runnable, 5000)
            }
        }
        handler.post(runnable)
    }

    private fun stopProgressUpdater() {
        isUpdating = false
        handler.removeCallbacks(runnable)
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

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidget::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val playerConnection = PlayerConnection.instance
                if (playerConnection != null) {
                    widgetIds.forEach { updateWidgetWithPlayer(context, appWidgetManager, it, playerConnection.player, playerConnection.isCurrentSongLiked()) }
                } else {
                    // Try to fetch state via MediaController
                    CoroutineScope(Dispatchers.Main).launch {
                        val sessionToken = SessionToken(context, ComponentName(context, com.Chenkham.Echofy.playback.MusicService::class.java))
                        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                        try {
                            val controller = withContext(Dispatchers.IO) { controllerFuture.get() }
                            widgetIds.forEach { updateWidgetWithPlayer(context, appWidgetManager, it, controller, false) }
                            MediaController.releaseFuture(controllerFuture)
                        } catch (e: Exception) {
                            widgetIds.forEach { updateWidgetWithPlayer(context, appWidgetManager, it, null, false) }
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
            if (playerConnection != null) {
                updateWidgetWithPlayer(context, appWidgetManager, appWidgetId, playerConnection.player, playerConnection.isCurrentSongLiked())
            } else {
                updateWidgetWithPlayer(context, appWidgetManager, appWidgetId, null, false)
            }
        }

        private fun updateWidgetWithPlayer(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            player: Player?,
            isLiked: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)

            // Configurar Pending Intents primero para mejor respuesta
            setPendingIntents(context, views)

            player?.let { player ->
                // Informacíon de la canción
                val songTitle = player.mediaMetadata.title?.toString()
                    ?: context.getString(R.string.song_title)
                val artist = player.mediaMetadata.artist?.toString()
                    ?: context.getString(R.string.artist_name)

                views.setTextViewText(R.id.widget_song_title, songTitle)
                views.setTextViewText(R.id.widget_artist, artist)

                // Controles
                val playPauseIcon = if (player.isPlaying) R.drawable.pause else R.drawable.play
                views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

                val shuffleIcon = if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                views.setImageViewResource(R.id.widget_shuffle, shuffleIcon)

                val likeIcon = if (isLiked)
                    R.drawable.heart_fill else R.drawable.heart
                views.setImageViewResource(R.id.widget_like, likeIcon)

                // Progress y tiempos
                val currentPos = player.currentPosition
                val duration = player.duration
                val currentTimeText = formatTime(currentPos)
                val durationText = formatTime(duration)

                views.setTextViewText(R.id.widget_current_time, currentTimeText)
                views.setTextViewText(R.id.widget_duration, durationText)

                // Progress bar - solo actualizar si la duraciÃ³n es vÃ¡lida
                val progress = if (duration > 0 && duration != Long.MAX_VALUE) {
                    (currentPos * 100 / duration).toInt()
                } else 0

                if (duration > 0 && duration != Long.MAX_VALUE) {
                    views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.VISIBLE)
                } else {
                    // Ocultar progress bar si no hay duraciÃ³n vÃ¡lida
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)
                }

                // Estado de reproducciÃ³n
                val playbackStateText = when {
                    player.repeatMode == Player.REPEAT_MODE_ONE -> context.getString(R.string.repeat_mode_one)
                    player.repeatMode == Player.REPEAT_MODE_ALL -> context.getString(R.string.repeat_mode_all)
                    else -> ""
                }

                if (playbackStateText.isNotEmpty()) {
                    views.setTextViewText(R.id.widget_playback_state, playbackStateText)
                    views.setViewVisibility(R.id.widget_playback_state, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_playback_state, android.view.View.GONE)
                }

                // Album art con manejo de errores mejorado y cache
                val thumbnailUrl = player.mediaMetadata.artworkUri?.toString()
                if (!thumbnailUrl.isNullOrEmpty()) {
                    // Verificar si ya tenemos una imagen cargada para evitar recargas frecuentes
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .size(160, 160) // Optimizado para el widget
                                .build()
                            val drawable = ImageLoader(context).execute(request).drawable
                            drawable?.let {
                                views.setImageViewBitmap(R.id.widget_album_art, it.toBitmap())
                                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                            }
                        } catch (e: Exception) {
                            // Fallback a imagen por defecto
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        }
                    }
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                }

                // Estado del widget cuando no hay mÃºsica
                if (player.mediaItemCount == 0) {
                    views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
                    views.setTextViewText(R.id.widget_artist, context.getString(R.string.tap_to_open))
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)
                }
            } ?: run {
                // Si no hay player, mostrar estado por defecto
                views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_artist, context.getString(R.string.tap_to_open))
                views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)
            }

            // Actualizar widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            val playPausePendingIntent = getBroadcastPendingIntent(context, ACTION_PLAY_PAUSE)
            val prevPendingIntent = getBroadcastPendingIntent(context, ACTION_PREV)
            val nextPendingIntent = getBroadcastPendingIntent(context, ACTION_NEXT)
            val shufflePendingIntent = getBroadcastPendingIntent(context, ACTION_SHUFFLE)
            val likePendingIntent = getBroadcastPendingIntent(context, ACTION_LIKE)
            val openAppPendingIntent = getBroadcastPendingIntent(context, ACTION_OPEN_APP)

            // Controles de reproducciÃ³n
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_shuffle, shufflePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_like, likePendingIntent)

            // Ãrea principal del widget para abrir la app
            views.setOnClickPendingIntent(R.id.widget_album_art, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_artist, openAppPendingIntent)

            // TambiÃ©n hacer que el Ã¡rea del progress bar abra la app
            views.setOnClickPendingIntent(R.id.widget_progress_bar, openAppPendingIntent)
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

        @SuppressLint("DefaultLocale")
        private fun formatTime(millis: Long): String {
            return if (millis < 0 || millis == Long.MAX_VALUE) "0:00" else String.format(
                "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            )
        }
    }
}