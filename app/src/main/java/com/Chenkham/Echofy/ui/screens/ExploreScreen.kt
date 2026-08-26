package com.Chenkham.Echofy.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.ui.component.BannerAdView
import com.Chenkham.Echofy.constants.AmbientSoundsEnabledKey
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.MixcloudEnabledKey
import com.Chenkham.Echofy.constants.RadioEnabledKey
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.ui.component.BackpaperBackground
import com.Chenkham.Echofy.ui.component.ErrorScreen
import com.Chenkham.Echofy.ui.component.NavigationTitle
import com.Chenkham.Echofy.utils.rememberPreference
import com.Chenkham.Echofy.viewmodels.AmbientSoundsViewModel
import com.Chenkham.Echofy.viewmodels.ChartsViewModel
import com.Chenkham.Echofy.viewmodels.MixcloudViewModel
import com.Chenkham.Echofy.viewmodels.MoodAndGenresViewModel
import com.Chenkham.Echofy.viewmodels.PodcastsViewModel
import com.Chenkham.Echofy.viewmodels.RadioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    initialTab: String? = null,
    adManager: AdManager? = null,
    viewModel: MoodAndGenresViewModel = hiltViewModel()
) {
    val chartsViewModel: ChartsViewModel = hiltViewModel()
    val podcastsViewModel: PodcastsViewModel = hiltViewModel()
    val radioViewModel: RadioViewModel = hiltViewModel()

    val (radioEnabled) = rememberPreference(RadioEnabledKey, defaultValue = true)
    val (mixesEnabled) = rememberPreference(MixcloudEnabledKey, defaultValue = false)
    val (ambientEnabled) = rememberPreference(AmbientSoundsEnabledKey, defaultValue = false)

    val tabs = remember(radioEnabled, mixesEnabled, ambientEnabled) {
        buildList {
            add(ExploreTabItem("genres", R.string.genres, R.drawable.explore))
            add(ExploreTabItem("charts", R.string.charts, R.drawable.trending_up))
            add(ExploreTabItem("podcasts", R.string.podcasts, R.drawable.podcast))
            if (radioEnabled) {
                add(ExploreTabItem("radio", R.string.radio, R.drawable.radio))
            }
            if (mixesEnabled) {
                add(ExploreTabItem("mixes", R.string.dj_mixes, R.drawable.graphic_eq))
            }
            if (ambientEnabled) {
                add(ExploreTabItem("ambient", R.string.ambient_sounds, R.drawable.music_note))
            }
        }
    }
    var selectedTab by rememberSaveable {
        mutableStateOf(
            tabs.indexOfFirst { it.key == initialTab }.takeIf { it >= 0 } ?: 0
        )
    }

    val mixesViewModel: MixcloudViewModel = hiltViewModel()
    val ambientViewModel: AmbientSoundsViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    val onRefresh: () -> Unit = {
        coroutineScope.launch {
            isRefreshing = true
            when (tabs.getOrNull(selectedTab)?.key) {
                "genres" -> viewModel.retry()
                "charts" -> chartsViewModel.loadCharts()
                "podcasts" -> podcastsViewModel.loadPodcasts()
                "radio" -> radioViewModel.loadTopStations()
                "mixes" -> mixesViewModel.loadMixes()
                "ambient" -> ambientViewModel.loadSounds()
            }
            delay(500)
            isRefreshing = false
        }
    }

    BackpaperBackground(screen = BackpaperScreen.EXPLORE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = LocalPlayerAwareWindowInsets.current
                        .asPaddingValues()
                        .calculateTopPadding()
                )
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ExploreTabSelector(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Tab content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    val tabKey = tabs.getOrNull(selectedTab)?.key
                    when (tabKey) {
                        "genres" -> GenresTab(navController, viewModel)
                        "charts" -> ChartsTab(navController, chartsViewModel)
                        "podcasts" -> PodcastsTab(navController, podcastsViewModel)
                        "radio" -> RadioTab(navController, radioViewModel)
                        "mixes" -> MixesTab(mixesViewModel)
                        "ambient" -> AmbientTab(ambientViewModel)
                    }
                }

                // Outside the weighted tab content and above the player-aware bottom
                // inset, so it never covers the bottom navigation bar.
                adManager?.let { manager ->
                    BannerAdView(
                        adManager = manager,
                        modifier = Modifier.padding(
                            bottom = LocalPlayerAwareWindowInsets.current
                                .asPaddingValues()
                                .calculateBottomPadding()
                        )
                    )
                }
            }

            Indicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
                isRefreshing = isRefreshing,
                state = pullRefreshState
            )
        }
    }
}

