package com.Chenkham.Echofy.jam

import com.Chenkham.Echofy.models.MediaMetadata
import kotlinx.serialization.Serializable
import java.security.SecureRandom

private val roomCodeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
private val roomCodeRandom = SecureRandom()

enum class JamShardStatus {
    ACTIVE,
    CANARY,
    DRAINING,
    EXHAUSTED,
    DISABLED,
    ;

    val canCreateRooms: Boolean
        get() = this == ACTIVE || this == CANARY

    val canJoinRooms: Boolean
        get() = this != DISABLED
}

enum class JamParticipantRole {
    HOST,
    GUEST,
}

enum class JamPlaybackTransportState {
    PLAYING,
    PAUSED,
    BUFFERING,
}

enum class JamSessionPhase {
    IDLE,
    CONNECTING,
    HOSTING,
    JOINED,
    ERROR,
}

@Serializable
data class JamShardCapacity(
    val softRooms: Int = 0,
    val hardRooms: Int = 0,
)

@Serializable
data class JamShardFeatures(
    val canCreateRooms: Boolean = true,
    val canJoinRooms: Boolean = true,
)

@Serializable
data class JamShardConfig(
    val id: String,
    val status: JamShardStatus,
    val region: String = "global",
    val weight: Int = 100,
    val capacity: JamShardCapacity = JamShardCapacity(),
    val features: JamShardFeatures = JamShardFeatures(),
) {
    val canCreateRooms: Boolean
        get() = status.canCreateRooms && features.canCreateRooms

    val canJoinRooms: Boolean
        get() = status.canJoinRooms && features.canJoinRooms
}

@Serializable
data class JamRegistry(
    val version: Int = 1,
    val defaultTtlSeconds: Long = 86_400,
    val inviteBaseUrl: String = "",
    val shards: List<JamShardConfig> = emptyList(),
) {
    fun findShard(shardId: String): JamShardConfig? = shards.firstOrNull { it.id == shardId }

    fun pickShardForNewRoom(): JamShardConfig? {
        val candidates = shards.filter { it.canCreateRooms }
        if (candidates.isEmpty()) return null

        val totalWeight = candidates.sumOf { it.weight.coerceAtLeast(1) }
        var remaining = roomCodeRandom.nextInt(totalWeight)
        for (candidate in candidates) {
            remaining -= candidate.weight.coerceAtLeast(1)
            if (remaining < 0) return candidate
        }
        return candidates.last()
    }
}

@Serializable
data class JamRoomAllocation(
    val roomCode: String,
    val shardId: String,
)

data class JamRoomCode(
    val shardId: String,
    val roomToken: String,
) {
    val roomCode: String
        get() = roomToken

    val roomId: String
        get() = "${shardId.lowercase()}_${roomToken.lowercase()}"

    companion object {
        fun create(shardId: String, tokenLength: Int = 6): JamRoomCode =
            JamRoomCode(
                shardId = shardId,
                roomToken = buildString(tokenLength) {
                    repeat(tokenLength) {
                        append(roomCodeAlphabet[roomCodeRandom.nextInt(roomCodeAlphabet.length)])
                    }
                },
            )

        fun parse(rawCode: String): JamRoomCode? {
            val normalized = rawCode.trim().uppercase()
            val parts = normalized.split("-", limit = 2)
            val (shardId, token) = if (parts.size == 2) {
                parts[0] to parts[1]
            } else {
                "01" to normalized
            }
            if (token.length < 4) return null
            if (!token.all { it in roomCodeAlphabet }) return null
            return JamRoomCode(shardId = shardId, roomToken = token)
        }
    }
}

data class JamActiveSession(
    val roomCode: JamRoomCode,
    val participantId: String,
    val role: JamParticipantRole,
    val displayName: String? = null,
    val authUid: String? = null,
)

