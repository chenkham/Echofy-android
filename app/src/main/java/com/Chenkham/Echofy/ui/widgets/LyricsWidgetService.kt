package com.Chenkham.Echofy.ui.widgets

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.Chenkham.Echofy.R

class LyricsWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return LyricsRemoteViewsFactory(this.applicationContext)
    }
}

class LyricsRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    
    override fun onCreate() {
        // No-op
    }

    override fun onDataSetChanged() {
        // Data is already updated in LyricsWidgetCache
    }

    override fun onDestroy() {
        // No-op
    }

    override fun getCount(): Int {
        return LyricsWidgetCache.lines.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_lyrics_item)
        if (position < LyricsWidgetCache.lines.size) {
            views.setTextViewText(R.id.widget_lyrics_line, LyricsWidgetCache.lines[position])
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }
}
