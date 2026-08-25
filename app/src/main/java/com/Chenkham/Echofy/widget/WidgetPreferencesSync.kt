package com.Chenkham.Echofy.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.Chenkham.Echofy.CompactMusicWidget
import com.Chenkham.Echofy.MusicWidget
import com.Chenkham.Echofy.ui.widgets.LyricsWidget
import timber.log.Timber

object WidgetPreferencesSync {
    suspend fun notifyChanged(context: Context) {
        val appContext = context.applicationContext ?: context

        // 1. Update Glance widget
        runCatching {
            val manager = GlanceAppWidgetManager(appContext)
            val glanceIds = manager.getGlanceIds(EchofyPlayerWidget::class.java)
            val widget = EchofyPlayerWidget()
            glanceIds.forEach { glanceId ->
                widget.update(appContext, glanceId)
            }
        }.onFailure {
            Timber.tag("WidgetPreferencesSync")
                .w(it, "Unable to refresh Glance widget after preference change")
        }

        // 2. Update Standard App Widgets
        runCatching {
            MusicWidget.updateAllWidgets(appContext)
            CompactMusicWidget.updateAllWidgets(appContext)
            val lyricsIntent = Intent(appContext, LyricsWidget::class.java).apply {
                action = LyricsWidget.ACTION_UPDATE_LYRICS
            }
            appContext.sendBroadcast(lyricsIntent)
        }.onFailure {
            Timber.tag("WidgetPreferencesSync")
                .w(it, "Unable to refresh standard app widgets after preference change")
        }
    }
}
