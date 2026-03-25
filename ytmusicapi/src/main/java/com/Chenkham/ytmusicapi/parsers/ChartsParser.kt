package com.Chenkham.ytmusicapi.parsers

import com.Chenkham.ytmusicapi.models.ChartArtist
import com.Chenkham.ytmusicapi.models.ChartPlaylist
import com.Chenkham.ytmusicapi.models.Thumbnail
import com.Chenkham.ytmusicapi.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser functions for charts data.
 * Based on ytmusicapi's parsers/explore.py
 */
object ChartsParser {

    /**
     * Parse a chart playlist from musicTwoRowItemRenderer
     */
    fun parseChartPlaylist(data: JsonObject): ChartPlaylist? {
        val title = navString(data, NavPath.TITLE_TEXT, true) ?: return null
        
        // playlistId is in title navigation, need to strip "VL" prefix
        val browseId = navString(data, NavPath.TITLE + NavPath.NAVIGATION_BROWSE_ID, true)
        val playlistId = browseId?.removePrefix("VL") ?: return null
        
        val thumbnails = parseThumbnails(data)
        
        return ChartPlaylist(
            title = title,
            playlistId = playlistId,
            thumbnails = thumbnails
        )
    }

    /**
     * Parse a chart artist from musicResponsiveListItemRenderer
     */
    fun parseChartArtist(data: JsonObject): ChartArtist? {
        val flexColumn0 = getFlexColumnItem(data, 0) ?: return null
        val title = navString(flexColumn0, NavPath.TEXT_RUN_TEXT, true) ?: return null
        
        val browseId = navString(data, NavPath.NAVIGATION_BROWSE_ID, true) ?: return null
        
        // Subscribers from second flex column
        val flexColumn1 = getFlexColumnItem(data, 1)
        val subscribers = flexColumn1?.let { 
            navString(it, NavPath.TEXT_RUN_TEXT, true)?.split(" ")?.firstOrNull() 
        }
        
        val thumbnails = parseThumbnails(data)
        val ranking = parseRanking(data)
        
        return ChartArtist(
            title = title,
            browseId = browseId,
            subscribers = subscribers,
            thumbnails = thumbnails,
            rank = ranking.first,
            trend = ranking.second
        )
    }

    /**
     * Parse ranking info (rank and trend) from customIndexColumn
     */
    fun parseRanking(data: JsonObject): Pair<String?, String?> {
        val indexColumn = data["customIndexColumn"] as? JsonObject
        val renderer = indexColumn?.get("musicCustomIndexColumnRenderer") as? JsonObject
        
        val rank = renderer?.let { navString(it, NavPath.TEXT_RUN_TEXT, true) }
        val iconType = renderer?.let { navString(it, NavPath.ICON_TYPE, true) }
        val trend = iconType?.let { TRENDS[it] }
        
        return rank to trend
    }

    /**
     * Parse thumbnails from data
     */
    fun parseThumbnails(data: JsonObject): List<Thumbnail> {
        val thumbnailArray = navList(data, NavPath.THUMBNAIL_RENDERER, true)
            ?: navList(data, NavPath.THUMBNAILS, true)
            ?: return emptyList()
        
        return thumbnailArray.mapNotNull { thumbElement ->
            val thumb = thumbElement as? JsonObject ?: return@mapNotNull null
            val url = thumb["url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            Thumbnail(
                url = url,
                width = thumb["width"]?.jsonPrimitive?.content?.toIntOrNull(),
                height = thumb["height"]?.jsonPrimitive?.content?.toIntOrNull()
            )
        }
    }

    /**
     * Parse content list with a given parser function
     */
    fun <T> parseContentList(
        contents: JsonArray,
        parser: (JsonObject) -> T?,
        rendererKey: String
    ): List<T> {
        return contents.mapNotNull { item ->
            val itemObj = item as? JsonObject ?: return@mapNotNull null
            val renderer = itemObj[rendererKey] as? JsonObject ?: return@mapNotNull null
            parser(renderer)
        }
    }
}
