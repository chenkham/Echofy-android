package com.Chenkham.mixcloud.models

/**
 * UI-ready DJ mix or radio show.
 *
 * Mixcloud does not expose direct audio stream URLs through its public API, so
 * [webUrl] opens the show on Mixcloud rather than playing in-app.
 */
data class DjMix(
    val key: String,
    val title: String,
    val artistName: String,
    val durationSeconds: Int,
    val thumbnailUrl: String?,
    val playCount: Int?,
    val tags: List<String>,
    val webUrl: String,
)
