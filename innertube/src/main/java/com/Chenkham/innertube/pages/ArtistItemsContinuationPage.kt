package com.Chenkham.innertube.pages

import com.Chenkham.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
