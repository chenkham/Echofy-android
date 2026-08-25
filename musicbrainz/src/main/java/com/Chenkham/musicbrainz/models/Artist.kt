package com.Chenkham.musicbrainz.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtistSearchResponse(
    val artists: List<MusicBrainzArtist> = emptyList(),
)

@Serializable
data class MusicBrainzArtist(
    val id: String,
    val name: String,
    val disambiguation: String? = null,
    val country: String? = null,
    val type: String? = null,
    @SerialName("life-span") val lifeSpan: LifeSpan? = null,
    val tags: List<Tag> = emptyList(),
    val relations: List<Relation> = emptyList(),
)

@Serializable
data class LifeSpan(
    val begin: String? = null,
    val end: String? = null,
    val ended: Boolean? = null,
)

@Serializable
data class Tag(
    val name: String,
    val count: Int = 0,
)

@Serializable
data class Relation(
    val type: String,
    val url: RelationUrl? = null,
)

@Serializable
data class RelationUrl(
    val resource: String,
)
