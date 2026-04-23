package com.Chenkham.Echofy.utils

fun parsePlaylistSongCount(songCountText: String?): Int? {
    if (songCountText.isNullOrBlank()) return null

    val digitsOnly = buildString(songCountText.length) {
        songCountText.forEach { char ->
            val digit = char.digitToIntOrNull() ?: return@forEach
            append(digit)
        }
    }

    return digitsOnly.toIntOrNull()
}
