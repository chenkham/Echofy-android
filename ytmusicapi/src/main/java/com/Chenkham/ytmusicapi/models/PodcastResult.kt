package com.Chenkham.ytmusicapi.models

import kotlinx.serialization.Serializable

/**
 * Result from getPodcast() API call.
 * Based on ytmusicapi's get_podcast() response structure.
 */
@Serializable
data class PodcastResult(
    val author: Author? = null,
    val title: String,
    val description: String? = null,
    val saved: Boolean = false,
    val thumbnails: List<Thumbnail> = emptyList(),
    val episodes: List<Episode> = emptyList()
)

/**
 * Author/Channel information
 */
@Serializable
data class Author(
    val name: String,
    val id: String? = null
)

/**
 * A podcast episode
 */
@Serializable
data class Episode(
    val index: Int? = null,
    val title: String,
    val description: String? = null,
    val duration: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val videoType: String? = null,
    val date: String? = null,
    val thumbnails: List<Thumbnail> = emptyList()
)

/**
 * Result from getEpisode() API call.
 */
@Serializable
data class EpisodeResult(
    val author: Author? = null,
    val title: String,
    val date: String? = null,
    val duration: String? = null,
    val progressPercentage: Int? = null,
    val saved: Boolean = false,
    val playlistId: String? = null,
    val thumbnails: List<Thumbnail> = emptyList(),
    val description: String? = null
)

/**
 * Result from getChannel() API call.
 */
@Serializable
data class ChannelResult(
    val title: String,
    val thumbnails: List<Thumbnail> = emptyList(),
    val episodes: ChannelSection? = null,
    val podcasts: ChannelSection? = null
)

@Serializable
data class ChannelSection(
    val browseId: String? = null,
    val results: List<Episode> = emptyList(),
    val params: String? = null
)

@Serializable
data class PodcastLandingResult(
    val title: String = "Podcasts",
    val sections: List<PodcastLandingSection> = emptyList()
)

@Serializable
data class PodcastLandingSection(
    val title: String,
    val items: List<PodcastItem> = emptyList()
)


/**
 * A podcast item shown under "Podcasts" on a channel page
 */
@Serializable
data class PodcastItem(
    val title: String,
    val browseId: String,
    val podcastId: String? = null,
    val channel: Author? = null,
    val thumbnails: List<Thumbnail> = emptyList()
)
