package com.Chenkham.Echofy.utils

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.ArtistEntity
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.db.entities.SongArtistMap
import com.Chenkham.Echofy.db.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Stable id prefix for songs sourced from on-device MediaStore audio. */
private const val LOCAL_SONG_ID_PREFIX = "LM"

/** Sentinel meaning "no folder filter" in preference sets. */
private const val ALL_FOLDERS_SENTINEL = "Todas"

/**
 * Scans the device's [MediaStore.Audio.Media] for playable audio files and upserts them into
 * the app's [MusicDatabase] as regular [SongEntity] rows (isLocal = true), so they show up in
 * the library/liked-songs UI alongside YouTube Music tracks.
 */
object LocalMediaScanner {

    /**
     * @param selectedFolders when non-empty (and not containing the "all folders" sentinel),
     * restricts both the MediaStore query and the dead-row cleanup below to files whose parent
     * folder name matches one of these — the same "last path segment" matching used by the
     * folder picker UI. Tracks outside the selected folders are left untouched (neither
     * re-verified nor deleted).
     */
    suspend fun scan(
        context: Context,
        database: MusicDatabase,
        selectedFolders: Set<String> = emptySet(),
    ): Int = withContext(Dispatchers.IO) {
        val folderFilter = selectedFolders.takeUnless { it.isEmpty() || ALL_FOLDERS_SENTINEL in it }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA,
        )
        val selection = buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            if (folderFilter != null) {
                append(" AND (")
                append(folderFilter.joinToString(" OR ") { "${MediaStore.Audio.Media.DATA} LIKE ?" })
                append(")")
            }
        }
        val selectionArgs = folderFilter?.map { "%/$it/%" }?.toTypedArray()

        var count = 0
        val seenSongIds = mutableSetOf<String>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumArtistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    mediaStoreId,
                )
                val storeTitle = cursor.getString(titleCol)
                val storeArtist = cursor.getString(artistCol)?.takeUnless { it.isUnknownTag() }
                val storeAlbumArtist = cursor.getString(albumArtistCol)?.takeUnless { it.isUnknownTag() }
                val albumName = cursor.getString(albumCol)?.takeUnless { it.isUnknownTag() }
                val storeYear = cursor.getInt(yearCol).takeIf { it > 0 }
                val durationMs = cursor.getLong(durationCol)
                val dateModifiedSec = cursor.getLong(dateModifiedCol)
                val filePath = cursor.getString(dataCol)

                val needsTagFallback = storeTitle.isNullOrBlank() || storeArtist == null || storeYear == null
                val tags = if (needsTagFallback) extractEmbeddedTags(context, contentUri) else null

                val title = storeTitle?.takeUnless { it.isBlank() } ?: tags?.title ?: "Unknown title"
                val artistName = storeArtist
                    ?: storeAlbumArtist
                    ?: tags?.artist
                    ?: tags?.albumArtist
                    ?: "Unknown artist"
                val year = storeYear ?: tags?.year

                val thumbnailUrl = tags?.embeddedArt?.let { art -> saveEmbeddedArt(context, contentUri, art) }
                    ?: contentUri.toString()

                val songId = "$LOCAL_SONG_ID_PREFIX$mediaStoreId"
                seenSongIds += songId
                val now = LocalDateTime.now()
                val dateModified = if (dateModifiedSec > 0) {
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(dateModifiedSec), ZoneId.systemDefault())
                } else {
                    null
                }

                val localPath = if (!filePath.isNullOrBlank()) {
                    Uri.fromFile(File(filePath)).toString()
                } else {
                    contentUri.toString()
                }

                upsertLocalSong(
                    database = database,
                    songId = songId,
                    title = title,
                    artistName = artistName,
                    durationSec = (durationMs / 1000).toInt(),
                    thumbnailUrl = thumbnailUrl,
                    albumName = albumName,
                    year = year,
                    dateModified = dateModified,
                    localPath = localPath,
                    now = now,
                )
                count++
            }
        }

        pruneMissingLocalSongs(
            context = context,
            database = database,
            scannedSongIds = seenSongIds,
            folderFilter = folderFilter,
        )

        count
    }

    private suspend fun pruneMissingLocalSongs(
        context: Context,
        database: MusicDatabase,
        scannedSongIds: Set<String>,
        folderFilter: Set<String>?,
    ) {
        val existingLocal = database.localSongs().first()
        existingLocal
            .filter { songItem ->
                if (songItem.song.id in scannedSongIds) return@filter false
                if (folderFilter != null) {
                    val folder = folderNameOfLocalPath(songItem.song.localPath)
                    if (folder == null || folder !in folderFilter) return@filter false
                }
                val path = songItem.song.localPath ?: return@filter true
                val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return@filter true
                val isAlive = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { true }
                }.getOrNull() ?: false
                !isAlive
            }
            .forEach { database.delete(it.song) }
    }

    private fun folderNameOfLocalPath(localPath: String?): String? {
        if (localPath.isNullOrBlank()) return null
        val filePath = runCatching { Uri.parse(localPath) }.getOrNull()?.path?.takeIf { it.isNotBlank() }
            ?: return null
        return runCatching { File(filePath).parentFile?.name }.getOrNull()
    }

    suspend fun listAvailableFolders(context: Context): List<String> = withContext(Dispatchers.IO) {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val folders = mutableSetOf<String>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null,
        )?.use { cursor ->
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataCol) ?: continue
                File(filePath).parentFile?.name?.takeIf { it.isNotBlank() }?.let { folders += it }
            }
        }

        folders.sorted()
    }

    suspend fun scanUri(context: Context, database: MusicDatabase, uri: Uri): Song? = withContext(Dispatchers.IO) {
        var displayName: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameCol = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameCol >= 0 && cursor.moveToFirst()) {
                displayName = cursor.getString(nameCol)
            }
        }

        val tags = extractEmbeddedTags(context, uri)
        val title = tags?.title
            ?: displayName?.substringBeforeLast('.')?.takeUnless { it.isBlank() }
            ?: "Unknown title"
        val artistName = tags?.artist ?: tags?.albumArtist ?: "Unknown artist"
        val thumbnailUrl = tags?.embeddedArt?.let { art -> saveEmbeddedArt(context, uri, art) }

        val songId = "$LOCAL_SONG_ID_PREFIX${uri.toString().stableHash()}"
        upsertLocalSong(
            database = database,
            songId = songId,
            title = title,
            artistName = artistName,
            durationSec = tags?.durationMs?.takeIf { it > 0 }?.let { (it / 1000).toInt() } ?: -1,
            thumbnailUrl = thumbnailUrl,
            albumName = tags?.album,
            year = tags?.year,
            dateModified = null,
            localPath = uri.toString(),
            now = LocalDateTime.now(),
        )

        database.song(songId).first()
    }

    private suspend fun upsertLocalSong(
        database: MusicDatabase,
        songId: String,
        title: String,
        artistName: String,
        durationSec: Int,
        thumbnailUrl: String?,
        albumName: String?,
        year: Int?,
        dateModified: LocalDateTime?,
        localPath: String,
        now: LocalDateTime,
    ) {
        val existing = database.song(songId).first()

        database.upsert(
            SongEntity(
                id = songId,
                title = title,
                duration = durationSec,
                thumbnailUrl = thumbnailUrl ?: existing?.song?.thumbnailUrl,
                albumName = albumName,
                year = year,
                dateModified = dateModified,
                liked = existing?.song?.liked ?: false,
                likedDate = existing?.song?.likedDate,
                inLibrary = existing?.song?.inLibrary ?: now,
                totalPlayTime = existing?.song?.totalPlayTime ?: 0,
                isLocal = true,
                localPath = localPath,
            ),
        )

        val currentMaps = database.songArtistMap(songId)
        val currentArtistNames =
            currentMaps.mapNotNull { database.artist(it.artistId).first()?.artist?.name }
        if (currentMaps.isEmpty() || currentArtistNames != listOf(artistName)) {
            currentMaps.forEach { database.delete(it) }
            val artistId = database.artistByName(artistName)?.id
                ?: ArtistEntity.generateArtistId()
            database.insert(ArtistEntity(id = artistId, name = artistName))
            database.insert(SongArtistMap(songId = songId, artistId = artistId, position = 0))
        }
    }

    private fun String.stableHash(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun saveEmbeddedArt(context: Context, sourceUri: Uri, art: ByteArray): String? = runCatching {
        val dir = File(context.filesDir, "local_album_art").apply { mkdirs() }
        val file = File(dir, "${sourceUri.toString().stableHash()}.jpg")
        FileOutputStream(file).use { it.write(art) }
        Uri.fromFile(file).toString()
    }.getOrNull()

    private fun String.isUnknownTag() = isBlank() || equals("<unknown>", ignoreCase = true)

    private data class EmbeddedTags(
        val title: String?,
        val artist: String?,
        val albumArtist: String?,
        val album: String?,
        val year: Int?,
        val durationMs: Long?,
        val embeddedArt: ByteArray?,
    )

    private fun extractEmbeddedTags(context: Context, uri: Uri): EmbeddedTags? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(context, uri)
            EmbeddedTags(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeUnless { it.isUnknownTag() },
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.takeUnless { it.isUnknownTag() },
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    ?.takeUnless { it.isUnknownTag() }
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                        ?.takeUnless { it.isUnknownTag() }
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
                        ?.takeUnless { it.isUnknownTag() },
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.takeUnless { it.isUnknownTag() },
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?.trim()
                    ?.take(4)
                    ?.toIntOrNull(),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull(),
                embeddedArt = retriever.embeddedPicture,
            )
        }.also {
            runCatching { retriever.release() }
        }.getOrNull()
    }
}