data class JamParticipant(
    val participantId: String,
    val displayName: String,
    val role: JamParticipantRole,
    val authUid: String? = null,
    val joinedAtEpochMs: Long = 0L,
    val lastSeenAtEpochMs: Long = 0L,
) {
    /** First letter of displayName, uppercased — used for avatar circles */
    val nameInitial: String
        get() = displayName.firstOrNull()?.uppercase() ?: "?"
    companion object {
        fun fromMap(map: Map<*, *>): JamParticipant? {
            val participantId = map["participantId"] as? String ?: return null
            val displayName = map["displayName"] as? String ?: "Listener"
            val role = (map["role"] as? String)
                ?.let { runCatching { JamParticipantRole.valueOf(it) }.getOrNull() }
                ?: JamParticipantRole.GUEST
            return JamParticipant(
                participantId = participantId,
                displayName = displayName,
                role = role,
                authUid = map["authUid"] as? String,
                joinedAtEpochMs = (map["joinedAtEpochMs"] as? Number)?.toLong() ?: 0L,
                lastSeenAtEpochMs = (map["lastSeenAtEpochMs"] as? Number)?.toLong() ?: 0L,
            )
        }
    }
}

data class JamRoomMeta(
    val roomCode: String,
    val roomId: String,
    val shardId: String = "",
    val hostParticipantId: String,
    val hostAuthUid: String = "",
    val hostName: String = "",
    val requireApproval: Boolean = false,
    val allowGuestControls: Boolean = true,
    val createdAtEpochMs: Long = 0L,
    val lastActivityAtEpochMs: Long = 0L,
    val status: String = "active",
    val closedAtEpochMs: Long? = null,
    val schemaVersion: Int = 1,
    // Playback state merged into room doc
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = -1,
    val playbackState: String = "PAUSED",
    val playbackSpeed: Float = 1f,
    val positionMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val stateVersion: Long = 0L,
    val issuedByParticipantId: String = "",
) {
    val isJoinable: Boolean
        get() = status == "active" && hostParticipantId.isNotBlank()

    companion object {
        fun fromMap(map: Map<*, *>): JamRoomMeta? {
            val roomCode = map["roomCode"] as? String ?: return null
            val roomId   = map["roomId"]   as? String ?: return null
            val hostParticipantId = map["hostParticipantId"] as? String ?: return null
            return JamRoomMeta(
                roomCode             = roomCode,
                roomId               = roomId,
                shardId              = map["shardId"] as? String ?: "",
                hostParticipantId    = hostParticipantId,
                hostAuthUid          = map["hostAuthUid"] as? String ?: "",
                hostName             = map["hostName"] as? String ?: "",
                requireApproval      = map["requireApproval"] as? Boolean ?: false,
                allowGuestControls   = map["allowGuestControls"] as? Boolean ?: true,
                createdAtEpochMs     = (map["createdAtEpochMs"] as? Number)?.toLong() ?: 0L,
                lastActivityAtEpochMs= (map["lastActivityAtEpochMs"] as? Number)?.toLong() ?: 0L,
                status               = map["status"] as? String ?: "active",
                closedAtEpochMs      = (map["closedAtEpochMs"] as? Number)?.toLong(),
                schemaVersion        = (map["schemaVersion"] as? Number)?.toInt() ?: 1,
                mediaId              = map["mediaId"] as? String ?: "",
                title                = map["title"] as? String ?: "",
                artist               = map["artist"] as? String ?: "",
                thumbnailUrl         = map["thumbnailUrl"] as? String ?: "",
                durationSeconds      = (map["durationSeconds"] as? Number)?.toInt() ?: -1,
                playbackState        = map["playbackState"] as? String ?: "PAUSED",
                playbackSpeed        = (map["playbackSpeed"] as? Number)?.toFloat() ?: 1f,
                positionMs           = (map["positionMs"] as? Number)?.toLong() ?: 0L,
                updatedAtEpochMs     = (map["updatedAtEpochMs"] as? Number)?.toLong() ?: 0L,
                stateVersion         = (map["stateVersion"] as? Number)?.toLong() ?: 0L,
                issuedByParticipantId= map["issuedByParticipantId"] as? String ?: "",
            )
        }
    }
}

