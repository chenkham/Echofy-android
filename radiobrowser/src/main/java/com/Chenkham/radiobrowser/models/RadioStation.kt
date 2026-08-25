package com.Chenkham.radiobrowser.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioStation(
    val stationuuid: String = "",
    val name: String = "",
    val url: String = "",
    // Defaulted to null because radio-browser omits url_resolved for some stations. As a
    // non-nullable field that made the whole station list fail to deserialize, and callers
    // already null-check it.
    @SerialName("url_resolved") val urlResolved: String? = null,
    val homepage: String? = null,
    val favicon: String? = null,
    val tags: String = "",
    val country: String = "",
    val countrycode: String = "",
    val state: String = "",
    val language: String = "",
    val languagecodes: String = "",
    val votes: Int = 0,
    val codec: String = "",
    val bitrate: Int = 0,
    @SerialName("lastcheckok") val lastCheckOk: Int = 0,
    @SerialName("lastchecktime") val lastCheckTime: String = "",
    @SerialName("clickcount") val clickCount: Int = 0,
    @SerialName("clicktrend") val clickTrend: Int = 0,
)
