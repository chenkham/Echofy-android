package com.Chenkham.Echofy.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.ChipSortTypeKey
import com.Chenkham.Echofy.constants.LibraryFilter
import com.Chenkham.Echofy.ui.component.BackpaperBackground
import com.Chenkham.Echofy.ui.component.ChipsRow
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.viewmodels.LibraryAlbumsViewModel
import com.Chenkham.Echofy.viewmodels.LibraryArtistsViewModel
import com.Chenkham.Echofy.viewmodels.LibraryMixViewModel
import com.Chenkham.Echofy.viewmodels.LibraryPlaylistsViewModel
import com.Chenkham.Echofy.viewmodels.LibrarySongsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val libraryMixViewModel: LibraryMixViewModel = hiltViewModel()
    val libraryPlaylistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()
    val librarySongsViewModel: LibrarySongsViewModel = hiltViewModel()
    val libraryAlbumsViewModel: LibraryAlbumsViewModel = hiltViewModel()
    val libraryArtistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips =
                    listOf(
                        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                        LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    val onRefresh: () -> Unit = remember(
        filterType,
        libraryMixViewModel,
        libraryPlaylistsViewModel,
        librarySongsViewModel,
        libraryAlbumsViewModel,
        libraryArtistsViewModel,
    ) {
        {
            coroutineScope.launch {
                isRefreshing = true
                try {
                    when (filterType) {
                        LibraryFilter.LIBRARY -> libraryMixViewModel.refresh().join()
                        LibraryFilter.PLAYLISTS -> libraryPlaylistsViewModel.refresh().join()
                        LibraryFilter.SONGS -> librarySongsViewModel.refresh().join()
                        LibraryFilter.ALBUMS -> libraryAlbumsViewModel.refresh().join()
                        LibraryFilter.ARTISTS -> libraryArtistsViewModel.refresh().join()
                    }
                } finally {
                    isRefreshing = false
                }
            }
            Unit
        }
    }

    BackpaperBackground(screen = BackpaperScreen.LIBRARY) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                ),
            contentAlignment = Alignment.TopStart,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                ) {
                    when (filterType) {
                        LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
                        LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
                        LibraryFilter.SONGS -> LibrarySongsScreen(
                            navController,
                            { filterType = LibraryFilter.LIBRARY },
                        )

                        LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                            navController,
                            { filterType = LibraryFilter.LIBRARY },
                        )

                        LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                            navController,
                            { filterType = LibraryFilter.LIBRARY },
                        )
                    }
                }


            }

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
    }
}
