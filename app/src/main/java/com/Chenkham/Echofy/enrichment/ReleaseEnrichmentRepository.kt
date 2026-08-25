package com.Chenkham.Echofy.enrichment

import android.content.Context
import com.Chenkham.Echofy.constants.DiscogsEnabledKey
import com.Chenkham.Echofy.constants.DiscogsTokenKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.discogs.Discogs
import com.Chenkham.discogs.models.ReleaseInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Physical release details for an album: pressing formats, label, catalog
 * number and alternate cover art, none of which YouTube exposes.
 *
 * Gated by a user preference and a Discogs token, and returns null on any
 * failure so the album screen is never blocked.
 */
@Singleton
class ReleaseEnrichmentRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun findRelease(artist: String, album: String): ReleaseInfo? {
        if (artist.isBlank() || album.isBlank()) return null
        if (context.dataStore[DiscogsEnabledKey] != true) return null

        val token = context.dataStore[DiscogsTokenKey]?.takeIf { it.isNotBlank() }
            ?: return null

        return Discogs.findReleaseInfo(artist, album, token)
            .onFailure { Timber.d("Enrichment: Discogs miss for '$album' (${it.message})") }
            .getOrNull()
    }
}
