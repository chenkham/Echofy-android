/*
 * Echofy Discord RPC
 */

package com.Chenkham.Echofy.utils

import android.content.Context
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.constants.*
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import timber.log.Timber

class DiscordRPC(
    val context: Context,
    token: String,
) : KizzyRPC(token) {

    companion object {
        private const val APPLICATION_ID = "1411019391843172514"
        private const val PAUSE_IMAGE_URL =
            "https://raw.githubusercontent.com/koiverse/ArchiveTune/main/fastlane/metadata/android/en-US/images/RPC/pause_icon.png"
        private const val APP_ICON_URL = 
            "https://raw.githubusercontent.com/Arturo254/Echofy/refs/heads/master/assets/icon.png"
        private const val logtag = "DiscordRPC"
    }

    private fun normalizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        return "https://$trimmed"
    }

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float,
        isPaused: Boolean,
    ) = runCatching {
        val currentTime = System.currentTimeMillis()
        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val songTitleWithRate = if (playbackSpeed != 1.0f) {
            "${song.song.title} [${String.format("%.2fx", playbackSpeed)}]"
        } else {
            song.song.title
        }

        val remainingDuration = song.song.duration * 1000L - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        val showWhenPaused = context.dataStore[DiscordShowWhenPausedKey] ?: true
        val statusPref = context.dataStore[DiscordPresenceStatusKey] ?: "ONLINE"

        val activityTypePref = (context.dataStore[DiscordActivityTypeKey] ?: "LISTENING").uppercase()
        val resolvedType = when (activityTypePref) {
            "PLAYING" -> Type.PLAYING
            "STREAMING" -> Type.STREAMING
            "WATCHING" -> Type.WATCHING
            "COMPETING" -> Type.COMPETING
            else -> Type.LISTENING
        }

        val activityName = (context.dataStore[DiscordActivityNameKey] ?: context.getString(R.string.app_name)).trim()
        val activityDetails = (context.dataStore[DiscordActivityDetailsKey] ?: songTitleWithRate).trim()
        val activityState = (context.dataStore[DiscordActivityStateKey] ?: song.artists.joinToString { it.name }).trim()

        val largeImage = song.song.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
        val smallImage = if (isPaused) {
            RpcImage.ExternalImage(PAUSE_IMAGE_URL)
        } else {
            song.artists.firstOrNull()?.thumbnailUrl?.let { RpcImage.ExternalImage(it) }
        }

        val buttons = mutableListOf<Pair<String, String>>()
        val btn1Enabled = context.dataStore[DiscordActivityButton1EnabledKey] ?: true
        val btn1Label = context.dataStore[DiscordActivityButton1LabelKey] ?: "Listen on YouTube Music"
        val btn1Url = "https://music.youtube.com/watch?v=${song.song.id}"
        if (btn1Enabled && btn1Label.isNotBlank()) {
            buttons.add(btn1Label to btn1Url)
        }

        val btn2Enabled = context.dataStore[DiscordActivityButton2EnabledKey] ?: true
        val btn2Label = context.dataStore[DiscordActivityButton2LabelKey] ?: "Visit Echofy"
        val btn2Url = "https://github.com/Arturo254/Echofy"
        if (btn2Enabled && btn2Label.isNotBlank()) {
            buttons.add(btn2Label to btn2Url)
        }

        val sendStartTime: Long?
        val sendEndTime: Long?
        val sendSince: Long?

        when {
            isPaused && showWhenPaused -> {
                sendStartTime = null
                sendEndTime = null
                sendSince = currentTime
            }
            isPaused && !showWhenPaused -> {
                sendStartTime = null
                sendEndTime = null
                sendSince = null
            }
            !isPaused -> {
                sendStartTime = calculatedStartTime
                sendEndTime = currentTime + adjustedRemainingDuration
                sendSince = null
            }
            else -> {
                sendStartTime = null
                sendEndTime = null
                sendSince = null
            }
        }

        setActivity(
            name = activityName.removeSuffix(" Debug"),
            details = activityDetails,
            state = activityState,
            detailsUrl = "https://music.youtube.com/watch?v=${song.song.id}",
            largeImage = largeImage,
            smallImage = smallImage,
            largeText = song.album?.title ?: song.song.title,
            smallText = song.artists.firstOrNull()?.name ?: "Echofy",
            buttons = buttons,
            type = resolvedType,
            statusDisplayType = StatusDisplayType.STATE,
            since = sendSince,
            startTime = sendStartTime,
            endTime = sendEndTime,
            applicationId = APPLICATION_ID,
            status = statusPref.lowercase()
        )
    }

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        isPaused: Boolean = false,
    ) = updateSong(
        song = song,
        currentPlaybackTimeMillis = currentPlaybackTimeMillis,
        playbackSpeed = 1.0f,
        isPaused = isPaused
    )

    suspend fun refreshActivity(song: Song, currentPlaybackTimeMillis: Long, isPaused: Boolean = false) = runCatching {
        updateSong(song, currentPlaybackTimeMillis, isPaused).getOrThrow()
    }
}
