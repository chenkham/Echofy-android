package com.Chenkham.Echofy.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.SongItem
import com.Chenkham.innertube.models.YTItem
import com.Chenkham.innertube.models.filterExplicit
import com.Chenkham.Echofy.constants.HideExplicitKey
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.SearchHistory
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    // Recent songs for horizontal display (last 30 days, limit 10)
    // Recent songs for horizontal display (last 30 days, limit 10)
    private val recentSongsFlow = database.recentSearchSongs(limit = 10)
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            combine(
                query
                    .debounce(220)
                    .map { it.trim() }
                    .distinctUntilChanged()
                    .flatMapLatest { queryText ->
                    if (queryText.isEmpty()) {
                        database.searchHistory().map { history ->
                            Triple(history, emptyList<String>(), emptyList<YTItem>())
                        }
                    } else {
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val result = YouTube.searchSuggestions(queryText).getOrNull()
                        val matchedSongs = YouTube
                            .search(queryText, YouTube.SearchFilter.FILTER_SONG)
                            .getOrNull()
                            ?.items
                            ?.filterIsInstance<SongItem>()
                            .orEmpty()
                        val recommendedSongs = result
                            ?.recommendedItems
                            ?.filterIsInstance<SongItem>()
                            .orEmpty()
                        val songSuggestions = (recommendedSongs + matchedSongs)
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .take(3)

                        database
                            .searchHistory(queryText)
                            .map { it.take(3) }
                            .map { history ->
                                Triple(
                                    history,
                                    result?.queries
                                        ?.filter { q -> history.none { it.query == q } }
                                        ?.take(6)
                                        .orEmpty(),
                                    songSuggestions
                                )
                            }
                    }
                },
                recentSongsFlow
            ) { searchData, recentSongs ->
                SearchSuggestionViewState(
                    history = searchData.first,
                    suggestions = searchData.second,
                    items = searchData.third,
                    recentSongs = recentSongs
                )
            }.collect {
                _viewState.value = it
            }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
)