data class JamSessionState(
    val phase: JamSessionPhase = JamSessionPhase.IDLE,
    val session: JamActiveSession? = null,
    val lastError: String? = null,
)

data class JamPlaybackSnapshot(
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = -1,
    val playbackState: JamPlaybackTransportState = JamPlaybackTransportState.PAUSED,
    val basePositionMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val updatedAtEpochMs: Long = 0L,
    val stateVersion: Long = 0L,
    val queueVersion: Long = 0L,
    val issuedByParticipantId: String = "",
    val issuedByAuthUid: String = "",
) {
    fun toMap(): Map<String, Any> =
        mapOf(
            "mediaId" to mediaId,
            "title" to title,
            "artist" to artist,
            "thumbnailUrl" to thumbnailUrl,
            "durationSeconds" to durationSeconds,
            "playbackState" to playbackState.name,
            "basePositionMs" to basePositionMs,
            "playbackSpeed" to playbackSpeed.toDouble(),
            "updatedAtEpochMs" to updatedAtEpochMs,
            "stateVersion" to stateVersion,
            "queueVersion" to queueVersion,
            "issuedByParticipantId" to issuedByParticipantId,
            "issuedByAuthUid" to issuedByAuthUid,
        )

    fun expectedPositionAt(nowEpochMs: Long): Long {
        if (playbackState != JamPlaybackTransportState.PLAYING) return basePositionMs
        val deltaMs = (nowEpochMs - updatedAtEpochMs).coerceAtLeast(0L)
        return basePositionMs + (deltaMs * playbackSpeed).toLong()
    }

    fun toMediaMetadata(): MediaMetadata =
        MediaMetadata(
            id = mediaId,
            title = title.ifBlank { mediaId },
            artists = artist
                .split(",")
                .map(String::trim)
                .filter(String::isNotBlank)
                .ifEmpty { listOf("Unknown Artist") }
                .map { MediaMetadata.Artist(id = null, name = it) },
            duration = durationSeconds,
            thumbnailUrl = thumbnailUrl.ifBlank { null },
        )

    companion object {
        fun fromMap(map: Map<*, *>): JamPlaybackSnapshot? {
            val mediaId = map["mediaId"] as? String ?: return null
            val playbackState = (map["playbackState"] as? String)
                ?.let { runCatching { JamPlaybackTransportState.valueOf(it) }.getOrNull() }
                ?: JamPlaybackTransportState.PAUSED
            val basePositionMs = (map["basePositionMs"] as? Number)?.toLong() ?: 0L
            val playbackSpeed = (map["playbackSpeed"] as? Number)?.toFloat() ?: 1f
            val updatedAtEpochMs = (map["updatedAtEpochMs"] as? Number)?.toLong() ?: 0L
            val stateVersion = (map["stateVersion"] as? Number)?.toLong() ?: 0L
            val queueVersion = (map["queueVersion"] as? Number)?.toLong() ?: 0L
            val issuedByParticipantId = map["issuedByParticipantId"] as? String ?: ""
            val issuedByAuthUid = map["issuedByAuthUid"] as? String ?: ""
            return JamPlaybackSnapshot(
                mediaId = mediaId,
                title = map["title"] as? String ?: "",
                artist = map["artist"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                durationSeconds = (map["durationSeconds"] as? Number)?.toInt() ?: -1,
                playbackState = playbackState,
                basePositionMs = basePositionMs,
                playbackSpeed = playbackSpeed,
                updatedAtEpochMs = updatedAtEpochMs,
                stateVersion = stateVersion,
                queueVersion = queueVersion,
                issuedByParticipantId = issuedByParticipantId,
                issuedByAuthUid = issuedByAuthUid,
            )
        }
    }
}

data class JamPlaybackState(
    val playbackState: JamPlaybackTransportState = JamPlaybackTransportState.PAUSED,
    val basePositionMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L,
    val stateVersion: Long = 0L,
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "playbackState" to playbackState.name,
            "basePositionMs" to basePositionMs,
            "updatedAtEpochMs" to updatedAtEpochMs,
            "stateVersion" to stateVersion,
        )
    }

    /**
     * Helper to calculate the estimated playback position right now
     * based on the last known start point and elapsed time.
     */
    fun currentEstimatedPositionMs(nowEpochMs: Long = System.currentTimeMillis()): Long {
        if (playbackState != JamPlaybackTransportState.PLAYING) return basePositionMs
        val deltaMs = (nowEpochMs - updatedAtEpochMs).coerceAtLeast(0L)
        return basePositionMs + deltaMs
    }

    companion object {
        fun fromMap(map: Map<String, Any>): JamPlaybackState {
            val rawState = map["playbackState"] as? String
            val playbackState = rawState?.let {
                runCatching { JamPlaybackTransportState.valueOf(it) }.getOrNull()
            }
                ?: JamPlaybackTransportState.PAUSED

            val basePositionMs = (map["basePositionMs"] as? Number)?.toLong() ?: 0L
            val updatedAtEpochMs = (map["updatedAtEpochMs"] as? Number)?.toLong() ?: 0L
            val stateVersion = (map["stateVersion"] as? Number)?.toLong() ?: 0L

            return JamPlaybackState(
                playbackState = playbackState,
                basePositionMs = basePositionMs,
                updatedAtEpochMs = updatedAtEpochMs,
                stateVersion = stateVersion,
            )
        }
    }
}

