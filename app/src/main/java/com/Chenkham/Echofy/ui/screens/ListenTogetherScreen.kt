package com.Chenkham.Echofy.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AccountEmailKey
import com.Chenkham.Echofy.constants.AccountNameKey
import com.Chenkham.Echofy.constants.InnerTubeCookieKey
import com.Chenkham.Echofy.data.models.ListeningRoom
import com.Chenkham.Echofy.data.models.ChatMessage
import com.Chenkham.Echofy.data.remote.AppwriteClient
import com.Chenkham.Echofy.data.repository.ListenTogetherRepository
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.queues.YouTubeQueue
import com.Chenkham.Echofy.utils.rememberPreference
import com.Chenkham.innertube.models.WatchEndpoint
import com.Chenkham.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Initialize Appwrite and Repository
    LaunchedEffect(Unit) {
        AppwriteClient.init(context)
        ListenTogetherRepository.init(context)
    }
    
    // Use singleton repository
    val currentRoom by ListenTogetherRepository.currentRoom.collectAsState()
    val participants by ListenTogetherRepository.participants.collectAsState()
    val participantNames by ListenTogetherRepository.participantNames.collectAsState()
    
    // Get user identity
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() && "SAPISID" in parseCookieString(innerTubeCookie)
    }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val userId = remember(accountEmail, isLoggedIn, currentRoom) {
        // CRITICAL: When in a session, use the saved userId from preferences
        // This ensures isHost == (room.hostId == userId) works correctly
        ListenTogetherRepository.currentUserId 
            ?: ListenTogetherRepository.getOrCreateUserId(accountEmail, isLoggedIn)
    }
    
    // displayName should update when session is restored
    val displayName by remember(currentRoom, accountName, isLoggedIn) {
        mutableStateOf(
            // First try to use the saved name from the session (entered during create/join)
            ListenTogetherRepository.savedUserName 
                ?: if (isLoggedIn && accountName.isNotBlank()) accountName 
                else "Listener_${(1000..9999).random()}"
        )
    }
    
    // Restore session on startup
    LaunchedEffect(userId) {
        ListenTogetherRepository.restoreSession(userId)
    }
    
    // Session view
    if (currentRoom != null) {
        ActiveSessionScreen(
            room = currentRoom!!,
            participantNames = participantNames,
            isHost = currentRoom!!.hostId == userId,
            userId = userId,
            displayName = displayName,
            onLeaveSession = {
                scope.launch {
                    isLoading = true
                    ListenTogetherRepository.leaveRoom(
                        roomCode = currentRoom!!.roomCode,
                        participantId = userId,
                        isHost = currentRoom!!.hostId == userId
                    )
                    isLoading = false
                }
            }
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.listen_together)) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.people_filled),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Listen Together",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Share music with friends in real-time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painterResource(R.drawable.add), null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create Session", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { showJoinDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painterResource(R.drawable.people_filled), null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Join Session", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    if (showCreateDialog) {
        CreateSessionDialog(
            onDismiss = { showCreateDialog = false },
            onCreateSession = { code, controlMode, userName ->
                scope.launch {
                    isLoading = true
                    showCreateDialog = false
                    val result = ListenTogetherRepository.createRoom(
                        roomCode = code,
                        hostId = userId,
                        hostName = userName,
                        hostEmail = if (isLoggedIn) accountEmail else null,
                        isHostConnected = isLoggedIn,
                        controlMode = controlMode
                    )
                    result.onSuccess {
                        ListenTogetherRepository.startPolling(code) {}
                        Toast.makeText(context, "Session created: $code", Toast.LENGTH_SHORT).show()
                    }
                    result.onFailure {
                        Toast.makeText(context, "Failed to create session", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            }
        )
    }
    
    if (showJoinDialog) {
        JoinSessionDialog(
            onDismiss = { showJoinDialog = false },
            onJoinSession = { code, userName ->
                scope.launch {
                    isLoading = true
                    showJoinDialog = false
                    val result = ListenTogetherRepository.joinRoom(
                        roomCode = code,
                        participantId = userId,
                        participantName = userName
                    )
                    result.onSuccess {
                        ListenTogetherRepository.startPolling(code) {}
                        Toast.makeText(context, "Joined session: $code", Toast.LENGTH_SHORT).show()
                    }
                    result.onFailure {
                        Toast.makeText(context, "Room not found", Toast.LENGTH_SHORT).show()
                    }
                    isLoading = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSessionScreen(
    room: ListeningRoom,
    participantNames: List<String>,
    isHost: Boolean,
    userId: String,
    displayName: String,
    onLeaveSession: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var elapsedTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(room.sessionStartTime) {
        while (true) {
            elapsedTime = System.currentTimeMillis() - room.sessionStartTime
            delay(1000)
        }
    }
    
    // Auto-sync track, play/pause, and position for ALL participants (Spotify-like)
    val playerConnection = LocalPlayerConnection.current
    var lastSyncedTrackId by remember { mutableStateOf<String?>(null) }
    var lastIsPlaying by remember { mutableStateOf<Boolean?>(null) }
    var lastSyncedPosition by remember { mutableLongStateOf(0L) }
    
    // Sync whenever room state changes from polling - LISTENERS ONLY
    // Host controls the session, listeners follow
    LaunchedEffect(room.currentTrackId, room.isPlaying, room.startedAt) {
        // Don't run sync for host - host controls the session
        if (isHost) return@LaunchedEffect
        
        val trackId = room.currentTrackId
        val isPlaying = room.isPlaying
        
        if (trackId == null || trackId.isEmpty()) return@LaunchedEffect
        
        val player = playerConnection?.player ?: return@LaunchedEffect
        val currentlyPlayingId = player.currentMediaItem?.mediaId
        
        // Only load new track if it's actually different
        if (trackId != currentlyPlayingId && trackId != lastSyncedTrackId) {
            lastSyncedTrackId = trackId
            val metadata = MediaMetadata(
                id = trackId,
                title = room.currentTrackTitle ?: "Unknown",
                artists = listOf(MediaMetadata.Artist(id = null, name = room.currentTrackArtist ?: "")),
                duration = -1,
                thumbnailUrl = room.currentTrackThumbnail
            )
            playerConnection.playQueue(YouTubeQueue(WatchEndpoint(trackId), metadata))
            
            // Sync to started_at position after loading
            if (room.startedAt > 0 && isPlaying) {
                kotlinx.coroutines.delay(1000)
                val elapsedMs = System.currentTimeMillis() - room.startedAt
                player.seekTo(elapsedMs.coerceAtLeast(0))
            }
        }
        
        // Play/pause sync
        if (isPlaying != lastIsPlaying) {
            lastIsPlaying = isPlaying
            if (isPlaying && !player.isPlaying) {
                player.play()
            } else if (!isPlaying && player.isPlaying) {
                player.pause()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Session: ${room.roomCode}")
                        Text(
                            text = formatElapsedTime(elapsedTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onLeaveSession,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(painterResource(R.drawable.close), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (isHost) "End" else "Leave")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp), // Extra bottom padding for player + navbar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Room code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = room.roomCode,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 8.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomCode))
                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(painterResource(R.drawable.content_copy), "Copy", Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Host: ${room.hostName}" + if (isHost) " (You)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isHost) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Participants with Transfer Host
                var showTransferDialog by remember { mutableStateOf(false) }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.people_filled), null, Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Listeners (${participantNames.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            if (isHost && participantNames.size > 1) {
                                TextButton(onClick = { showTransferDialog = true }) {
                                    Icon(painterResource(R.drawable.people_outlined), null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Transfer", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        participantNames.forEachIndexed { index, name ->
                            val isCurrentHost = name == room.hostName
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isCurrentHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        name.firstOrNull()?.uppercase() ?: "?",
                                        color = if (isCurrentHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isCurrentHost) FontWeight.SemiBold else FontWeight.Normal)
                                    if (isCurrentHost) Text("🎧 Host", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                
                // Transfer Host Dialog
                if (showTransferDialog) {
                    val participants by ListenTogetherRepository.participants.collectAsState()
                    Dialog(onDismissRequest = { showTransferDialog = false }) {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(24.dp)) {
                                Text(stringResource(R.string.transfer_host_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.transfer_host_message), style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(16.dp))
                                
                                // List other participants (not host) - use zip to ensure ID-name pairing
                                participants.zip(participantNames).forEach { (participantId, name) ->
                                    // Skip the current host by ID comparison (more reliable)
                                    if (participantId != room.hostId) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    scope.launch {
                                                        ListenTogetherRepository.transferHost(
                                                            roomCode = room.roomCode,
                                                            currentHostId = userId,
                                                            newHostId = participantId,
                                                            newHostName = name
                                                        )
                                                        showTransferDialog = false
                                                        Toast.makeText(context, "Host transferred to $name!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Text(name, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                                
                                Spacer(Modifier.height(16.dp))
                                TextButton(onClick = { showTransferDialog = false }, modifier = Modifier.align(Alignment.End)) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Embedded Player Card with Sync Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.music_note), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Synced Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            if (room.isPlaying) {
                                Text("● LIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        if (room.currentTrackTitle.isNullOrEmpty()) {
                            // No track synced
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(painterResource(R.drawable.music_note), null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(Modifier.height(8.dp))
                                Text("No track synced", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text("Tap ⋮ on any song → Sync with Session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        } else {
                            // Track info
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(R.drawable.music_note), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(room.currentTrackTitle ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(room.currentTrackArtist ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            // Use actual player state for the icon
                            val actualIsPlaying = playerConnection?.isPlaying?.collectAsState()?.value ?: false
                            
                            // Playback controls - HOST ONLY
                            if (isHost) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous
                                    IconButton(onClick = { playerConnection?.seekToPrevious() }) {
                                        Icon(painterResource(R.drawable.skip_previous), "Previous", Modifier.size(32.dp))
                                    }
                                    
                                    Spacer(Modifier.width(16.dp))
                                    
                                    // Play/Pause
                                    IconButton(
                                        onClick = {
                                            if (room.currentTrackId != null) {
                                                val player = playerConnection?.player
                                                if (player?.isPlaying == true) {
                                                    player.pause()
                                                } else {
                                                    if (player?.currentMediaItem == null) {
                                                        room.currentTrackId?.let { trackId ->
                                                            val metadata = MediaMetadata(
                                                                id = trackId,
                                                                title = room.currentTrackTitle ?: "Unknown",
                                                                artists = listOf(MediaMetadata.Artist(id = null, name = room.currentTrackArtist ?: "")),
                                                                duration = -1,
                                                                thumbnailUrl = room.currentTrackThumbnail
                                                            )
                                                            playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(trackId), metadata))
                                                        }
                                                    } else {
                                                        player?.play()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            painterResource(if (actualIsPlaying) R.drawable.pause else R.drawable.play),
                                            if (actualIsPlaying) "Pause" else "Play",
                                            Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    
                                    Spacer(Modifier.width(16.dp))
                                    
                                    // Next
                                    IconButton(onClick = { playerConnection?.seekToNext() }) {
                                        Icon(painterResource(R.drawable.skip_next), "Next", Modifier.size(32.dp))
                                    }
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                Text(
                                    text = "Your controls sync to everyone",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                // Listener view - read-only with play indicator
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(if (actualIsPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painterResource(if (actualIsPlaying) R.drawable.equalizer else R.drawable.pause),
                                            if (actualIsPlaying) "Playing" else "Paused",
                                            Modifier.size(24.dp),
                                            tint = if (actualIsPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                Text(
                                    text = stringResource(R.string.synced_with_host),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Live-Stream Style Chat Section
                val messages by ListenTogetherRepository.messages.collectAsState()
                var messageText by remember { mutableStateOf("") }
                
                // Fetch messages on load and poll for new ones
                LaunchedEffect(room.roomCode) {
                    ListenTogetherRepository.fetchMessages(room.roomCode)
                    while (true) {
                        kotlinx.coroutines.delay(3000)
                        ListenTogetherRepository.fetchMessages(room.roomCode)
                    }
                }
                
                // Chat Container - messages persist (no fading)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (messages.isEmpty()) {
                        // Empty state
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "💬 No messages yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                "Start the conversation!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            reverseLayout = true,
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(messages.takeLast(50), key = { it.id }) { msg ->
                                val isMe = msg.senderId == userId
                                
                                // Message row - no fading, persists
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Small avatar
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isMe) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            msg.senderName.firstOrNull()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMe) MaterialTheme.colorScheme.onPrimary 
                                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(Modifier.width(8.dp))
                                    
                                    // Username and message inline
                                    Row {
                                        Text(
                                            text = if (isMe) "You" else msg.senderName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = ": ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = msg.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Fixed input field with proper sizing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { 
                            Text(
                                "Say something...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            ) 
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageText.isNotBlank()) {
                                    scope.launch {
                                        ListenTogetherRepository.sendMessage(
                                            roomCode = room.roomCode,
                                            senderId = userId,
                                            senderName = displayName,
                                            content = messageText.trim()
                                        )
                                        messageText = ""
                                    }
                                }
                            }
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                scope.launch {
                                    ListenTogetherRepository.sendMessage(
                                        roomCode = room.roomCode,
                                        senderId = userId,
                                        senderName = displayName,
                                        content = messageText.trim()
                                    )
                                    messageText = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_forward),
                            "Send",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateSessionDialog(onDismiss: () -> Unit, onCreateSession: (String, String, String) -> Unit) {
    val roomCode = remember { generateRoomCode() }
    var userName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Create Session", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                // Username input
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it.take(20) },
                    label = { Text("Your Name") },
                    placeholder = { Text("Enter your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                Text("Your room code:", style = MaterialTheme.typography.bodyMedium)
                Text(roomCode, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Share this code with friends to listen together!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onCreateSession(roomCode, "HOST_ONLY", userName.trim()) },
                        enabled = userName.isNotBlank()
                    ) { Text("Create") }
                }
            }
        }
    }
}

@Composable
private fun JoinSessionDialog(onDismiss: () -> Unit, onJoinSession: (String, String) -> Unit) {
    var roomCode by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Join Session", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                // Username input
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it.take(20) },
                    label = { Text("Your Name") },
                    placeholder = { Text("Enter your name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Room code
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { if (it.length <= 6) roomCode = it.uppercase() },
                    label = { Text("Room Code") },
                    placeholder = { Text("6-character code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { 
                        if (roomCode.length == 6 && userName.isNotBlank()) {
                            onJoinSession(roomCode, userName.trim())
                        }
                    })
                )
                
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onJoinSession(roomCode, userName.trim()) },
                        enabled = roomCode.length == 6 && userName.isNotBlank()
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}

private fun generateRoomCode(): String = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")

private fun formatElapsedTime(millis: Long): String {
    val s = (millis / 1000) % 60
    val m = (millis / 60000) % 60
    val h = millis / 3600000
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

private fun formatMessageTime(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

