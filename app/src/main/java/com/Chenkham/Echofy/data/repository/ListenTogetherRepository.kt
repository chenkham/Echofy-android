package com.Chenkham.Echofy.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.Chenkham.Echofy.data.models.ChatMessage
import com.Chenkham.Echofy.data.models.ListeningRoom
import com.Chenkham.Echofy.data.models.QueueItem
import com.Chenkham.Echofy.data.remote.AppwriteClient
import com.Chenkham.Echofy.data.remote.JamSessionManager
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Singleton repository for Listen Together room operations.
 * Session persists across navigation until user explicitly ends it.
 */
object ListenTogetherRepository {
    
    private var context: Context? = null
    private val databases get() = context?.let { AppwriteClient.getDatabases(it) }
    private val prefs: SharedPreferences? get() = context?.getSharedPreferences("listen_together", Context.MODE_PRIVATE)
    
    private val _currentRoom = MutableStateFlow<ListeningRoom?>(null)
    val currentRoom: StateFlow<ListeningRoom?> = _currentRoom
    
    private val _participants = MutableStateFlow<List<String>>(emptyList())
    val participants: StateFlow<List<String>> = _participants
    
    private val _participantNames = MutableStateFlow<List<String>>(emptyList())
    val participantNames: StateFlow<List<String>> = _participantNames
    
    private val _sessionQueue = MutableStateFlow<List<QueueItem>>(emptyList())
    val sessionQueue: StateFlow<List<QueueItem>> = _sessionQueue
    
    // Chat messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private const val TAG = "ListenTogetherRepo"
    private const val POLL_INTERVAL_MS = 2000L
    private const val PREF_ROOM_CODE = "current_room_code"
    private const val PREF_USER_ID = "current_user_id"
    private const val PREF_IS_HOST = "is_host"
    private const val PREF_USER_NAME = "user_display_name"
    private const val PREF_PERSISTENT_USER_ID = "persistent_user_id"
    
    /**
     * Get the saved user display name for this session.
     */
    val savedUserName: String?
        get() = prefs?.getString(PREF_USER_NAME, null)
    
    /**
     * Get or create a persistent user ID that survives app restarts.
     * This prevents duplicate participants when users close and reopen the app.
     */
    fun getOrCreateUserId(accountEmail: String?, isLoggedIn: Boolean): String {
        // If logged in, use account email hash for consistent ID
        if (isLoggedIn && !accountEmail.isNullOrBlank()) {
            return accountEmail.hashCode().let { kotlin.math.abs(it) }.toString(36)
        }
        
        // For guests, use persistent ID from preferences
        val existingId = prefs?.getString(PREF_PERSISTENT_USER_ID, null)
        if (existingId != null) {
            return existingId
        }
        
        // Generate new persistent ID for first-time guests
        val newId = "guest_${System.currentTimeMillis().toString(36)}"
        prefs?.edit()?.putString(PREF_PERSISTENT_USER_ID, newId)?.apply()
        return newId
    }
    
    /**
     * Check if the current user is the host of the active session.
     */
    val isCurrentUserHost: Boolean
        get() = prefs?.getBoolean(PREF_IS_HOST, false) ?: false
    
    /**
     * Get the current user's ID from saved session.
     */
    val currentUserId: String?
        get() = prefs?.getString(PREF_USER_ID, null)
    
    fun init(ctx: Context) {
        if (context == null) {
            context = ctx.applicationContext
        }
    }
    
