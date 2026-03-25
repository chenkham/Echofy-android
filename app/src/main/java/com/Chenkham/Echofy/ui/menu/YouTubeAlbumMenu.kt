package com.Chenkham.Echofy.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.Chenkham.innertube.YouTube
import com.Chenkham.innertube.models.AlbumItem
import com.Chenkham.Echofy.db.insert
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalDownloadUtil
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ListItemHeight
import com.Chenkham.Echofy.constants.ListThumbnailSize
import com.Chenkham.Echofy.db.entities.Song
import com.Chenkham.Echofy.extensions.toMediaItem
import com.Chenkham.Echofy.playback.ExoDownloadService
import com.Chenkham.Echofy.playback.queues.YouTubeAlbumRadio
import com.Chenkham.Echofy.ui.component.ListDialog
import com.Chenkham.Echofy.ui.component.SongListItem
import com.Chenkham.Echofy.ui.component.YouTubeListItem
import com.Chenkham.Echofy.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import com.Chenkham.Echofy.ui.component.LocalAdManager
import com.Chenkham.Echofy.ui.component.ListItem as EchofyListItem

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeAlbumMenu(
    albumItem: AlbumItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val adManager = LocalAdManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val album by remember(albumItem.id) { 
        database.albumWithSongs(albumItem.id) 
    }.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.album(albumItem.id).collect { album ->
            if (album == null) {
                YouTube
                    .album(albumItem.id)
                    .onSuccess { albumPage ->
                        database.transaction {
                            insert(albumPage)
                        }
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(album) {
        val songs = album?.songs?.map { it.id } ?: return@LaunchedEffect
        downloadUtil.downloads
            .collect { downloads ->
                downloadState = withContext(Dispatchers.Default) {
                    if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                        Download.STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it]?.state == Download.STATE_QUEUED ||
                                    downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                    downloads[it]?.state == Download.STATE_COMPLETED
                        }
                    ) {
                        Download.STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
                }
            }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    album?.album?.playlistId?.let { addPlaylistId ->
                        YouTube.addPlaylistToPlaylist(playlistId, addPlaylistId)
                    }
                }
            }
            album?.songs?.map { it.id }.orEmpty()
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                EchofyListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
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
                items = album?.artists.orEmpty(),
                key = { it.id },
            ) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillParentMaxWidth()
                                .height(ListItemHeight)
                                .clickable {
                                    showSelectArtistDialog = false
                                    onDismiss()
                                    navController.navigate("artist/${artist.id}")
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
    }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            YouTubeListItem(
            item = albumItem,
            badges = {},
            trailingContent = {
                IconButton(
                    onClick = {
                        database.query {
                            album?.album?.toggleLike()?.let(::update)
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(if (album?.album?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border),
                        tint = if (album?.album?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                        contentDescription = null,
                    )
                }
            },
        )

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        // Quick action buttons row (4 buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Start radio button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        playerConnection.playQueue(YouTubeAlbumRadio(albumItem.playlistId))
                        onDismiss()
                    }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.radio),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.start_radio),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .basicMarquee()
                        .padding(top = 4.dp),
                )
            }

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
                        album
                            ?.songs
                            ?.map { it.toMediaItem() }
                            ?.let(playerConnection::playNext)
                        onDismiss()
                    }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_play),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.play_next),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
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
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.add_to_playlist),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
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
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, albumItem.shareLink)
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.share),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .basicMarquee()
                        .padding(top = 4.dp),
                )
            }
        }

        // Menu items list
        Column(
            modifier = Modifier
                .padding(
                    start = 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp,
                ),
        ) {


            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    album
                        ?.songs
                        ?.map { it.toMediaItem() }
                        ?.let(playerConnection::addToQueue)
                    onDismiss()
                }
            )

            albumItem.artists?.let { artists ->
                ListItem(
                    headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable {
                        if (artists.size == 1) {
                            navController.navigate("artist/${artists[0].id}")
                            onDismiss()
                        } else {
                            showSelectArtistDialog = true
                        }
                    }
                )
            }

            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(
                            when (downloadState) {
                                Download.STATE_COMPLETED -> R.string.remove_download
                                Download.STATE_DOWNLOADING,
                                Download.STATE_QUEUED -> R.string.downloading
                                else -> R.string.download
                            }
                        )
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            when (downloadState) {
                                Download.STATE_COMPLETED -> R.drawable.offline
                                Download.STATE_DOWNLOADING,
                                Download.STATE_QUEUED -> R.drawable.download
                                else -> R.drawable.download
                            }
                        ),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    when (downloadState) {
                        Download.STATE_COMPLETED -> {
                            album?.songs?.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        }
                        Download.STATE_DOWNLOADING,
                        Download.STATE_QUEUED -> {
                            // Do nothing while downloading
                        }
                        else -> {
                            if (adManager?.isPremium?.value != true) {
                                android.widget.Toast.makeText(context, R.string.premium_required, android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                album?.songs?.forEach { song ->
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
                                }
                            }
                        }
                    }
                    onDismiss()
                }
            )
        }
    }
}

