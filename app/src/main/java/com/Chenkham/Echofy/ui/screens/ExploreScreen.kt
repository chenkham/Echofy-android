package com.Chenkham.Echofy.ui.screens

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ads.AdManager
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.ui.component.BackpaperBackground
import com.Chenkham.Echofy.ui.component.BannerAdView
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.NavigationTitle
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.viewmodels.MoodAndGenresViewModel
import com.Chenkham.Echofy.ui.component.ErrorScreen

import androidx.compose.ui.zIndex
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.Chenkham.Echofy.viewmodels.ChartsViewModel
import com.Chenkham.Echofy.viewmodels.PodcastsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    adManager: AdManager? = null,
) {
    val chartsViewModel: ChartsViewModel = hiltViewModel()
    val podcastsViewModel: PodcastsViewModel = hiltViewModel()

    val tabs = remember {
        listOf(
            "genres" to R.string.genres,
            "charts" to R.string.charts,
            "podcasts" to R.string.podcasts
        )
    }
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    BackpaperBackground(screen = BackpaperScreen.EXPLORE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = LocalPlayerAwareWindowInsets.current
                        .asPaddingValues()
                        .calculateTopPadding() + with(androidx.compose.ui.platform.LocalDensity.current) { scrollBehavior.state.heightOffset.toDp() }
                )
        ) {
            // TabRow for navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, (key, titleRes) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }
            
            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> GenresTab(navController, viewModel, adManager)
                    1 -> ChartsTab(navController, chartsViewModel, adManager)
                    2 -> PodcastsTab(navController, podcastsViewModel, adManager)
                }
            }
        }
    }
}

@Composable
private fun GenresTab(
    navController: NavController,
    viewModel: MoodAndGenresViewModel,
    adManager: AdManager?
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
                top = 16.dp,
                bottom = bottom + if (adManager?.shouldShowAds() == true) 60.dp else 16.dp
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
                moodAndGenresList?.forEach { moodAndGenres ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NavigationTitle(
                            title = moodAndGenres.title,
                            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
                        )
                    }

                    itemsIndexed(
                        items = moodAndGenres.items,
                        key = { index, item -> "${item.endpoint.params ?: item.title}_$index" }
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

    // Banner ad at the bottom
    adManager?.let { manager ->
        if (manager.shouldShowAds()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                BannerAdView(
                    adManager = manager,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChartsTab(
    navController: NavController,
    viewModel: ChartsViewModel,
    adManager: AdManager?
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
                         androidx.compose.material3.FilterChip(
                             selected = true,
                             onClick = { showCountryMenu = true },
                             label = { 
                                 // Display "Global" for ZZ, otherwise show country name
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

                // Charts Sections from YouTube Music API
                // Detect content type from items to ensure correct title assignment
                charts?.sections?.forEachIndexed { index, section ->
                    // Only show sections with items
                    if (section.items.isNotEmpty()) {
                        // Detect content type based on first item's characteristics
                        val firstItem = section.items.first()
                        val isArtistSection = firstItem.browseId.startsWith("UC") && firstItem.playlistId == null
                        val hasSubscribers = firstItem.subscribers != null
                        
                        // Determine correct title based on content type
                        val sectionTitle = when {
                            // If items are artists (UC browseId, no playlistId, has subscribers)
                            isArtistSection || hasSubscribers -> "Top artists"
                            // If API title contains useful info, use it
                            section.title.isNotBlank() && !section.title.contains("Top artists", ignoreCase = true) -> section.title
                            // Fallback based on index - first section with playlists is usually Trending
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
                                         // Fallback: mostly likely a playlist if it has browseId but not UC
                                         // But could be a specific browse page. Try online_playlist if it starts with VL or PL
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
    viewModel: PodcastsViewModel,
    adManager: AdManager?
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
                                navController.navigate("podcast/$id")
                            }
                        )
                    }
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
    color: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Background image or icon (fills the card)
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val drawableRes = remember(title) { getGenreDrawable(title) }
                if (drawableRes != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(drawableRes)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Title text at bottom left
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .fillMaxWidth()
        )
    }
}

private fun getGenreDrawable(title: String): Int? {
    val t = title.lowercase()
    return when {
        "pop" in t -> R.drawable.genre_pop
        "rock" in t || "punk" in t -> R.drawable.genre_rock
        "metal" in t || "heavy" in t -> R.drawable.genre_metal
        "hip hop" in t || "hip-hop" in t || "rap" in t -> R.drawable.genre_hiphop
        "electronic" in t || "edm" in t || "dance" in t || "house" in t || "techno" in t -> R.drawable.genre_electronic
        "jazz" in t || "swing" in t || "bebop" in t -> R.drawable.genre_jazz
        "classical" in t || "orchestra" in t || "symphony" in t -> R.drawable.genre_classical
        "country" in t || "bluegrass" in t -> R.drawable.genre_country
        "folk" in t || "acoustic" in t -> R.drawable.genre_folk
        "r&b" in t || "r\\&b" in t || "soul" in t -> R.drawable.genre_rnb
        "indie" in t || "alternative" in t -> R.drawable.genre_indie
        "latin" in t || "salsa" in t || "reggaeton" in t || "bachata" in t -> R.drawable.genre_latin
        "k-pop" in t || "kpop" in t || "korean" in t -> R.drawable.genre_kpop
        "blues" in t -> R.drawable.genre_blues
        "reggae" in t || "ska" in t -> R.drawable.genre_reggae
        "gospel" in t || "christian" in t -> R.drawable.genre_gospel
        "chill" in t || "relax" in t || "ambient" in t || "lo-fi" in t || "lofi" in t -> R.drawable.genre_chill
        "workout" in t || "fitness" in t || "gym" in t || "energy" in t || "motivation" in t -> R.drawable.genre_workout
        "sleep" in t || "night" in t || "calm" in t -> R.drawable.genre_sleep
        "feel good" in t || "happy" in t || "positive" in t || "mood" in t -> R.drawable.genre_feel_good
        "sad" in t || "melancholy" in t || "breakup" in t || "rain" in t -> R.drawable.genre_sad
        "party" in t || "club" in t -> R.drawable.genre_party
        "focus" in t || "study" in t || "concentration" in t || "work" in t -> R.drawable.genre_focus
        "romance" in t || "love" in t || "romantic" in t -> R.drawable.genre_romance
        "retro" in t || "80s" in t || "90s" in t || "oldies" in t || "nostalgia" in t -> R.drawable.genre_retro
        "soundtrack" in t || "cinema" in t || "movie" in t || "film" in t || "ost" in t -> R.drawable.genre_soundtrack
        "world" in t || "global" in t || "international" in t || "africa" in t || "india" in t -> R.drawable.genre_world
        "kids" in t || "family" in t || "children" in t || "disney" in t -> R.drawable.genre_kids
        else -> {
            val hash = kotlin.math.abs(title.hashCode()) % 4
            when (hash) {
                0 -> R.drawable.genre_fallback_1
                1 -> R.drawable.genre_fallback_2
                2 -> R.drawable.genre_fallback_3
                else -> R.drawable.genre_fallback_4
            }
        }
    }
}