private data class ExploreTabItem(
    val key: String,
    val titleRes: Int,
    val icon: Int,
)

@Composable
private fun ExploreTabSelector(
    tabs: List<ExploreTabItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        itemsIndexed(tabs) { index, tab ->
            val selected = selectedTab == index
            FilterChip(
                selected = selected,
                onClick = { onTabSelected(index) },
                label = {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = null,
            )
        }
    }
}

@Composable
private fun GenresTab(
    navController: NavController,
    viewModel: MoodAndGenresViewModel
) {
    val localConfiguration = LocalConfiguration.current
    val isLandscape = localConfiguration.orientation == ORIENTATION_LANDSCAPE
    val gridCells = if (isLandscape) GridCells.Adaptive(150.dp) else GridCells.Fixed(2)

    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LazyVerticalGrid(
        columns = gridCells,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding().let { bottom ->
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = bottom + 16.dp
            )
        },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isLoading -> {
                items(10) {
                    Box(
                        modifier = Modifier
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
            moodAndGenresList == null && error != null -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorScreen(
                        message = error ?: "Unknown error",
                        onRetry = viewModel::retry
                    )
                }
            }
            moodAndGenresList.isNullOrEmpty() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyStateContent(
                        icon = R.drawable.explore,
                        message = "Unable to load genres"
                    )
                }
            }
            else -> {
                moodAndGenresList?.forEachIndexed { sectionIdx, moodAndGenres ->
                    item(
                        key = "title_${sectionIdx}_${moodAndGenres.title}",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        NavigationTitle(
                            title = moodAndGenres.title,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                    }

                    itemsIndexed(
                        items = moodAndGenres.items,
                        key = { index, item -> "${moodAndGenres.title}_${sectionIdx}_${item.endpoint.params ?: item.title}_$index" }
                    ) { index, item ->
                        GenreCard(
                            title = item.title,
                            color = item.stripeColor,
                            onClick = {
                                navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartsTab(
    navController: NavController,
    viewModel: ChartsViewModel
) {
    val charts by viewModel.charts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val countryList by viewModel.countryList.collectAsState()

    var showCountryMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (error != null) {
             ErrorScreen(
                message = error ?: "Unknown error",
                onRetry = { viewModel.loadCharts() }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding().let { bottom ->
                    PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 80.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                 // Country Selector
                item(span = { GridItemSpan(maxLineSpan) }) {
                     Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                         FilterChip(
                             selected = true,
                             onClick = { showCountryMenu = true },
                             label = { 
                                 val displayName = if (selectedCountry == "ZZ") "Global" else (countryList[selectedCountry] ?: selectedCountry)
                                 Text(displayName)
                             },
                             leadingIcon = { Icon(painterResource(R.drawable.location_on), contentDescription = null) },
                             modifier = Modifier.align(Alignment.CenterEnd)
                         )
                         
                         androidx.compose.material3.DropdownMenu(
                             expanded = showCountryMenu,
                             onDismissRequest = { showCountryMenu = false }
                         ) {
                             countryList.forEach { (code, name) ->
                                 androidx.compose.material3.DropdownMenuItem(
                                     text = { Text(name) },
                                     onClick = {
                                         viewModel.loadCharts(code)
                                         showCountryMenu = false
                                     },
                                     leadingIcon = if (code == selectedCountry) {
                                         { Icon(painterResource(R.drawable.check), contentDescription = null) }
                                     } else null
                                 )
                             }
                         }
                     }
                }

                charts?.sections?.forEachIndexed { index, section ->
                    if (section.items.isNotEmpty()) {
                        val firstItem = section.items.first()
                        val isArtistSection = firstItem.browseId.startsWith("UC") && firstItem.playlistId == null
                        val hasSubscribers = firstItem.subscribers != null
                        
                        val sectionTitle = when {
                            isArtistSection || hasSubscribers -> "Top artists"
                            section.title.isNotBlank() && !section.title.contains("Top artists", ignoreCase = true) -> section.title
                            section.items.all { it.playlistId != null } -> {
                                when {
                                    section.title.contains("video", ignoreCase = true) -> "Top music videos"
                                    section.title.contains("trend", ignoreCase = true) -> "Trending"
                                    index == 0 -> "Trending"
                                    index == 1 -> "Top music videos" 
                                    else -> section.title.ifBlank { "Charts" }
                                }
                            }
                            else -> section.title.ifBlank { "Charts" }
                        }
                        
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        itemsIndexed(
                            items = section.items,
                            key = { index, item -> "${item.browseId}_$index" }
                        ) { index, item ->
                            GenreCard(
                                title = item.title,
                                imageUrl = item.thumbnails.lastOrNull()?.url,
                                color = 0,
                                onClick = {
                                    if (item.playlistId != null) {
                                         navController.navigate("online_playlist/${item.playlistId}")
                                    } else if (item.browseId.startsWith("UC")) {
                                         navController.navigate("artist/${item.browseId}")
                                    } else {
                                         if (item.browseId.startsWith("VL") || item.browseId.startsWith("PL")) {
                                             navController.navigate("online_playlist/${item.browseId}")
                                         } else {
                                             navController.navigate("youtube_browse/${item.browseId}")
                                         }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastsTab(
    navController: NavController,
    viewModel: PodcastsViewModel
) {
    val podcastPage by viewModel.podcastPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
         if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (error != null) {
             ErrorScreen(
                message = error ?: "Unavailable",
                onRetry = { viewModel.loadPodcasts() }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding().let { bottom ->
                    PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 80.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                podcastPage?.sections?.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    itemsIndexed(
                        items = section.items,
                        key = { index, item -> "${item.podcastId ?: item.browseId}_$index" }
                    ) { index, item ->
                        GenreCard(
                            title = item.title,
                            imageUrl = item.thumbnails.lastOrNull()?.url,
                            color = 0,
                            onClick = { 
                                val id = item.podcastId ?: item.browseId
                                if (!id.isNullOrBlank()) {
                                    navController.navigate("podcast/${Uri.encode(id)}")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioTab(
    navController: NavController,
    viewModel: RadioViewModel
) {
    val stations by viewModel.stations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    val playerConnection = LocalPlayerConnection.current ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (error != null) {
            ErrorScreen(
                message = error ?: "Unavailable",
                onRetry = { viewModel.loadTopStations() }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding().let { bottom ->
                    PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 80.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { viewModel.loadTopStations() },
                                label = { Text(stringResource(R.string.radio_popular)) }
                            )
                        }
                        items(tags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { viewModel.selectTag(tag) },
                                label = { Text(tag.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                items(
                    items = stations,
                    key = { it.stationuuid }
                ) { station ->
                    GenreCard(
                        title = station.name,
                        imageUrl = station.favicon?.takeIf { it.isNotBlank() },
                        color = 0L,
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = station.name,
                                    items = listOf(station.toMediaItem())
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MixesTab(
    viewModel: MixcloudViewModel = hiltViewModel()
) {
    val mixes by viewModel.mixes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (error != null) {
            ErrorScreen(
                message = error ?: "Unavailable",
                onRetry = { viewModel.loadMixes() }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding().let { bottom ->
                    PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 80.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTag == null,
                                onClick = { viewModel.selectTag(null) },
                                label = { Text(stringResource(R.string.radio_popular)) }
                            )
                        }
                        items(items = viewModel.availableTags.keys.toList()) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = { viewModel.selectTag(tag) },
                                label = { Text(viewModel.availableTags[tag] ?: tag) }
                            )
                        }
                    }
                }

                items(
                    items = mixes,
                    key = { it.key }
                ) { mix ->
                    GenreCard(
                        title = mix.title,
                        imageUrl = mix.thumbnailUrl,
                        color = 0L,
                        onClick = {
                            if (playerConnection != null) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val query = if (mix.artistName.isNotBlank()) "${mix.title} ${mix.artistName}" else mix.title
                                    val songItem = com.arturo254.opentune.innertube.YouTube.search(query, com.arturo254.opentune.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                        ?.items?.filterIsInstance<com.arturo254.opentune.innertube.models.SongItem>()?.firstOrNull()
                                        ?: com.arturo254.opentune.innertube.YouTube.search(query, com.arturo254.opentune.innertube.YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                                            ?.items?.filterIsInstance<com.arturo254.opentune.innertube.models.SongItem>()?.firstOrNull()

                                    withContext(Dispatchers.Main) {
                                        if (songItem != null) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = mix.title,
                                                    items = listOf(songItem.toMediaItem())
                                                )
                                            )
                                        } else {
                                            uriHandler.openUri(mix.webUrl)
                                        }
                                    }
                                }
                            } else {
                                uriHandler.openUri(mix.webUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientTab(
    viewModel: AmbientSoundsViewModel = hiltViewModel()
) {
    val sounds by viewModel.sounds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val needsApiKey by viewModel.needsApiKey.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val playerConnection = LocalPlayerConnection.current ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        if (needsApiKey) {
            EmptyStateContent(
                icon = R.drawable.music_note,
                message = stringResource(R.string.ambient_sounds_needs_key)
            )
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (error != null) {
            ErrorScreen(
                message = error ?: "Unavailable",
                onRetry = { viewModel.loadSounds() }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding().let { bottom ->
                    PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottom + 80.dp)
                },
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(items = viewModel.categories.keys.toList()) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { viewModel.selectCategory(category) },
                                label = { Text(category.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                items(
                    items = sounds,
                    key = { it.id }
                ) { sound ->
                    GenreCard(
                        title = sound.name,
                        imageUrl = null,
                        color = 0L,
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = sound.name,
                                    items = listOf(sound.toMediaItem())
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: Int,
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun GenreCard(
    title: String,
    imageUrl: String? = null,
    color: Long = 0L,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardColor = remember(color, title) {
        if (color != 0L) {
            Color(color)
        } else {
            val hue = (title.hashCode().toLong() and 0x7FFFFFFF) % 360f
            Color.hsl(hue = hue.toFloat(), saturation = 0.65f, lightness = 0.40f)
        }
    }

    val gradientBrush = remember(cardColor) {
        Brush.linearGradient(
            colors = listOf(
                cardColor,
                cardColor.copy(alpha = 0.75f),
                Color.Black.copy(alpha = 0.50f),
            )
        )
    }

    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .clickable(onClick = onClick)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.85f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.70f)
                            )
                        )
                    )
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
                .fillMaxWidth()
        )
    }
}

private fun com.Chenkham.radiobrowser.models.RadioStation.toMediaItem(): androidx.media3.common.MediaItem {
    val streamUri = (urlResolved?.takeIf { it.isNotBlank() } ?: url).trim()
    val radioMediaId = "${com.Chenkham.Echofy.playback.MusicService.RADIO_MEDIA_ID_PREFIX}$stationuuid"
    val artistName = country.ifBlank { tags?.split(",")?.firstOrNull()?.trim() ?: "Live Radio" }
    val thumb = favicon?.takeIf { it.isNotBlank() }
    val metadata = com.Chenkham.Echofy.models.MediaMetadata(
        id = radioMediaId,
        title = name.ifBlank { "Radio Station" },
        artists = listOf(com.Chenkham.Echofy.models.MediaMetadata.Artist(id = null, name = artistName)),
        duration = -1,
        thumbnailUrl = thumb,
    )
    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(radioMediaId)
        .setUri(streamUri)
        .setCustomCacheKey(radioMediaId)
        .setTag(metadata)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(artistName)
                .setArtworkUri(thumb?.let { android.net.Uri.parse(it) })
                .build()
        )
        .build()
}

private fun com.Chenkham.freesound.models.AmbientSound.toMediaItem(): androidx.media3.common.MediaItem {
    val ambientMediaId = "${com.Chenkham.Echofy.playback.MusicService.AMBIENT_MEDIA_ID_PREFIX}$id"
    val metadata = com.Chenkham.Echofy.models.MediaMetadata(
        id = ambientMediaId,
        title = name.ifBlank { "Ambient Sound" },
        artists = listOf(com.Chenkham.Echofy.models.MediaMetadata.Artist(id = null, name = author.ifBlank { "Ambient" })),
        duration = -1,
        thumbnailUrl = null,
    )
    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(ambientMediaId)
        .setUri(streamUrl)
        .setCustomCacheKey(ambientMediaId)
        .setTag(metadata)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(author)
                .build()
        )
        .build()
}

