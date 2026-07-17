package com.Chenkham.Echofy.utils

import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.innertube.models.ArtistItem
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.utils.completed
import com.Chenkham.innertube.utils.completedLibraryPage
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.ArtistEntity
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.Chenkham.Echofy.db.entities.PlaylistSongMap
import com.Chenkham.Echofy.db.entities.SongEntity
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.db.update
import com.Chenkham.Echofy.models.toMediaMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SyncUtils @Inject constructor(
    val database: MusicDatabase,
) {
    // Mutex to prevent concurrent syncSavedPlaylists calls from racing
    private val playlistSyncMutex = Mutex()

    suspend fun syncLikedSongs() {
        YouTube.playlist("LM").completed().onSuccess { page ->

            val songs = page.songs.reversed()

            database.likedSongsByNameAsc().first()
                .filterNot { it.id in songs.map(SongItem::id) }
                .forEach { database.update(it.song.localToggleLike()) }

            songs.forEach { song ->
                val dbSong = database.song(song.id).firstOrNull()
                database.transaction {
                    when (dbSong) {
                        null -> insert(song.toMediaMetadata(), SongEntity::localToggleLike)
                        else -> if (!dbSong.song.liked) update(dbSong.song.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncLibrarySongs() {
        YouTube.library("FEmusic_liked_videos").completedLibraryPage().onSuccess { page ->
            val songs = page.items.filterIsInstance<SongItem>().reversed()

            database.songsByNameAsc().first()
                .filterNot { it.id in songs.map(SongItem::id) }
                .forEach { database.update(it.song.toggleLibrary()) }

            songs.forEach { song ->
                val dbSong = database.song(song.id).firstOrNull()
                database.transaction {
                    when (dbSong) {
                        null -> insert(song.toMediaMetadata(), SongEntity::toggleLibrary)
                        else -> if (dbSong.song.inLibrary == null) update(dbSong.song.toggleLibrary())
                    }
                }
            }
        }
    }



    suspend fun syncUploadedSongs() {
        YouTube.library("FEmusic_library_privately_owned_tracks", tabIndex = 1).onSuccess { libraryPage ->
            val librarySongs = libraryPage.items.filterIsInstance<SongItem>()
            val uploadedSongs = database.uploadedSongsByNameAsc().first()
            uploadedSongs.filter { it.id !in librarySongs.map { song -> song.id } }.forEach { song ->
                database.query { update(song.song.toggleUploaded()) }
            }
            librarySongs.forEach { song ->
                database.query {
                    val songEntity = database.getSongById(song.id)
                    if (songEntity == null) {
                        insert(song.toMediaMetadata()) {
                            it.toggleUploaded()
                        }
                    } else if (!songEntity.song.isUploaded) {
                        update(songEntity.song.toggleUploaded())
                    }
                }
            }
        }
    }

    suspend fun syncLikedAlbums() {
        YouTube.library("FEmusic_liked_albums").completedLibraryPage().onSuccess { page ->
            val albums = page.items.filterIsInstance<AlbumItem>().reversed()

            database.albumsLikedByNameAsc().first()
                .filterNot { it.id in albums.map(AlbumItem::id) }
                .forEach { database.update(it.album.localToggleLike()) }

            albums.forEach { album ->
                val dbAlbum = database.album(album.id).firstOrNull()
                YouTube.album(album.browseId).onSuccess { albumPage ->
                    when (dbAlbum) {
                        null -> {
                            database.insert(albumPage)
                            database.album(album.id).firstOrNull()?.let {
                                database.update(it.album.localToggleLike())
                            }
                        }
                        else -> if (dbAlbum.album.bookmarkedAt == null)
                            database.update(dbAlbum.album.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncArtistsSubscriptions() {
        YouTube.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess { page ->
            val artists = page.items.filterIsInstance<ArtistItem>()

            database.artistsBookmarkedByNameAsc().first()
                .filterNot { it.id in artists.map(ArtistItem::id) }
                .forEach { database.update(it.artist.localToggleLike()) }

            artists.forEach { artist ->
                val dbArtist = database.artist(artist.id).firstOrNull()
                database.transaction {
                    when (dbArtist) {
                        null -> {
                            insert(
                                ArtistEntity(
                                    id = artist.id,
                                    name = artist.title,
                                    thumbnailUrl = artist.thumbnail,
                                    channelId = artist.channelId,
                                    bookmarkedAt = LocalDateTime.now()
                                )
                            )
                        }
                        else -> if (dbArtist.artist.bookmarkedAt == null)
                            update(dbArtist.artist.localToggleLike())
                    }
                }
            }
        }
    }

    suspend fun syncSavedPlaylists() = playlistSyncMutex.withLock {
        YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess { page ->
            val playlistList = page.items.filterIsInstance<PlaylistItem>()
                .filterNot { it.id == "LM" ||  it.id == "SE" }
                .reversed()

            // Step 1: Clean up any duplicate playlists sharing the same browseId
            // (caused by historical VL-prefix bug or race conditions)
            val dbPlaylists = database.playlistsByNameAsc().first()
            val groupedByRemote = dbPlaylists.filter { it.playlist.browseId != null }
                .groupBy { it.playlist.browseId!!.removePrefix("VL") }
            groupedByRemote.values.filter { it.size > 1 }.forEach { duplicates ->
                val sorted = duplicates.sortedBy { it.playlist.createdAt }
                sorted.drop(1).forEach { duplicate ->
                    database.delete(duplicate.playlist)
                }
            }

            // Step 2: Un-bookmark any local synced playlists that no longer exist on YTM
            val freshDbPlaylists = database.playlistsByNameAsc().first()
            freshDbPlaylists
                .filter { it.playlist.browseId != null }
                .filterNot { it.playlist.browseId!!.removePrefix("VL") in playlistList.map(PlaylistItem::id) }
                .forEach { database.update(it.playlist.localToggleLike()) }

            // Step 3: For each remote playlist, find or create the local entity
            playlistList.forEach { playlist ->
                // CRITICAL: Do a FRESH DB lookup for each playlist to avoid race conditions
                // with CreatePlaylistDialog which may have just inserted this browseId
                val existingPlaylist = database.playlistByBrowseId(playlist.id).firstOrNull()
                    ?: database.playlistByBrowseId("VL${playlist.id}").firstOrNull()

                val playlistEntity: PlaylistEntity
                if (existingPlaylist != null) {
                    // Found existing - update it with latest remote data and fix browseId if needed
                    playlistEntity = existingPlaylist.playlist
                    if (playlistEntity.browseId != playlist.id) {
                        // Fix any lingering VL-prefixed browseId
                        database.update(playlistEntity.copy(browseId = playlist.id))
                    }
                    database.update(playlistEntity, playlist)
                } else {
                    // Truly new playlist from YTM - create locally
                    playlistEntity = PlaylistEntity(
                        name = playlist.title,
                        browseId = playlist.id,
                        isEditable = playlist.isEditable,
                        bookmarkedAt = LocalDateTime.now(),
                        remoteSongCount = playlist.songCountText?.replace(Regex("""\D"""), "")?.toIntOrNull()
                    )
                    database.insert(playlistEntity)
                }

                syncPlaylist(playlist.id, playlistEntity.id)
            }
        }
    }

    suspend fun syncPlaylist(browseId: String, playlistId: String) {
        val playlistPage = YouTube.playlist(browseId).completed().getOrNull() ?: return
        database.transaction {
            clearPlaylist(playlistId)
            playlistPage.songs
                .map(SongItem::toMediaMetadata)
                .onEach(::insert)
                .mapIndexed { position, song ->
                    PlaylistSongMap(
                        songId = song.id,
                        playlistId = playlistId,
                        position = position,
                        setVideoId = song.setVideoId
                    )
                }.forEach(::insert)
        }
    }
}