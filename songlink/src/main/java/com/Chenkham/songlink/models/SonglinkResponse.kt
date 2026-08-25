package com.Chenkham.songlink.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SonglinkResponse(
    val entityUniqueId: String = "",
    val userCountry: String = "",
    val pageUrl: String = "",
    val entitiesByUniqueId: Map<String, SonglinkEntity> = emptyMap(),
    val linksByPlatform: Map<String, SonglinkPlatformLink> = emptyMap(),
)

@Serializable
data class SonglinkEntity(
    val id: String = "",
    val type: String? = null,
    val title: String? = null,
    val artistName: String? = null,
    val thumbnailUrl: String? = null,
    @SerialName("apiProvider") val apiProvider: String? = null,
)

@Serializable
data class SonglinkPlatformLink(
    val url: String = "",
    val entityUniqueId: String = "",
    val nativeAppUriMobile: String? = null,
)
