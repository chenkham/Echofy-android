package com.Chenkham.tastedive.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TasteDiveResponse(
    @SerialName("Similar") val similar: TasteDiveSimilar? = null,
)

@Serializable
data class TasteDiveSimilar(
    @SerialName("Info") val info: List<TasteDiveItem> = emptyList(),
    @SerialName("Results") val results: List<TasteDiveItem> = emptyList(),
)

@Serializable
data class TasteDiveItem(
    @SerialName("Name") val name: String = "",
    @SerialName("Type") val type: String = "",
    @SerialName("wTeaser") val teaser: String? = null,
    @SerialName("wUrl") val wikipediaUrl: String? = null,
)
