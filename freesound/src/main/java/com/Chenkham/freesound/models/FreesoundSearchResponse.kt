package com.Chenkham.freesound.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FreesoundSearchResponse(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<FreesoundSound> = emptyList(),
)

@Serializable
data class FreesoundSound(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val username: String = "",
    val duration: Double = 0.0,
    val license: String? = null,
    val tags: List<String> = emptyList(),
    val previews: FreesoundPreviews? = null,
    val images: FreesoundImages? = null,
)

@Serializable
data class FreesoundPreviews(
    @SerialName("preview-hq-mp3") val previewHqMp3: String? = null,
    @SerialName("preview-lq-mp3") val previewLqMp3: String? = null,
    @SerialName("preview-hq-ogg") val previewHqOgg: String? = null,
    @SerialName("preview-lq-ogg") val previewLqOgg: String? = null,
)

@Serializable
data class FreesoundImages(
    @SerialName("waveform_m") val waveformM: String? = null,
    @SerialName("spectral_m") val spectralM: String? = null,
)
