package com.Chenkham.Echofy.db

import com.Chenkham.Echofy.constants.AlbumSortType
import com.Chenkham.Echofy.constants.ArtistSongSortType
import com.Chenkham.Echofy.constants.ArtistSortType
import com.Chenkham.Echofy.constants.PlaylistSortType
import com.Chenkham.Echofy.constants.SongSortType
import com.Chenkham.Echofy.db.entities.Album
import com.Chenkham.Echofy.db.entities.AlbumArtistMap
import com.Chenkham.Echofy.db.entities.AlbumEntity
import com.Chenkham.Echofy.db.entities.AlbumWithSongs
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.db.entities.ArtistEntity
import com.Chenkham.Echofy.db.entities.LyricsEntity
import com.Chenkham.Echofy.db.entities.PlayCountEntity
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.PlaylistEntity
import com.Chenkham.Echofy.db.entities.PlaylistSongMap
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.db.entities.SongAlbumMap
import com.Chenkham.Echofy.db.entities.SongArtistMap
import com.Chenkham.Echofy.db.entities.SongEntity
import com.Chenkham.Echofy.extensions.reversed
import com.Chenkham.Echofy.extensions.toSQLiteQuery
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.ui.utils.resize
import com.Chenkham.innertube.models.PlaylistItem
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.pages.AlbumPage
import com.Chenkham.innertube.pages.ArtistPage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.text.Collator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

import androidx.room.Transaction

fun DatabaseDao.songs(
    sortType: SongSortType,
    descending: Boolean,
) = when (sortType) {
    SongSortType.CREATE_DATE -> songsByCreateDateAsc()
    SongSortType.NAME ->
        songsByNameAsc().map { songs ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs.sortedWith(compareBy(collator) { it.song.title })
        }

    SongSortType.ARTIST ->
        songsByRowIdAsc().map { songs ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs
                .sortedWith(
                    compareBy(collator) { song ->
                        song.artists.joinToString(
                            "",
                        ) { it.name }
                    },
                ).groupBy { it.album?.title }
                .flatMap { (_, songsByAlbum) ->
                    songsByAlbum.sortedBy { album ->
                        album.artists.joinToString(
                            "",
                        ) { it.name }
                    }
                }
        }

    SongSortType.PLAY_TIME -> songsByPlayTimeAsc()
}.map { it.reversed(descending) }

fun DatabaseDao.likedSongs(
    sortType: SongSortType,
    descending: Boolean,
) = when (sortType) {
    SongSortType.CREATE_DATE -> likedSongsByCreateDateAsc()
    SongSortType.NAME ->
        likedSongsByNameAsc().map { songs ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs.sortedWith(compareBy(collator) { it.song.title })
        }

    SongSortType.ARTIST ->
        likedSongsByRowIdAsc().map { songs ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            songs
                .sortedWith(
                    compareBy(collator) { song ->
                        song.artists.joinToString(
                            "",
                        ) { it.name }
                    },
                ).groupBy { it.album?.title }
                .flatMap { (_, songsByAlbum) ->
                    songsByAlbum.sortedBy { album ->
                        album.artists.joinToString(
                            "",
                        ) { it.name }
                    }
                }
        }

    SongSortType.PLAY_TIME -> likedSongsByPlayTimeAsc()
}.map { it.reversed(descending) }

fun DatabaseDao.artistSongs(
    artistId: String,
    sortType: ArtistSongSortType,
    descending: Boolean,
) = when (sortType) {
    ArtistSongSortType.CREATE_DATE -> artistSongsByCreateDateAsc(artistId)
    ArtistSongSortType.NAME ->
        artistSongsByNameAsc(artistId).map { artistSongs ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            artistSongs.sortedWith(compareBy(collator) { it.song.title })
        }

    ArtistSongSortType.PLAY_TIME -> artistSongsByPlayTimeAsc(artistId)
}.map { it.reversed(descending) }

fun DatabaseDao.artists(
    sortType: ArtistSortType,
    descending: Boolean,
) = when (sortType) {
    ArtistSortType.CREATE_DATE -> artistsByCreateDateAsc()
    ArtistSortType.NAME ->
        artistsByNameAsc().map { artist ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            artist.sortedWith(compareBy(collator) { it.artist.name })
        }

    ArtistSortType.SONG_COUNT -> artistsBySongCountAsc()
    ArtistSortType.PLAY_TIME -> artistsByPlayTimeAsc()
}.map { artists ->
    artists
        .filter { it.artist.isYouTubeArtist }
        .reversed(descending)
}

