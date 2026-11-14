package com.Chenkham.Echofy.ui.player

import com.Chenkham.Echofy.models.MediaMetadata

fun isVideoAvailableFor(
    mediaMetadata: MediaMetadata?,
    fallbackId: String?,
): Boolean {
    val mediaId = mediaMetadata?.id ?: fallbackId.orEmpty()
    if (mediaId.isBlank() || mediaId.startsWith("LA-") || mediaId.startsWith("local:") || mediaId.startsWith("radio:") || mediaId.startsWith("ambient:")) return false
    val thumbnailUrl = mediaMetadata?.thumbnailUrl.orEmpty()
    return thumbnailUrl.contains("i.ytimg.com", ignoreCase = true) ||
        thumbnailUrl.contains("ytimg.com/vi", ignoreCase = true) ||
        thumbnailUrl.contains("vi_webp", ignoreCase = true)
}
