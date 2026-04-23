package com.Chenkham.Echofy.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.Chenkham.Echofy.db.entities.FormatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object RingtoneUtils {
    /**
     * Checks if the app has required storage permissions based on Android version.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10-12, scoped storage handles writing to Ringtones without explicit permission
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun setTrackAsRingtone(
        context: Context,
        trackTitle: String,
        format: FormatEntity,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val playbackUrl =
                format.playbackUrl ?: throw IOException("Audio stream is not available for this track.")

            // Basic validation of audio format compatibility
            val mimeType = format.mimeType.substringBefore(';').ifBlank { "audio/mp4" }
            if (!isFormatCompatible(mimeType)) {
                throw IOException("Unsupported audio format: $mimeType. Device may not support this as a ringtone.")
            }

            val extension =
                when {
                    "webm" in mimeType -> "webm"
                    "ogg" in mimeType -> "ogg"
                    "mpeg" in mimeType || "mp3" in mimeType -> "mp3"
                    else -> "m4a"
                }

            val displayName = "${sanitizeFileName(trackTitle)}.$extension"
            val resolver = context.contentResolver
            val collection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

            val values =
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.TITLE, trackTitle)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.Audio.Media.IS_RINGTONE, true)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                    put(MediaStore.Audio.Media.IS_ALARM, false)
                    put(MediaStore.Audio.Media.IS_MUSIC, false)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_RINGTONES}/Echofy",
                        )
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

            val itemUri =
                resolver.insert(collection, values)
                    ?: throw IOException("Unable to create entry in MediaStore for the ringtone.")

            val connection = openPlaybackConnection(playbackUrl)
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("Server returned error code: $responseCode")
                }

                connection.inputStream.use { input ->
                    resolver.openOutputStream(itemUri)?.use { output ->
                        input.copyTo(output)
                    } ?: throw IOException("Unable to open output stream for the ringtone file.")
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        itemUri,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        },
                        null,
                        null,
                    )
                }

                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    itemUri,
                )
            } catch (error: Throwable) {
                resolver.delete(itemUri, null, null)
                throw error
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun isFormatCompatible(mimeType: String): Boolean {
        val supported = listOf("audio/mpeg", "audio/mp3", "audio/m4a", "audio/mp4", "audio/ogg", "audio/wav", "audio/aac")
        return supported.any { it in mimeType.lowercase() } || mimeType.isBlank()
    }

    private fun openPlaybackConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connect()
        }

    private fun sanitizeFileName(input: String): String =
        input
            .trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .replace(Regex("\\s+"), " ")
            .ifBlank { "Echofy Ringtone" }
}
