package com.Chenkham.Echofy.utils

import android.content.Context
import com.Chenkham.Echofy.constants.CustomShareDomainKey
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ShareUtils {
    const val DEFAULT_SHARE_DOMAIN = "https://chenkham.github.io"

    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            value
        }
    }

    fun getBaseShareUrl(context: Context): String {
        val customDomain = context.dataStore[CustomShareDomainKey]?.trim()
        return if (!customDomain.isNullOrBlank()) {
            customDomain.removeSuffix("/")
        } else {
            DEFAULT_SHARE_DOMAIN
        }
    }

    fun buildTrackShareUrl(
        context: Context,
        songId: String,
        title: String? = null,
        artist: String? = null
    ): String {
        val base = getBaseShareUrl(context)
        val builder = StringBuilder("$base/?track=$songId")
        if (!title.isNullOrBlank()) {
            builder.append("&title=${urlEncode(title)}")
        }
        if (!artist.isNullOrBlank()) {
            builder.append("&artist=${urlEncode(artist)}")
        }
        return builder.toString()
    }

    fun buildTrackShareText(
        context: Context,
        songId: String,
        title: String,
        artist: String
    ): String {
        val url = buildTrackShareUrl(context, songId, title, artist)
        return "Check out \"$title\" by $artist on Echofy!\n$url"
    }

    fun buildJamShareUrl(
        context: Context,
        roomCode: String
    ): String {
        val base = getBaseShareUrl(context)
        return "$base/?jam=${roomCode.trim().uppercase()}"
    }

    fun buildJamShareText(
        context: Context,
        roomCode: String
    ): String {
        val url = buildJamShareUrl(context, roomCode)
        return "Join my Echofy Jam Together session! 🎧\nRoom code: ${roomCode.trim().uppercase()}\nTap to listen together: $url"
    }

    fun buildPlaylistShareUrl(
        context: Context,
        playlistId: String,
        title: String? = null
    ): String {
        val base = getBaseShareUrl(context)
        val builder = StringBuilder("$base/?playlist=$playlistId")
        if (!title.isNullOrBlank()) {
            builder.append("&title=").append(urlEncode(title))
        }
        return builder.toString()
    }

    fun buildPlaylistShareText(
        context: Context,
        playlistId: String,
        title: String
    ): String {
        val url = buildPlaylistShareUrl(context, playlistId, title)
        return "Listen to playlist \"$title\" on Echofy:\n$url"
    }

    fun buildAlbumShareUrl(
        context: Context,
        albumId: String,
        title: String? = null
    ): String {
        val base = getBaseShareUrl(context)
        val builder = StringBuilder("$base/?album=$albumId")
        if (!title.isNullOrBlank()) {
            builder.append("&title=").append(urlEncode(title))
        }
        return builder.toString()
    }

    fun buildAlbumShareText(
        context: Context,
        albumId: String,
        title: String,
        artist: String? = null
    ): String {
        val url = buildAlbumShareUrl(context, albumId, title)
        val byArtist = if (!artist.isNullOrBlank()) " by $artist" else ""
        return "Listen to album \"$title\"$byArtist on Echofy:\n$url"
    }

    fun buildArtistShareUrl(
        context: Context,
        artistId: String,
        name: String? = null
    ): String {
        val base = getBaseShareUrl(context)
        val builder = StringBuilder("$base/?artist=$artistId")
        if (!name.isNullOrBlank()) {
            builder.append("&name=").append(urlEncode(name))
        }
        return builder.toString()
    }

    fun buildArtistShareText(
        context: Context,
        artistId: String,
        name: String
    ): String {
        val url = buildArtistShareUrl(context, artistId, name)
        return "Check out $name on Echofy:\n$url"
    }
}
