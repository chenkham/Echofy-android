package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.jam.JamParticipantRole
import com.Chenkham.Echofy.jam.JamRoomCode
import com.Chenkham.Echofy.jam.JamSessionPhase
import com.Chenkham.Echofy.spotify.SpotifyLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchofyJamSheet(
    onDismiss: () -> Unit,
    initialRoomCode: String? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val jamSessionManager = playerConnection.service.jamSessionManager
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val sessionState by jamSessionManager.sessionState.collectAsState()
    val roomMeta by jamSessionManager.roomMeta.collectAsState()
    val participants by jamSessionManager.participants.collectAsState()
    val registry by jamSessionManager.registry.collectAsState()
    val currentSong by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val playlists by database.playlistsByCreateDateAsc().collectAsState(initial = emptyList())
    val spotifyViewModel: SpotifyLibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
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
    val jamTitle =
        remember(participants, session, roomCode, roomMeta) {
            val hostName =
                roomMeta?.hostName?.takeIf { it.isNotBlank() }
                    ?: participants.firstOrNull { it.role == JamParticipantRole.HOST }?.displayName
                    ?: session?.takeIf { it.role == JamParticipantRole.HOST }?.displayName
            when {
                hostName != null -> "$hostName's Together"
                roomCode.isNotBlank() -> "Together Session"
                else -> "Echofy Together"
            }
        }

    val launcher =
        remember(coroutineScope, context, playerConnection, database) {
            JamSessionLauncher(
                scope = coroutineScope,
                context = context,
                playerConnection = playerConnection,
                database = database,
                onWorkingState = { working, msg ->
                    isWorking = working
                    workingMessage = msg
                },
                onError = { transientError = it },
                onSessionStarted = { screen = JamSheetScreen.ACTIVE },
            )
        }

    LaunchedEffect(initialRoomCode) {
        val normalizedCode = initialRoomCode?.trim()?.uppercase()?.takeIf { JamRoomCode.parse(it) != null } ?: return@LaunchedEffect
        if (session == null) {
            joinCode = normalizedCode
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !isWorking }),
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
                    JamSheetScreen.HOME ->
                        JamHomeScreen(
                            errorMessage = transientError ?: sessionState.lastError,
                            onDismiss = onDismiss,
                            onStart = { screen = JamSheetScreen.START },
                            onJoin = { screen = JamSheetScreen.JOIN },
                        )
                    JamSheetScreen.START ->
                        JamStartSetupScreen(
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
                                when (selectedSeedOption) {
                                    JamSeedOption.CURRENT_QUEUE -> launcher.launchFromCurrentQueue(hostDisplayName)
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
                    JamSheetScreen.PLAYLISTS ->
                        PlaylistPickerScreen(
                            playlists = playlists,
                            spotifyPlaylists = spotifyPlaylists,
                            isWorking = isWorking,
                            onBack = { screen = JamSheetScreen.START },
                            onPickPlaylist = { playlist -> launcher.launchFromPlaylist(playlist, hostDisplayName) },
                            onPickSpotifyPlaylist = {
                                    spotifyPlaylist ->
                                launcher.launchFromSpotifyPlaylist(spotifyPlaylist, hostDisplayName)
                            },
                        )
                    JamSheetScreen.JOIN ->
                        JoinJamScreen(
                            roomCode = joinCode,
                            guestDisplayName = guestDisplayName,
                            isWorking = isWorking,
                            lastError = transientError ?: sessionState.lastError,
                            onBack = { screen = JamSheetScreen.HOME },
                            onRoomCodeChange = { joinCode = it.uppercase() },
                            onGuestNameChange = { guestDisplayName = it },
                            onJoin = { launcher.joinRoom(joinCode, guestDisplayName) },
                        )
                    JamSheetScreen.ACTIVE -> {
                        val canControl = session?.role == JamParticipantRole.HOST || (roomMeta?.allowGuestControls ?: true)
                        val isSyncing =
                            session?.role == JamParticipantRole.GUEST &&
                                remotePlayback != null &&
                                currentSong?.id != remotePlayback?.mediaId

                        if (isSyncing) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                canControlPlayback = canControl,
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
                                onToggleGuestControls = { enabled -> jamSessionManager.setAllowGuestControls(enabled) },
                                onTogglePlayPause = { if (canControl) playerConnection.togglePlayPause() },
                                onSkipPrevious = { if (canControl) playerConnection.seekToPrevious() },
                                onSkipNext = { if (canControl) playerConnection.seekToNext() },
                                onToggleShuffle = { if (canControl) playerConnection.toggleShuffle() },
                                onToggleRepeat = { if (canControl) playerConnection.toggleReplayMode() },
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
        JamInviteBottomSheet(
            roomCode = session.roomCode.roomCode,
            inviteLink = inviteLink,
            context = context,
            clipboardManager = clipboardManager,
            onDismissRequest = { showInviteSheet = false },
        )
    }
}
