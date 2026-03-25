package com.Chenkham.ytmusicapi.models

import kotlinx.serialization.Serializable

/**
 * Result from getCharts() API call.
 * Based on ytmusicapi's get_charts() response structure.
 */
@Serializable
data class ChartsResult(
    val countries: CountryInfo = CountryInfo(),
    val videos: List<ChartPlaylist> = emptyList(),
    val daily: List<ChartPlaylist> = emptyList(),
    val weekly: List<ChartPlaylist> = emptyList(),
    val artists: List<ChartArtist> = emptyList(),
    val genres: List<ChartPlaylist> = emptyList(),
    val sections: List<ChartSection> = emptyList() // Flexible sections
)

@Serializable
data class ChartSection(
    val title: String,
    val items: List<ChartItem> = emptyList()
)

@Serializable
data class ChartItem(
    val title: String,
    val browseId: String,
    val playlistId: String? = null,
    val thumbnails: List<Thumbnail> = emptyList(),
    val subscribers: String? = null,
    val rank: String? = null,
    val trend: String? = null
)

@Serializable
data class CountryInfo(
    val selected: String? = null,
    val options: List<String> = emptyList()
)

/**
 * A playlist item in charts (e.g., "Daily Top Music Videos")
 */
@Serializable
data class ChartPlaylist(
    val title: String,
    val playlistId: String,
    val thumbnails: List<Thumbnail> = emptyList()
)

/**
 * An artist item in charts with rank and trend
 */
@Serializable
data class ChartArtist(
    val title: String,
    val browseId: String,
    val subscribers: String? = null,
    val thumbnails: List<Thumbnail> = emptyList(),
    val rank: String? = null,
    val trend: String? = null  // "up", "down", or "neutral"
)

/**
 * Common thumbnail model
 */
@Serializable
data class Thumbnail(
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)
