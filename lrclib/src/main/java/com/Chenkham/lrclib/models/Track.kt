package com.Chenkham.lrclib.models

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class Track(
    val id: Int = 0,
    val trackName: String = "",
    val artistName: String = "",
    // lrclib returns null here for some entries. Declaring it non-nullable made kotlinx
    // serialization throw mid-response, and because the search runs on a coroutine
    // dispatcher without a catch the whole app died with "Failed to parse type 'double'
    // for input 'null'".
    val duration: Double? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

internal fun List<Track>.bestMatchingFor(duration: Int) =
    firstOrNull { track -> track.duration?.let { abs(it.toInt() - duration) <= 5 } == true }
