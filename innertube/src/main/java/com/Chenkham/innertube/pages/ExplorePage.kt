package com.Chenkham.innertube.pages

import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.SongItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
    val trendingSongs: List<SongItem> = emptyList(),
)
