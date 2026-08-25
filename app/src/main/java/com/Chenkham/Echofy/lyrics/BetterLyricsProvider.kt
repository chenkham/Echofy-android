package com.Chenkham.Echofy.lyrics

import android.content.Context
import android.util.Log
import com.arturo254.opentune.betterlyrics.BetterLyrics
import com.Chenkham.Echofy.constants.EnableBetterLyricsKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import timber.log.Timber

object BetterLyricsProvider : LyricsProvider {
    init {
        BetterLyrics.logger = { message ->
            Timber.tag("BetterLyrics").i(message)
        }
    }

    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title = title, artist = artist, album = null, durationSeconds = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(
            title = title,
            artist = artist,
            album = null,
            durationSeconds = duration,
            callback = callback,
        )
    }
}
