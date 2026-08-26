package com.Chenkham.Echofy.ui.component

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.MainActivity
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.Playlist
import com.Chenkham.Echofy.jam.JamParticipant
import com.Chenkham.Echofy.jam.JamParticipantRole
import com.Chenkham.Echofy.jam.JamQueueItem
import com.Chenkham.Echofy.jam.JamRegistry
import com.Chenkham.Echofy.jam.JamRoomCode
import com.Chenkham.Echofy.jam.JamSessionPhase
import com.Chenkham.Echofy.jam.JamSessionState
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.models.toMediaMetadata
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class JamSheetScreen {
    HOME,
    START,
    PLAYLISTS,
    JOIN,
    ACTIVE,
}

private enum class JamSeedOption {
    CURRENT_QUEUE,
    PLAYLIST,
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun EchofyJamSheet(
    onDismiss: () -> Unit,
    initialRoomCode: String? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val jamSessionManager = playerConnection.service.jamSessionManager
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboardManager = LocalClipboardManager.current

    val sessionState by jamSessionManager.sessionState.collectAsState()
    val roomMeta by jamSessionManager.roomMeta.collectAsState()
    val queueItems by jamSessionManager.queueSnapshot.collectAsState()
    val participants by jamSessionManager.participants.collectAsState()
    val registry by jamSessionManager.registry.collectAsState()
    val currentSong by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val playlists by database.playlistsByCreateDateAsc().collectAsState(initial = emptyList())
    val spotifyViewModel: com.Chenkham.Echofy.spotify.SpotifyLibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val spotifyPlaylists by spotifyViewModel.playlists.collectAsState()
    val remotePlayback by jamSessionManager.remotePlayback.collectAsState()

    var screen by rememberSaveable { mutableStateOf(JamSheetScreen.HOME) }
    var showInviteSheet by rememberSaveable { mutableStateOf(false) }
    var hostDisplayName by rememberSaveable { mutableStateOf("") }
    var guestDisplayName by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var selectedSeedOption by rememberSaveable { mutableStateOf(JamSeedOption.CURRENT_QUEUE) }
    var transientError by rememberSaveable { mutableStateOf<String?>(null) }
    var isWorking by rememberSaveable { mutableStateOf(false) }
    var workingMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val session = sessionState.session
    val roomCode = session?.roomCode?.roomCode.orEmpty()
    val inviteLink = remember(registry, roomCode) { buildInviteLink(registry, roomCode) }
    val hostParticipant = remember(participants, session) {
        participants.firstOrNull { it.role == JamParticipantRole.HOST }
            ?: session?.takeIf { it.role == JamParticipantRole.HOST }?.displayName?.let {
                JamParticipant(
                    participantId = session.participantId,
                    displayName = it,
                    role = session.role,
                    authUid = session.authUid,
                )
            }
    }
    val jamTitle = remember(participants, session, roomCode, roomMeta) {
        val hostName = roomMeta?.hostName?.takeIf { it.isNotBlank() }
            ?: participants.firstOrNull { it.role == JamParticipantRole.HOST }?.displayName
            ?: session?.takeIf { it.role == JamParticipantRole.HOST }?.displayName
        when {
            hostName != null -> "$hostName's Together"
            roomCode.isNotBlank() -> "Together Session"
            else -> "Echofy Together"
        }
    }

    LaunchedEffect(initialRoomCode) {
        val normalizedRoomCode = initialRoomCode
            ?.trim()
            ?.uppercase()
            ?.takeIf { JamRoomCode.parse(it) != null }
            ?: return@LaunchedEffect
        if (session == null) {
            joinCode = normalizedRoomCode
            screen = JamSheetScreen.JOIN
        }
    }

    LaunchedEffect(sessionState.phase) {
        if (session != null && sessionState.phase != JamSessionPhase.IDLE && sessionState.phase != JamSessionPhase.ERROR) {
            screen = JamSheetScreen.ACTIVE
        } else if (screen == JamSheetScreen.ACTIVE) {
            screen = JamSheetScreen.HOME
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isWorking) onDismiss() },
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !isWorking }
        ),
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                label = "JamSheetScreen",
                modifier = Modifier.fillMaxSize(),
            ) { activeScreen ->
                when (activeScreen) {
                    JamSheetScreen.HOME -> JamHomeScreen(
                        errorMessage = transientError ?: sessionState.lastError,
                        onDismiss = onDismiss,
                        onStart = { screen = JamSheetScreen.START },
                        onJoin = { screen = JamSheetScreen.JOIN },
                    )

                    JamSheetScreen.START -> JamStartSetupScreen(
                    currentSong = currentSong,
                    hostDisplayName = hostDisplayName,
                    selectedSeedOption = selectedSeedOption,
                    isWorking = isWorking,
                    playlistsAvailable = playlists.isNotEmpty() || spotifyPlaylists.isNotEmpty(),
                    onBack = { screen = JamSheetScreen.HOME },
                    onHostNameChange = { hostDisplayName = it },
                    onSeedOptionChange = { selectedSeedOption = it },
                    onPickPlaylist = { screen = JamSheetScreen.PLAYLISTS },
                    onStart = {
                        transientError = null
                        when (selectedSeedOption) {
                            JamSeedOption.CURRENT_QUEUE -> {
                                lifecycleOwner.lifecycleScope.launch {
                                    isWorking = true
                                    workingMessage = "Creating your room..."
                                    val playbackQueue = playerConnection.service.currentPlaybackQueueForJam()
                                    if (playbackQueue.isEmpty()) {
                                        transientError = "Play a song first, then start the Jam from your current queue."
                                        isWorking = false
                                        workingMessage = null
                                        return@launch
                                    }
                                    jamSessionManager.createHostedSession(hostDisplayName.ifBlank { "Host" })
                                        .onSuccess { activeSession ->
                                            playerConnection.service.seedJamQueue(playbackQueue.drop(1))
                                            playerConnection.service.syncJamStateNow()
                                            screen = JamSheetScreen.ACTIVE
                                            Toast.makeText(
                                                context,
                                                "Room ${activeSession.roomCode.roomCode} is live",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        .onFailure { error ->
                                            transientError = error.message ?: "Unable to start this Jam"
                                        }
                                    isWorking = false
                                    workingMessage = null
                                }
                            }

                            JamSeedOption.PLAYLIST -> {
                                if (playlists.isEmpty() && spotifyPlaylists.isEmpty()) {
                                    transientError = "Add or bookmark a playlist first, then start the Jam from it."
                                } else {
                                    screen = JamSheetScreen.PLAYLISTS
                                }
                            }
                        }
                    },
                    )

                    JamSheetScreen.PLAYLISTS -> PlaylistPickerScreen(
                    playlists = playlists,
                    spotifyPlaylists = spotifyPlaylists,
                    isWorking = isWorking,
                    onBack = { screen = JamSheetScreen.START },
                    onPickPlaylist = { playlist ->
                        lifecycleOwner.lifecycleScope.launch {
                            isWorking = true
                            workingMessage = "Creating your room..."
                            transientError = null
                            val songs = database.playlistSongs(playlist.playlist.id)
                                .first()
                                .map { it.song.toMediaMetadata() }
                            if (songs.isEmpty()) {
                                transientError = "That playlist is empty."
                                isWorking = false
                                workingMessage = null
                                return@launch
                            }
                            jamSessionManager.createHostedSession(hostDisplayName.ifBlank { "Host" })
                                .onSuccess { activeSession ->
                                    playerConnection.service.replacePlayerQueueForJam(songs)
                                    playerConnection.service.seedJamQueue(songs.drop(1))
                                    playerConnection.service.syncJamStateNow()
                                    screen = JamSheetScreen.ACTIVE
                                    Toast.makeText(
                                        context,
                                        "Room ${activeSession.roomCode.roomCode} is live",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .onFailure { error ->
                                    transientError = error.message ?: "Unable to start this Jam"
                                }
                            isWorking = false
                            workingMessage = null
                        }
                    },
                    onPickSpotifyPlaylist = { spotifyPlaylist ->
                        lifecycleOwner.lifecycleScope.launch {
                            isWorking = true
                            workingMessage = "Fetching Spotify tracks..."
                            transientError = null
                            val paging = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                com.arturo254.opentune.spotify.Spotify.playlistTracks(spotifyPlaylist.id).getOrNull()
                            }
                            val tracks = paging?.items?.mapNotNull { it.track } ?: emptyList()
                            if (tracks.isEmpty()) {
                                transientError = "That playlist is empty or could not be loaded."
                                isWorking = false
                                workingMessage = null
                                return@launch
                            }
                            workingMessage = "Resolving music..."
                            val songs = tracks.take(50).mapNotNull { track ->
                                com.Chenkham.Echofy.spotify.SpotifyPlaybackResolver.resolveToMetadata(track)
                            }
                            jamSessionManager.createHostedSession(hostDisplayName.ifBlank { "Host" })
                                .onSuccess { activeSession ->
                                    playerConnection.service.replacePlayerQueueForJam(songs)
                                    playerConnection.service.seedJamQueue(songs.drop(1))
                                    playerConnection.service.syncJamStateNow()
                                    screen = JamSheetScreen.ACTIVE
                                    Toast.makeText(
                                        context,
                                        "Room ${activeSession.roomCode.roomCode} is live",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .onFailure { error ->
                                    transientError = error.message ?: "Unable to start this Jam"
                                }
                            isWorking = false
                            workingMessage = null
                        }
                    },
                    )

                    JamSheetScreen.JOIN -> JoinJamScreen(
                    roomCode = joinCode,
                    guestDisplayName = guestDisplayName,
                    isWorking = isWorking,
                    lastError = transientError ?: sessionState.lastError,
                    onBack = { screen = JamSheetScreen.HOME },
                    onRoomCodeChange = { joinCode = it.uppercase() },
                    onGuestNameChange = { guestDisplayName = it },
                    onJoin = {
                        lifecycleOwner.lifecycleScope.launch {
                            transientError = null
                            isWorking = true
                            workingMessage = "Finding room..."
                            jamSessionManager.joinSession(
                                rawRoomCode = joinCode,
                                displayName = guestDisplayName.ifBlank { "Listener" },
                            ).onSuccess {
                                screen = JamSheetScreen.ACTIVE
                                Toast.makeText(context, "Joined ${it.roomCode.roomCode}", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                transientError = error.message ?: "Room not found"
                            }
                            isWorking = false
                            workingMessage = null
                        }
                    },
                    )

                    JamSheetScreen.ACTIVE -> {
                        val isSyncing = session?.role == JamParticipantRole.GUEST &&
                            remotePlayback != null &&
                            currentSong?.id != remotePlayback?.mediaId

                        if (isSyncing) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = Color(0xFF1DB954))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Syncing with host...", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        } else {
                            ActiveJamScreen(
                            jamTitle = jamTitle,
                            roomCode = roomCode,
                            isHost = session?.role == JamParticipantRole.HOST,
                            canControlPlayback = session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true),
                            participants = participants,
                            allowGuestControls = roomMeta?.allowGuestControls ?: true,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            requireApproval = roomMeta?.requireApproval ?: false,
                            onRequireApprovalChange = { jamSessionManager.updateRequireApproval(it) },
                            canSkipPrevious = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            shuffleEnabled = playerConnection.shuffleModeEnabled.collectAsState().value,
                            repeatMode = playerConnection.repeatMode.collectAsState().value,
                            onDismiss = onDismiss,
                            onInviteClick = { showInviteSheet = true },
                            onLeaveClick = {
                                jamSessionManager.leaveSession()
                                transientError = null
                                screen = JamSheetScreen.HOME
                            },
                            onToggleGuestControls = { enabled ->
                                jamSessionManager.setAllowGuestControls(enabled)
                            },
                            onTogglePlayPause = {
                                if (session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true)) {
                                    playerConnection.togglePlayPause()
                                }
                            },
                            onSkipPrevious = {
                                if (session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true)) {
                                    playerConnection.seekToPrevious()
                                }
                            },
                            onSkipNext = {
                                if (session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true)) {
                                    playerConnection.seekToNext()
                                }
                            },
                            onToggleShuffle = {
                                if (session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true)) {
                                    playerConnection.toggleShuffle()
                                }
                            },
                            onToggleRepeat = {
                                if (session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true)) {
                                    playerConnection.toggleReplayMode()
                                }
                            },
                            )
                        }
                    }
                }
            }
            workingMessage?.let { message ->
                JamWorkingOverlay(message = message)
            }
        }
    }

    if (showInviteSheet && session != null) {
        ModalBottomSheet(
            onDismissRequest = { showInviteSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF121212),
        ) {
            InviteJamSheetContent(
                roomCode = session.roomCode.roomCode,
                inviteLink = inviteLink,
                onCopyCode = {
                    clipboardManager.setText(AnnotatedString(session.roomCode.roomCode))
                    Toast.makeText(context, "Room code copied", Toast.LENGTH_SHORT).show()
                },
                onShare = {
                    val shareText = buildInviteShareText(session.roomCode.roomCode, inviteLink)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Jam invite"))
                },
            )
        }
    }
}

