package com.Chenkham.kugou.models

import kotlinx.serialization.Serializable

// Defaults everywhere for the same reason as SearchLyricsResponse: a missing "data" or a
// KuGou error envelope must not throw MissingFieldException out of a coroutine.
@Serializable
data class SearchSongResponse(
    val status: Int = 0,
    val errcode: Int = 0,
    val error: String = "",
    val data: Data = Data(),
) {
    @Serializable
    data class Data(
        val info: List<Info> = emptyList(),
    ) {
        @Serializable
        data class Info(
            val duration: Int = 0,
            val hash: String = "",
            val songname: String = "",
            val singername: String = "",
        )
    }
}
