package com.Chenkham.innertube.pages

import com.Chenkham.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
