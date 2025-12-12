package com.Chenkham.innertube.pages

import com.Chenkham.innertube.models.Album
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.Artist
import com.Chenkham.innertube.models.ArtistItem
import com.Chenkham.innertube.models.MusicResponsiveListItemRenderer
import com.Chenkham.innertube.models.MusicTwoRowItemRenderer
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.YTItem
import com.Chenkham.innertube.models.oddElements
import com.Chenkham.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
