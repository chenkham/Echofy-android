package com.Chenkham.Echofy.enrichment

import android.content.Context
import com.Chenkham.Echofy.constants.ArtistBioEnabledKey
import com.Chenkham.Echofy.constants.ArtistInfoEnabledKey
import com.Chenkham.Echofy.constants.BandsintownAppIdKey
import com.Chenkham.Echofy.constants.ConcertsEnabledKey
import com.Chenkham.Echofy.constants.SimilarArtistsEnabledKey
import com.Chenkham.Echofy.constants.TasteDiveApiKeyKey
import com.Chenkham.Echofy.constants.TheAudioDbApiKeyKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import com.Chenkham.bandsintown.Bandsintown
import com.Chenkham.bandsintown.models.Concert
import com.Chenkham.musicbrainz.MusicBrainz
import com.Chenkham.tastedive.TasteDive
import com.Chenkham.tastedive.models.Recommendation
import com.Chenkham.theaudiodb.TheAudioDb
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for artist metadata from external catalogues.
 *
 * All four sources are fetched concurrently and merged into one
 * [ArtistEnrichment]. Every source is individually gated by a user preference
 * and degrades to null/empty on failure, so this never throws and never blocks
 * the artist screen.
 */
@Singleton
class ArtistEnrichmentRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Fetch every enabled source for [artistName] in parallel.
     */
    suspend fun enrich(artistName: String): ArtistEnrichment = coroutineScope {
        if (artistName.isBlank()) return@coroutineScope ArtistEnrichment()

        val infoTask = async { if (isInfoEnabled()) fetchInfo(artistName) else null }
        val bioTask = async { if (isBioEnabled()) fetchBio(artistName) else null }
        val concertsTask = async { fetchConcerts(artistName) }
        val similarTask = async { fetchSimilar(artistName) }

        ArtistEnrichment(
            info = infoTask.await(),
            bio = bioTask.await(),
            concerts = concertsTask.await(),
            similarArtists = similarTask.await(),
        )
    }

    private fun isInfoEnabled() = context.dataStore[ArtistInfoEnabledKey] ?: true

    private fun isBioEnabled() = context.dataStore[ArtistBioEnabledKey] ?: true

    private suspend fun fetchInfo(artistName: String) =
        MusicBrainz.searchArtist(artistName, limit = 1)
            .mapCatching { matches ->
                val mbid = matches.firstOrNull()?.id
                    ?: error("No MusicBrainz match for \"$artistName\"")
                MusicBrainz.getArtist(mbid).getOrThrow()
            }.onFailure { Timber.d("Enrichment: MusicBrainz miss for '$artistName' (${it.message})") }
            .getOrNull()

    private suspend fun fetchBio(artistName: String) =
        TheAudioDb.getArtist(artistName, context.dataStore[TheAudioDbApiKeyKey])
            .onFailure { Timber.d("Enrichment: TheAudioDB miss for '$artistName' (${it.message})") }
            .getOrNull()

    private suspend fun fetchConcerts(artistName: String): List<Concert> {
        if (context.dataStore[ConcertsEnabledKey] != true) return emptyList()
        val appId = context.dataStore[BandsintownAppIdKey]?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        return Bandsintown.getUpcomingEvents(artistName, appId)
            .onFailure { Timber.d("Enrichment: Bandsintown miss for '$artistName' (${it.message})") }
            .getOrDefault(emptyList())
    }

    private suspend fun fetchSimilar(artistName: String): List<Recommendation> {
        if (context.dataStore[SimilarArtistsEnabledKey] != true) return emptyList()
        val apiKey = context.dataStore[TasteDiveApiKeyKey]?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        return TasteDive.getSimilarArtists(artistName, apiKey)
            .onFailure { Timber.d("Enrichment: TasteDive miss for '$artistName' (${it.message})") }
            .getOrDefault(emptyList())
    }
}
