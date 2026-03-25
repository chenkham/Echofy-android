package com.Chenkham.Echofy.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ChipSortTypeKey
import com.Chenkham.Echofy.constants.LibraryFilter
import com.Chenkham.Echofy.ui.component.ChipsRow
import com.Chenkham.Echofy.ui.component.BannerAdView
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.ui.component.BackpaperBackground


@Composable
fun LibraryScreen(
    navController: NavController,
    adManager: AdManager? = null,
) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

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
                modifier = Modifier.weight(1f),
            )
        }
    }



    BackpaperBackground(screen = BackpaperScreen.LIBRARY) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Main content takes remaining space
        Box(
            modifier = Modifier.weight(1f),
        ) {
            when (filterType) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
                LibraryFilter.SONGS -> LibrarySongsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })

                LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                    navController,
                    { filterType = LibraryFilter.LIBRARY })
            }
        }
        
        // Banner ad at the bottom
        adManager?.let { manager ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                BannerAdView(
                    adManager = manager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
}
