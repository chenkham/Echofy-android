package com.Chenkham.Echofy.utils

import android.content.Context
import android.net.Uri
import com.Chenkham.Echofy.db.entities.EventWithSong
import java.time.format.DateTimeFormatter

/**
 * Serialises listening history to CSV or JSON so users can take their data elsewhere.
 *
 * Both writers stream straight into the [Uri] opened by the Storage Access Framework,
 * so a long history never has to be held in memory as one large string.
 */
object HistoryExporter {

    enum class Format(val mimeType: String, val extension: String) {
        CSV("text/csv", "csv"),
        JSON("application/json", "json"),
    }

    private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Writes [events] to [uri]. Returns the number of rows written, or null if the
     * document could not be opened.
     */
    fun export(
        context: Context,
        uri: Uri,
        events: List<EventWithSong>,
        format: Format,
    ): Int? {
        val stream = context.contentResolver.openOutputStream(uri) ?: return null

        stream.bufferedWriter().use { writer ->
            when (format) {
                Format.CSV -> {
                    writer.append("timestamp,title,artists,album,duration_seconds,play_time_ms,song_id\n")
                    events.forEach { entry ->
                        writer.append(entry.toCsvRow()).append('\n')
                    }
                }

                Format.JSON -> {
                    writer.append("[\n")
                    events.forEachIndexed { index, entry ->
                        writer.append("  ").append(entry.toJsonObject())
                        if (index != events.lastIndex) writer.append(',')
                        writer.append('\n')
                    }
                    writer.append("]\n")
                }
            }
        }

        return events.size
    }

    private fun EventWithSong.toCsvRow(): String {
        val artists = song.artists.joinToString("; ") { it.name }
        return listOf(
            event.timestamp.format(timestampFormatter),
            song.song.title,
            artists,
            song.album?.title.orEmpty(),
            (song.song.duration).toString(),
            event.playTime.toString(),
            song.song.id,
        ).joinToString(",") { it.csvEscaped() }
    }

    private fun EventWithSong.toJsonObject(): String {
        val artists = song.artists.joinToString(", ") { "\"${it.name.jsonEscaped()}\"" }
        return buildString {
            append('{')
            append("\"timestamp\":\"").append(event.timestamp.format(timestampFormatter)).append("\",")
            append("\"title\":\"").append(song.song.title.jsonEscaped()).append("\",")
            append("\"artists\":[").append(artists).append("],")
            append("\"album\":\"").append(song.album?.title.orEmpty().jsonEscaped()).append("\",")
            append("\"durationSeconds\":").append(song.song.duration).append(',')
            append("\"playTimeMs\":").append(event.playTime).append(',')
            append("\"songId\":\"").append(song.song.id.jsonEscaped()).append('"')
            append('}')
        }
    }

    /**
     * Quotes a CSV field only when it contains a delimiter, quote or newline, doubling
     * any embedded quotes as RFC 4180 requires. Song titles routinely contain commas.
     */
    private fun String.csvEscaped(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

    private fun String.jsonEscaped(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
}
