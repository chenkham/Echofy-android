package com.Chenkham.Echofy.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.MusicWidget
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.lyrics.LrcLibLyricsProvider
import com.Chenkham.Echofy.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class LyricsWidget : AppWidgetProvider() {

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
        val action = intent.action
        if (action == ACTION_UPDATE_LYRICS ||
            action == "com.Chenkham.Echofy.ACTION_STATE_CHANGED" ||
            action == "com.Chenkham.Echofy.ACTION_NEXT" ||
            action == "com.Chenkham.Echofy.ACTION_PREV" ||
            action == MusicWidget.ACTION_PLAY_PAUSE
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
            onUpdate(context, appWidgetManager, ids)
        }
    }

    companion object {
        const val ACTION_UPDATE_LYRICS = "com.Chenkham.Echofy.ACTION_UPDATE_LYRICS"

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

        private fun getOpenAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getActivity(context, 0, intent, flags)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_lyrics)
            val playerConnection = PlayerConnection.instance

            val bgMode = com.Chenkham.Echofy.widget.WidgetPreferences.cachedBackgroundMode
            val scrim = com.Chenkham.Echofy.widget.WidgetPreferences.cachedScrimOpacity
            when (bgMode) {
                com.Chenkham.Echofy.constants.WidgetBackgroundMode.DOMINANT_COLOR -> {
                    views.setInt(R.id.widget_lyrics_root, "setBackgroundColor", android.graphics.Color.argb((scrim * 255).toInt(), 30, 18, 10))
                }
                com.Chenkham.Echofy.constants.WidgetBackgroundMode.SOLID -> {
                    views.setInt(R.id.widget_lyrics_root, "setBackgroundColor", android.graphics.Color.argb(((0.8f + scrim * 0.2f) * 255).toInt(), 20, 12, 6))
                }
                else -> {
                    views.setInt(R.id.widget_lyrics_root, "setBackgroundResource", R.drawable.widget_lyrics_bg)
                }
            }

            // Click interactions
            val openAppPendingIntent = getOpenAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_lyrics_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_lyrics_art, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_lyrics_logo, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_lyrics_edit, openAppPendingIntent)

            // Play / Pause action
            val playPausePendingIntent = getBroadcastPendingIntent(context, MusicWidget.ACTION_PLAY_PAUSE)
            views.setOnClickPendingIntent(R.id.widget_lyrics_play_pause, playPausePendingIntent)

            // Refresh / Output action
            val refreshIntent = Intent(context, LyricsWidget::class.java).apply {
                action = ACTION_UPDATE_LYRICS
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 1001, refreshIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_lyrics_output, refreshPendingIntent)

            val player = playerConnection?.player
            if (player == null) {
                views.setTextViewText(R.id.widget_lyrics_title, "Echofy")
                views.setTextViewText(R.id.widget_lyrics_artist, "Tap to open app")
                views.setTextViewText(R.id.widget_lyrics_play_pause, "▶ Play")
                views.setImageViewResource(R.id.widget_lyrics_art, R.drawable.previewalbum)

                // Clear lyrics list
                LyricsWidgetCache.lines = listOf("No music playing")

                val serviceIntent = Intent(context, LyricsWidgetService::class.java)
                views.setRemoteAdapter(R.id.widget_lyrics_list, serviceIntent)

                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_lyrics_list)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val meta = player.mediaMetadata
            val title = meta.title?.toString() ?: "Unknown Track"
            val artist = meta.artist?.toString() ?: "Unknown Artist"
            val duration = (player.duration / 1000).toInt()
            val mediaId = player.currentMediaItem?.mediaId ?: ""
            val isPlaying = player.isPlaying

            views.setTextViewText(R.id.widget_lyrics_title, title)
            views.setTextViewText(R.id.widget_lyrics_artist, artist)
            views.setTextViewText(
                R.id.widget_lyrics_play_pause,
                if (isPlaying) "❚❚ Pause" else "▶ Play"
            )

            // Load album art
            val artworkUri = meta.artworkUri
            if (artworkUri != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val loader = ImageLoader(context)
                        val req = ImageRequest.Builder(context)
                            .data(artworkUri)
                            .allowHardware(false)
                            .build()
                        val result = loader.execute(req)
                        val bitmap = result.drawable?.toBitmap()
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                views.setImageViewBitmap(R.id.widget_lyrics_art, bitmap)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                views.setImageViewResource(R.id.widget_lyrics_art, R.drawable.previewalbum)
            }

            views.setTextViewText(R.id.widget_lyrics_status, "Loading lyrics...")
            views.setViewVisibility(R.id.widget_lyrics_status, android.view.View.VISIBLE)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Fetch lyrics
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val serviceIntent = Intent(context, LyricsWidgetService::class.java)
                    views.setRemoteAdapter(R.id.widget_lyrics_list, serviceIntent)
                    views.setEmptyView(R.id.widget_lyrics_list, R.id.widget_lyrics_status)

                    val result = LrcLibLyricsProvider.getLyrics(mediaId, title, artist, duration)
                    val lyricsText = result.getOrNull()

                    withContext(Dispatchers.Main) {
                        if (!lyricsText.isNullOrBlank()) {
                            val lines = lyricsText.lines().map { line ->
                                line.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]"), "").trim()
                            }.filter { it.isNotBlank() }

                            LyricsWidgetCache.lines = lines
                            views.setViewVisibility(R.id.widget_lyrics_status, android.view.View.GONE)
                        } else {
                            LyricsWidgetCache.lines = listOf("Lyrics not found")
                            views.setViewVisibility(R.id.widget_lyrics_status, android.view.View.GONE)
                        }

                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_lyrics_list)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        LyricsWidgetCache.lines = listOf("Error loading lyrics")
                        views.setViewVisibility(R.id.widget_lyrics_status, android.view.View.GONE)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_lyrics_list)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }
}