data class JamQueueSeed(
    val mediaId: String,
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = -1,
)

data class JamQueueItem(
    val id: String,
    val mediaId: String,
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = -1,
    val queuePosition: Int = 0,
    val addedAtEpochMs: Long = 0L,
    val addedByParticipantId: String = "",
    val addedByAuthUid: String = "",
) {
    fun toMap(): Map<String, Any> =
        buildMap {
            put("id", id)
            put("mediaId", mediaId)
            put("title", title)
            put("artist", artist)
            put("thumbnailUrl", thumbnailUrl)
            put("durationSeconds", durationSeconds)
            put("queuePosition", queuePosition)
            put("addedAtEpochMs", addedAtEpochMs)
            put("addedByParticipantId", addedByParticipantId)
            put("addedByAuthUid", addedByAuthUid)
        }

    fun toMediaMetadata(): MediaMetadata =
        MediaMetadata(
            id = mediaId,
            title = title.ifBlank { mediaId },
            artists = artist
                .split(",")
                .map(String::trim)
                .filter(String::isNotBlank)
                .ifEmpty { listOf("Unknown Artist") }
                .map { MediaMetadata.Artist(id = null, name = it) },
            duration = durationSeconds,
            thumbnailUrl = thumbnailUrl.ifBlank { null },
        )

    companion object {
        fun fromMap(map: Map<*, *>): JamQueueItem? {
            val id = map["id"] as? String ?: return null
            val mediaId = map["mediaId"] as? String ?: return null
            return JamQueueItem(
                id = id,
                mediaId = mediaId,
                title = map["title"] as? String ?: "",
                artist = map["artist"] as? String ?: "",
                thumbnailUrl = map["thumbnailUrl"] as? String ?: "",
                durationSeconds = (map["durationSeconds"] as? Number)?.toInt() ?: -1,
                queuePosition = (map["queuePosition"] as? Number)?.toInt() ?: 0,
                addedAtEpochMs = (map["addedAtEpochMs"] as? Number)?.toLong() ?: 0L,
                addedByParticipantId = map["addedByParticipantId"] as? String ?: "",
                addedByAuthUid = map["addedByAuthUid"] as? String ?: "",
            )
        }
    }
}

data class JamQueueSelection(
    val nextItem: JamQueueItem,
    val remainingQueue: List<JamQueueItem>,
)
