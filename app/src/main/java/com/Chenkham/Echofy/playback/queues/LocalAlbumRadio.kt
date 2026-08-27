package com.Chenkham.Echofy.playback.queues

import androidx.media3.common.MediaItem
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.Chenkham.Echofy.db.entities.AlbumWithSongs
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class LocalAlbumRadio(
    private val albumWithSongs: AlbumWithSongs,
    private val startIndex: Int = 0,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private lateinit var playlistId: String
    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId,
            params = "wAEB"
        )

    private var continuation: String? = null
    private var firstTimeLoaded: Boolean = false

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        Queue.Status(
            title = albumWithSongs.album.title,
            items = albumWithSongs.songs.map { it.toMediaItem() },
            mediaItemIndex = startIndex
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaItem> = withContext(IO) {
        if (!firstTimeLoaded) {
            val albumResult = YouTube.album(albumWithSongs.album.id).getOrNull()
            playlistId = albumResult?.album?.playlistId ?: albumWithSongs.album.playlistId ?: albumWithSongs.album.id
            val nextResult = YouTube.next(endpoint, continuation).getOrNull()
            firstTimeLoaded = true
            if (nextResult == null) {
                return@withContext emptyList()
            }
            continuation = nextResult.continuation
            val fromIndex = minOf(albumWithSongs.songs.size, nextResult.items.size)
            return@withContext nextResult.items.subList(
                fromIndex,
                nextResult.items.size
            ).map { it.toMediaItem() }
        }
        val nextResult = YouTube.next(endpoint, continuation).getOrNull()
        continuation = nextResult?.continuation
        nextResult?.items?.map { it.toMediaItem() } ?: emptyList()
    }
}
