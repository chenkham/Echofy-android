package com.Chenkham.Echofy.ui.menu

import com.Chenkham.Echofy.db.update
import com.Chenkham.Echofy.ui.component.LocalAdManager

import android.content.Intent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import android.graphics.Bitmap
import android.widget.Toast
import java.io.File
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.constants.ThumbnailCornerRadius
import com.Chenkham.Echofy.db.entities.SongEntity
import com.Chenkham.Echofy.db.update
import com.Chenkham.Echofy.extensions.toMediaItem
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.Chenkham.innertube.YouTube
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.LocalSyncUtils
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListItemHeight
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.db.entities.Event
import com.Chenkham.Echofy.db.entities.PlaylistSong
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.playback.ExoDownloadService
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.ui.component.LocalBottomSheetPageState
import com.Chenkham.Echofy.ui.component.SongListItem
import com.Chenkham.Echofy.ui.component.TextFieldDialog
import com.Chenkham.Echofy.ui.component.ListDialog
import com.Chenkham.Echofy.ads.AdManager
import android.app.Activity
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongMenu(
    originalSong: Song,
    event: Event? = null,
    navController: NavController,
    playlistSong: PlaylistSong? = null,
    playlistBrowseId: String? = null,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    adManager: AdManager? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    // Optimized: use remember to avoid recomposition lag
    val songState = remember(originalSong.id) { 
        database.song(originalSong.id) 
    }.collectAsState(initial = originalSong)
    val song = songState.value ?: originalSong
    val download by remember(originalSong.id) { 
        downloadUtil.getDownload(originalSong.id) 
    }.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val syncUtils = LocalSyncUtils.current
    val scope = rememberCoroutineScope()
    var refetchIconDegree by remember { mutableFloatStateOf(0f) }


    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    if (showDetailsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            title = { Text(stringResource(R.string.details)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to song.song.title,
                        stringResource(R.string.song_artists) to song.artists.joinToString { it.name },
                        stringResource(R.string.media_id) to song.id,
                        "Album" to (song.song.albumName ?: "N/A"),
                        "Duration" to song.song.duration?.let { 
                            "%d:%02d".format(it / 60, it % 60)
                        },
                        "Date Added" to song.song.inLibrary?.toString()
                    ).forEach { (label, text) ->
                        val displayText = text ?: "Unknown"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(R.string.edit_song)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue = TextFieldValue(
                song.song.title,
                TextRange(song.song.title.length)
            ),
            onDone = { title ->
                onDismiss()
                database.query {
                    update(song.song.copy(title = title))
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(
                items = listOf(song),
                key = { it.id }
            ) { song ->
                SongListItem(song = song)
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = song.artists,
                key = { it.id },
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    val bottomSheetPageState = LocalBottomSheetPageState.current

    val ghostRippleConfig = RippleConfiguration(
        color = Color.White,
        rippleAlpha = RippleAlpha(
            pressedAlpha = 0.0f,
            draggedAlpha = 0.0f,
            focusedAlpha = 0.0f,
            hoveredAlpha = 0.0f
        )
    )

    // LazyListState to track scroll position for nested scroll handling
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Nested scroll connection to properly handle scroll gestures
    // Prevents vibration/bounce when over-scrolling at the top of the menu
    val nestedScrollConnection = remember(lazyListState) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val isAtTop = !lazyListState.canScrollBackward
                val isAtBottom = !lazyListState.canScrollForward
                val isScrollingUp = available.y > 0
                val isScrollingDown = available.y < 0

                // When at top and trying to scroll up (pull down gesture), consume the scroll
                // to prevent bounce/vibration, but allow large gestures for dismiss
                if (isAtTop && isScrollingUp && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
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
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
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

    CompositionLocalProvider(
        LocalRippleConfiguration provides ghostRippleConfig
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .nestedScroll(nestedScrollConnection),
            userScrollEnabled = true
        ) {
            item {
                SongListItem(
                    song = song,
                    badges = {},
                    trailingContent = {
                        IconButton(
                            onClick = {
                                database.query {
                                    update(song.song.toggleLike())
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(if (song.song.liked) R.drawable.heart_fill else R.drawable.heart),
                                tint = if (song.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }

            item {
                HorizontalDivider()
            }
            
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                // Quick action buttons row (4 buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    // Play next button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDismiss()
                                playerConnection.playNext(song.toMediaItem())
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_play),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.play_next),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }

                    // Add to playlist button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                showChoosePlaylistDialog = true
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.add_to_playlist),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }

                    // Share button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onDismiss()
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Generating share card...", Toast.LENGTH_SHORT).show()
                                        }
                                        
                                        // Generate branded share card
                                        val shareCard = com.Chenkham.Echofy.utils.ShareCardGenerator.generateShareCard(
                                            context = context,
                                            songTitle = song.title,
                                            artistName = song.artists.joinToString { it.name },
                                            albumArtUrl = song.thumbnailUrl
                                        )
                                        
                                        // Save to cache directory
                                        val cacheDir = File(context.cacheDir, "share_cards")
                                        cacheDir.mkdirs()
                                        val imageFile = File(cacheDir, "echofy_share_${System.currentTimeMillis()}.png")
                                        
                                        imageFile.outputStream().use { out ->
                                            shareCard.compress(Bitmap.CompressFormat.PNG, 100, out)
                                        }
                                        
                                        // Share via FileProvider
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            imageFile
                                        )
                                        
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_TEXT, "Check out this song on Echofy!\n${song.title} - ${song.artists.joinToString { it.name }}")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        
                                        withContext(Dispatchers.Main) {
                                            context.startActivity(Intent.createChooser(intent, "Share via"))
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ShareCard", "Failed to generate share card", e)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Share card failed: ${e.message}. Using text share.", Toast.LENGTH_LONG).show()
                                            
                                            // Fallback to text sharing
                                            val fallbackIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "Check out this song on Echofy!\n${song.title} - ${song.artists.joinToString { it.name }}\nhttps://music.youtube.com/watch?v=${song.id}")
                                            }
                                            context.startActivity(Intent.createChooser(fallbackIntent, "Share via"))
                                        }
                                    }
                                }
                            }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.share),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .basicMarquee()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.start_radio)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.radio),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                    }
                )
            }
            
            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.edit)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        showEditDialog = true
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        onDismiss()
                        playerConnection.addToQueue(song.toMediaItem())
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(
                                if (song.song.inLibrary == null) R.string.add_to_library
                                else R.string.remove_from_library
                            )
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                if (song.song.inLibrary == null) R.drawable.library_add
                                else R.drawable.library_add_check
                            ),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        database.query {
                            update(song.song.toggleLibrary())
                        }
                    }
                )
            }

            if (event != null) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.remove_from_history)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            onDismiss()
                            database.query {
                                delete(event)
                            }
                        }
                    )
                }
            }

            if (playlistSong != null) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.remove_from_playlist)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            database.transaction {
                                coroutineScope.launch {
                                    playlistBrowseId?.let { playlistId ->
                                        if (playlistSong.map.setVideoId != null) {
                                            YouTube.removeFromPlaylist(
                                                playlistId,
                                                playlistSong.map.songId,
                                                playlistSong.map.setVideoId
                                            )
                                        }
                                    }
                                }
                                move(
                                    playlistSong.map.playlistId,
                                    playlistSong.map.position,
                                    Int.MAX_VALUE
                                )
                                delete(playlistSong.map.copy(position = Int.MAX_VALUE))
                            }
                            onDismiss()
                        }
                    )
                }
            }

            item {
                when (download?.state) {
                    Download.STATE_COMPLETED -> {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = stringResource(R.string.remove_download),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.offline),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        )
                    }

                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.downloading)) },
                            leadingContent = {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            },
                            modifier = Modifier.clickable {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        )
                    }

                    else -> {
                        val adManager = com.Chenkham.Echofy.ui.component.LocalAdManager.current
                        ListItem(
                            headlineContent = { Text(text = stringResource(R.string.download)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier.clickable {
                                if (adManager?.isPremium?.value == true) {
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(song.id, song.id.toUri())
                                            .setCustomCacheKey(song.id)
                                            .setData(song.song.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                    Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, R.string.premium_required, Toast.LENGTH_SHORT).show()
                                }
                                }
                        )
                    }
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        if (song.artists.size == 1) {
                            navController.navigate("artist/${song.artists[0].id}")
                            onDismiss()
                        } else {
                            showSelectArtistDialog = true
                        }
                    }
                )
            }

            if (song.song.albumId != null) {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.view_album)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.album),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            onDismiss()
                            navController.navigate("album/${song.song.albumId}")
                        }
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.refetch)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                        )
                    },
                    modifier = Modifier.clickable {
                        refetchIconDegree -= 360
                        scope.launch(Dispatchers.IO) {
                            YouTube.queue(listOf(song.id)).onSuccess {
                                val newSong = it.firstOrNull()
                                if (newSong != null) {
                                    database.transaction {
                                        update(song, newSong.toMediaMetadata())
                                    }
                                }
                            }
                        }
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.details)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        showDetailsDialog = true
                    }
                )
            }
        }
    }
}
