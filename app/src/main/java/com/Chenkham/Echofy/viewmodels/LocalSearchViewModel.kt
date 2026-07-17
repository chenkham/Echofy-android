package com.Chenkham.Echofy.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.Album
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.db.entities.LocalItem
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel
@Inject
constructor(
    private val database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    val result =
        combine(query, filter) { query, filter ->
            query to filter
        }.flatMapLatest { (query, filter) ->
            if (query.isBlank()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                // Build a list of search queries: always include the full query first
                // (so exact phrase matches like "O Rangrez" still work), then add each
                // individual token. For a query like "pritam and pedro" this lets the
                // user find songs by Pritam or Pedro even when no song title contains
                // the full phrase.
                val tokens = query
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.length > 1 }
                    .distinct()
                val queries = (listOf(query.trim()) + tokens)
                    .filter { it.isNotBlank() }
                    .distinct()

                when (filter) {
                    LocalFilter.ALL ->
                        combine(
                            combineSearches(queries) { database.searchSongs(it, PREVIEW_SIZE) },
                            combineSearches(queries) { database.searchAlbums(it, PREVIEW_SIZE) },
                            combineSearches(queries) { database.searchArtists(it, PREVIEW_SIZE) },
                            combineSearches(queries) { database.searchPlaylists(it, PREVIEW_SIZE) },
                        ) { songs, albums, artists, playlists ->
                            songs + albums + artists + playlists
                        }

                    LocalFilter.SONG -> combineSearches(queries) { database.searchSongs(it) }
                    LocalFilter.ALBUM -> combineSearches(queries) { database.searchAlbums(it) }
                    LocalFilter.ARTIST -> combineSearches(queries) { database.searchArtists(it) }
                    LocalFilter.PLAYLIST -> combineSearches(queries) { database.searchPlaylists(it) }
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                            list.groupBy {
                                when (it) {
                                    is Song -> LocalFilter.SONG
                                    is Album -> LocalFilter.ALBUM
                                    is Artist -> LocalFilter.ARTIST
                                    is Playlist -> LocalFilter.PLAYLIST
                                }
                            },
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    /**
     * Runs [search] for each query in [queries] and merges the results, deduplicating
     * by item id. This is what enables multi-word queries such as "pritam and pedro"
     * to find songs by Pritam or Pedro even when no title contains the full phrase.
     */
    private fun <T : LocalItem> combineSearches(
        queries: List<String>,
        search: (String) -> Flow<List<T>>,
    ): Flow<List<T>> = when {
        queries.isEmpty() -> flowOf(emptyList())
        queries.size == 1 -> search(queries.first())
        else ->
            queries
                .map(search)
                .reduce { acc, flow ->
                    combine(acc, flow) { a, b -> (a + b).distinctBy { it.id } }
                }
    }

    companion object {
        const val PREVIEW_SIZE = 3
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
