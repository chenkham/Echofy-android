package com.Chenkham.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    // Defaulted so an error response without "content" yields empty lyrics rather than
    // throwing MissingFieldException out of the coroutine.
    val content: String = "",
)