    /**
     * Check for saved session on startup.
     */
    suspend fun restoreSession(userId: String): ListeningRoom? {
        val savedRoomCode = prefs?.getString(PREF_ROOM_CODE, null)
        if (savedRoomCode != null && _currentRoom.value == null) {
            when (val result = fetchRoom(savedRoomCode)) {
                is FetchResult.Success -> {
                    val room = result.room
                    if (userId in room.participants) {
                        _currentRoom.value = room
                        _participants.value = room.participants
                        _participantNames.value = room.participantNames
                        startPolling(savedRoomCode) {}
                        Log.d(TAG, "Session restored: $savedRoomCode")
                        return room
                    } else {
                        // User was removed from the room
                        Log.d(TAG, "User no longer in room, clearing session")
                        clearSavedSession()
                    }
                }
                is FetchResult.RoomNotFound -> {
                    // Room was deleted
                    Log.d(TAG, "Room not found during restore, clearing session")
                    clearSavedSession()
                }
                is FetchResult.NetworkError -> {
                    // Network error - keep the saved session, try to start polling anyway
                    Log.w(TAG, "Network error during restore, keeping saved session")
                    startPolling(savedRoomCode) {}
                    // Return null but don't clear - polling will restore when network is back
                }
            }
        }
        return _currentRoom.value
    }
    
    private fun saveSession(roomCode: String, userId: String, isHost: Boolean, userName: String? = null) {
        prefs?.edit()
            ?.putString(PREF_ROOM_CODE, roomCode)
            ?.putString(PREF_USER_ID, userId)
            ?.putBoolean(PREF_IS_HOST, isHost)
            ?.apply { userName?.let { putString(PREF_USER_NAME, it) } }
            ?.apply()
    }
    
    private fun clearSavedSession() {
        prefs?.edit()?.clear()?.apply()
    }
    
    /**
     * Create a new listening room.
     */
    suspend fun createRoom(
        roomCode: String,
        hostId: String,
        hostName: String,
        hostEmail: String?,
        isHostConnected: Boolean,
        controlMode: String = "HOST_ONLY"
    ): Result<ListeningRoom> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        try {
            val room = ListeningRoom(
                roomCode = roomCode,
                hostId = hostId,
                hostName = hostName,
                hostEmail = hostEmail,
                isHostConnected = isHostConnected,
                controlMode = controlMode,
                participants = listOf(hostId),
                participantNames = listOf(hostName),
                sessionStartTime = System.currentTimeMillis(),
                lastActivity = System.currentTimeMillis()
            )
            
            db.createDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode,
                data = mapOf(
                    "roomCode" to room.roomCode,
                    "hostId" to room.hostId,
                    "hostName" to room.hostName,
                    "hostEmail" to (room.hostEmail ?: ""),
                    "isHostConnected" to room.isHostConnected,
                    "controlMode" to room.controlMode,
                    "currentTrackId" to "",
                    "currentTrackTitle" to "",
                    "currentTrackArtist" to "",
                    "currentTrackThumbnail" to "",
                    "playbackPosition" to 0L,
                    "isPlaying" to false,
                    "startedAt" to 0L,
                    "playbackSpeed" to 1.0f,
                    "participants" to Json.encodeToString(room.participants),
                    "participantNames" to Json.encodeToString(room.participantNames),
                    "reactions" to "[]",
                    "sessionStartTime" to room.sessionStartTime,
                    "lastActivity" to room.lastActivity
                )
            )
            
            _currentRoom.value = room
            _participants.value = room.participants
            _participantNames.value = room.participantNames
            saveSession(roomCode, hostId, true, hostName)
            
            // Subscribe to realtime updates
            JamSessionManager.init(context!!)
            JamSessionManager.setCurrentRoom(room)
            JamSessionManager.subscribeToSession(roomCode)
            
            Log.d(TAG, "Room created: $roomCode")
            Result.success(room)
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to create room: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Join an existing room by code.
     */
    suspend fun joinRoom(
        roomCode: String,
        participantId: String,
        participantName: String
    ): Result<ListeningRoom> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        try {
            val document = db.getDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode
            )
            
            val data = document.data
            val currentParticipants = try {
                Json.decodeFromString<List<String>>(data["participants"] as? String ?: "[]")
            } catch (e: Exception) { emptyList() }
            
            val currentNames = try {
                Json.decodeFromString<List<String>>(data["participantNames"] as? String ?: "[]")
            } catch (e: Exception) { emptyList() }
            
