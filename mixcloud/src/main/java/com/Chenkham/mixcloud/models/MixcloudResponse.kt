package com.Chenkham.mixcloud.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MixcloudResponse(
    val data: List<MixcloudCloudcast> = emptyList(),
)

@Serializable
data class MixcloudCloudcast(
    val key: String = "",
    val url: String = "",
    val name: String = "",
    @SerialName("audio_length") val audioLength: Int = 0,
    @SerialName("play_count") val playCount: Int? = null,
    @SerialName("favorite_count") val favoriteCount: Int? = null,
    @SerialName("created_time") val createdTime: String? = null,
    val user: MixcloudUser? = null,
    val pictures: MixcloudPictures? = null,
    val tags: List<MixcloudTag> = emptyList(),
)

@Serializable
data class MixcloudUser(
    val username: String = "",
    val name: String = "",
    val url: String = "",
)

@Serializable
data class MixcloudPictures(
    val medium: String? = null,
    val large: String? = null,
    @SerialName("extra_large") val extraLarge: String? = null,
    @SerialName("640wx640h") val square640: String? = null,
)

@Serializable
data class MixcloudTag(
    val key: String = "",
    val name: String = "",
    val url: String = "",
)