fun DatabaseDao.artistsBookmarked(
    sortType: ArtistSortType,
    descending: Boolean,
) = when (sortType) {
    ArtistSortType.CREATE_DATE -> artistsBookmarkedByCreateDateAsc()
    ArtistSortType.NAME ->
        artistsBookmarkedByNameAsc().map { artist ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            artist.sortedWith(compareBy(collator) { it.artist.name })
        }

    ArtistSortType.SONG_COUNT -> artistsBookmarkedBySongCountAsc()
    ArtistSortType.PLAY_TIME -> artistsBookmarkedByPlayTimeAsc()
}.map { artists ->
    artists
        .filter { it.artist.isYouTubeArtist }
        .reversed(descending)
}

fun DatabaseDao.albums(
    sortType: AlbumSortType,
    descending: Boolean,
) = when (sortType) {
    AlbumSortType.CREATE_DATE -> albumsByCreateDateAsc()
    AlbumSortType.NAME ->
        albumsByNameAsc().map { albums ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            albums.sortedWith(compareBy(collator) { it.album.title })
        }

    AlbumSortType.ARTIST ->
        albumsByCreateDateAsc().map { albums ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            albums.sortedWith(compareBy(collator) { album -> album.artists.joinToString("") { it.name } })
        }

    AlbumSortType.YEAR -> albumsByYearAsc()
    AlbumSortType.SONG_COUNT -> albumsBySongCountAsc()
    AlbumSortType.LENGTH -> albumsByLengthAsc()
    AlbumSortType.PLAY_TIME -> albumsByPlayTimeAsc()
}.map { it.reversed(descending) }

fun DatabaseDao.albumsLiked(
    sortType: AlbumSortType,
    descending: Boolean,
) = when (sortType) {
    AlbumSortType.CREATE_DATE -> albumsLikedByCreateDateAsc()
    AlbumSortType.NAME ->
        albumsLikedByNameAsc().map { albums ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            albums.sortedWith(compareBy(collator) { it.album.title })
        }

    AlbumSortType.ARTIST ->
        albumsLikedByCreateDateAsc().map { albums ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            albums.sortedWith(compareBy(collator) { album -> album.artists.joinToString("") { it.name } })
        }

    AlbumSortType.YEAR -> albumsLikedByYearAsc()
    AlbumSortType.SONG_COUNT -> albumsLikedBySongCountAsc()
    AlbumSortType.LENGTH -> albumsLikedByLengthAsc()
    AlbumSortType.PLAY_TIME -> albumsLikedByPlayTimeAsc()
}.map { it.reversed(descending) }

fun DatabaseDao.playlists(
    sortType: PlaylistSortType,
    descending: Boolean,
) = when (sortType) {
    PlaylistSortType.CREATE_DATE -> playlistsByCreateDateAsc()
    PlaylistSortType.NAME ->
        playlistsByNameAsc().map { playlists ->
            val collator = Collator.getInstance(Locale.getDefault())
            collator.strength = Collator.PRIMARY
            playlists.sortedWith(compareBy(collator) { it.playlist.name })
        }

    PlaylistSortType.SONG_COUNT -> playlistsBySongCountAsc()
    PlaylistSortType.LAST_UPDATED -> playlistsByUpdatedDateAsc()
}.map { it.reversed(descending) }

suspend fun DatabaseDao.getLyrics(id: String?): LyricsEntity? {
    return lyrics(id).first()
}

/** Increment by one the play count with today's year and month. */
fun DatabaseDao.incrementPlayCount(songId: String) {
    val time = LocalDateTime.now().atOffset(ZoneOffset.UTC)
    var oldCount: Int
    runBlocking {
        oldCount = getPlayCountByMonth(songId, time.year, time.monthValue).first()
    }

    // add new
    if (oldCount <= 0) {
        insert(PlayCountEntity(songId, time.year, time.monthValue, 0))
    }
    incrementPlayCount(songId, time.year, time.monthValue)
}

fun DatabaseDao.addSongToPlaylist(playlist: Playlist, songIds: List<String>) {
    var position = playlist.songCount
    songIds.forEach { id ->
        val songExists = getSongById(id) != null
        if (songExists) {
            insert(
                PlaylistSongMap(
                    songId = id,
                    playlistId = playlist.id,
                    position = position++
                )
            )
        } else {
            android.util.Log.e("DatabaseDaoExt", "Skipping adding missing song $id to playlist ${playlist.id}")
        }
    }
}

fun DatabaseDao.insert(
    mediaMetadata: MediaMetadata,
    block: (SongEntity) -> SongEntity = { it },
) {
    if (insert(mediaMetadata.toSongEntity().let(block)) == -1L) return
    mediaMetadata.artists.forEachIndexed { index, artist ->
        val artistId =
            artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
        insert(
            ArtistEntity(
                id = artistId,
                name = artist.name,
            ),
        )
        insert(
            SongArtistMap(
                songId = mediaMetadata.id,
                artistId = artistId,
                position = index,
            ),
        )
    }
}

