package com.Chenkham.Echofy.models

import com.Chenkham.innertube.models.YTItem
import com.Chenkham.Echofy.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
