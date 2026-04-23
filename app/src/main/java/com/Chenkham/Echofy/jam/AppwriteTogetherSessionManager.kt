package com.Chenkham.Echofy.jam

import android.content.Context
import com.Chenkham.Echofy.appwrite.AppwriteClientProvider
import com.Chenkham.Echofy.appwrite.AppwriteConfig
import com.Chenkham.Echofy.appwrite.AppwriteSessionAuth
import com.Chenkham.Echofy.models.MediaMetadata
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.RealtimeSubscription
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Lean Together session manager — no queue, no separate playback collection.
 *
 * Everything lives in ONE Appwrite document (together_rooms/{roomId}):
 *   - Room meta: roomCode, hostParticipantId, allowGuestControls, status
 *   - Current playback: mediaId, title, artist, thumbnailUrl, durationSeconds,
 *                       playbackState, positionMs, updatedAtEpochMs, stateVersion
 *
 * Writes per action:
 *   - Play/Pause/Seek/Next/Prev/Song change → 1 update to the room doc
 *   - Heartbeat → 1 update to presence doc every 15s
 *
 * Guests subscribe to the room doc via Realtime and apply whatever the host writes.
 * If guest controls are enabled, a guest write also updates the same doc.
 */
class AppwriteTogetherSessionManager(
    context: Context,
    private val scope: CoroutineScope,
    private val configRepository: JamConfigRepository = JamRemoteConfigRepository(context),
) {
    private val appContext = context.applicationContext
    private val participantId by lazy { loadOrCreateParticipantId() }

    private val _sessionState  = MutableStateFlow(JamSessionState())
    val sessionState: StateFlow<JamSessionState> = _sessionState.asStateFlow()

    private val _remotePlayback = MutableStateFlow<JamPlaybackSnapshot?>(null)
    val remotePlayback: StateFlow<JamPlaybackSnapshot?> = _remotePlayback.asStateFlow()

    // Queue is gone — kept as empty stub so MusicService compiles unchanged
    private val _queueSnapshot = MutableStateFlow<List<JamQueueItem>>(emptyList())
    val queueSnapshot: StateFlow<List<JamQueueItem>> = _queueSnapshot.asStateFlow()

    private val _participants = MutableStateFlow<List<JamParticipant>>(emptyList())
    val participants: StateFlow<List<JamParticipant>> = _participants.asStateFlow()

    private val _roomMeta = MutableStateFlow<JamRoomMeta?>(null)
    val roomMeta: StateFlow<JamRoomMeta?> = _roomMeta.asStateFlow()

    private val _serverTimeOffsetMs = MutableStateFlow(0L)
    val serverTimeOffsetMs: StateFlow<Long> = _serverTimeOffsetMs.asStateFlow()

    val registry: StateFlow<JamRegistry> = configRepository.registry

    private val stateVersion = AtomicLong(0L)

    private var roomSub: RealtimeSubscription? = null
    private var presenceSub: RealtimeSubscription? = null

    private var presenceHeartbeatJob: Job? = null
    private var guestJoinValidationJob: Job? = null
    private var guestHealthJob: Job? = null

    private var latestRoomMeta: JamRoomMeta? = null
    private var pendingGuestActivation: JamActiveSession? = null
    private var currentRoomDocId: String? = null
    private var currentRoomId: String? = null
    private val roomPermissions = listOf(
        Permission.read(Role.any()),
        Permission.write(Role.any()),
        Permission.delete(Role.any()),
    )
    private val presencePermissions = listOf(
        Permission.read(Role.any()),
        Permission.write(Role.any()),
        Permission.delete(Role.any()),
    )

    init {
        scope.launch { configRepository.refresh(forceRefresh = false) }
    }

    fun refreshRegistry(forceRefresh: Boolean = true) {
        scope.launch { configRepository.refresh(forceRefresh = forceRefresh) }
    }

    // ─── Session lifecycle ────────────────────────────────────────────────

    suspend fun createHostedSession(displayName: String? = null): Result<JamActiveSession> {
        val shard = registry.value.pickShardForNewRoom()
            ?: return Result.failure(IllegalStateException("No active shard configured"))
        repeat(HOST_CREATE_MAX_ATTEMPTS) { attempt ->
            val roomCode = JamRoomCode.create(shard.id)
            val session = JamActiveSession(
                roomCode = roomCode,
                participantId = participantId,
                role = JamParticipantRole.HOST,
                displayName = displayName,
            )
            val result = attachToSession(session)
            val failure = result.exceptionOrNull()
            if (failure !is RoomIdConflictException) return result
            Timber.tag(TAG).w(
                failure,
                "Room id collision for %s (attempt %d/%d)",
                roomCode.roomId,
                attempt + 1,
                HOST_CREATE_MAX_ATTEMPTS,
            )
        }
        return Result.failure(IllegalStateException("Unable to allocate a unique room id"))
    }

    suspend fun joinSession(rawRoomCode: String, displayName: String? = null): Result<JamActiveSession> {
        val roomCode = JamRoomCode.parse(rawRoomCode)
            ?: return Result.failure(IllegalArgumentException("Invalid room code"))
        val shard = registry.value.findShard(roomCode.shardId)
            ?: return Result.failure(IllegalStateException("Shard ${roomCode.shardId} not configured"))
        if (!shard.canJoinRooms)
            return Result.failure(IllegalStateException("Shard not accepting joins"))
        val session = JamActiveSession(
            roomCode = roomCode,
            participantId = participantId,
            role = JamParticipantRole.GUEST,
            displayName = displayName,
        )
        return attachToSession(session)
    }

    fun leaveSession() {
        val session = _sessionState.value.session
        if (session?.role == JamParticipantRole.HOST) {
            scope.launch(Dispatchers.IO) { markRoomClosed() }
        } else if (session != null) {
            scope.launch(Dispatchers.IO) { deletePresence(session) }
        }
        teardown()
        _sessionState.value  = JamSessionState()
        _remotePlayback.value = null
        _queueSnapshot.value  = emptyList()
        _participants.value   = emptyList()
        _roomMeta.value       = null
    }

    fun shutdown() = leaveSession()

    // ─── Playback publishing ──────────────────────────────────────────────

    /**
     * Called by MusicService on every playback state change.
     * Writes ONE update to the room doc — covers play, pause, seek, next, prev, song change.
     */
    fun publishPlaybackState(
        mediaId: String,
        title: String,
        artist: String,
        thumbnailUrl: String,
        durationSeconds: Int,
        positionMs: Long,
        playbackSpeed: Float,
        playbackState: JamPlaybackTransportState,
        queueVersion: Long = 0L,
    ) {
        val session = _sessionState.value.session ?: return
        if (_sessionState.value.phase == JamSessionPhase.CONNECTING) return
        if (!canControlPlayback(session)) return

        val now = currentServerTimeMs()
        val version = stateVersion.incrementAndGet()

        scope.launch(Dispatchers.IO) {
            val docId = currentRoomDocId ?: currentRoomId ?: return@launch
            runCatching {
                AppwriteClientProvider.databases(appContext).updateDocument(
                    databaseId   = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.COL_ROOMS,
                    documentId   = docId,
                    data = mapOf(
                        "mediaId"               to mediaId,
                        "title"                 to title,
                        "artist"                to artist,
                        "thumbnailUrl"          to thumbnailUrl,
                        "durationSeconds"       to durationSeconds,
                        "playbackState"         to playbackState.name,
                        "positionMs"            to positionMs,
                        "updatedAtEpochMs"      to now,
                        "stateVersion"          to version,
                        "issuedByParticipantId" to session.participantId,
                        "lastActivityAtEpochMs" to now,
                    ),
                )
            }.onFailure { Timber.tag(TAG).w(it, "Failed to publish playback") }
        }
    }

    // ─── Stubs for queue API — kept so MusicService compiles unchanged ─────

    fun publishQueueSnapshot(items: List<JamQueueItem>) { /* no-op: queue removed */ }
    fun reconcileHostQueue(seeds: List<JamQueueSeed>)   { /* no-op: queue removed */ }
    fun enqueueSong(mediaMetadata: MediaMetadata): Result<JamQueueItem> =
        Result.failure(UnsupportedOperationException("Queue removed — song sync is automatic"))
    fun enqueueNextSong(mediaMetadata: MediaMetadata): Result<JamQueueItem> =
        Result.failure(UnsupportedOperationException("Queue removed — song sync is automatic"))
    fun removeQueueItem(itemId: String)  { /* no-op */ }
    fun moveQueueItemUp(itemId: String)  { /* no-op */ }
    fun moveQueueItemDown(itemId: String){ /* no-op */ }
    fun replaceQueue(items: List<MediaMetadata>) { /* no-op */ }
    fun popNextQueueItem(): JamQueueSelection? = null

    fun setAllowGuestControls(enabled: Boolean) {
        val session = _sessionState.value.session ?: return
        if (_sessionState.value.phase == JamSessionPhase.CONNECTING) return
        if (session.role != JamParticipantRole.HOST) return
        _roomMeta.value = _roomMeta.value?.copy(allowGuestControls = enabled)
        scope.launch(Dispatchers.IO) {
            val docId = currentRoomDocId ?: currentRoomId ?: return@launch
            runCatching {
                AppwriteClientProvider.databases(appContext).updateDocument(
                    databaseId   = AppwriteConfig.DATABASE_ID,
                    collectionId = AppwriteConfig.COL_ROOMS,
                    documentId   = docId,
                    data         = mapOf("allowGuestControls" to enabled),
                )
            }.onFailure { Timber.tag(TAG).w(it, "Failed to update guestControls") }
        }
    }

    fun currentServerTimeMs(): Long = System.currentTimeMillis() + _serverTimeOffsetMs.value

    // ─── Session attachment ───────────────────────────────────────────────

    private suspend fun attachToSession(session: JamActiveSession): Result<JamActiveSession> {
        teardown()
        _sessionState.value = JamSessionState(phase = JamSessionPhase.CONNECTING, session = session)
        currentRoomId = session.roomCode.roomId
        latestRoomMeta = null
        pendingGuestActivation = if (session.role == JamParticipantRole.GUEST) session else null

        val ready = CompletableDeferred<Result<JamActiveSession>>()

        AppwriteSessionAuth.ensureSession(appContext, scope) { authUid ->
            val resolved = session.copy(authUid = authUid)
            scope.launch(Dispatchers.IO) {
                try {
                    if (resolved.authUid.isNullOrBlank()) {
                        error(
                            AppwriteSessionAuth.lastFailureMessage()
                                ?: "Unable to authenticate with Appwrite"
                        )
                    }
                    if (resolved.role == JamParticipantRole.HOST) {
                        currentRoomDocId = writeRoomDoc(resolved)
                        subscribeToRoom(resolved)
                        subscribeToPresence(resolved)
                        activateSession(resolved)
                        fetchRoomDoc(resolved.roomCode.roomId)?.let { roomMeta ->
                            latestRoomMeta = roomMeta
                            _roomMeta.value = roomMeta
                        }
                        fetchAndApplyPresence(resolved.roomCode.roomId)
                        ready.complete(Result.success(resolved))
                    } else {
                        // Fetch room doc first — don't rely on Realtime arriving in time
                        val fetched = fetchRoomDoc(resolved.roomCode.roomId)
                        if (fetched != null) {
                            latestRoomMeta = fetched
                            _roomMeta.value = fetched
                        }
                        subscribeToRoom(resolved)
                        subscribeToPresence(resolved)

                        when {
                            fetched == null -> startGuestJoinValidation(resolved, ready)
                            fetched.status == "closed" ->
                                ready.complete(Result.failure(IllegalStateException("This room has ended")))
                            fetched.isJoinable -> {
                                pendingGuestActivation = null
                                activateSession(resolved)
                                startGuestHealthMonitor(resolved)
                                fetchAndApplyPresence(resolved.roomCode.roomId)
                                // Apply current playback immediately
                                applyRoomDocToPlayback(fetched, resolved)
                                ready.complete(Result.success(resolved))
                            }
                            else -> startGuestJoinValidation(resolved, ready)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Session setup failed")
                    ready.complete(Result.failure(e))
                }
            }
        }

        return try {
            ready.await()
        } catch (e: Exception) {
            Result.failure(e)
        }.also { result ->
            if (result.isFailure) {
                _sessionState.value = JamSessionState(
                    phase = JamSessionPhase.ERROR,
                    session = null,
                    lastError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    // ─── Appwrite writes ──────────────────────────────────────────────────

    private suspend fun writeRoomDoc(session: JamActiveSession): String {
        val db  = AppwriteClientProvider.databases(appContext)
        val now = System.currentTimeMillis()
        val roomId = session.roomCode.roomId
        val data = mapOf(
            "roomCode"              to session.roomCode.roomCode,
            "roomId"                to roomId,
            "hostParticipantId"     to session.participantId,
            "allowGuestControls"    to true,
            "status"                to "active",
            "createdAtEpochMs"      to now,
            "lastActivityAtEpochMs" to now,
            "mediaId"               to "",
            "title"                 to "",
            "artist"                to "",
            "thumbnailUrl"          to "",
            "durationSeconds"       to -1,
            "playbackState"         to "PAUSED",
            "positionMs"            to 0,
            "updatedAtEpochMs"      to now,
            "stateVersion"          to 0,
            "issuedByParticipantId" to session.participantId,
        )
        return try {
            db.createDocument(
                AppwriteConfig.DATABASE_ID,
                AppwriteConfig.COL_ROOMS,
                roomId,
                data,
                permissions = roomPermissions,
            ).id
        } catch (error: Exception) {
            if (error is AppwriteException && error.code == 409) {
                throw RoomIdConflictException(roomId)
            }
            Timber.tag(TAG).e(error, "Failed to write room doc")
            throw error
        }
    }

    private suspend fun markRoomClosed() {
        val docId = currentRoomDocId ?: currentRoomId ?: return
        runCatching {
            AppwriteClientProvider.databases(appContext).updateDocument(
                databaseId   = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.COL_ROOMS,
                documentId   = docId,
                data         = mapOf("status" to "closed"),
            )
        }.onFailure { Timber.tag(TAG).w(it, "Failed to mark room closed") }
    }

    private suspend fun writePresence(session: JamActiveSession) {
        val docId = "${session.roomCode.roomId}_${session.participantId}"
        val data  = mapOf(
            "roomId"            to session.roomCode.roomId,
            "participantId"     to session.participantId,
            "displayName"       to (session.displayName ?: session.role.name.lowercase().replaceFirstChar(Char::uppercase)),
            "role"              to session.role.name,
            "lastSeenAtEpochMs" to System.currentTimeMillis(),
        )
        runCatching {
            AppwriteClientProvider.databases(appContext)
                .createDocument(
                    AppwriteConfig.DATABASE_ID,
                    AppwriteConfig.COL_PRESENCE,
                    docId,
                    data,
                    permissions = presencePermissions,
                )
        }.recoverCatching { error ->
            if (error is AppwriteException && error.code == 409) {
                AppwriteClientProvider.databases(appContext)
                    .updateDocument(
                        AppwriteConfig.DATABASE_ID,
                        AppwriteConfig.COL_PRESENCE,
                        docId,
                        data,
                        permissions = presencePermissions,
                    )
            } else {
                throw error
            }
        }.onFailure { Timber.tag(TAG).w(it, "Failed to write presence") }
    }

    private suspend fun deletePresence(session: JamActiveSession) {
        val docId = "${session.roomCode.roomId}_${session.participantId}"
        runCatching {
            AppwriteClientProvider.databases(appContext)
                .deleteDocument(AppwriteConfig.DATABASE_ID, AppwriteConfig.COL_PRESENCE, docId)
        }.onFailure { Timber.tag(TAG).w(it, "Failed to delete presence") }
    }

    // ─── Appwrite reads ───────────────────────────────────────────────────

    private suspend fun fetchRoomDoc(roomId: String): JamRoomMeta? =
        runCatching {
            val doc = AppwriteClientProvider.databases(appContext).getDocument(
                databaseId   = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.COL_ROOMS,
                documentId   = roomId,
            )
            JamRoomMeta.fromMap(doc.data)
        }.onFailure { Timber.tag(TAG).d("fetchRoomDoc failed: %s", it.message) }
            .getOrNull()

    private suspend fun fetchAndApplyPresence(roomId: String) {
        runCatching {
            val result = AppwriteClientProvider.databases(appContext).listDocuments(
                databaseId   = AppwriteConfig.DATABASE_ID,
                collectionId = AppwriteConfig.COL_PRESENCE,
                queries      = listOf(Query.equal("roomId", roomId), Query.limit(50)),
            )
            val list = result.documents.mapNotNull { JamParticipant.fromMap(it.data) }
                .sortedWith(compareBy<JamParticipant> { it.role != JamParticipantRole.HOST }.thenBy { it.joinedAtEpochMs })
            _participants.value = list
        }.onFailure { Timber.tag(TAG).w(it, "Failed to fetch presence") }
    }

    // ─── Realtime subscriptions ───────────────────────────────────────────

    private fun subscribeToRoom(session: JamActiveSession) {
        val rt = AppwriteClientProvider.realtime(appContext)
        val channel = "databases.${AppwriteConfig.DATABASE_ID}.collections.${AppwriteConfig.COL_ROOMS}.documents.${session.roomCode.roomId}"
        roomSub = rt.subscribe(channel) { event ->
            val payload = event.payload as? Map<*, *> ?: return@subscribe
            val meta = JamRoomMeta.fromMap(payload) ?: return@subscribe
            latestRoomMeta = meta
            _roomMeta.value = meta

            if (session.role != JamParticipantRole.HOST) {
                when {
                    meta.status == "closed" -> handleSessionFailure("The host ended this Together session")
                    meta.isStale()          -> handleSessionFailure("Host connection was lost")
                    meta.isJoinable         -> {
                        // Activate pending guest if not yet active
                        if (_sessionState.value.phase == JamSessionPhase.CONNECTING) {
                            pendingGuestActivation = null
                            scope.launch(Dispatchers.IO) {
                                activateSession(session)
                                startGuestHealthMonitor(session)
                                fetchAndApplyPresence(session.roomCode.roomId)
                            }
                        }
                        // Apply playback from this event
                        applyRoomDocToPlayback(meta, session)
                    }
                }
            }
        }
    }

    private fun subscribeToPresence(session: JamActiveSession) {
        val rt = AppwriteClientProvider.realtime(appContext)
        val channel = "databases.${AppwriteConfig.DATABASE_ID}.collections.${AppwriteConfig.COL_PRESENCE}.documents"
        presenceSub = rt.subscribe(channel) { _ ->
            scope.launch(Dispatchers.IO) { fetchAndApplyPresence(session.roomCode.roomId) }
        }
        // Initial fetch immediately
        scope.launch(Dispatchers.IO) { fetchAndApplyPresence(session.roomCode.roomId) }
    }

    // ─── Playback application ─────────────────────────────────────────────

    /**
     * Converts a room doc snapshot into a JamPlaybackSnapshot and pushes it to remotePlayback.
     * MusicService's existing collector picks this up and applies it to the player.
     */
    private fun applyRoomDocToPlayback(meta: JamRoomMeta, session: JamActiveSession) {
        // Don't apply our own writes back to ourselves
        if (meta.issuedByParticipantId == session.participantId) return
        if (meta.mediaId.isBlank()) return

        val snapshot = JamPlaybackSnapshot(
            mediaId               = meta.mediaId,
            title                 = meta.title,
            artist                = meta.artist,
            thumbnailUrl          = meta.thumbnailUrl,
            durationSeconds       = meta.durationSeconds,
            playbackState         = runCatching {
                JamPlaybackTransportState.valueOf(meta.playbackState)
            }.getOrDefault(JamPlaybackTransportState.PAUSED),
            basePositionMs        = meta.positionMs,
            playbackSpeed         = meta.playbackSpeed,
            updatedAtEpochMs      = meta.updatedAtEpochMs,
            stateVersion          = meta.stateVersion,
            issuedByParticipantId = meta.issuedByParticipantId,
        )
        _remotePlayback.value = snapshot
    }

    // ─── Session activation ───────────────────────────────────────────────

    private suspend fun activateSession(session: JamActiveSession) {
        writePresence(session)
        startPresenceHeartbeat(session)
        withContext(Dispatchers.Main) {
            _sessionState.value = JamSessionState(
                phase = if (session.role == JamParticipantRole.HOST) JamSessionPhase.HOSTING else JamSessionPhase.JOINED,
                session = session,
            )
        }
    }

    // ─── Guest join validation ────────────────────────────────────────────

    private fun startGuestJoinValidation(
        session: JamActiveSession,
        ready: CompletableDeferred<Result<JamActiveSession>>,
    ) {
        guestJoinValidationJob?.cancel()
        guestJoinValidationJob = scope.launch {
            delay(GUEST_JOIN_VALIDATION_TIMEOUT_MS)
            val active = _sessionState.value.session
            if (active?.roomCode?.roomId != session.roomCode.roomId) return@launch

            // One more DB fetch before giving up
            val fresh = withContext(Dispatchers.IO) { fetchRoomDoc(session.roomCode.roomId) }
            when {
                fresh == null ->
                    ready.complete(Result.failure(IllegalStateException("Room not found")))
                fresh.status == "closed" ->
                    ready.complete(Result.failure(IllegalStateException("This room has ended")))
                fresh.isJoinable -> {
                    latestRoomMeta = fresh
                    _roomMeta.value = fresh
                    pendingGuestActivation = null
                    withContext(Dispatchers.IO) {
                        activateSession(session)
                        startGuestHealthMonitor(session)
                        fetchAndApplyPresence(session.roomCode.roomId)
                        applyRoomDocToPlayback(fresh, session)
                    }
                    ready.complete(Result.success(session))
                }
                else ->
                    ready.complete(Result.failure(IllegalStateException("Room not accepting joins")))
            }
        }
    }

    private fun startGuestHealthMonitor(session: JamActiveSession) {
        guestHealthJob?.cancel()
        guestHealthJob = scope.launch {
            while (isActive) {
                delay(GUEST_HEALTH_CHECK_MS)
                val meta = latestRoomMeta ?: continue
                if (meta.roomId != session.roomCode.roomId) continue
                if (meta.status == "closed") { handleSessionFailure("The host ended this Together session"); break }
                if (meta.isStale()) { handleSessionFailure("Host connection was lost"); break }
            }
        }
    }

    // ─── Heartbeat ────────────────────────────────────────────────────────

    private fun startPresenceHeartbeat(session: JamActiveSession) {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(PRESENCE_HEARTBEAT_MS)
                if (_sessionState.value.phase == JamSessionPhase.CONNECTING) continue
                val now = System.currentTimeMillis()
                val presenceDocId = "${session.roomCode.roomId}_${session.participantId}"
                runCatching {
                    val db = AppwriteClientProvider.databases(appContext)
                    db.updateDocument(
                        AppwriteConfig.DATABASE_ID, AppwriteConfig.COL_PRESENCE,
                        presenceDocId, mapOf("lastSeenAtEpochMs" to now),
                    )
                    if (session.role == JamParticipantRole.HOST) {
                        val roomDocId = currentRoomDocId ?: session.roomCode.roomId
                        db.updateDocument(
                            AppwriteConfig.DATABASE_ID, AppwriteConfig.COL_ROOMS,
                            roomDocId, mapOf("lastActivityAtEpochMs" to now),
                        )
                    }
                }.onFailure { Timber.tag(TAG).w(it, "Heartbeat failed") }
            }
        }
    }

    // ─── Teardown & error handling ────────────────────────────────────────

    private fun teardown() {
        guestJoinValidationJob?.cancel(); guestJoinValidationJob = null
        guestHealthJob?.cancel();         guestHealthJob = null
        presenceHeartbeatJob?.cancel();   presenceHeartbeatJob = null
        runCatching { roomSub?.close() };     roomSub = null
        runCatching { presenceSub?.close() }; presenceSub = null
        currentRoomDocId = null
        currentRoomId    = null
        latestRoomMeta   = null
        pendingGuestActivation = null
        _roomMeta.value = null
    }

    private fun handleSessionFailure(message: String) {
        teardown()
        _remotePlayback.value = null
        _queueSnapshot.value  = emptyList()
        _participants.value   = emptyList()
        _roomMeta.value       = null
        _sessionState.value   = JamSessionState(phase = JamSessionPhase.ERROR, session = null, lastError = message)
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun canControlPlayback(session: JamActiveSession): Boolean {
        if (session.role == JamParticipantRole.HOST) return true
        return latestRoomMeta?.allowGuestControls ?: true
    }

    private fun loadOrCreateParticipantId(): String {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_PARTICIPANT_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_PARTICIPANT_ID, generated).apply()
        return generated
    }

    private fun JamRoomMeta.isStale(nowMs: Long = System.currentTimeMillis()): Boolean =
        lastActivityAtEpochMs > 0 && nowMs - lastActivityAtEpochMs > ROOM_STALE_TIMEOUT_MS

    companion object {
        private const val TAG = "AppwriteTogether"
        private const val PREFS_NAME = "echofy_together"
        private const val KEY_PARTICIPANT_ID = "participant_id"
        private const val HOST_CREATE_MAX_ATTEMPTS = 5
        private const val PRESENCE_HEARTBEAT_MS = 15_000L
        private const val GUEST_JOIN_VALIDATION_TIMEOUT_MS = 12_000L
        private const val GUEST_HEALTH_CHECK_MS = 10_000L
        private const val ROOM_STALE_TIMEOUT_MS = 45_000L
    }

    private class RoomIdConflictException(roomId: String) :
        IllegalStateException("Room id already exists: $roomId")
}