fun DatabaseDao.insert(albumPage: AlbumPage) {
    if (insert(
            AlbumEntity(
                id = albumPage.album.browseId,
                playlistId = albumPage.album.playlistId,
                title = albumPage.album.title,
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songCount = albumPage.songs.size,
                duration = albumPage.songs.sumOf { it.duration ?: 0 },
            ),
        ) == -1L
    ) {
        return
    }
    albumPage.songs
        .map(SongItem::toMediaMetadata)
        .onEach(::insert)
        .onEach {
            val existingSong = getSongById(it.id)
            if (existingSong != null) {
                update(existingSong, it)
            }
        }.mapIndexed { index, song ->
            SongAlbumMap(
                songId = song.id,
                albumId = albumPage.album.browseId,
                index = index,
            )
        }.forEach(::upsert)
    albumPage.album.artists
        ?.map { artist ->
            ArtistEntity(
                id = artist.id ?: artistByName(artist.name)?.id
                ?: ArtistEntity.generateArtistId(),
                name = artist.name,
            )
        }?.onEach(::insert)
        ?.mapIndexed { index, artist ->
            AlbumArtistMap(
                albumId = albumPage.album.browseId,
                artistId = artist.id,
                order = index,
            )
        }?.forEach(::insert)
}

fun DatabaseDao.update(
    song: Song,
    mediaMetadata: MediaMetadata,
) {
    update(
        song.song.copy(
            title = mediaMetadata.title,
            duration = mediaMetadata.duration,
            thumbnailUrl = mediaMetadata.thumbnailUrl,
            albumId = mediaMetadata.album?.id,
            albumName = mediaMetadata.album?.title,
        ),
    )
    songArtistMap(song.id).forEach(::delete)
    mediaMetadata.artists.forEachIndexed { index, artist ->
        val artistId =
            artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
        insert(
            ArtistEntity(
                id = artistId,
                name = artist.name,
            ),
        )
        insert(
            SongArtistMap(
                songId = mediaMetadata.id,
                artistId = artistId,
                position = index,
            ),
        )
    }
}

fun DatabaseDao.update(
    artist: ArtistEntity,
    artistPage: ArtistPage,
) {
    update(
        artist.copy(
            name = artistPage.artist.title,
            thumbnailUrl = artistPage.artist.thumbnail.resize(544, 544),
            lastUpdateTime = LocalDateTime.now(),
        ),
    )
}

fun DatabaseDao.update(
    album: AlbumEntity,
    albumPage: AlbumPage,
    artists: List<ArtistEntity>? = emptyList(),
) {
    update(
        album.copy(
            id = albumPage.album.browseId,
            playlistId = albumPage.album.playlistId,
            title = albumPage.album.title,
            year = albumPage.album.year,
            thumbnailUrl = albumPage.album.thumbnail,
            songCount = albumPage.songs.size,
            duration = albumPage.songs.sumOf { it.duration ?: 0 },
        ),
    )
    if (artists?.size != albumPage.album.artists?.size) {
        artists?.forEach(::delete)
    }
    albumPage.songs
        .map(SongItem::toMediaMetadata)
        .onEach(::insert)
        .onEach {
            val existingSong = getSongById(it.id)
            if (existingSong != null) {
                update(existingSong, it)
            }
        }.mapIndexed { index, song ->
            SongAlbumMap(
                songId = song.id,
                albumId = albumPage.album.browseId,
                index = index,
            )
        }.forEach(::upsert)

    albumPage.album.artists?.let { artists ->
        // Recreate album artists
        albumArtistMaps(album.id).forEach(::delete)
        artists
            .map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistByName(artist.name)?.id
                    ?: ArtistEntity.generateArtistId(),
                    name = artist.name,
                )
            }.onEach(::insert)
            .mapIndexed { index, artist ->
                AlbumArtistMap(
                    albumId = albumPage.album.browseId,
                    artistId = artist.id,
                    order = index,
                )
            }.forEach(::insert)
    }
}

fun DatabaseDao.update(playlistEntity: PlaylistEntity, playlistItem: PlaylistItem) {
    update(
        playlistEntity.copy(
            name = playlistItem.title,
            browseId = playlistItem.id,
            isEditable = playlistItem.isEditable,
            remoteSongCount = playlistItem.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
            playEndpointParams = playlistItem.playEndpoint?.params,
            shuffleEndpointParams = playlistItem.shuffleEndpoint?.params,
            radioEndpointParams = playlistItem.radioEndpoint?.params
        )
    )
}

suspend fun DatabaseDao.updateArtistSongsCount(artistId: String) {
    val count = getSongCountForArtist(artistId)
    updateArtistSongCount(artistId, count)
}

fun DatabaseDao.checkpoint() {
    raw("PRAGMA wal_checkpoint(FULL)".toSQLiteQuery())
}
