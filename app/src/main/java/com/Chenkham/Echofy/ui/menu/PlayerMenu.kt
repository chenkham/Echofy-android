package com.Chenkham.Echofy.ui.menu

import com.Chenkham.Echofy.db.addSongToPlaylist
import com.Chenkham.Echofy.db.insert as insertMediaMetadata
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListItemHeight
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.ExoDownloadService
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.BottomSheetState
import com.Chenkham.Echofy.ui.component.ListDialog
import com.Chenkham.Echofy.ui.component.ListItem
import com.Chenkham.Echofy.utils.joinByBullet
import com.Chenkham.Echofy.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.constants.VideoQualityKey
import com.Chenkham.Echofy.constants.AudioQuality
import com.Chenkham.Echofy.constants.AudioQualityKey
import com.Chenkham.Echofy.utils.YTPlayerUtils
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.constants.PlaybackModeKey
import com.Chenkham.Echofy.utils.rememberEnumPreference
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.foundation.lazy.rememberLazyListState

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateAway: (() -> Unit)? = null,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists = remember(mediaMetadata.artists) {
        mediaMetadata.artists.filter { it.id != null }
    }

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSelectArtistDialog by rememberSaveable { mutableStateOf(false) }
    var showPitchTempoDialog by rememberSaveable { mutableStateOf(false) }
    var showVideoQualityDialog by rememberSaveable { mutableStateOf(false) }
    var showAudioQualityDialog by rememberSaveable { mutableStateOf(false) }
    
    // Check playback mode to conditionally show video quality option
    val (playbackMode) = rememberEnumPreference(
        key = PlaybackModeKey,
        defaultValue = PlaybackMode.AUDIO
    )
    val isVideoMode = playbackMode == PlaybackMode.VIDEO

    // Dialogs
    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction { insertMediaMetadata(mediaMetadata) }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(onDismiss = { showErrorPlaylistAddDialog = false; onDismiss() }) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.acc_close),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }
            item {
                ListItem(
                    title = mediaMetadata.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = stringResource(R.string.song_cover),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle = joinByBullet(
                        mediaMetadata.artists.joinToString { it.name },
                        makeTimeString(mediaMetadata.duration * 1000L),
                    ),
                )
            }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(onDismiss = { showSelectArtistDialog = false }) {
            items(
                items = artists,
                key = { it.id ?: it.name }
            ) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(onDismiss = { showPitchTempoDialog = false })
    }

    if (showVideoQualityDialog) {
        VideoQualityDialog(onDismiss = { showVideoQualityDialog = false })
    }

    if (showAudioQualityDialog) {
        AudioQualityDialog(onDismiss = { showAudioQualityDialog = false })
    }

    if (showAudioQualityDialog) {
        AudioQualityDialog(onDismiss = { showAudioQualityDialog = false })
    }

    // LazyListState to track scroll position
    val lazyListState = rememberLazyListState()

    // Nested scroll connection to properly handle scroll gestures
    // Prevents vibration/bounce when over-scrolling at the top of the menu
    val nestedScrollConnection = remember(lazyListState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val isAtTop = !lazyListState.canScrollBackward
                val isAtBottom = !lazyListState.canScrollForward
                val isScrollingUp = available.y > 0
                val isScrollingDown = available.y < 0

                // When at top and trying to scroll up (pull down gesture), consume the scroll
                // to prevent bounce/vibration, but allow small amount for dismiss gesture
                if (isAtTop && isScrollingUp && source == NestedScrollSource.UserInput) {
                    // Only pass through if it's a significant gesture (for dismiss)
                    return if (available.y > 50f) {
                        androidx.compose.ui.geometry.Offset.Zero
                    } else {
                        // Consume small scroll to prevent vibration
                        available.copy(x = 0f)
                    }
                }

                // When at bottom and trying to scroll down, consume to prevent bounce
                if (isAtBottom && isScrollingDown) {
                    return available.copy(x = 0f)
                }

                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // Consume any leftover scroll to prevent vibration
                val isAtTop = !lazyListState.canScrollBackward
                val isAtBottom = !lazyListState.canScrollForward

                if ((isAtTop && available.y > 0) || (isAtBottom && available.y < 0)) {
                    return available.copy(x = 0f)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(bottom = 16.dp),
            userScrollEnabled = true
        ) {
        item {
            // Song Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                AsyncImage(
                    model = mediaMetadata.thumbnailUrl,
                    contentDescription = stringResource(R.string.song_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = mediaMetadata.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = mediaMetadata.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "• ${makeTimeString(mediaMetadata.duration * 1000L)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Like button
                IconButton(
                    onClick = {
                        database.query {
                            if (librarySong?.song?.liked == true) {
                                update(librarySong!!.song.copy(liked = false))
                            } else {
                                librarySong?.song?.let { update(it.copy(liked = true)) }
                            }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (librarySong?.song?.liked == true) R.drawable.heart_fill
                            else R.drawable.heart
                        ),
                        contentDescription = stringResource(if (librarySong?.song?.liked == true) R.string.acc_unfavorite else R.string.acc_favorite),
                        tint = if (librarySong?.song?.liked == true)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Close button
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.acc_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            // Quick Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start Radio
                QuickActionButton(
                    icon = R.drawable.radio,
                    label = stringResource(R.string.start_radio),
                    modifier = Modifier.weight(1f)
                ) {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = mediaMetadata.id),
                            mediaMetadata
                        )
                    )
                    onDismiss()
                }

                // Add to Playlist
                QuickActionButton(
                    icon = R.drawable.playlist_add,
                    label = stringResource(R.string.add_to_playlist),
                    modifier = Modifier.weight(1f)
                ) {
                    showChoosePlaylistDialog = true
                }

                // Share
                QuickActionButton(
                    icon = R.drawable.share,
                    label = stringResource(R.string.share),
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Menu List Items
        // Add to Library / Remove from Library
        if (librarySong?.song?.inLibrary != null) {
            item {
                MenuListItem(
                    icon = R.drawable.library_add_check,
                    title = stringResource(R.string.remove_from_library)
                ) {
                    database.query { inLibrary(mediaMetadata.id, null) }
                    onDismiss()
                }
            }
        } else {
            item {
                MenuListItem(
                    icon = R.drawable.library_add,
                    title = stringResource(R.string.add_to_library)
                ) {
                    database.transaction {
                        insertMediaMetadata(mediaMetadata)
                        inLibrary(mediaMetadata.id, LocalDateTime.now())
                    }
                    onDismiss()
                }
            }
        }

        // Download
        item {
            val adManager = com.Chenkham.Echofy.ui.component.LocalAdManager.current
            when (download?.state) {
                Download.STATE_COMPLETED -> {
                    MenuListItem(
                        icon = R.drawable.offline,
                        title = stringResource(R.string.remove_download)
                    ) {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            mediaMetadata.id,
                            false
                        )
                        onDismiss()
                    }
                }
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    MenuListItem(
                        icon = R.drawable.download,
                        title = stringResource(R.string.downloading)
                    ) {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            mediaMetadata.id,
                            false
                        )
                        onDismiss()
                    }
                }
                else -> {
                    MenuListItem(
                        icon = R.drawable.download,
                        title = stringResource(R.string.download)
                    ) {
                        // PREMIUM CHECK
                        if (adManager?.isPremium?.value != true) {
                            android.widget.Toast.makeText(context, R.string.premium_required, android.widget.Toast.LENGTH_SHORT).show()
                            onDismiss()
                            return@MenuListItem
                        }

                        database.transaction { insertMediaMetadata(mediaMetadata) }
                        val downloadRequest = DownloadRequest
                            .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                            .setCustomCacheKey(mediaMetadata.id)
                            .setData(mediaMetadata.title.toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false
                        )
                        onDismiss()
                    }
                }
            }
        }

        // Go to Album
        if (mediaMetadata.album != null) {
            item {
                MenuListItem(
                    icon = R.drawable.album,
                    title = stringResource(R.string.view_album)
                ) {
                    navController.navigate("album/${mediaMetadata.album.id}")
                    onDismiss()
                }
            }
        }

        // Go to Artist
        if (artists.isNotEmpty()) {
            item {
                MenuListItem(
                    icon = R.drawable.artist,
                    title = stringResource(R.string.view_artist)
                ) {
                    if (mediaMetadata.artists.size == 1) {
                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                }
            }
        }

        // Details
        item {
            MenuListItem(
                icon = R.drawable.info,
                title = stringResource(R.string.details)
            ) {
                onShowDetailsDialog()
                onDismiss()
            }
        }

        // Equalizer
        item {
            MenuListItem(
                icon = R.drawable.equalizer,
                title = stringResource(R.string.equalizer)
            ) {
                onNavigateAway?.invoke()
                navController.navigate("settings/equalizer")
                onDismiss()
            }
        }

        // Video Quality (only in video mode)
        if (isVideoMode) {
            item {
                MenuListItem(
                    icon = R.drawable.settings,
                    title = stringResource(R.string.video_quality)
                ) {
                    showVideoQualityDialog = true
                }
            }
        }

        // Audio Quality (only in audio mode)
        if (!isVideoMode) {
            item {
                MenuListItem(
                    icon = R.drawable.graphic_eq,
                    title = stringResource(R.string.audio_quality)
                ) {
                    showAudioQualityDialog = true
                }
            }
        }

        // Advanced (Tempo/Pitch)
        item {
            MenuListItem(
                icon = R.drawable.tune,
                title = stringResource(R.string.advanced)
            ) {
                showPitchTempoDialog = true
            }
        }
    }

}

