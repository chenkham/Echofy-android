package com.Chenkham.ytmusicapi

import com.Chenkham.ytmusicapi.models.*
import com.Chenkham.ytmusicapi.parsers.ChartsParser
import com.Chenkham.ytmusicapi.parsers.PodcastParser
import com.Chenkham.ytmusicapi.utils.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/**
 * Main API class for YouTube Music.
 * Based on ytmusicapi's YTMusic class with Charts and Podcasts mixins.
 */
class YTMusicApi(
    locale: Locale = Locale.getDefault(),
    visitorData: String? = null,
    cookie: String? = null
) {
    private val client = InnerTubeClient(locale, visitorData, cookie)

    /**
     * Get latest charts data from YouTube Music.
     * Returns Artists and playlists of top videos.
     * 
     * Based on ytmusicapi's get_charts() from mixins/charts.py
     * 
     * @param country ISO 3166-1 Alpha-2 country code. Default: "ZZ" = Global
     * @return ChartsResult containing chart sections
     */
    suspend fun getCharts(country: String = "ZZ"): Result<ChartsResult> = runCatching {
        val formData = if (country.isNotEmpty()) {
            mapOf("selectedValues" to listOf(country))
        } else null

        val response = client.browse(
            browseId = "FEmusic_charts",
            formData = formData
        )

        val results = navList(response, NavPath.SINGLE_COLUMN_TAB + NavPath.SECTION_LIST, true)
            ?: return@runCatching ChartsResult()

        // Parse country info from first section's menu
        val countryInfo = parseCountryInfo(response, results)

        val sections = mutableListOf<ChartSection>()

        // Skip first result (country selector) and parse all others as sections
        for (i in 1 until results.size) {
            val result = results[i] as? JsonObject ?: continue
            val carousel = result[NavPath.CAROUSEL.last()] as? JsonObject
            val grid = result[NavPath.GRID.last()] as? JsonObject
            
            val renderer = carousel ?: grid ?: continue
            
            val header = renderer["header"] as? JsonObject
            val title = if (header != null) {
                navString(header, listOf("musicCarouselShelfBasicHeaderRenderer") + NavPath.TITLE, true)
                    ?: navString(header, listOf("musicResponsiveHeaderRenderer") + NavPath.TITLE_TEXT, true)
                    ?: ""
            } else ""

            // Get contents from the SAME renderer as the title to ensure alignment
            // For carousel: contents is in renderer["contents"]
            // For grid: items is in renderer["items"]
            val contents = (renderer["contents"] as? JsonArray) ?: (renderer["items"] as? JsonArray) ?: continue

             val items = contents.mapNotNull { item ->
                 val itemObj = item as? JsonObject ?: return@mapNotNull null
                 // Try parsing as Playlist or Artist
                 // We can use existing parsers but need to map to ChartItem
                 val mtrir = itemObj[NavPath.MTRIR] as? JsonObject
                 val mrlir = itemObj[NavPath.MRLIR] as? JsonObject
                 
                 if (mtrir != null) {
                     val playlist = ChartsParser.parseChartPlaylist(mtrir)
                     if (playlist != null) {
                         ChartItem(
                             title = playlist.title,
                             browseId = playlist.playlistId, // BrowseId = PlaylistId for charts usually
                             playlistId = playlist.playlistId,
                             thumbnails = playlist.thumbnails
                         )
                     } else null
                 } else if (mrlir != null) {
                     val artist = ChartsParser.parseChartArtist(mrlir)
                     if (artist != null) {
                        ChartItem(
                            title = artist.title,
                            browseId = artist.browseId,
                            thumbnails = artist.thumbnails,
                            subscribers = artist.subscribers,
                            rank = artist.rank,
                            trend = artist.trend
                        )
                     } else null
                 } else null
             }

             if (items.isNotEmpty()) {
                 sections.add(ChartSection(title = title, items = items))
             }
        }

        // Map back to legacy fields for compatibility if needed, using simple heuristics or just empty
        // Or filter sections by title keywords
        val videos = sections.find { it.title.contains("Videos", ignoreCase = true) }?.items?.map { ChartPlaylist(it.title, it.playlistId ?: it.browseId, it.thumbnails) } ?: emptyList()
        val artists = sections.find { it.title.contains("Artists", ignoreCase = true) }?.items?.map { ChartArtist(it.title, it.browseId, it.subscribers, it.thumbnails, it.rank, it.trend) } ?: emptyList()
        val genres = sections.filter { !it.title.contains("Videos", ignoreCase = true) && !it.title.contains("Artists", ignoreCase = true) }.flatMap { it.items }.map { ChartPlaylist(it.title, it.playlistId ?: it.browseId, it.thumbnails) }


        ChartsResult(
            countries = countryInfo,
            videos = videos,
            daily = sections.find { it.title.contains("Daily", ignoreCase = true) }?.items?.map { ChartPlaylist(it.title, it.playlistId ?: it.browseId, it.thumbnails) } ?: emptyList(),
            weekly = sections.find { it.title.contains("Weekly", ignoreCase = true) }?.items?.map { ChartPlaylist(it.title, it.playlistId ?: it.browseId, it.thumbnails) } ?: emptyList(),
            artists = artists,
            genres = genres,
            sections = sections
        )
    }

    private fun parseCountryInfo(response: JsonObject, results: JsonArray): CountryInfo {
        // Get selected country from first result's menu
        val firstResult = results.getOrNull(0) as? JsonObject
        val menu = firstResult?.let {
            navObject(it, listOf(
                "musicShelfRenderer", "subheaders", 0,
                "musicSideAlignedItemRenderer", "startItems", 0,
                "musicSortFilterButtonRenderer"
            ), true)
        }
        val selected = menu?.let { navString(it, NavPath.TITLE_TEXT, true) }

        // Get country options from framework mutations
        val mutations = navList(response, NavPath.FRAMEWORK_MUTATIONS, true) ?: return CountryInfo(selected = selected)
        val options = mutations.mapNotNull { mutation ->
            val mutObj = mutation as? JsonObject ?: return@mapNotNull null
            navString(mutObj, listOf("payload", "musicFormBooleanChoice", "opaqueToken"), true)
        }

        return CountryInfo(selected = selected, options = options)
    }

    private fun normalizePodcastBrowseId(id: String): String {
        val trimmedId = id.trim()
        return when {
            trimmedId.startsWith("MPSP") ||
                trimmedId.startsWith("MPPD") ||
                trimmedId.startsWith("VL") ||
                trimmedId.startsWith("RDPN") -> trimmedId
            trimmedId.startsWith("PL") || trimmedId.startsWith("OLAK") -> "VL$trimmedId"
            else -> "MPSP$trimmedId"
        }
    }

    private fun parseEpisodesFromResponse(response: JsonObject): List<Episode> {
        val directShelf = navObject(
            response,
            NavPath.TWO_COLUMN_RENDERER + listOf("secondaryContents") + NavPath.SECTION_LIST_ITEM + NavPath.MUSIC_SHELF,
            true
        )
        val directContents = directShelf?.get("contents") as? JsonArray
        if (!directContents.isNullOrEmpty()) {
            return ChartsParser.parseContentList(directContents, PodcastParser::parseEpisode, NavPath.MMRIR)
        }

        val singleColumnSections = navList(response, NavPath.SINGLE_COLUMN_TAB + NavPath.SECTION_LIST, true)
            ?: return emptyList()

        return singleColumnSections.flatMap { section ->
            val sectionObj = section as? JsonObject ?: return@flatMap emptyList()
            val shelfContents = navList(sectionObj, NavPath.MUSIC_SHELF + listOf("contents"), true)
            val gridContents = navList(sectionObj, NavPath.GRID_ITEMS, true)
            val contents = shelfContents ?: gridContents ?: return@flatMap emptyList()
            ChartsParser.parseContentList(contents, PodcastParser::parseEpisode, NavPath.MMRIR)
        }
    }

    /**
     * Get podcast metadata and episodes.
     * 
     * Based on ytmusicapi's get_podcast() from mixins/podcasts.py
     * 
     * @param playlistId Playlist ID (with or without MPSP prefix)
     * @return PodcastResult with podcast info and episodes
     */
    suspend fun getPodcast(playlistId: String): Result<PodcastResult> = runCatching {
        val browseId = normalizePodcastBrowseId(playlistId)
        
        val response = client.browse(browseId = browseId)
        
        val twoColumns = navObject(response, NavPath.TWO_COLUMN_RENDERER, true)
        if (twoColumns == null) {
            val playlist = PodcastParser.parsePlaylistHeader(response)
            return@runCatching playlist.copy(episodes = parseEpisodesFromResponse(response))
        }
        
        // Parse header
        val header = navObject(twoColumns, NavPath.TAB_CONTENT + NavPath.SECTION_LIST_ITEM + NavPath.RESPONSIVE_HEADER, true)
        val podcast = if (header != null) {
            PodcastParser.parsePodcastHeader(header)
        } else {
            PodcastResult(title = "")
        }

        // Parse episodes from secondary contents
        val episodes = parseEpisodesFromResponse(response)

        podcast.copy(episodes = episodes)
    }

    /**
     * Get single episode details.
     * 
     * Based on ytmusicapi's get_episode() from mixins/podcasts.py
     * 
     * @param videoId Video ID or browse ID (with or without MPED prefix)
     * @return EpisodeResult with episode details
     */
    suspend fun getEpisode(videoId: String): Result<EpisodeResult> = runCatching {
        val browseId = if (videoId.startsWith("MPED")) videoId else "MPED$videoId"
        
        val response = client.browse(browseId = browseId)
        
        val twoColumns = navObject(response, NavPath.TWO_COLUMN_RENDERER, true)
            ?: return@runCatching EpisodeResult(title = "")
        
        // Parse header
        val header = navObject(twoColumns, NavPath.TAB_CONTENT + NavPath.SECTION_LIST_ITEM + NavPath.RESPONSIVE_HEADER, true)
        val episode = if (header != null) {
            PodcastParser.parseEpisodeHeader(header)
        } else {
            EpisodeResult(title = "")
        }

        // Parse description from secondary contents
        val descriptionShelf = navObject(twoColumns, listOf("secondaryContents") + NavPath.SECTION_LIST_ITEM + NavPath.DESCRIPTION_SHELF, true)
        val description = descriptionShelf?.let { 
            navString(it, listOf("description", "runs", 0, "text"), true) 
        }

        episode.copy(description = description)
    }

    /**
     * Get podcast channel info with episodes and podcasts.
     * 
     * Based on ytmusicapi's get_channel() from mixins/podcasts.py
     * 
     * @param channelId Channel ID
     * @return ChannelResult with channel info
     */
    suspend fun getChannel(channelId: String): Result<ChannelResult> = runCatching {
        val response = client.browse(browseId = channelId)
        
        val title = navString(response, listOf("header", "musicVisualHeaderRenderer") + NavPath.TITLE_TEXT, true) ?: ""
        val thumbnails = navList(response, listOf("header", "musicVisualHeaderRenderer") + NavPath.THUMBNAILS, true)
            ?.mapNotNull { thumb ->
                val thumbObj = thumb as? JsonObject ?: return@mapNotNull null
                Thumbnail(
                    url = thumbObj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    width = thumbObj["width"]?.jsonPrimitive?.content?.toIntOrNull(),
                    height = thumbObj["height"]?.jsonPrimitive?.content?.toIntOrNull()
                )
            } ?: emptyList()

        val results = navList(response, NavPath.SINGLE_COLUMN_TAB + NavPath.SECTION_LIST, true)

        // Parse sections
        var episodesSection: ChannelSection? = null
        var podcastsSection: ChannelSection? = null

        results?.forEach { section ->
            val sectionObj = section as? JsonObject ?: return@forEach
            
            // Episodes section (grid)
            val gridItems = navList(sectionObj, NavPath.GRID_ITEMS, true)
            if (gridItems != null) {
                val episodes = gridItems.mapNotNull { item ->
                    val itemObj = item as? JsonObject ?: return@mapNotNull null
                    val renderer = itemObj[NavPath.MMRIR] as? JsonObject ?: return@mapNotNull null
                    PodcastParser.parseEpisode(renderer)
                }
                episodesSection = ChannelSection(
                    browseId = channelId,
                    results = episodes
                )
            }
            
            // Podcasts section (carousel)
            val carouselContents = navList(sectionObj, NavPath.CAROUSEL_CONTENTS, true)
            if (carouselContents != null) {
                val podcasts = carouselContents.mapNotNull { item ->
                    val itemObj = item as? JsonObject ?: return@mapNotNull null
                    val renderer = itemObj[NavPath.MTRIR] as? JsonObject ?: return@mapNotNull null
                    // Convert PodcastItem to Episode for now (they share similar structure)
                    val podcast = PodcastParser.parsePodcast(renderer) ?: return@mapNotNull null
                    Episode(
                        title = podcast.title,
                        browseId = podcast.browseId,
                        thumbnails = podcast.thumbnails
                    )
                }
                podcastsSection = ChannelSection(
                    results = podcasts
                )
            }
        }

        ChannelResult(
            title = title,
            thumbnails = thumbnails,
            episodes = episodesSection,
            podcasts = podcastsSection
        )
    }

    /**
     * Get all channel episodes.
     * 
     * Based on ytmusicapi's get_channel_episodes() from mixins/podcasts.py
     * 
     * @param channelId Channel ID
     * @param params Params parsing from getChannel
     * @return List of episodes
     */
    suspend fun getChannelEpisodes(channelId: String, params: String): Result<List<Episode>> = runCatching {
        val body = mapOf("browseId" to listOf(channelId), "params" to listOf(params))
        val response = client.browse(browseId = channelId, params = params)
        
        val results = navList(response, NavPath.SINGLE_COLUMN_TAB + NavPath.SECTION_LIST_ITEM + NavPath.GRID_ITEMS, true)
            ?: return@runCatching emptyList()
            
        ChartsParser.parseContentList(results, PodcastParser::parseEpisode, NavPath.MMRIR)
    }

    /**
     * Get all episodes in an episodes playlist.
     * 
     * Based on ytmusicapi's get_episodes_playlist() from mixins/podcasts.py
     * 
     * @param playlistId Playlist ID, defaults to "RDPN" (New Episodes)
     * @return PodcastResult with episodes
     */
    suspend fun getEpisodesPlaylist(playlistId: String = "RDPN"): Result<PodcastResult> = runCatching {
        val browseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        
        val response = client.browse(browseId = browseId)
        
        val podcast = PodcastParser.parsePlaylistHeader(response)
        
        val results = navObject(response, NavPath.TWO_COLUMN_RENDERER + listOf("secondaryContents") + NavPath.SECTION_LIST_ITEM + NavPath.MUSIC_SHELF, true)
        val contents = results?.get("contents") as? JsonArray
        
        val episodes = contents?.let {
            ChartsParser.parseContentList(it, PodcastParser::parseEpisode, NavPath.MMRIR)
        } ?: emptyList()
        
        podcast.copy(episodes = episodes)
    }

    /**
     * Get the main podcasts landing page.
     * Uses FEmusic_podcasts browse ID to fetch podcast categories (News, Sports, etc.).
     * Parses all carousel sections including musicTwoRowItemRenderer, musicResponsiveListItemRenderer,
     * and musicNavigationButtonRenderer to capture all available content.
     */
    suspend fun getPodcastsLanding(): Result<PodcastLandingResult> = runCatching {
        val response = client.browse(browseId = "FEmusic_podcasts")
        
        val results = navList(response, NavPath.SINGLE_COLUMN_TAB + NavPath.SECTION_LIST, true)
            ?: return@runCatching PodcastLandingResult()
            
        val sections = results.mapNotNull { section ->
            val sectionObj = section as? JsonObject ?: return@mapNotNull null
            
            // Try all section renderers: Carousel, Grid, and Shelf
            val carousel = sectionObj["musicCarouselShelfRenderer"] as? JsonObject
            val grid = sectionObj["gridRenderer"] as? JsonObject
            val shelf = sectionObj["musicShelfRenderer"] as? JsonObject
            
            val renderer = carousel ?: grid ?: shelf ?: return@mapNotNull null
            
            // Parse header for section title
            val header = renderer["header"] as? JsonObject
            val title = if (header != null) {
                navString(header, listOf("musicCarouselShelfBasicHeaderRenderer") + NavPath.TITLE, true)
                    ?: navString(header, listOf("musicResponsiveHeaderRenderer") + NavPath.TITLE_TEXT, true)
                    ?: navString(header, listOf("gridHeaderRenderer") + NavPath.TITLE, true)
                    ?: navString(header, listOf("musicShelfBasicHeaderRenderer") + NavPath.TITLE, true)
                    ?: ""
            } else ""
            
            // Get items array - "contents" for carousels/shelves, "items" for grids
            val itemsArray = (renderer["contents"] as? JsonArray)
                ?: (renderer["items"] as? JsonArray)
            
            val items = itemsArray?.mapNotNull { item ->
                val itemObj = item as? JsonObject ?: return@mapNotNull null
                
                // Try musicTwoRowItemRenderer (most common for podcast cards)
                val mtrir = itemObj[NavPath.MTRIR] as? JsonObject
                if (mtrir != null) return@mapNotNull PodcastParser.parsePodcast(mtrir)
                
                // Try musicResponsiveListItemRenderer (list-style items)
                val mrlir = itemObj[NavPath.MRLIR] as? JsonObject
                if (mrlir != null) return@mapNotNull PodcastParser.parsePodcast(mrlir)
                
                // Try musicNavigationButtonRenderer (category buttons/chips)
                val navButton = itemObj["musicNavigationButtonRenderer"] as? JsonObject
                if (navButton != null) {
                    val navTitle = navString(navButton, listOf("buttonText") + NavPath.RUN_TEXT, true)
                        ?: navString(navButton, NavPath.TITLE, true)
                        ?: return@mapNotNull null
                    val browseId = navString(navButton, listOf("clickCommand") + NavPath.NAVIGATION_BROWSE_ID, true)
                        ?: navString(navButton, NavPath.NAVIGATION_BROWSE_ID, true)
                        ?: return@mapNotNull null
                    val thumbnails = ChartsParser.parseThumbnails(navButton)
                    return@mapNotNull PodcastItem(
                        title = navTitle,
                        browseId = browseId,
                        thumbnails = thumbnails
                    )
                }
                
                null
            } ?: emptyList()
            
            if (items.isNotEmpty()) {
                PodcastLandingSection(title = title, items = items)
            } else null
        }
        
        PodcastLandingResult(sections = sections)
    }

    /**
     * Close the API client
     */
    fun close() {
        client.close()
    }
}
