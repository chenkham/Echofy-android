package com.Chenkham.Echofy.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.StatPeriod
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.extensions.togglePlayPause
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.queues.ListQueue
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.AnimatedMusicalMinion
import com.Chenkham.Echofy.ui.component.ChoiceChipsRow
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.LocalMenuState
import com.Chenkham.Echofy.ui.menu.SongMenu
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString
import com.Chenkham.Echofy.viewmodels.StatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class OptionStats {
    CONTINUOUS,
    WEEKS,
    MONTHS,
    YEARS,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val currentDate = LocalDateTime.now()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsState()

    var showInsightDialog by remember { mutableStateOf(false) }

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate, ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusYears(1).withDayOfYear(1).minusDays(1)
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfYear(1),
                    )
                }.mapIndexed { index, date ->
                    Pair(index, date.year.toString())
                }.toList()
        } else {
            emptyList()
        }

    val favoriteArtist = mostPlayedArtists.firstOrNull()
    val favoriteSong = mostPlayedSongsStats.firstOrNull()
    val totalTimeListenedMs = remember(mostPlayedSongsStats) {
        mostPlayedSongsStats.sumOf { it.timeListened ?: 0L }
    }

    if (showInsightDialog) {
        EchofyInsightDialog(
            topSongs = mostPlayedSongsStats,
            topArtists = mostPlayedArtists,
            totalListenTimeMs = totalTimeListenedMs,
            onDismiss = { showInsightDialog = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                title = {
                    Text(
                        text = "Stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // OpenTune Note-Type Insight Action Button
                    IconButton(onClick = { showInsightDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.calendar_today),
                            contentDescription = "Insight",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                // Animated Musical Minion Mascot (Dancing in the top gap)
                item(key = "animatedMinion") {
                    AnimatedMusicalMinion()
                }

                // Filter Choice Chips (Continuous ⌵ | 1 week | 1 month | 3 months ...)
                item(key = "choiceChips") {
                    ChoiceChipsRow(
                        chips =
                            when (selectedOption) {
                                OptionStats.WEEKS -> weeklyDates
                                OptionStats.MONTHS -> monthlyDates
                                OptionStats.YEARS -> yearlyDates
                                OptionStats.CONTINUOUS -> {
                                    listOf(
                                        StatPeriod.WEEK_1.ordinal to pluralStringResource(
                                            R.plurals.n_week,
                                            1,
                                            1
                                        ),
                                        StatPeriod.MONTH_1.ordinal to pluralStringResource(
                                            R.plurals.n_month,
                                            1,
                                            1
                                        ),
                                        StatPeriod.MONTH_3.ordinal to pluralStringResource(
                                            R.plurals.n_month,
                                            3,
                                            3
                                        ),
                                        StatPeriod.MONTH_6.ordinal to pluralStringResource(
                                            R.plurals.n_month,
                                            6,
                                            6
                                        ),
                                        StatPeriod.YEAR_1.ordinal to pluralStringResource(
                                            R.plurals.n_year,
                                            1,
                                            1
                                        ),
                                        StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                                    )
                                }
                            },
                        options =
                            listOf(
                                OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                                OptionStats.WEEKS to stringResource(R.string.weeks),
                                OptionStats.MONTHS to stringResource(R.string.months),
                                OptionStats.YEARS to stringResource(R.string.years),
                            ),
                        selectedOption = selectedOption,
                        onSelectionChange = {
                            viewModel.selectedOption.value = it
                            viewModel.indexChips.value = 0
                        },
                        currentValue = indexChips,
                        onValueUpdate = { viewModel.indexChips.value = it },
                    )
                }

                // 1. Favourite Artist Card (OpenTune layout: media_1787641439229.png)
                if (favoriteArtist != null) {
                    item(key = "favArtistCard") {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("artist/${favoriteArtist.id}")
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = favoriteArtist.artist.thumbnailUrl,
                                    contentDescription = favoriteArtist.artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Your Favourite Artist",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = favoriteArtist.artist.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${favoriteArtist.songCount} songs played • ${makeTimeString(favoriteArtist.timeListened?.toLong()?.times(1000L))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Favourite Song Card (OpenTune layout: media_1787641439229.png)
                if (favoriteSong != null) {
                    item(key = "favSongCard") {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            endpoint = WatchEndpoint(favoriteSong.id),
                                            preloadItem = mostPlayedSongs.firstOrNull()?.toMediaMetadata(),
                                        ),
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = favoriteSong.thumbnailUrl,
                                    contentDescription = favoriteSong.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Your Favourite Song",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = favoriteSong.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${favoriteSong.songCountListened} plays • ${makeTimeString(favoriteSong.timeListened)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Total Time Listened Section (OpenTune collage circle + big number)
                if (totalTimeListenedMs > 0) {
                    item(key = "totalTimeCard") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Quadrant circle collage of top artists
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                val top4Artists = mostPlayedArtists.take(4)
                                if (top4Artists.size >= 4) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            AsyncImage(
                                                model = top4Artists[0].artist.thumbnailUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                            AsyncImage(
                                                model = top4Artists[1].artist.thumbnailUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                        }
                                        Row(modifier = Modifier.weight(1f)) {
                                            AsyncImage(
                                                model = top4Artists[2].artist.thumbnailUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                            AsyncImage(
                                                model = top4Artists[3].artist.thumbnailUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                            )
                                        }
                                    }
                                } else {
                                    AsyncImage(
                                        model = favoriteArtist?.artist?.thumbnailUrl ?: favoriteSong?.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            Column {
                                Text(
                                    text = "Total Time Listened",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = makeTimeString(totalTimeListenedMs),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF90CAF9)
                                )
                            }
                        }
                    }
                }

                // 4. Songs Header
                if (mostPlayedSongsStats.isNotEmpty()) {
                    item(key = "songsHeader") {
                        Text(
                            text = "${mostPlayedSongsStats.size} Songs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // 5. Ranked Songs List
                itemsIndexed(
                    items = mostPlayedSongsStats,
                    key = { _, song -> song.id },
                ) { index, song ->
                    val isSongActive = song.id == mediaMetadata?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (isSongActive) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                endpoint = WatchEndpoint(song.id),
                                                preloadItem = mostPlayedSongs.getOrNull(index)?.toMediaMetadata(),
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    mostPlayedSongs.getOrNull(index)?.let { originalSong ->
                                        menuState.show {
                                            SongMenu(
                                                originalSong = originalSong,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${song.title}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSongActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${song.songCountListened} time • ${makeTimeString(song.timeListened)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Shuffle FAB (Blue floating button on bottom right)
            if (mostPlayedSongs.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.most_played_songs),
                                items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled()
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xFF0066CC),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
