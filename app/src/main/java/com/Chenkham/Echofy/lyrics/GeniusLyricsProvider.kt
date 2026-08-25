package com.Chenkham.Echofy.lyrics

import android.content.Context
import com.Chenkham.Echofy.constants.EnableGeniusKey
import com.Chenkham.Echofy.constants.GeniusAccessTokenKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.genius.Genius
import timber.log.Timber

/**
 * Genius lyrics provider.
 *
 * Genius only ever returns plain text with no timestamps, so this sits last in
 * the chain: it is a coverage fallback for songs the timed providers miss, not
 * a competitor to them. Requires a user-supplied access token, so it stays
 * disabled until one is entered.
 */
object GeniusLyricsProvider : LyricsProvider {
    override val name = "Genius"

    /**
     * The [LyricsProvider] interface hands a Context to [isEnabled] but not to
     * [getLyrics], so the token is captured here. LyricsHelper always filters
     * on isEnabled before calling getLyrics, so this is populated in time.
     */
    @Volatile
    private var cachedToken: String? = null

    override fun isEnabled(context: Context): Boolean {
        val enabled = context.dataStore[EnableGeniusKey] ?: false
        val token = context.dataStore[GeniusAccessTokenKey]?.takeIf { it.isNotBlank() }
        cachedToken = token
        return enabled && token != null
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        val token = cachedToken
            ?: return Result.failure(IllegalStateException("Genius access token not configured"))

        Timber.d("Genius: searching for '$title' by '$artist'")

        return Genius.getLyrics(title, artist, token)
            .onFailure { Timber.d("Genius: no lyrics for '$title' (${it.message})") }
    }
}
