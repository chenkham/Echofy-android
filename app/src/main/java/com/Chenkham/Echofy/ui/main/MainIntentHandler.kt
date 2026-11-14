package com.Chenkham.Echofy.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.exoplayer.offline.Download
import com.Chenkham.Echofy.constants.SongSortType
import com.Chenkham.Echofy.constants.VoiceControlEnabledKey
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.DownloadUtil
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.utils.DynamicShortcuts
import com.Chenkham.Echofy.utils.reportException
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.innertube.pages.SearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Handles all deep-link, Google Assistant, Jam join/config, and shared media intents for MainActivity.
 */
class MainIntentHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    private val dataStore: DataStore<Preferences>,
    private val playerConnectionProvider: () -> PlayerConnection?,
    private val onPendingJamRoomCode: (String) -> Unit,
) {
    companion object {
        private const val TAG = "MainIntentHandler"
        val JamRegistryUrlKey = MainJamIntentHandler.JamRegistryUrlKey
        val JamInviteBaseUrlKey = MainJamIntentHandler.JamInviteBaseUrlKey
        val JamAllocationUrlKey = MainJamIntentHandler.JamAllocationUrlKey

        fun extractJamRoomCode(uri: Uri): String? = MainJamIntentHandler.extractJamRoomCode(uri)
    }

    fun handleIntent(intent: Intent) {
        if (MainJamIntentHandler.handleJamConfigIntent(context, intent, scope, dataStore, playerConnectionProvider)) return
        if (MainJamIntentHandler.handleJamJoinIntent(context, intent, onPendingJamRoomCode)) return

        if (intent.action == DynamicShortcuts.ACTION_PLAY_PLAYLIST) {
            intent.getStringExtra(DynamicShortcuts.EXTRA_PLAYLIST_ID)?.let { playSmartPlaylist(it) }
            return
        }

        if (intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
            handleAssistantMediaSearch(intent)
            return
        }

        handleSharedYouTubeUrl(intent)
    }

    private fun handleAssistantMediaSearch(intent: Intent) {
        scope.launch {
            val isEnabled = dataStore.data.first()[VoiceControlEnabledKey] ?: false

            if (!isEnabled) {
                Toast.makeText(context, "Enable Google Assistant Integration in AI settings to use voice playback", Toast.LENGTH_LONG).show()
                return@launch
            }

            val query = intent.getStringExtra(SearchManager.QUERY)
            if (!query.isNullOrEmpty()) {
                val lowerQuery = query.lowercase()
                if (lowerQuery.contains("download") || lowerQuery.contains("offline")) {
                    playSmartPlaylist(PlaylistEntity.DOWNLOADED_PLAYLIST_ID)
                    return@launch
                }
                if (lowerQuery.contains("liked") || lowerQuery.contains("favorite")) {
                    playSmartPlaylist(PlaylistEntity.LIKED_PLAYLIST_ID)
                    return@launch
                }
                performOnlineSearchAndPlay(query)
            } else {
                playerConnectionProvider()?.player?.play()
            }
        }
    }

    private fun handleSharedYouTubeUrl(intent: Intent) {
        val rawSharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.extras?.getString(Intent.EXTRA_TEXT)
        val uri =
            intent.data ?: rawSharedText?.let { text ->
                val match = Regex("""https?://[^\s<>"']+""").find(text)
                match?.value?.toUri() ?: runCatching { text.trim().toUri() }.getOrNull()
            } ?: return

        val videoId =
            when {
                uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
                uri.host?.contains("youtu.be", ignoreCase = true) == true -> uri.pathSegments.firstOrNull()
                uri.pathSegments.firstOrNull() == "shorts" -> uri.pathSegments.getOrNull(1)
                uri.pathSegments.firstOrNull() == "live" -> uri.pathSegments.getOrNull(1)
                uri.getQueryParameter("v") != null -> uri.getQueryParameter("v")
                else -> {
                    val urlString = uri.toString()
                    val idMatch = Regex("""(?:v=|youtu\.be/|shorts/|live/)([a-zA-Z0-9_-]{11})""").find(urlString)
                    idMatch?.groupValues?.getOrNull(1)
                }
            }?.takeIf { it.isNotBlank() }

        if (videoId != null) {
            scope.launch {
                val playerConn =
                    withTimeoutOrNull(20000) {
                        snapshotFlow { playerConnectionProvider() }.filterNotNull().first()
                    } ?: playerConnectionProvider()

                if (playerConn != null) {
                    playerConn.playQueue(
                        YouTubeQueue.radio(
                            MediaMetadata(
                                id = videoId,
                                title = "Loading...",
                                artists = emptyList(),
                                duration = 0,
                                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                            ),
                        ),
                    )
                } else {
                    Timber.tag(TAG).e("Failed to bind playerConnection for shared link $videoId")
                }
            }
        }
    }

    fun playSmartPlaylist(playlistId: String) {
        scope.launch {
            if (playerConnectionProvider() == null) {
                delay(1000)
            }

            val songs: List<Song> =
                withContext(Dispatchers.IO) {
                    when (playlistId) {
                        PlaylistEntity.LIKED_PLAYLIST_ID -> {
                            database.likedSongs(SongSortType.CREATE_DATE, descending = true).first()
                        }
                        PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> {
                            val downloads = downloadUtil.downloads.value
                            database.allSongs().first()
                                .filter { downloads[it.id]?.state == Download.STATE_COMPLETED }
                                .sortedBy { downloads[it.id]?.updateTimeMs ?: 0L }
                        }
                        else -> emptyList()
                    }
                }

            if (songs.isNotEmpty()) {
                playerConnectionProvider()?.playQueue(
                    ListQueue(
                        title = if (playlistId == PlaylistEntity.LIKED_PLAYLIST_ID) "Liked Songs" else "Downloads",
                        items = songs.map { it.toMediaItem() },
                        startIndex = 0,
                    ),
                )
            } else {
                Toast.makeText(context, "No songs found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun performOnlineSearchAndPlay(query: String) {
        scope.launch {
            Toast.makeText(context, "Searching for $query...", Toast.LENGTH_SHORT).show()

            val result: Result<SearchResult> = withContext(Dispatchers.IO) {
                YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            }

            result.onSuccess { searchResult ->
                val firstSong = searchResult.items.firstNotNullOfOrNull { it as? SongItem }
                if (firstSong != null) {
                    playerConnectionProvider()?.playQueue(
                        YouTubeQueue(WatchEndpoint(videoId = firstSong.id), firstSong.toMediaMetadata()),
                    )
                } else {
                    Toast.makeText(context, "No songs found for $query", Toast.LENGTH_LONG).show()
                }
            }.onFailure {
                reportException(it)
                Toast.makeText(context, "Search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
