package com.Chenkham.genius.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeniusSearchResponse(
    val response: GeniusSearchPayload? = null,
)

@Serializable
data class GeniusSearchPayload(
    val hits: List<GeniusHit> = emptyList(),
)

@Serializable
data class GeniusHit(
    val type: String? = null,
    val result: GeniusSong? = null,
)

@Serializable
data class GeniusSong(
    val id: Long = 0,
    val title: String = "",
    @SerialName("full_title") val fullTitle: String? = null,
    @SerialName("primary_artist") val primaryArtist: GeniusArtist? = null,
    val url: String = "",
    @SerialName("song_art_image_url") val songArtImageUrl: String? = null,
)

@Serializable
data class GeniusArtist(
    val id: Long = 0,
    val name: String = "",
)
