package com.Chenkham.musicbrainz.models

/**
 * Flattened, UI-ready view of a MusicBrainz artist. Keeping the network models
 * separate from this means the app module never depends on MusicBrainz JSON shapes.
 */
data class ArtistInfo(
    val mbid: String,
    val name: String,
    /** Short qualifier MusicBrainz uses to tell same-named artists apart. */
    val disambiguation: String?,
    val country: String?,
    /** "Person", "Group", "Orchestra", etc. */
    val type: String?,
    val beginDate: String?,
    val endDate: String?,
    val isEnded: Boolean,
    /** Community tags ordered by vote count, most agreed-upon first. */
    val genres: List<String>,
    val officialHomepage: String?,
    val wikipediaUrl: String?,
)
