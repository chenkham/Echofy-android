package com.Chenkham.Echofy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.viewmodels.PodcastDetailViewModel
import com.Chenkham.ytmusicapi.models.Episode

import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.innertube.models.WatchEndpoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: PodcastDetailViewModel = hiltViewModel()
) {
    val podcast by viewModel.podcast.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val playerConnection = LocalPlayerConnection.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(podcast?.title ?: "Podcast") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Error loading podcast")
                }
            } else if (podcast != null) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        PodcastHeader(
                            title = podcast!!.title,
                            author = podcast!!.author?.name ?: "",
                            description = podcast!!.description ?: "",
                            thumbnailUrl = podcast!!.thumbnails.lastOrNull()?.url
                        )
                    }
                    
                    val episodes = podcast!!.episodes
                    if (episodes.isNotEmpty()) {
                        items(episodes) { episode ->
                            EpisodeItem(episode = episode, onClick = {
                                if (playerConnection != null && episode.videoId != null) {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            endpoint = WatchEndpoint(videoId = episode.videoId!!),
                                            preloadItem = MediaMetadata(
                                                id = episode.videoId!!,
                                                title = episode.title,
                                                artists = listOf(
                                                    MediaMetadata.Artist(
                                                        id = podcast?.author?.id,
                                                        name = podcast?.author?.name ?: ""
                                                    )
                                                ),
                                                duration = -1,
                                                thumbnailUrl = episode.thumbnails.lastOrNull()?.url
                                            )
                                        )
                                    )
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastHeader(
    title: String,
    author: String,
    description: String,
    thumbnailUrl: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        } else {
             Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (author.isNotEmpty()) {
             Text(
                text = author,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        
        if (description.isNotEmpty()) {
             Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (episode.thumbnails.isNotEmpty()) {
                 AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(episode.thumbnails.first().url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.size(16.dp))
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    episode.date?.let { date ->
                         Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                    episode.duration?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
