package com.Chenkham.Echofy.ui.component

import android.content.Context
import android.widget.Toast
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.spotify.SpotifyPlaybackResolver
import com.arturo254.opentune.spotify.Spotify
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles asynchronous room creation, playlist loading, and session joining for Echofy Jam.
 */
class JamSessionLauncher(
    private val scope: CoroutineScope,
    private val context: Context,
    private val playerConnection: PlayerConnection,
    private val database: MusicDatabase,
    private val onWorkingState: (isWorking: Boolean, message: String?) -> Unit,
    private val onError: (String?) -> Unit,
    private val onSessionStarted: () -> Unit,
) {
    private val jamSessionManager = playerConnection.service.jamSessionManager

    fun launchFromCurrentQueue(hostDisplayName: String) {
        scope.launch {
            onWorkingState(true, "Creating your room...")
            onError(null)
            val playbackQueue = playerConnection.service.currentPlaybackQueueForJam()
            if (playbackQueue.isEmpty()) {
                onError("Play a song first, then start the Jam from your current queue.")
                onWorkingState(false, null)
                return@launch
            }
            jamSessionManager.createHostedSession(hostDisplayName.ifBlank { "Host" })
                .onSuccess { activeSession ->
                    playerConnection.service.seedJamQueue(playbackQueue.drop(1))
                    playerConnection.service.syncJamStateNow()
                    onSessionStarted()
                    Toast.makeText(context, "Room ${activeSession.roomCode.roomCode} is live", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    onError(error.message ?: "Unable to start this Jam")
                }
            onWorkingState(false, null)
        }
    }

    fun launchFromPlaylist(playlist: Playlist, hostDisplayName: String) {
        scope.launch {
            onWorkingState(true, "Creating your room...")
            onError(null)
            val songs = database.playlistSongs(playlist.playlist.id).first().map { it.song.toMediaMetadata() }
            if (songs.isEmpty()) {
                onError("That playlist is empty.")
                onWorkingState(false, null)
                return@launch
            }
            jamSessionManager.createHostedSession(hostDisplayName.ifBlank { "Host" })
                .onSuccess { activeSession ->
                    playerConnection.service.replacePlayerQueueForJam(songs)
                    playerConnection.service.seedJamQueue(songs.drop(1))
                    playerConnection.service.syncJamStateNow()
                    onSessionStarted()
                    Toast.makeText(context, "Room ${activeSession.roomCode.roomCode} is live", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    onError(error.message ?: "Unable to start this Jam")
                }
            onWorkingState(false, null)
        }
    }

    fun launchFromSpotifyPlaylist(spotifyPlaylist: SpotifyPlaylist, hostDisplayName: String) {
        scope.launch {
            onWorkingState(true, "Fetching Spotify tracks...")
            onError(null)
            val paging = withContext(Dispatchers.IO) { Spotify.playlistTracks(spotifyPlaylist.id).getOrNull() }
            val tracks = paging?.items?.mapNotNull { it.track } ?: emptyList()
            if (tracks.isEmpty()) {
                onError("That playlist is empty or could not be loaded.")
                onWorkingState(false, null)
                return@launch
            }
            onWorkingState(true, "Resolving music...")
            val songs = tracks.take(50).mapNotNull { track -> SpotifyPlaybackResolver.resolveToMetadata(track) }
            jamSessionManager.createHostedSession(hostDisplayName.ifBlank { "Host" })
                .onSuccess { activeSession ->
                    playerConnection.service.replacePlayerQueueForJam(songs)
                    playerConnection.service.seedJamQueue(songs.drop(1))
                    playerConnection.service.syncJamStateNow()
                    onSessionStarted()
                    Toast.makeText(context, "Room ${activeSession.roomCode.roomCode} is live", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    onError(error.message ?: "Unable to start this Jam")
                }
            onWorkingState(false, null)
        }
    }

    fun joinRoom(roomCode: String, guestDisplayName: String) {
        scope.launch {
            onError(null)
            onWorkingState(true, "Finding room...")
            jamSessionManager.joinSession(
                rawRoomCode = roomCode,
                displayName = guestDisplayName.ifBlank { "Listener" },
            ).onSuccess {
                onSessionStarted()
                Toast.makeText(context, "Joined ${it.roomCode.roomCode}", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                onError(error.message ?: "Room not found")
            }
            onWorkingState(false, null)
        }
    }
}
