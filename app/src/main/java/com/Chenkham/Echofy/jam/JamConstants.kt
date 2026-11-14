package com.Chenkham.Echofy.jam

/**
 * Constants and document keys for Appwrite Together Session documents.
 * Centralizing these keys prevents string typos and ensures schema consistency.
 */
object JamDocFields {
    // Room Document Fields
    const val ROOM_CODE = "roomCode"
    const val ROOM_ID = "roomId"
    const val HOST_PARTICIPANT_ID = "hostParticipantId"
    const val SHARD_ID = "shardId"
    const val HOST_AUTH_UID = "hostAuthUid"
    const val HOST_NAME = "hostName"
    const val REQUIRE_APPROVAL = "requireApproval"
    const val ALLOW_GUEST_CONTROLS = "allowGuestControls"
    const val STATUS = "status"
    const val STATUS_ACTIVE = "active"
    const val STATUS_CLOSED = "closed"
    const val SCHEMA_VERSION = "schemaVersion"
    const val CREATED_AT_EPOCH_MS = "createdAtEpochMs"
    const val LAST_ACTIVITY_AT_EPOCH_MS = "lastActivityAtEpochMs"
    const val MEDIA_ID = "mediaId"
    const val TITLE = "title"
    const val ARTIST = "artist"
    const val THUMBNAIL_URL = "thumbnailUrl"
    const val DURATION_SECONDS = "durationSeconds"
    const val PLAYBACK_STATE = "playbackState"
    const val PLAYBACK_SPEED = "playbackSpeed"
    const val POSITION_MS = "positionMs"
    const val UPDATED_AT_EPOCH_MS = "updatedAtEpochMs"
    const val STATE_VERSION = "stateVersion"
    const val ISSUED_BY_PARTICIPANT_ID = "issuedByParticipantId"

    // Presence Document Fields
    const val PARTICIPANT_ID = "participantId"
    const val DISPLAY_NAME = "displayName"
    const val NAME_INITIAL = "nameInitial"
    const val ROLE = "role"
    const val AUTH_UID = "authUid"
    const val JOINED_AT_EPOCH_MS = "joinedAtEpochMs"
    const val LAST_SEEN_AT_EPOCH_MS = "lastSeenAtEpochMs"
}