@Composable
private fun JamWorkingOverlay(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Please keep this screen open.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.64f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun JamHomeScreen(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onJoin: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF14321F), Color(0xFF121212), Color(0xFF121212)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(painter = painterResource(R.drawable.close), contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Together (Beta)",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Start a shared listening session with your current song or a playlist, then invite friends to listen together in sync.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.72f),
            )

            errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    color = Color(0xFF442B2E),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFFFC7CE),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Start a Together session",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Choose the music source first, then your room becomes shareable by code or link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.68f),
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                    ) {
                        Text("Start Together", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onJoin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                    ) {
                        Text("Join with code", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun JamStartSetupScreen(
    currentSong: MediaMetadata?,
    hostDisplayName: String,
    selectedSeedOption: JamSeedOption,
    playlistsAvailable: Boolean,
    isWorking: Boolean,
    onBack: () -> Unit,
    onHostNameChange: (String) -> Unit,
    onSeedOptionChange: (JamSeedOption) -> Unit,
    onPickPlaylist: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        SheetHeader(
            title = "Start Together",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = hostDisplayName,
            onValueChange = onHostNameChange,
            singleLine = true,
            label = { Text("Your name") },
            placeholder = { Text("Host") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Choose what starts the room",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SourceOptionCard(
            title = "Current song",
            subtitle = currentSong?.let {
                "Everyone will hear \"${it.title}\" — manage what plays next from your main player."
            } ?: "Play a song first, then start the session.",
            selected = selectedSeedOption == JamSeedOption.CURRENT_QUEUE,
            enabled = currentSong != null,
            onClick = { onSeedOptionChange(JamSeedOption.CURRENT_QUEUE) },
        )

        Spacer(modifier = Modifier.height(10.dp))

        SourceOptionCard(
            title = "From a playlist",
            subtitle = if (playlistsAvailable) {
                "Pick a playlist — the first song plays for everyone. Manage what's next from your main player."
            } else {
                "No playlists found yet. Add or bookmark one first."
            },
            selected = selectedSeedOption == JamSeedOption.PLAYLIST,
            enabled = playlistsAvailable,
            onClick = {
                onSeedOptionChange(JamSeedOption.PLAYLIST)
                onPickPlaylist()
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedSeedOption == JamSeedOption.CURRENT_QUEUE) {
            CurrentSourcePreview(currentSong = currentSong)
            Spacer(modifier = Modifier.height(20.dp))
        }

        Button(
            onClick = onStart,
            enabled = !isWorking,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = CircleShape,
        ) {
            Text(if (isWorking) "Starting..." else "Start Together", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaylistPickerScreen(
    playlists: List<Playlist>,
    spotifyPlaylists: List<com.arturo254.opentune.spotify.models.SpotifyPlaylist> = emptyList(),
    isWorking: Boolean,
    onBack: () -> Unit,
    onPickPlaylist: (Playlist) -> Unit,
    onPickSpotifyPlaylist: (com.arturo254.opentune.spotify.models.SpotifyPlaylist) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        SheetHeader(
            title = "Choose playlist",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (playlists.isEmpty() && spotifyPlaylists.isEmpty()) {
            Text(
                text = "No playlists are available yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(playlists, key = { it.playlist.id }) { playlist ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isWorking) { onPickPlaylist(playlist) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${playlist.songCount} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.66f),
                            )
                        }
                    }
                }
            }

            items(spotifyPlaylists, key = { "spotify_${it.id}" }) { spotifyPlaylist ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isWorking) { onPickSpotifyPlaylist(spotifyPlaylist) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1DB954).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.spotify_icon),
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = spotifyPlaylist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = spotifyPlaylist.tracks?.total?.let { "$it songs • Spotify" } ?: "Spotify Playlist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1DB954).copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinJamScreen(
    roomCode: String,
    guestDisplayName: String,
    isWorking: Boolean,
    lastError: String?,
    onBack: () -> Unit,
    onRoomCodeChange: (String) -> Unit,
    onGuestNameChange: (String) -> Unit,
    onJoin: () -> Unit,
) {
    val isJoinEnabled = JamRoomCode.parse(roomCode) != null && !isWorking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        SheetHeader(
            title = "Join Together",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedTextField(
            value = roomCode,
            onValueChange = onRoomCodeChange,
            singleLine = true,
            label = { Text("Room code") },
            placeholder = { Text("01-AB92FQ") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = guestDisplayName,
            onValueChange = onGuestNameChange,
            singleLine = true,
            label = { Text("Your name") },
            placeholder = { Text("Listener") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (!lastError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = Color(0xFF442B2E),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = lastError,
                    color = Color(0xFFFFC7CE),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onJoin,
            enabled = isJoinEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = CircleShape,
        ) {
            Text(if (isWorking) "Joining..." else "Join Together", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActiveJamScreen(
    jamTitle: String,
    roomCode: String,
    isHost: Boolean,
    canControlPlayback: Boolean,
    participants: List<JamParticipant>,
    allowGuestControls: Boolean,
    currentSong: MediaMetadata?,
    isPlaying: Boolean,
    requireApproval: Boolean,
    onRequireApprovalChange: (Boolean) -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onDismiss: () -> Unit,
    onInviteClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onToggleGuestControls: (Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    var showParticipantsDialog by remember { mutableStateOf(false) }

    if (showParticipantsDialog) {
        ParticipantsDialog(
            participants = participants,
            isHost = isHost,
            requireApproval = requireApproval,
            onRequireApprovalChange = onRequireApprovalChange,
            onDismiss = { showParticipantsDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
    ) {
        if (!currentSong?.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentSong?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1DB954).copy(alpha = 0.18f),
                            Color(0xFF121212),
                            Color(0xFF121212),
                        ),
                    ),
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(painter = painterResource(R.drawable.close), contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isHost) "You're hosting" else "Listening on this phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1DB954),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = jamTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = roomCode,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.58f),
                )
                if (participants.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${participants.size} ${if (participants.size == 1) "listener" else "listeners"}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1DB954),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ParticipantRow(
                        participants = participants,
                        onClick = { showParticipantsDialog = true }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onInviteClick, shape = CircleShape) {
                            Text("Invite")
                        }
                        OutlinedButton(onClick = onLeaveClick, shape = CircleShape) {
                            Text(if (isHost) "End" else "Leave")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Let others change what's playing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Switch(
                        checked = allowGuestControls,
                        enabled = isHost,
                        onCheckedChange = onToggleGuestControls,
                    )
                }
            }

            // ── Big now-playing card — fills remaining space ──────────────
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Album art
                    if (!currentSong?.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentSong?.thumbnailUrl,
                            contentDescription = currentSong?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(20.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF282828)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = currentSong?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong?.artists?.joinToString { it.name } ?: "—",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (!isHost && !canControlPlayback) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Manage your queue from the main player",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.45f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // ── Playback bar ──────────────────────────────────────────────
            JamPlaybackBar(
                canControlPlayback = canControlPlayback,
                isPlaying = isPlaying,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                shuffleEnabled = shuffleEnabled,
                repeatMode = repeatMode,
                onTogglePlayPause = onTogglePlayPause,
                onSkipPrevious = onSkipPrevious,
                onSkipNext = onSkipNext,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
            )
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun SourceOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF1C2D22) else Color(0xFF181818),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) Color(0xFF1DB954) else Color.Transparent,
                    ),
            ) {
                if (!selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (enabled) 0.26f else 0.12f)),
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) Color.White.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.32f),
                )
            }
        }
    }
}

@Composable
private fun CurrentSourcePreview(currentSong: MediaMetadata?) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!currentSong?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = currentSong?.thumbnailUrl,
                    contentDescription = currentSong?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF282828)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.66f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "Nothing is playing yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = currentSong?.artists?.joinToString { it.name } ?: "Start playback to build the Together queue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.66f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CurrentJamTrackRow(
    currentSong: MediaMetadata?,
    canControlPlayback: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!currentSong?.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentSong?.thumbnailUrl,
                contentDescription = currentSong?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF282828)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.66f),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong?.title ?: "Nothing is playing yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (currentSong != null) Color(0xFF1DB954) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentSong?.artists?.joinToString { it.name } ?: "Start playback to sync the room",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "More options",
                    tint = Color.White.copy(alpha = 0.72f),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                if (currentSong != null) {
                    DropdownMenuItem(
                        text = { Text("View artist") },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, currentSong.artists.firstOrNull()?.name ?: "Unknown artist", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.artist), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("View album") },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, currentSong.album?.title ?: "Unknown album", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.album), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share song") },
                        onClick = {
                            showMenu = false
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${currentSong.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    item: JamQueueItem,
    canControl: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifBlank { item.mediaId },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist.ifBlank { "Unknown Artist" },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.64f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (canControl) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "Queue item options",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        onClick = { showMenu = false; onMoveUp() },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.arrow_upward), contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        onClick = { showMenu = false; onMoveDown() },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.arrow_downward), contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Remove", color = Color(0xFFFF6B6B)) },
                        onClick = { showMenu = false; onRemove() },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.remove), contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                        },
                    )
                }
            }
        } else {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun JamPlaybackBar(
    canControlPlayback: Boolean,
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onToggleShuffle,
                enabled = canControlPlayback,
            ) {
                Icon(
                    painter = painterResource(if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = when {
                        !canControlPlayback -> Color.White.copy(alpha = 0.36f)
                        shuffleEnabled -> Color(0xFF1DB954)
                        else -> Color.White.copy(alpha = 0.72f)
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = onSkipPrevious,
                enabled = canControlPlayback && canSkipPrevious,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    tint = if (canControlPlayback && canSkipPrevious) Color.White else Color.White.copy(alpha = 0.36f),
                    modifier = Modifier.size(28.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (canControlPlayback) Color.White else Color(0xFF2A2A2A))
                    .clickable(enabled = canControlPlayback, onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (canControlPlayback) Color.Black else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(
                onClick = onSkipNext,
                enabled = canControlPlayback && canSkipNext,
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    tint = if (canControlPlayback && canSkipNext) Color.White else Color.White.copy(alpha = 0.36f),
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(
                onClick = onToggleRepeat,
                enabled = canControlPlayback,
            ) {
                val repeatIcon = when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                    else -> R.drawable.repeat
                }
                Icon(
                    painter = painterResource(repeatIcon),
                    contentDescription = "Repeat",
                    tint = when {
                        !canControlPlayback -> Color.White.copy(alpha = 0.36f)
                        repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF -> Color(0xFF1DB954)
                        else -> Color.White.copy(alpha = 0.72f)
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun InviteJamSheetContent(
    roomCode: String,
    inviteLink: String?,
    onCopyCode: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Invite friends",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "Share the room code now. If your control plane has a public invite URL, the same share message will include it too.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.68f),
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Room code",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.68f),
                )
                Text(
                    text = roomCode,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (!inviteLink.isNullOrBlank()) {
                    Text(
                        text = inviteLink,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8CE3AE),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCopyCode,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text("Copy code")
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Text("Share")
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun TogetherAddSongsScreen(
    onBack: () -> Unit,
    onAddToQueue: (MediaMetadata) -> Unit,
    onPlayNext: (MediaMetadata) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White)
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search songs to add...", color = Color.White.copy(alpha = 0.5f)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF1DB954),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    if (query.isNotBlank()) {
                        isSearching = true
                        searchError = null
                        coroutineScope.launch {
                            val searchResult = withContext(Dispatchers.IO) {
                                runCatching { YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow() }
                            }
                            searchResult.onSuccess { page ->
                                results = page.items.filterIsInstance<SongItem>()
                            }.onFailure {
                                searchError = "Search failed. Check your connection."
                            }
                            isSearching = false
                        }
                    }
                }),
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = ""; results = emptyList() }) {
                    Icon(painter = painterResource(R.drawable.close), contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isSearching -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Searching...", color = Color.White.copy(alpha = 0.6f))
                }
            }
            searchError != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(searchError!!, color = Color(0xFFFFC7CE))
                }
            }
            results.isEmpty() && query.isNotBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found", color = Color.White.copy(alpha = 0.6f))
                }
            }
            results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Search for songs to add to Together", color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(results, key = { it.id }) { song ->
                        TogetherSearchResultRow(
                            song = song,
                            onAddToQueue = {
                                val metadata = song.toMediaMetadata()
                                onAddToQueue(metadata)
                            },
                            onPlayNext = {
                                val metadata = song.toMediaMetadata()
                                onPlayNext(metadata)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TogetherSearchResultRow(
    song: SongItem,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onAddToQueue() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (song.thumbnail.isNotBlank()) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF282828)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.music_note), contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString { it.name },
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "Options",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to Together queue") },
                    onClick = { showMenu = false; onAddToQueue() },
                    leadingIcon = { Icon(painterResource(R.drawable.queue_music), contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                DropdownMenuItem(
                    text = { Text("Play next") },
                    onClick = { showMenu = false; onPlayNext() },
                    leadingIcon = { Icon(painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun ParticipantRow(participants: List<JamParticipant>, onClick: () -> Unit = {}) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        participants.take(4).forEach { participant ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (participant.role == JamParticipantRole.HOST) Color(0xFF1DB954)
                        else Color(0xFF2B2B2B),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = participant.nameInitial,
                    color = if (participant.role == JamParticipantRole.HOST) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (participants.size > 4) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF181818)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+${participants.size - 4}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        // Listener count chip
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
        ) {
            Text(
                text = "${participants.size}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ParticipantsDialog(
    participants: List<JamParticipant>,
    isHost: Boolean,
    requireApproval: Boolean,
    onRequireApprovalChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = {
            Column {
                Text(
                    "Room Participants",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    "${participants.size} ${if (participants.size == 1) "person" else "people"} listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1DB954)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isHost) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Require Approval to Join",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Switch(
                            checked = requireApproval,
                            onCheckedChange = onRequireApprovalChange
                        )
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(participants, key = { it.participantId }) { participant ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (participant.role == JamParticipantRole.HOST) Color(0xFF1DB954)
                                        else Color(0xFF2B2B2B),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                            Text(
                                    text = participant.nameInitial,
                                    color = if (participant.role == JamParticipantRole.HOST) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = participant.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = if (participant.role == JamParticipantRole.HOST) "Host" else "Listener",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (participant.role == JamParticipantRole.HOST) Color(0xFF1DB954) else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF1DB954))
            }
        }
    )
}

private fun buildInviteLink(
    registry: JamRegistry,
    roomCode: String,
): String? {
    if (roomCode.isBlank()) return null
    val base = registry.inviteBaseUrl.removeSuffix("/")
    if (base.isBlank()) return null
    return if (base.endsWith("/r")) {
        "$base/$roomCode"
    } else {
        "$base/r/$roomCode"
    }
}

private fun buildInviteShareText(
    roomCode: String,
    inviteLink: String?,
): String = buildString {
    append("Join my Echofy Together session")
    appendLine()
    append("Room code: $roomCode")
    inviteLink?.takeIf { it.isNotBlank() }?.let {
        appendLine()
        append(it)
    }
}
