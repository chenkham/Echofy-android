package com.Chenkham.bandsintown.models

/**
 * UI-ready concert entry, flattened from the raw Bandsintown event payload.
 */
data class Concert(
    val id: String,
    val artistName: String,
    val venueName: String,
    val city: String,
    val country: String,
    /** Raw ISO-8601 datetime as returned by the API. */
    val datetime: String,
    val ticketUrl: String,
    val lineup: List<String>,
) {
    val location: String
        get() = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
}
