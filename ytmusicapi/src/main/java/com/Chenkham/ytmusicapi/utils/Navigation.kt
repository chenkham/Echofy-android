package com.Chenkham.ytmusicapi.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Navigation path constants based on ytmusicapi's navigation.py
 */
object NavPath {
    val CONTENT = listOf("contents", 0)
    val RUN_TEXT = listOf("runs", 0, "text")
    val TAB_CONTENT = listOf("tabs", 0, "tabRenderer", "content")
    val SINGLE_COLUMN = listOf("contents", "singleColumnBrowseResultsRenderer")
    val SINGLE_COLUMN_TAB = SINGLE_COLUMN + TAB_CONTENT
    val SECTION_LIST = listOf("sectionListRenderer", "contents")
    val SECTION_LIST_ITEM = listOf("sectionListRenderer") + CONTENT
    val TWO_COLUMN_RENDERER = listOf("contents", "twoColumnBrowseResultsRenderer")
    
    val MUSIC_SHELF = listOf("musicShelfRenderer")
    val GRID = listOf("gridRenderer")
    val GRID_ITEMS = GRID + listOf("items")
    val CAROUSEL = listOf("musicCarouselShelfRenderer")
    val CAROUSEL_CONTENTS = CAROUSEL + listOf("contents")
    val CAROUSEL_TITLE = listOf("header", "musicCarouselShelfBasicHeaderRenderer", "title", "runs", 0)
    
    val TITLE = listOf("title", "runs", 0)
    val TITLE_TEXT = listOf("title") + RUN_TEXT
    val SUBTITLE = listOf("subtitle") + RUN_TEXT
    val SUBTITLE_RUNS = listOf("subtitle", "runs")
    
    val THUMBNAIL = listOf("thumbnail", "thumbnails")
    val THUMBNAILS = listOf("thumbnail", "musicThumbnailRenderer") + THUMBNAIL
    val THUMBNAIL_RENDERER = listOf("thumbnailRenderer", "musicThumbnailRenderer") + THUMBNAIL
    
    val NAVIGATION_BROWSE_ID = listOf("navigationEndpoint", "browseEndpoint", "browseId")
    val WATCH_VIDEO_ID = listOf("watchEndpoint", "videoId")
    val WATCH_PLAYLIST_ID = listOf("watchEndpoint", "playlistId")
    
    val MULTI_SELECT = listOf("musicMultiSelectMenuItemRenderer")
    val HEADER = listOf("header")
    val HEADER_DETAIL = HEADER + listOf("musicDetailHeaderRenderer")
    val EDITABLE_PLAYLIST_DETAIL_HEADER = listOf("musicEditablePlaylistDetailHeaderRenderer")
    val HEADER_EDITABLE_DETAIL = HEADER + EDITABLE_PLAYLIST_DETAIL_HEADER
    val HEADER_SIDE = HEADER + listOf("musicSideAlignedItemRenderer")
    val HEADER_MUSIC_VISUAL = HEADER + listOf("musicVisualHeaderRenderer")
    
    val TEXT_RUNS = listOf("text", "runs")
    val TEXT_RUN = TEXT_RUNS + listOf(0)
    val TEXT_RUN_TEXT = TEXT_RUN + listOf("text")
    
    val RESPONSIVE_HEADER = listOf("musicResponsiveHeaderRenderer")
    val DESCRIPTION_SHELF = listOf("musicDescriptionShelfRenderer")
    val DESCRIPTION = listOf("description") + RUN_TEXT
    
    val FRAMEWORK_MUTATIONS = listOf("frameworkUpdates", "entityBatchUpdate", "mutations")
    
    val MRLIR = "musicResponsiveListItemRenderer"
    val MTRIR = "musicTwoRowItemRenderer"
    val MMRIR = "musicMultiRowListItemRenderer"
    
    val FLEX_COLUMN = listOf("flexColumns")
    val ICON_TYPE = listOf("icon", "iconType")
}

/**
 * TRENDS mapping from ytmusicapi
 */
val TRENDS = mapOf(
    "ARROW_DROP_UP" to "up",
    "ARROW_DROP_DOWN" to "down",
    "ARROW_CHART_NEUTRAL" to "neutral"
)

/**
 * Safe navigation through nested JSON structure.
 * Based on ytmusicapi's nav() function.
 */
fun nav(root: JsonElement?, path: List<Any>, noneIfAbsent: Boolean = false): JsonElement? {
    if (root == null) return null
    
    var current: JsonElement? = root
    for (key in path) {
        current = when {
            current == null -> return if (noneIfAbsent) null else throw IllegalArgumentException("Path not found: $path")
            key is String && current is JsonObject -> current[key]
            key is Int && current is JsonArray -> current.getOrNull(key)
            else -> return if (noneIfAbsent) null else throw IllegalArgumentException("Invalid path type at $key in $path")
        }
    }
    return current
}

/**
 * Get string value from navigation path
 */
fun navString(root: JsonElement?, path: List<Any>, noneIfAbsent: Boolean = false): String? {
    val element = nav(root, path, noneIfAbsent)
    return (element as? JsonPrimitive)?.contentOrNull
}

/**
 * Get int value from navigation path
 */
fun navInt(root: JsonElement?, path: List<Any>, noneIfAbsent: Boolean = false): Int? {
    val element = nav(root, path, noneIfAbsent)
    return (element as? JsonPrimitive)?.intOrNull
}

/**
 * Get list from navigation path
 */
fun navList(root: JsonElement?, path: List<Any>, noneIfAbsent: Boolean = false): JsonArray? {
    val element = nav(root, path, noneIfAbsent)
    return element as? JsonArray
}

/**
 * Get object from navigation path
 */
fun navObject(root: JsonElement?, path: List<Any>, noneIfAbsent: Boolean = false): JsonObject? {
    val element = nav(root, path, noneIfAbsent)
    return element as? JsonObject
}

/**
 * Get flex column item from musicResponsiveListItemRenderer
 */
fun getFlexColumnItem(data: JsonObject, index: Int): JsonObject? {
    val flexColumns = data["flexColumns"] as? JsonArray ?: return null
    val column = flexColumns.getOrNull(index) as? JsonObject ?: return null
    return column["musicResponsiveListItemFlexColumnRenderer"] as? JsonObject
}