            // Check if participant already exists - if so, update their name instead of adding duplicate
            val existingIndex = currentParticipants.indexOf(participantId)
            val (updatedParticipants, updatedNames) = if (existingIndex == -1) {
                // New participant - add them
                Log.d(TAG, "Adding new participant: $participantName")
                Pair(currentParticipants + participantId, currentNames + participantName)
            } else {
                // Existing participant - just update their name if it changed
                Log.d(TAG, "Participant already exists, updating name: $participantName")
                val mutableNames = currentNames.toMutableList()
                if (existingIndex < mutableNames.size) {
                    mutableNames[existingIndex] = participantName
                }
                Pair(currentParticipants, mutableNames)
            }
            
            db.updateDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode,
                data = mapOf(
                    "participants" to Json.encodeToString(updatedParticipants),
                    "participantNames" to Json.encodeToString(updatedNames),
                    "lastActivity" to System.currentTimeMillis()
                )
            )
            
            val room = parseRoomFromData(data, roomCode, updatedParticipants, updatedNames)
            
            _currentRoom.value = room
            _participants.value = updatedParticipants
            _participantNames.value = updatedNames
            saveSession(roomCode, participantId, false, participantName)
            
            // Subscribe to realtime updates
            JamSessionManager.init(context!!)
            JamSessionManager.setCurrentRoom(room)
            JamSessionManager.subscribeToSession(roomCode)
            
            Log.d(TAG, "Joined room: $roomCode")
            Result.success(room)
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to join room: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Leave the current room - ONLY when user explicitly clicks End/Leave.
     */
    suspend fun leaveRoom(
        roomCode: String,
        participantId: String,
        isHost: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        try {
            val room = when (val result = fetchRoom(roomCode)) {
                is FetchResult.Success -> result.room
                else -> null
            }
            val controlMode = room?.controlMode ?: "HOST_ONLY"
            
            if (isHost && controlMode == "HOST_ONLY") {
                deleteRoom(roomCode)
            } else {
                try {
                    val document = db.getDocument(
                        databaseId = AppwriteClient.DATABASE_ID,
                        collectionId = AppwriteClient.COLLECTION_ROOMS,
                        documentId = roomCode
                    )
                    
                    val data = document.data
                    val currentParticipants = try {
                        Json.decodeFromString<List<String>>(data["participants"] as? String ?: "[]")
                    } catch (e: Exception) { emptyList() }
                    
                    val currentNames = try {
                        Json.decodeFromString<List<String>>(data["participantNames"] as? String ?: "[]")
                    } catch (e: Exception) { emptyList() }
                    
                    val indexToRemove = currentParticipants.indexOf(participantId)
                    val updatedParticipants = currentParticipants - participantId
                    val updatedNames = if (indexToRemove >= 0 && indexToRemove < currentNames.size) {
                        currentNames.toMutableList().apply { removeAt(indexToRemove) }
                    } else {
                        currentNames
                    }
                    
                    if (updatedParticipants.isEmpty()) {
                        deleteRoom(roomCode)
                    } else if (isHost) {
                        val newHostId = updatedParticipants.first()
                        val newHostName = updatedNames.firstOrNull() ?: "Unknown"
                        db.updateDocument(
                            databaseId = AppwriteClient.DATABASE_ID,
                            collectionId = AppwriteClient.COLLECTION_ROOMS,
                            documentId = roomCode,
                            data = mapOf(
                                "hostId" to newHostId,
                                "hostName" to newHostName,
                                "participants" to Json.encodeToString(updatedParticipants),
                                "participantNames" to Json.encodeToString(updatedNames),
                                "lastActivity" to System.currentTimeMillis()
                            )
                        )
                    } else {
                        db.updateDocument(
                            databaseId = AppwriteClient.DATABASE_ID,
                            collectionId = AppwriteClient.COLLECTION_ROOMS,
                            documentId = roomCode,
                            data = mapOf(
                                "participants" to Json.encodeToString(updatedParticipants),
                                "participantNames" to Json.encodeToString(updatedNames),
                                "lastActivity" to System.currentTimeMillis()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating: ${e.message}")
                }
            }
            
            stopPolling()
            clearSavedSession()
            JamSessionManager.clear() // Clear realtime subscription
            _currentRoom.value = null
            _participants.value = emptyList()
            _participantNames.value = emptyList()
            
            Log.d(TAG, "Left room: $roomCode")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave: ${e.message}")
            Result.failure(e)
        }
    }
    
    private suspend fun deleteRoom(roomCode: String) {
        try {
            databases?.deleteDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode
            )
            Log.d(TAG, "Room deleted: $roomCode")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete: ${e.message}")
        }
    }
    
    /**
     * Update playback state.
     */
    suspend fun updatePlaybackState(
        roomCode: String,
        userId: String,
        trackId: String?,
        trackTitle: String?,
        trackArtist: String?,
        trackThumbnail: String?,
        playbackPosition: Long,
        isPlaying: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        try {
            val room = _currentRoom.value
            val canControl = room?.controlMode == "EVERYONE" || room?.hostId == userId
            
            if (!canControl) {
                return@withContext Result.failure(Exception("Not authorized"))
            }
            
            // CRITICAL: Set startedAt to current time when starting playback
            // This allows all clients to calculate their seek position
            val startedAt = if (isPlaying) System.currentTimeMillis() - playbackPosition else 0L
            
            db.updateDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode,
                data = mapOf(
                    "currentTrackId" to (trackId ?: ""),
                    "currentTrackTitle" to (trackTitle ?: ""),
                    "currentTrackArtist" to (trackArtist ?: ""),
                    "currentTrackThumbnail" to (trackThumbnail ?: ""),
                    "playbackPosition" to playbackPosition,
                    "isPlaying" to isPlaying,
                    "startedAt" to startedAt,
                    "lastActivity" to System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Playback updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update playback: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send a reaction.
     */
    suspend fun sendReaction(
        roomCode: String,
        emoji: String,
        senderId: String,
        senderName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        try {
            val reaction = "$emoji|$senderId|$senderName|${System.currentTimeMillis()}"
            val room = when (val result = fetchRoom(roomCode)) {
                is FetchResult.Success -> result.room
                else -> null
            }
            val currentReactions = room?.reactions?.takeLast(9) ?: emptyList()
            val updatedReactions = currentReactions + reaction
            
            db.updateDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode,
                data = mapOf(
                    "reactions" to Json.encodeToString(updatedReactions),
                    "lastActivity" to System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Reaction sent: $emoji")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reaction: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Start polling for room updates.
     */
    fun startPolling(roomCode: String, onUpdate: (ListeningRoom) -> Unit) {
        if (pollingJob?.isActive == true) return // Already polling
        
        pollingJob = scope.launch {
            Log.d(TAG, "Started polling: $roomCode")
            var consecutiveErrors = 0
            val maxConsecutiveErrors = 10 // Allow 10 consecutive errors (20 seconds) before giving up
            
            while (isActive) {
                try {
                    when (val result = fetchRoom(roomCode)) {
                        is FetchResult.Success -> {
                            consecutiveErrors = 0 // Reset error counter on success
                            val room = result.room
                            _currentRoom.value = room
                            _participants.value = room.participants
                            _participantNames.value = room.participantNames
                            
                            // Check if host has changed and update local isHost preference
                            val currentUserId = prefs?.getString(PREF_USER_ID, null)
                            if (currentUserId != null) {
                                val isNowHost = room.hostId == currentUserId
                                val wasHost = prefs?.getBoolean(PREF_IS_HOST, false) ?: false
                                if (isNowHost != wasHost) {
                                    prefs?.edit()?.putBoolean(PREF_IS_HOST, isNowHost)?.apply()
                                    Log.d(TAG, "Host status updated: isHost=$isNowHost")
                                }
                            }
                            
                            onUpdate(room)
                        }
                        is FetchResult.RoomNotFound -> {
                            // Room was explicitly deleted (404) - clear session
                            Log.d(TAG, "Room deleted (404), clearing session")
                            clearSavedSession()
                            _currentRoom.value = null
                            stopPolling()
                            break
                        }
                        is FetchResult.NetworkError -> {
                            // Temporary network error - keep trying
                            consecutiveErrors++
                            Log.w(TAG, "Network error during polling (attempt $consecutiveErrors): ${result.message}")
                            
                            // Only give up after too many consecutive errors
                            if (consecutiveErrors >= maxConsecutiveErrors) {
                                Log.e(TAG, "Too many consecutive errors, but keeping session alive")
                                // Don't clear session - just log the issue
                                // User can manually leave if needed
                            }
                        }
                    }
                } catch (e: Exception) {
                    consecutiveErrors++
                    Log.e(TAG, "Polling error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }
    
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Stopped polling")
    }
    
    /**
     * Result type for fetch operations to distinguish between different failure modes.
     */
    private sealed class FetchResult {
        data class Success(val room: ListeningRoom) : FetchResult()
        object RoomNotFound : FetchResult() // 404 - room was deleted
        data class NetworkError(val message: String) : FetchResult() // Temporary error, retry
    }
    
    private suspend fun fetchRoom(roomCode: String): FetchResult = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext FetchResult.NetworkError("Not initialized")
        try {
            val document = db.getDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode
            )
            
            val data = document.data
            val participants = try {
                Json.decodeFromString<List<String>>(data["participants"] as? String ?: "[]")
            } catch (e: Exception) { emptyList() }
            
            val participantNames = try {
                Json.decodeFromString<List<String>>(data["participantNames"] as? String ?: "[]")
            } catch (e: Exception) { emptyList() }
            
            FetchResult.Success(parseRoomFromData(data, roomCode, participants, participantNames))
        } catch (e: AppwriteException) {
            // Check if it's a 404 (room not found/deleted)
            if (e.code == 404) {
                Log.d(TAG, "Room not found (404): $roomCode")
                FetchResult.RoomNotFound
            } else {
                Log.e(TAG, "Appwrite error fetching room: ${e.code} - ${e.message}")
                FetchResult.NetworkError(e.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error fetching room: ${e.message}")
            FetchResult.NetworkError(e.message ?: "Unknown error")
        }
    }
    
    suspend fun roomExists(roomCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            databases?.getDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun parseRoomFromData(
        data: Map<String, Any>,
        roomCode: String,
        participants: List<String>,
        participantNames: List<String>
    ): ListeningRoom {
        val reactions = try {
            Json.decodeFromString<List<String>>(data["reactions"] as? String ?: "[]")
        } catch (e: Exception) { emptyList() }
        
        return ListeningRoom(
            roomCode = data["roomCode"] as? String ?: roomCode,
            hostId = data["hostId"] as? String ?: "",
            hostName = data["hostName"] as? String ?: "",
            hostEmail = data["hostEmail"] as? String,
            isHostConnected = data["isHostConnected"] as? Boolean ?: true,
            controlMode = data["controlMode"] as? String ?: "HOST_ONLY",
            currentTrackId = data["currentTrackId"] as? String,
            currentTrackTitle = data["currentTrackTitle"] as? String,
            currentTrackArtist = data["currentTrackArtist"] as? String,
            currentTrackThumbnail = data["currentTrackThumbnail"] as? String,
            playbackPosition = (data["playbackPosition"] as? Number)?.toLong() ?: 0L,
            isPlaying = data["isPlaying"] as? Boolean ?: false,
            startedAt = (data["startedAt"] as? Number)?.toLong() ?: 0L,
            playbackSpeed = (data["playbackSpeed"] as? Number)?.toFloat() ?: 1.0f,
            participants = participants,
            participantNames = participantNames,
            reactions = reactions,
            sessionStartTime = (data["sessionStartTime"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            lastActivity = (data["lastActivity"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
    
    // ==================== HOST TRANSFER ====================
    
    /**
     * Transfer host privileges to another participant.
     * Only the current host can transfer.
     */
    suspend fun transferHost(
        roomCode: String,
        currentHostId: String,
        newHostId: String,
        newHostName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        
        try {
            // Verify caller is current host
            val room = _currentRoom.value
            if (room?.hostId != currentHostId) {
                return@withContext Result.failure(Exception("Only the host can transfer control"))
            }
            
            db.updateDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_ROOMS,
                documentId = roomCode,
                data = mapOf(
                    "hostId" to newHostId,
                    "hostName" to newHostName,
                    "lastActivity" to System.currentTimeMillis()
                )
            )
            
            // Update local state
            _currentRoom.value = room.copy(
                hostId = newHostId,
                hostName = newHostName
            )
            
            // CRITICAL: Update local isHost preference - old host is no longer host
            prefs?.edit()?.putBoolean(PREF_IS_HOST, false)?.apply()
            
            Log.d(TAG, "Host transferred from $currentHostId to $newHostId ($newHostName)")
            Result.success(Unit)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to transfer host: ${e.message}")
            Result.failure(e)
        }
    }
    
    // ==================== QUEUE METHODS ====================
    
    /**
     * Add a song to the session queue.
     */
    suspend fun addToQueue(
        roomCode: String,
        trackId: String,
        trackTitle: String,
        trackArtist: String?,
        trackThumbnail: String?,
        addedBy: String,
        addedByName: String
    ): Result<QueueItem> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        
        try {
            // Get current queue to determine order
            val currentQueue = _sessionQueue.value
            val nextOrder = (currentQueue.maxOfOrNull { it.order } ?: 0) + 1
            
            val queueItem = QueueItem(
                id = "",
                sessionId = roomCode,
                trackId = trackId,
                trackTitle = trackTitle,
                trackArtist = trackArtist,
                trackThumbnail = trackThumbnail,
                addedBy = addedBy,
                addedByName = addedByName,
                order = nextOrder,
                isPlayed = false
            )
            
            val doc = db.createDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_QUEUE,
                documentId = io.appwrite.ID.unique(),
                data = mapOf(
                    "sessionId" to roomCode,
                    "trackId" to trackId,
                    "trackTitle" to trackTitle,
                    "trackArtist" to (trackArtist ?: ""),
                    "trackThumbnail" to (trackThumbnail ?: ""),
                    "addedBy" to addedBy,
                    "addedByName" to addedByName,
                    "order" to nextOrder,
                    "isPlayed" to false
                )
            )
            
            val newItem = queueItem.copy(id = doc.id)
            _sessionQueue.value = _sessionQueue.value + newItem
            
            Log.d(TAG, "Added to queue: $trackTitle by $addedByName")
            Result.success(newItem)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to add to queue: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch the session queue from Appwrite.
     */
    suspend fun fetchQueue(roomCode: String): Result<List<QueueItem>> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        
        try {
            val docs = db.listDocuments(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_QUEUE,
                queries = listOf(
                    io.appwrite.Query.equal("sessionId", roomCode),
                    io.appwrite.Query.equal("isPlayed", false),
                    io.appwrite.Query.orderAsc("order")
                )
            )
            
            val items = docs.documents.map { doc ->
                val data = doc.data
                QueueItem(
                    id = doc.id,
                    sessionId = data["sessionId"] as? String ?: roomCode,
                    trackId = data["trackId"] as? String ?: "",
                    trackTitle = data["trackTitle"] as? String ?: "",
                    trackArtist = data["trackArtist"] as? String,
                    trackThumbnail = data["trackThumbnail"] as? String,
                    addedBy = data["addedBy"] as? String ?: "",
                    addedByName = data["addedByName"] as? String ?: "",
                    order = (data["order"] as? Number)?.toInt() ?: 0,
                    isPlayed = data["isPlayed"] as? Boolean ?: false
                )
            }
            
            _sessionQueue.value = items
            Log.d(TAG, "Fetched queue: ${items.size} items")
            Result.success(items)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to fetch queue: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Remove an item from the queue.
     */
    suspend fun removeFromQueue(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        
        try {
            db.deleteDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_QUEUE,
                documentId = itemId
            )
            
            _sessionQueue.value = _sessionQueue.value.filter { it.id != itemId }
            Log.d(TAG, "Removed from queue: $itemId")
            Result.success(Unit)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to remove from queue: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Play the next song in the queue (host only).
     */
    suspend fun playNextInQueue(roomCode: String, userId: String): Result<QueueItem?> = withContext(Dispatchers.IO) {
        val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
        
        try {
            // Get next unplayed item
            val queue = _sessionQueue.value.filter { !it.isPlayed }
            val nextItem = queue.minByOrNull { it.order }
            
            if (nextItem == null) {
                Log.d(TAG, "Queue is empty")
                return@withContext Result.success(null)
            }
            
            // Mark as played
            db.updateDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_QUEUE,
                documentId = nextItem.id,
                data = mapOf("isPlayed" to true)
            )
            
            // Update session with new track
            updatePlaybackState(
                roomCode = roomCode,
                userId = userId,
                trackId = nextItem.trackId,
                trackTitle = nextItem.trackTitle,
                trackArtist = nextItem.trackArtist,
                trackThumbnail = nextItem.trackThumbnail,
                playbackPosition = 0L,
                isPlaying = true
            )
            
            // Update local queue
            _sessionQueue.value = _sessionQueue.value.map { 
                if (it.id == nextItem.id) it.copy(isPlayed = true) else it 
            }
            
            Log.d(TAG, "Playing next in queue: ${nextItem.trackTitle}")
            Result.success(nextItem)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to play next: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clear the queue when leaving.
     */
    fun clearLocalQueue() {
        _sessionQueue.value = emptyList()
    }
    
    // ============== CHAT MESSAGING ==============
    
    /**
     * Send a chat message to the session.
     */
    suspend fun sendMessage(
        roomCode: String,
        senderId: String,
        senderName: String,
        content: String
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
            
            val message = ChatMessage(
                sessionId = roomCode,
                senderId = senderId,
                senderName = senderName,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            
            val doc = db.createDocument(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_MESSAGES,
                documentId = io.appwrite.ID.unique(),
                data = mapOf(
                    "sessionId" to message.sessionId,
                    "senderId" to message.senderId,
                    "senderName" to message.senderName,
                    "content" to message.content,
                    "timestamp" to message.timestamp
                )
            )
            
            val newMessage = message.copy(id = doc.id)
            
            // Add to local list immediately
            _messages.value = _messages.value + newMessage
            
            Log.d(TAG, "Message sent: ${content.take(20)}...")
            Result.success(newMessage)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch messages for the current session.
     */
    suspend fun fetchMessages(roomCode: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val db = databases ?: return@withContext Result.failure(Exception("Not initialized"))
            
            val docs = db.listDocuments(
                databaseId = AppwriteClient.DATABASE_ID,
                collectionId = AppwriteClient.COLLECTION_MESSAGES,
                queries = listOf(
                    io.appwrite.Query.equal("sessionId", roomCode),
                    io.appwrite.Query.orderAsc("timestamp"),
                    io.appwrite.Query.limit(100)
                )
            )
            
            val messages = docs.documents.map { doc ->
                ChatMessage(
                    id = doc.id,
                    sessionId = doc.data["sessionId"]?.toString() ?: "",
                    senderId = doc.data["senderId"]?.toString() ?: "",
                    senderName = doc.data["senderName"]?.toString() ?: "Guest",
                    content = doc.data["content"]?.toString() ?: "",
                    timestamp = (doc.data["timestamp"] as? Number)?.toLong() ?: 0L
                )
            }
            
            _messages.value = messages
            Log.d(TAG, "Fetched ${messages.size} messages")
            Result.success(messages)
            
        } catch (e: AppwriteException) {
            Log.e(TAG, "Failed to fetch messages: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Clear messages when leaving.
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }
}
