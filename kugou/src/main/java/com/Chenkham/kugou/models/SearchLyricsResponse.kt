package com.Chenkham.kugou.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Every field carries a default so a partial or error-shaped response degrades into "no
// lyrics" instead of throwing out of the coroutine and killing the process, which is what
// the equivalent lrclib model used to do.
@Serializable
data class SearchLyricsResponse(
    val status: Int = 0,
    val info: String = "",
    val errcode: Int = 0,
    val errmsg: String = "",
    val expire: Int = 0,
    val candidates: List<Candidate> = emptyList(),
) {
    @Serializable
    data class Candidate(
        val id: Long = 0,
        @SerialName("product_from")
        val productFrom: String = "",
        val duration: Long = 0,
        val accesskey: String = "",
    )
}