/**
 * Quick action button for the top row (YouTube Music style)
 */
@Composable
private fun QuickActionButton(
    @DrawableRes icon: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Menu list item with left-aligned icon (YouTube Music style)
 */
@Composable
private fun MenuListItem(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tempo_and_pitch)) },
        dismissButton = {
            TextButton(onClick = {
                tempo = 1f
                transposeValue = 0
                updatePlaybackParameters()
            }) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.speed,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = { tempo = it; updatePlaybackParameters() },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = { transposeValue = it; updatePlaybackParameters() },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        IconButton(
            enabled = currentValue != values.first(),
            onClick = { onValueUpdate(values[values.indexOf(currentValue) - 1]) },
        ) {
            Icon(painter = painterResource(R.drawable.remove), contentDescription = stringResource(R.string.acc_rewind))
        }
        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )
        IconButton(
            enabled = currentValue != values.last(),
            onClick = { onValueUpdate(values[values.indexOf(currentValue) + 1]) },
        ) {
            Icon(painter = painterResource(R.drawable.add), contentDescription = stringResource(R.string.acc_fast_forward))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoQualityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedQuality by remember(context) {
        context.dataStore.data
            .map { it[VideoQualityKey] ?: "Auto" }
    }.collectAsState(initial = "Auto")
        
    val availableQualities by YTPlayerUtils.availableQualities.collectAsState()
    // Only show dynamic qualities - no fallback
    val qualityOptions = listOf("Auto") + availableQualities

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.video_quality)) },
        text = {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                qualityOptions.forEach { quality ->
                    val isSelected = selectedQuality == quality
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                context.dataStore.edit { it[VideoQualityKey] = quality }
                            }
                            onDismiss()
                        },
                        label = {
                            Text(
                                text = quality,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun AudioQualityDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Use direct DataStore collection for reliable state
    val audioQualityString by remember(context) {
        context.dataStore.data
            .map { it[AudioQualityKey] ?: "AUTO" }
    }.collectAsState(initial = "AUTO")
    
    val audioQuality = try {
        AudioQuality.valueOf(audioQualityString)
    } catch (e: Exception) {
        AudioQuality.AUTO
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.audio_quality)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Auto option
                androidx.compose.material3.FilterChip(
                    selected = audioQuality == AudioQuality.AUTO,
                    onClick = {
                        coroutineScope.launch {
                            context.dataStore.edit { it[AudioQualityKey] = AudioQuality.AUTO.name }
                        }
                        onDismiss()
                    },
                    label = { Text("Auto (based on network)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // High option
                androidx.compose.material3.FilterChip(
                    selected = audioQuality == AudioQuality.HIGH,
                    onClick = {
                        coroutineScope.launch {
                            context.dataStore.edit { it[AudioQualityKey] = AudioQuality.HIGH.name }
                        }
                        onDismiss()
                    },
                    label = { Text("High (better quality)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Low option
                androidx.compose.material3.FilterChip(
                    selected = audioQuality == AudioQuality.LOW,
                    onClick = {
                        coroutineScope.launch {
                            context.dataStore.edit { it[AudioQualityKey] = AudioQuality.LOW.name }
                        }
                        onDismiss()
                    },
                    label = { Text("Low (saves data)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
