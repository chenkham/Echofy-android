package com.Chenkham.Echofy.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.Chenkham.Echofy.MainActivity
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
        if (action == ACTION_UPDATE_LYRICS || action == "com.Chenkham.Echofy.ACTION_STATE_CHANGED" || action == "com.Chenkham.Echofy.ACTION_NEXT" || action == "com.Chenkham.Echofy.ACTION_PREV") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, LyricsWidget::class.java))
            onUpdate(context, appWidgetManager, ids)
        }
    }

    companion object {
        const val ACTION_UPDATE_LYRICS = "com.Chenkham.Echofy.ACTION_UPDATE_LYRICS"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_lyrics)
            val playerConnection = PlayerConnection.instance
            
            // Pending Intent to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            views.setOnClickPendingIntent(R.id.widget_lyrics_root, pendingIntent)
            
            // Refresh Intent
            val refreshIntent = Intent(context, LyricsWidget::class.java).apply {
                action = ACTION_UPDATE_LYRICS
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_lyrics_refresh, refreshPendingIntent)

            val player = playerConnection?.player
            if (player == null) {
                views.setTextViewText(R.id.widget_lyrics_title, "Echofy")
                views.setTextViewText(R.id.widget_lyrics_artist, "Tap to open")
                
                // Clear lyrics list
                LyricsWidgetCache.lines = listOf("No music playing")
                
                // Set adapter intent
                val serviceIntent = Intent(context, LyricsWidgetService::class.java)
                views.setRemoteAdapter(R.id.widget_lyrics_list, serviceIntent)
                
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_lyrics_list)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }

            val meta = player.mediaMetadata
            val title = meta.title?.toString() ?: "Unknown"
            val artist = meta.artist?.toString() ?: "Unknown"
            val duration = (player.duration / 1000).toInt()
            val mediaId = player.currentMediaItem?.mediaId ?: ""

            views.setTextViewText(R.id.widget_lyrics_title, title)
            views.setTextViewText(R.id.widget_lyrics_artist, artist)
            views.setTextViewText(R.id.widget_lyrics_status, "Loading lyrics...")
            views.setViewVisibility(R.id.widget_lyrics_status, android.view.View.VISIBLE)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // fetch lyrics
            CoroutineScope(Dispatchers.IO).launch {
                try {
                
                    // Set adapter intent
                    val serviceIntent = Intent(context, LyricsWidgetService::class.java)
                    views.setRemoteAdapter(R.id.widget_lyrics_list, serviceIntent)
                    views.setEmptyView(R.id.widget_lyrics_list, R.id.widget_lyrics_status)

                    // Try to get lyrics
                    val result = LrcLibLyricsProvider.getLyrics(mediaId, title, artist, duration)
                    val lyricsText = result.getOrNull()
                    
                    withContext(Dispatchers.Main) {
                        if (!lyricsText.isNullOrBlank()) {
                            // Split into lines and remove brackets
                            val lines = lyricsText.lines().map { line ->
                                line.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}]"), "").trim()
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
                        LyricsWidgetCache.lines = listOf("Error: ${e.message}")
                        views.setViewVisibility(R.id.widget_lyrics_status, android.view.View.GONE)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_lyrics_list)
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        Timber.e(e, "LyricsWidget Error")
                    }
                }
            }
        }
    }
}
