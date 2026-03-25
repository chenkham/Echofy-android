package com.Chenkham.ytmusicapi.parsers

import com.Chenkham.ytmusicapi.models.*
import com.Chenkham.ytmusicapi.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser functions for podcast data.
 * Based on ytmusicapi's parsers/podcasts.py
 */
object PodcastParser {

    /**
     * Parse base header (common to episode and podcast pages)
     */
    private fun parseBaseHeader(header: JsonObject): Triple<Author?, String?, List<Thumbnail>> {
        val strapline = header["straplineTextOne"] as? JsonObject
        val authorName = strapline?.let { navString(it, NavPath.RUN_TEXT, true) }
        val authorId = strapline?.let { navString(it, listOf("runs", 0) + NavPath.NAVIGATION_BROWSE_ID, true) }
        
        val author = if (authorName != null) Author(name = authorName, id = authorId) else null
        val title = navString(header, NavPath.TITLE_TEXT, true)
        val thumbnails = ChartsParser.parseThumbnails(header)
        
        return Triple(author, title, thumbnails)
    }

    /**
     * Parse podcast header from musicResponsiveHeaderRenderer
     */
    fun parsePodcastHeader(header: JsonObject): PodcastResult {
        val (author, title, thumbnails) = parseBaseHeader(header)
        
        val descriptionPath = listOf("description") + NavPath.DESCRIPTION_SHELF + NavPath.DESCRIPTION
        val description = navString(header, descriptionPath, true)
        
        val saved = header["buttons"]?.let { buttons ->
            val buttonsArray = buttons as? JsonArray ?: return@let false
            val button = buttonsArray.getOrNull(1) as? JsonObject ?: return@let false
            val toggleRenderer = button["toggleButtonRenderer"] as? JsonObject ?: return@let false
            toggleRenderer["isToggled"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        } ?: false
        
        return PodcastResult(
            author = author,
            title = title ?: "",
            description = description,
            saved = saved,
            thumbnails = thumbnails
        )
    }

    /**
     * Parse episode from musicMultiRowListItemRenderer
     */
    fun parseEpisode(data: JsonObject): Episode {
        val thumbnails = ChartsParser.parseThumbnails(data)
        val title = navString(data, NavPath.TITLE_TEXT, true) ?: ""
        val description = navString(data, NavPath.DESCRIPTION, true)
        val date = navString(data, NavPath.SUBTITLE, true)
        
        // Duration from playbackProgress
        val progressRenderer = data["playbackProgress"] as? JsonObject
        val duration = progressRenderer?.get("musicPlaybackProgressRenderer")?.let { renderer ->
            navString(renderer as JsonObject, listOf("durationText", "runs", 1, "text"), true)
        }
        
        // Video ID from onTap.watchEndpoint
        val onTap = data["onTap"] as? JsonObject
        val videoId = onTap?.let { navString(it, NavPath.WATCH_VIDEO_ID, true) }
        
        // Browse ID from title navigation
        val browseId = navString(data, NavPath.TITLE + NavPath.NAVIGATION_BROWSE_ID, true)
        
        // Video type
        val videoType = onTap?.let { 
            navString(it, listOf("watchEndpoint", "watchEndpointMusicSupportedConfigs", 
                "watchEndpointMusicConfig", "musicVideoType"), true) 
        }
        
        // Index
        val index = onTap?.let { navInt(it, listOf("watchEndpoint", "index"), true) }
        
        return Episode(
            index = index,
            title = title,
            description = description,
            duration = duration,
            videoId = videoId,
            browseId = browseId,
            videoType = videoType,
            date = date,
            thumbnails = thumbnails
        )
    }

    /**
     * Parse episode header for single episode view
     */
    fun parseEpisodeHeader(header: JsonObject): EpisodeResult {
        val (author, title, thumbnails) = parseBaseHeader(header)
        
        val date = navString(header, NavPath.SUBTITLE, true)
        
        val progressRenderer = header["progress"]?.let { 
            (it as? JsonObject)?.get("musicPlaybackProgressRenderer") as? JsonObject 
        }
        val duration = progressRenderer?.let { 
            navString(it, listOf("durationText", "runs", 1, "text"), true) 
        }
        val progressPercentage = progressRenderer?.let { 
            navInt(it, listOf("playbackProgressPercentage"), true) 
        }
        
        // Saved status
        val saved = header["buttons"]?.let { buttons ->
            val buttonsArray = buttons as? JsonArray ?: return@let false
            val button = buttonsArray.getOrNull(0) as? JsonObject ?: return@let false
            val toggleRenderer = button["toggleButtonRenderer"] as? JsonObject ?: return@let false
            toggleRenderer["isToggled"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        } ?: false
        
        // Playlist ID from menu buttons
        var playlistId: String? = null
        val menuButtons = header["buttons"]?.let { buttons ->
            val buttonsArray = buttons as? JsonArray ?: return@let null
            val lastButton = buttonsArray.lastOrNull() as? JsonObject ?: return@let null
            val menuRenderer = lastButton["menuRenderer"] as? JsonObject ?: return@let null
            menuRenderer["items"] as? JsonArray
        }
        menuButtons?.forEach { buttonElement ->
            val button = buttonElement as? JsonObject ?: return@forEach
            val navItem = button["menuNavigationItemRenderer"] as? JsonObject ?: return@forEach
            val iconType = navString(navItem, NavPath.ICON_TYPE, true)
            if (iconType == "BROADCAST") {
                playlistId = navString(navItem, NavPath.NAVIGATION_BROWSE_ID, true)
            }
        }
        
        return EpisodeResult(
            author = author,
            title = title ?: "",
            date = date,
            duration = duration,
            progressPercentage = progressPercentage,
            saved = saved,
            playlistId = playlistId,
            thumbnails = thumbnails
        )
    }

    /**
     * Parse podcast item from musicTwoRowItemRenderer (on channel pages)
     */
    fun parsePodcast(data: JsonObject): PodcastItem? {
        val title = navString(data, NavPath.TITLE_TEXT, true) ?: return null
        val browseId = navString(data, NavPath.TITLE + NavPath.NAVIGATION_BROWSE_ID, true) ?: return null
        
        // Channel info from subtitle
        val subtitleRuns = data["subtitle"]?.let { (it as? JsonObject)?.get("runs") } as? JsonArray
        val channelRun = subtitleRuns?.getOrNull(0) as? JsonObject
        val channelName = channelRun?.get("text")?.jsonPrimitive?.content
        val channelId = channelRun?.let { navString(it, NavPath.NAVIGATION_BROWSE_ID, true) }
        val channel = if (channelName != null) Author(name = channelName, id = channelId) else null
        
        // Podcast ID from thumbnail overlay
        val podcastId = navString(data, listOf("thumbnailOverlay", "musicItemThumbnailOverlayRenderer", 
            "content", "musicPlayButtonRenderer", "playNavigationEndpoint", "watchPlaylistEndpoint", "playlistId"), true)
        
        val thumbnails = ChartsParser.parseThumbnails(data)
        
        return PodcastItem(
            title = title,
            browseId = browseId,
            podcastId = podcastId,
            channel = channel,
            thumbnails = thumbnails
        )
    }

    /**
     * Parse playlist header for episodes playlist
     */
    fun parsePlaylistHeader(response: JsonObject): PodcastResult {
        var header = navObject(response, NavPath.HEADER_DETAIL, true)
        if (header == null) {
            header = navObject(response, 
                NavPath.TWO_COLUMN_RENDERER + NavPath.TAB_CONTENT + NavPath.SECTION_LIST_ITEM + NavPath.RESPONSIVE_HEADER, 
                true
            )
        }
        
        val title = header?.let { h -> 
            val titleObj = h["title"] as? JsonObject
            val runs = titleObj?.get("runs") as? JsonArray
            runs?.joinToString("") { 
                (it as? JsonObject)?.get("text")?.jsonPrimitive?.content ?: "" 
            }
        } ?: ""

        val thumbnails = header?.let { ChartsParser.parseThumbnails(it) } ?: emptyList()
        val description = header?.let { 
             navString(it, listOf("description") + NavPath.DESCRIPTION_SHELF + NavPath.DESCRIPTION, true)
        }
        
        // Use default author for now or try to extraction if needed
        // For New Episodes playlist (RDPN), it's usually auto-generated
        
        return PodcastResult(
            title = title,
            description = description,
            thumbnails = thumbnails,
            saved = false // Default
        )
    }
}
