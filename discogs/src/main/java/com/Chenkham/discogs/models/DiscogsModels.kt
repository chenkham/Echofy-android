package com.Chenkham.discogs.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscogsSearchResponse(
    val results: List<DiscogsSearchResult> = emptyList(),
)

@Serializable
data class DiscogsSearchResult(
    val id: Long = 0,
    val title: String = "",
    val type: String? = null,
    val year: String? = null,
    val country: String? = null,
    val format: List<String> = emptyList(),
    val label: List<String> = emptyList(),
    val genre: List<String> = emptyList(),
    val style: List<String> = emptyList(),
    val thumb: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
    @SerialName("resource_url") val resourceUrl: String? = null,
    @SerialName("master_id") val masterId: Long? = null,
)

@Serializable
data class DiscogsRelease(
    val id: Long = 0,
    val title: String = "",
    val year: Int? = null,
    val country: String? = null,
    val notes: String? = null,
    val genres: List<String> = emptyList(),
    val styles: List<String> = emptyList(),
    val images: List<DiscogsImage> = emptyList(),
    val labels: List<DiscogsLabel> = emptyList(),
    val formats: List<DiscogsFormat> = emptyList(),
    val tracklist: List<DiscogsTrack> = emptyList(),
    val uri: String? = null,
)

@Serializable
data class DiscogsImage(
    val type: String? = null,
    val uri: String? = null,
    @SerialName("uri150") val uri150: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class DiscogsLabel(
    val name: String? = null,
    val catno: String? = null,
)

@Serializable
data class DiscogsFormat(
    val name: String? = null,
    val qty: String? = null,
    val descriptions: List<String> = emptyList(),
)

@Serializable
data class DiscogsTrack(
    val position: String? = null,
    val title: String? = null,
    val duration: String? = null,
)
