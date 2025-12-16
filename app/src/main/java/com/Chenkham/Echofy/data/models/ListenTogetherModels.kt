package com.Chenkham.Echofy.data.models

import kotlinx.serialization.Serializable

/**
 * Control mode for listening room.
 */
enum class ControlMode {
    HOST_ONLY,  // Only host can control playback
    EVERYONE    // Anyone can control playback
}

/**
 * Represents a listening room for the Listen Together feature.
 */
@Serializable
data class ListeningRoom(
    val roomCode: String,
    val hostId: String,
    val hostName: String,
    val hostEmail: String? = null,
    val isHostConnected: Boolean = true,
    val controlMode: String = "HOST_ONLY", // "HOST_ONLY" or "EVERYONE"
    val currentTrackId: String? = null,
    val currentTrackTitle: String? = null,
    val currentTrackArtist: String? = null,
    val currentTrackThumbnail: String? = null,
    val playbackPosition: Long = 0L,
    val isPlaying: Boolean = false,
    val startedAt: Long = 0L, // CRITICAL: Unix timestamp (ms) when current track started playing
    val playbackSpeed: Float = 1.0f,
    val participants: List<String> = emptyList(), // List of unique participant IDs
    val participantNames: List<String> = emptyList(), // Display names for participants
    val reactions: List<String> = emptyList(), // Recent reactions (emoji + senderId)
    val sessionStartTime: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis()
)

/**
 * Queue item for session playlist.
 */
@Serializable
data class QueueItem(
    val id: String = "",
    val sessionId: String,
    val trackId: String,
    val trackTitle: String,
    val trackArtist: String?,
    val trackThumbnail: String?,
    val addedBy: String,
    val addedByName: String,
    val order: Int,
    val isPlayed: Boolean = false
)

/**
 * Represents a participant in a listening room.
 */
@Serializable
data class Participant(
    val id: String,
    val name: String,
    val isConnected: Boolean = true,
    val joinedAt: Long = System.currentTimeMillis()
)

/**
 * Current track info for syncing.
 */
@Serializable
data class TrackInfo(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null
)

/**
 * Reaction sent by a participant.
 */
@Serializable
data class Reaction(
    val emoji: String,
    val senderId: String,
    val senderName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Chat message in a listening session.
 */
@Serializable
data class ChatMessage(
    val id: String = "",
    val sessionId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
