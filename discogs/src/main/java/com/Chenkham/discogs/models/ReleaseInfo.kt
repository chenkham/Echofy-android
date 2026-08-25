package com.Chenkham.discogs.models

/**
 * UI-ready release details: physical formats, label, and alternate cover art
 * that YouTube thumbnails cannot provide.
 */
data class ReleaseInfo(
    val id: Long,
    val title: String,
    val year: Int?,
    val country: String?,
    val genres: List<String>,
    val styles: List<String>,
    val labelName: String?,
    val catalogNumber: String?,
    val formats: List<String>,
    val coverImages: List<String>,
    val tracklist: List<ReleaseTrack>,
    val discogsUrl: String?,
)

data class ReleaseTrack(
    val position: String,
    val title: String,
    val duration: String,
)
