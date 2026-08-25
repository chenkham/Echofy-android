package com.Chenkham.Echofy.models

import com.arturo254.opentune.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
