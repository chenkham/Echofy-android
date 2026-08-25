package com.Chenkham.tastedive.models

/**
 * A cross-domain recommendation. [type] is one of music, movie, show, book,
 * author, game or podcast.
 */
data class Recommendation(
    val name: String,
    val type: String,
    val teaser: String?,
    val wikipediaUrl: String?,
)
