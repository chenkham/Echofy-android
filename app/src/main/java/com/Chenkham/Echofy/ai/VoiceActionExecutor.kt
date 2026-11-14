package com.Chenkham.Echofy.ai

import android.media.AudioManager
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.Chenkham.Echofy.db.entities.PlaylistSongMap
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.innertube.models.WatchEndpoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Handles execution of parsed AgentActions against the audio subsystem, player, and local database.
 */
class VoiceActionExecutor(
    private val audioManager: AudioManager,
    private val database: MusicDatabase,
) {
    suspend fun executeAgentAction(
        action: AgentAction,
        playerConnection: PlayerConnection?,
        source: InputSource = InputSource.TEXT,
        onRespond: suspend (String, InputSource) -> Unit,
        onWakeAck: suspend () -> Unit,
    ) {
        when (action.actionType) {
            "WAKE_ACK" -> {
                onWakeAck()
            }
            "PAUSE" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.pause() }
                onRespond(action.speakResponse, source)
            }
            "RESUME" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.play() }
                onRespond(action.speakResponse, source)
            }
            "NEXT" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.seekToNext() }
                onRespond(action.speakResponse, source)
            }
            "PREVIOUS" -> {
                withContext(Dispatchers.Main) { playerConnection?.player?.seekToPrevious() }
                onRespond(action.speakResponse, source)
            }
            "SET_VOLUME" -> {
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val targetVol = (maxVol * (action.volumeLevel / 100f)).toInt().coerceIn(0, maxVol)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                onRespond(action.speakResponse, source)
            }
            "SHUFFLE" -> {
                withContext(Dispatchers.Main) {
                    playerConnection?.player?.let {
                        it.shuffleModeEnabled = !it.shuffleModeEnabled
                    }
                }
                onRespond(action.speakResponse, source)
            }
            "REPEAT" -> {
                withContext(Dispatchers.Main) {
                    playerConnection?.player?.let {
                        it.repeatMode =
                            when (it.repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_OFF -> androidx.media3.common.Player.REPEAT_MODE_ALL
                                androidx.media3.common.Player.REPEAT_MODE_ALL -> androidx.media3.common.Player.REPEAT_MODE_ONE
                                else -> androidx.media3.common.Player.REPEAT_MODE_OFF
                            }
                    }
                }
                onRespond(action.speakResponse, source)
            }
            "WHATS_PLAYING" -> {
                val metadata = withContext(Dispatchers.Main) { playerConnection?.mediaMetadata?.value }
                if (metadata != null) {
                    val artistNames = metadata.artists.joinToString(", ") { it.name }
                    onRespond("Now playing: \"${metadata.title}\" by $artistNames 🎵", source)
                } else {
                    onRespond("Nothing is playing right now. Ask me to play something!", source)
                }
            }
            "LIKE_SONG" -> {
                try {
                    val metadata = withContext(Dispatchers.Main) { playerConnection?.mediaMetadata?.value }
                    if (metadata != null) {
                        withContext(Dispatchers.Main) {
                            playerConnection?.toggleLike()
                        }
                        onRespond("Toggled like for \"${metadata.title}\"! ❤️", source)
                    } else {
                        onRespond("No song is currently playing to like.", source)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to like current song")
                    onRespond("Couldn't like the song. Try again?", source)
                }
            }
            "CREATE_PLAYLIST" -> {
                val playlistTitle = action.playlistName.ifEmpty { "AI Mix" }
                val songIds = mutableListOf<String>()

                // Search for songs if query is provided
                if (action.query.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val searchResult = YouTube.search(action.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                            searchResult?.items?.filterIsInstance<SongItem>()?.take(15)?.forEach { songItem ->
                                runCatching {
                                    database.query {
                                        insert(songItem.toMediaMetadata().toSongEntity())
                                    }
                                }
                                songIds.add(songItem.id)
                            }
                        }
                    }
                }

                // Fall back to history/liked if no songs found from search
                if (songIds.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val events = database.events().first().take(10)
                            events.forEach { songIds.add(it.song.song.id) }
                        }
                        runCatching {
                            val liked = database.likedSongsByRowIdAsc().first().take(10)
                            liked.forEach { if (!songIds.contains(it.song.id)) songIds.add(it.song.id) }
                        }
                    }
                }

                // Actually create the playlist with songs
                withContext(Dispatchers.IO) {
                    runCatching {
                        val newPlaylist = PlaylistEntity(name = playlistTitle)
                        database.query {
                            insert(newPlaylist)
                            songIds.distinct().forEachIndexed { idx, sId ->
                                runCatching {
                                    insert(PlaylistSongMap(playlistId = newPlaylist.id, songId = sId, position = idx))
                                }
                            }
                        }
                    }
                }

                val count = songIds.distinct().size
                val finalMessage =
                    if (action.speakResponse.isNotBlank() && !action.speakResponse.startsWith("Created playlist")) {
                        "${action.speakResponse}\n\nCreated your playlist \"$playlistTitle\" with $count songs in your Library!"
                    } else {
                        "Created your playlist \"$playlistTitle\" with $count songs! Check your Library to see it."
                    }
                onRespond(finalMessage, source)
            }
            "PLAY_SONG" -> {
                if (action.query.isNotEmpty()) {
                    executePlaySongAction(action.query, playerConnection, source, onRespond)
                } else {
                    onRespond(action.speakResponse, source)
                }
            }
            else -> {
                onRespond(action.speakResponse, source)
            }
        }
    }

    private suspend fun executePlaySongAction(
        query: String,
        playerConnection: PlayerConnection?,
        source: InputSource = InputSource.TEXT,
        onRespond: suspend (String, InputSource) -> Unit,
    ) {
        val songResult =
            withContext(Dispatchers.IO) {
                val songSearch =
                    runCatching {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.firstOrNull() as? SongItem
                    }.getOrNull()

                if (songSearch != null) {
                    songSearch
                } else {
                    runCatching {
                        YouTube.searchSummary(query).getOrNull()?.summaries
                            ?.firstOrNull()?.items?.firstOrNull()
                    }.getOrNull()
                }
            }

        if (songResult != null) {
            val videoId = songResult.id
            val title = songResult.title

            withContext(Dispatchers.Main) {
                playerConnection?.playQueue(
                    YouTubeQueue(WatchEndpoint(videoId = videoId)),
                )
            }
            onRespond("Playing \"$title\" on Echofy now! 🎵", source)
        } else {
            onRespond("Couldn't find \"$query\" right now. Try another song?", source)
        }
    }
}
