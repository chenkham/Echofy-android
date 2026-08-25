package com.Chenkham.genius.models

/**
 * A Genius search match, reduced to what the lyrics provider needs.
 */
data class GeniusSongMatch(
    val id: Long,
    val title: String,
    val artistName: String,
    val url: String,
    val artworkUrl: String?,
)
