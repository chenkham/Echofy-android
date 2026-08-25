package com.Chenkham.Echofy.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.DynamicShortcutsEnabledKey
import com.Chenkham.Echofy.db.MusicDatabase
import kotlinx.coroutines.flow.first

/**
 * Publishes long-press launcher shortcuts for the playlists the user actually reaches for.
 *
 * The shortcuts are opt-in via [DynamicShortcutsEnabledKey]; when the preference is turned
 * off any previously published shortcuts are removed so the launcher goes back to the
 * static set declared in `shortcuts.xml`.
 */
object DynamicShortcuts {
    const val ACTION_PLAY_PLAYLIST = "com.Chenkham.Echofy.action.PLAY_PLAYLIST"
    const val EXTRA_PLAYLIST_ID = "playlistId"

    private const val ID_LIKED = "dynamic_liked"
    private const val ID_DOWNLOADED = "dynamic_downloaded"

    /**
     * Rebuilds the dynamic shortcut list. Safe to call often — it is a no-op beyond a
     * single preference read when the feature is disabled.
     */
    suspend fun refresh(
        context: Context,
        database: MusicDatabase,
    ) {
        val enabled = context.dataStore.data.first()[DynamicShortcutsEnabledKey] ?: false
        if (!enabled) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }

        val shortcuts = mutableListOf<ShortcutInfoCompat>()

        if (database.likedSongsCount().first() > 0) {
            shortcuts += buildShortcut(
                context = context,
                id = ID_LIKED,
                label = context.getString(R.string.liked_songs),
                iconRes = R.drawable.favorite,
                playlistId = com.Chenkham.Echofy.db.entities.PlaylistEntity.LIKED_PLAYLIST_ID,
            )
        }

        shortcuts += buildShortcut(
            context = context,
            id = ID_DOWNLOADED,
            label = context.getString(R.string.downloaded_songs),
            iconRes = R.drawable.offline,
            playlistId = com.Chenkham.Echofy.db.entities.PlaylistEntity.DOWNLOADED_PLAYLIST_ID,
        )

        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    private fun buildShortcut(
        context: Context,
        id: String,
        label: String,
        iconRes: Int,
        playlistId: String,
    ): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, iconRes))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_PLAY_PLAYLIST
                    putExtra(EXTRA_PLAYLIST_ID, playlistId)
                },
            )
            .build()
}
