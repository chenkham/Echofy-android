package com.Chenkham.Echofy.lyrics

import android.content.Context
import com.Chenkham.lrclib.LrcLib
import com.Chenkham.Echofy.constants.EnableLrcLibKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = LrcLib.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        LrcLib.getAllLyrics(title, artist, duration, null, callback)
    }
}
