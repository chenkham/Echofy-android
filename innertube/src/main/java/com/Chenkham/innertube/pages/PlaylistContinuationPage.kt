package com.Chenkham.innertube.pages

import com.Chenkham.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
