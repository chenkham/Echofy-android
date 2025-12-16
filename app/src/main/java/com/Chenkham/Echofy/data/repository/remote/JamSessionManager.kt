package com.Chenkham.Echofy.data.remote

import android.content.Context
import android.util.Log
import com.Chenkham.Echofy.data.models.ListeningRoom
import io.appwrite.Client
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.RealtimeSubscription
import io.appwrite.services.Databases
import io.appwrite.services.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * Singleton manager for Jam Session real-time synchronization.
 * 
 * Handles Appwrite Realtime subscriptions for Listen Together feature.
 * Implements the proper `started_at` sync algorithm to ensure all
 * participants are synced to the correct playback position.
 */
object JamSessionManager {
    private const val TAG = "JamSessionManager"
    
    // Sync thresholds
    private const val SNAP_SYNC_THRESHOLD_MS = 200L // Snap if >200ms drift
    private const val DRIFT_CORRECTION_INTERVAL_MS = 10_000L // Check every 10 seconds
    private const val DRIFT_CORRECTION_THRESHOLD_MS = 2_000L // Correct if >2s drift
    
    private var context: Context? = null
    private var realtime: Realtime? = null
    private var subscription: RealtimeSubscription? = null
    private var driftCorrectionJob: Job? = null
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Session state exposed to UI and player
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Disconnected)
    val sessionState: StateFlow<SessionState> = _sessionState
    
    private val _currentRoom = MutableStateFlow<ListeningRoom?>(null)
    val currentRoom: StateFlow<ListeningRoom?> = _currentRoom
    
    // Callback for player sync
    private var onSyncRequired: ((SyncEvent) -> Unit)? = null
    
    /**
     * Initialize the manager with context.
     */
    fun init(ctx: Context) {
        if (context == null) {
            context = ctx.applicationContext
            realtime = AppwriteClient.getRealtime(ctx)
        }
    }
    
    /**
     * Set callback for sync events. Called from PlayerConnection/MusicService.
     */
    fun setOnSyncRequired(callback: (SyncEvent) -> Unit) {
        onSyncRequired = callback
    }
    
    /**
     * Subscribe to a session's real-time updates.
     * 
     * @param sessionId The session/room code to subscribe to
     */
    fun subscribeToSession(sessionId: String) {
        val rt = realtime ?: run {
            Log.e(TAG, "Realtime not initialized")
            return
        }
        
        // Unsubscribe from any existing subscription
        unsubscribe()
        
        _sessionState.value = SessionState.Connecting
        
        val channel = "databases.${AppwriteClient.DATABASE_ID}.collections.${AppwriteClient.COLLECTION_ROOMS}.documents.$sessionId"
        
        Log.d(TAG, "Subscribing to channel: $channel")
        
        try {
            subscription = rt.subscribe(channel) { message ->
                Log.d(TAG, "Received realtime event: ${message.events}")
                @Suppress("UNCHECKED_CAST")
                val payload = message.payload as? Map<String, Any> ?: return@subscribe
                handleRealtimeEvent(payload)
            }
            
            _sessionState.value = SessionState.Connected(sessionId)
            Log.d(TAG, "Successfully subscribed to session: $sessionId")
            
            // Start drift correction
            startDriftCorrection()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe: ${e.message}")
            _sessionState.value = SessionState.Error(e.message ?: "Subscription failed")
        }
    }
    
    /**
     * Parse and handle realtime payload.
     */
    private fun handleRealtimeEvent(payload: Map<String, Any>) {
        try {
            // Parse the payload safely
            val isPlaying = payload["isPlaying"] as? Boolean ?: false
            val startedAt = (payload["startedAt"] as? Number)?.toLong() 
                ?: (payload["sessionStartTime"] as? Number)?.toLong()
                ?: 0L
            val currentTrackId = payload["currentTrackId"] as? String
            val currentTrackTitle = payload["currentTrackTitle"] as? String
            val currentTrackArtist = payload["currentTrackArtist"] as? String
            val currentTrackThumbnail = payload["currentTrackThumbnail"] as? String
            val playbackPosition = (payload["playbackPosition"] as? Number)?.toLong() ?: 0L
            
            // Update current room state
            val room = _currentRoom.value?.copy(
                isPlaying = isPlaying,
                startedAt = startedAt,
                currentTrackId = currentTrackId,
                currentTrackTitle = currentTrackTitle,
                currentTrackArtist = currentTrackArtist,
                currentTrackThumbnail = currentTrackThumbnail,
                playbackPosition = playbackPosition
            )
            _currentRoom.value = room
            
            // Calculate sync event
            val syncEvent = if (isPlaying && startedAt > 0) {
                val expectedPosition = System.currentTimeMillis() - startedAt
                SyncEvent.Play(
                    trackId = currentTrackId,
                    trackTitle = currentTrackTitle,
                    trackArtist = currentTrackArtist,
                    trackThumbnail = currentTrackThumbnail,
                    expectedPosition = expectedPosition.coerceAtLeast(0),
                    startedAt = startedAt
                )
            } else {
                SyncEvent.Pause
            }
            
            Log.d(TAG, "Sync event: $syncEvent")
            
            // Dispatch to player
            onSyncRequired?.invoke(syncEvent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing realtime event: ${e.message}")
        }
    }
    
    /**
     * Start periodic drift correction.
     */
    private fun startDriftCorrection() {
        driftCorrectionJob?.cancel()
        driftCorrectionJob = scope.launch {
            while (isActive) {
                delay(DRIFT_CORRECTION_INTERVAL_MS)
                
                val room = _currentRoom.value
                if (room != null && room.isPlaying && room.startedAt > 0) {
                    val expectedPosition = System.currentTimeMillis() - room.startedAt
                    
                    // Dispatch drift check event
                    onSyncRequired?.invoke(
                        SyncEvent.DriftCheck(
                            expectedPosition = expectedPosition.coerceAtLeast(0),
                            threshold = DRIFT_CORRECTION_THRESHOLD_MS
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Update session state (for host to broadcast changes).
     */
    suspend fun updateSessionState(
        sessionId: String,
        trackId: String?,
        trackTitle: String?,
        trackArtist: String?,
        trackThumbnail: String?,
        isPlaying: Boolean
    ): Result<Unit> {
        val ctx = context ?: return Result.failure(Exception("Not initialized"))
        val db = AppwriteClient.getDatabases(ctx)
        
        return try {
            val startedAt = if (isPlaying) System.currentTimeMillis() else 0L
            
            db.updateDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = sessionId,
                data = mapOf(
                    "currentTrackId" to (trackId ?: ""),
                    "currentTrackTitle" to (trackTitle ?: ""),
                    "currentTrackArtist" to (trackArtist ?: ""),
                    "currentTrackThumbnail" to (trackThumbnail ?: ""),
                    "isPlaying" to isPlaying,
                    "startedAt" to startedAt,
                    "lastActivity" to System.currentTimeMillis()
                )
            )
            
            Log.d(TAG, "Session updated: isPlaying=$isPlaying, startedAt=$startedAt")
            Result.success(Unit)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to update session: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Set the current room (called when joining/creating).
     */
    fun setCurrentRoom(room: ListeningRoom) {
        _currentRoom.value = room
    }
    
    /**
     * Unsubscribe from current session.
     */
    fun unsubscribe() {
        try {
            subscription?.close()
            subscription = null
            driftCorrectionJob?.cancel()
            driftCorrectionJob = null
            _sessionState.value = SessionState.Disconnected
            Log.d(TAG, "Unsubscribed")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing: ${e.message}")
        }
    }
    
    /**
     * Clear all state (called when leaving session).
     */
    fun clear() {
        unsubscribe()
        _currentRoom.value = null
        onSyncRequired = null
    }
    
    /**
     * Calculate expected playback position from started_at timestamp.
     */
    fun calculateExpectedPosition(startedAt: Long): Long {
        return (System.currentTimeMillis() - startedAt).coerceAtLeast(0)
    }
    
    /**
     * Check if position drift exceeds threshold.
     */
    fun shouldSnapSync(currentPosition: Long, expectedPosition: Long): Boolean {
        return abs(currentPosition - expectedPosition) > SNAP_SYNC_THRESHOLD_MS
    }
    
    /**
     * Check if drift correction is needed.
     */
    fun needsDriftCorrection(currentPosition: Long, expectedPosition: Long): Boolean {
        return abs(currentPosition - expectedPosition) > DRIFT_CORRECTION_THRESHOLD_MS
    }
}

/**
 * Represents the connection state to the session.
 */
sealed class SessionState {
    object Disconnected : SessionState()
    object Connecting : SessionState()
    data class Connected(val sessionId: String) : SessionState()
    data class Error(val message: String) : SessionState()
}

/**
 * Events dispatched to the player for synchronization.
 */
sealed class SyncEvent {
    data class Play(
        val trackId: String?,
        val trackTitle: String?,
        val trackArtist: String?,
        val trackThumbnail: String?,
        val expectedPosition: Long,
        val startedAt: Long
    ) : SyncEvent()
    
    object Pause : SyncEvent()
    
    data class DriftCheck(
        val expectedPosition: Long,
        val threshold: Long
    ) : SyncEvent()
    
    data class TrackChange(
        val trackId: String,
        val trackTitle: String?,
        val trackArtist: String?,
        val trackThumbnail: String?,
        val startedAt: Long
    ) : SyncEvent()
}
