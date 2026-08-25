package com.Chenkham.theaudiodb.models

/**
 * UI-ready artist extras from TheAudioDB. Complements MusicBrainz, which has
 * structured tags and dates but no biography or artwork.
 */
data class AudioDbArtistInfo(
    val id: String?,
    val name: String?,
    val biography: String?,
    val thumbUrl: String?,
    val fanartUrls: List<String>,
    val bannerUrl: String?,
    val logoUrl: String?,
    val genre: String?,
    val style: String?,
    val mood: String?,
    val formedYear: String?,
    val country: String?,
    val website: String?,
)
