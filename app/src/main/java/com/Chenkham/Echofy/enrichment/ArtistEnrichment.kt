package com.Chenkham.Echofy.enrichment

import com.Chenkham.bandsintown.models.Concert
import com.Chenkham.musicbrainz.models.ArtistInfo
import com.Chenkham.tastedive.models.Recommendation
import com.Chenkham.theaudiodb.models.AudioDbArtistInfo

/**
 * Everything the artist screen knows about an artist beyond what YouTube returns.
 *
 * Each section is independent and nullable, so one API being down or unconfigured
 * never blocks the others from rendering.
 */
data class ArtistEnrichment(
    /** Structured metadata: genres, origin, active years, official links. */
    val info: ArtistInfo? = null,
    /** Biography and high-resolution artwork. */
    val bio: AudioDbArtistInfo? = null,
    /** Upcoming tour dates. */
    val concerts: List<Concert> = emptyList(),
    /** Artists with a similar sound. */
    val similarArtists: List<Recommendation> = emptyList(),
) {
    val isEmpty: Boolean
        get() = info == null &&
            bio == null &&
            concerts.isEmpty() &&
            similarArtists.isEmpty()
}
