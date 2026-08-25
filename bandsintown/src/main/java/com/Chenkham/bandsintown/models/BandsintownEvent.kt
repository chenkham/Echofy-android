package com.Chenkham.bandsintown.models

import kotlinx.serialization.Serializable

@Serializable
data class BandsintownEvent(
    val id: String = "",
    val url: String = "",
    val datetime: String = "",
    val title: String? = null,
    val description: String? = null,
    val venue: BandsintownVenue? = null,
    val lineup: List<String> = emptyList(),
    val offers: List<BandsintownOffer> = emptyList(),
)

@Serializable
data class BandsintownVenue(
    val name: String? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
)

@Serializable
data class BandsintownOffer(
    val type: String? = null,
    val url: String? = null,
    val status: String? = null,
)
