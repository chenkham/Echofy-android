package com.Chenkham.songlink.models

/**
 * UI-ready flattened result: one song, and where it can be played.
 */
data class SongLinks(
    val pageUrl: String,
    val title: String?,
    val artistName: String?,
    val thumbnailUrl: String?,
    val platforms: List<PlatformLink>,
)

data class PlatformLink(
    val platform: String,
    val displayName: String,
    val url: String,
)
