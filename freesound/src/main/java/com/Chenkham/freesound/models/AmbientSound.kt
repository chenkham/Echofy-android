package com.Chenkham.freesound.models

/**
 * UI-ready ambient sound. [streamUrl] points at a Freesound preview, which is
 * directly playable by ExoPlayer without authentication.
 */
data class AmbientSound(
    val id: Long,
    val name: String,
    val description: String?,
    val author: String,
    val durationSeconds: Double,
    val license: String?,
    val tags: List<String>,
    val streamUrl: String,
    val waveformUrl: String?,
)
