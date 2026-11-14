package com.Chenkham.Echofy.ui.component

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.common.Player
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.constants.RotateBackgroundKey
import com.Chenkham.Echofy.constants.ShowLyricsKey
import com.Chenkham.Echofy.constants.lyricsSyncOffsetKey
import com.Chenkham.Echofy.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.lyrics.LyricsUtils.parseLyrics
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid")
@Composable
fun Lyrics(
    onNavigateBack: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isFullscreen = onNavigateBack != null
    val prefs = rememberLyricsPreferences()
    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val infiniteTransition = rememberInfiniteTransition(label = "bgRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation",
    )
    val rotateBackground by rememberPreference(RotateBackgroundKey, false)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSongId = mediaMetadata?.id
    val state = rememberLyricsState(currentSongId, mediaMetadata)

    LaunchedEffect(currentSongId) {
        currentSongId?.let { state.syncOffsetMs = state.context.dataStore.data.map { p -> p[lyricsSyncOffsetKey(it)] ?: 0L }.first() }
    }
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    LaunchedEffect(lyricsEntity) {
        val songId = lyricsEntity?.id ?: return@LaunchedEffect
        val cached = state.lyricsCache[songId]
        if (cached != null && lyricsEntity != null && cached.lyrics != lyricsEntity!!.lyrics) {
            state.lyricsCache = state.lyricsCache.toMutableMap().also { it.remove(songId) }
            state.currentLyricsEntity = lyricsEntity
            state.currentProvider = lyricsEntity?.providerName ?: "Manual"
        }
    }

    LaunchedEffect(currentSongId, state.refetchTrigger, state.reloadTrigger) {
        currentSongId?.let { state.fetchLyricsStreaming(it, state.refetchTrigger > 0) }
    }

    val lines = remember(lyrics, state.scope) {
        if (lyrics == null || lyrics == LYRICS_NOT_FOUND) emptyList()
        else if (lyrics.startsWith("[")) listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parseLyrics(lyrics)
        else lyrics.lines().mapIndexed { i, l -> LyricsEntry(i * 100L, l) }
    }

    LaunchedEffect(lines) {
        state.isSelectionModeActive = false
        state.selectedIndices.clear()
        state.currentLineIndex = -1
        state.deferredCurrentLineIndex = 0
        state.previousLineIndex = 0
        state.initialScrollDone = false
        state.shouldScrollToFirstLine = true
    }

    val isSynced = remember(lyrics) { !lyrics.isNullOrEmpty() && lyrics.startsWith("[") }
    val textColor = when (prefs.playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> if (prefs.useDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    }
    val lyricsBackdropBrush = Brush.verticalGradient(0.0f to Color(0xFF03050B), 0.34f to Color(0xFF07131F), 0.68f to Color(0xFF130815), 1.0f to Color.Black)
    val lyricsGlowBrush = Brush.linearGradient(listOf(Color(0xFF20D8FF).copy(0.30f), Color.Transparent, Color(0xFFFF4FCC).copy(0.22f), Color.Transparent))

    var position by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.duration) }

    BackHandler(enabled = state.isSelectionModeActive || isFullscreen) {
        if (state.isSelectionModeActive) { state.isSelectionModeActive = false; state.selectedIndices.clear() }
        else if (isFullscreen) onNavigateBack?.invoke()
    }

    LaunchedEffect(playbackState) {
        if (isFullscreen && playbackState == Player.STATE_READY) {
            while (isActive) { delay(100); position = playerConnection.player.currentPosition; duration = playerConnection.player.duration }
        }
    }

    val imageScale by animateFloatAsState(targetValue = if (state.showImageOverlay) 1f else 0.95f, animationSpec = spring(), label = "imageScale")

    LyricsScrollHandler(
        state = state,
        lines = lines,
        lyrics = lyrics,
        isSynced = isSynced,
        scrollLyrics = prefs.scrollLyrics,
    )

    Box(
        modifier = modifier.fillMaxSize().background(if (isFullscreen) MaterialTheme.colorScheme.background else Color.Transparent).then(
            if (isFullscreen) Modifier.pointerInput(Unit) { detectTapGestures(onTap = { if (!state.isSelectionModeActive) state.showControls = !state.showControls }) } else Modifier,
        ),
    ) {
        if (isFullscreen) {
            LyricsFullscreenContainer(
                state = state,
                lines = lines,
                lyrics = lyrics,
                mediaMetadata = mediaMetadata,
                playerBackground = prefs.playerBackground,
                rotateBackground = rotateBackground,
                rotation = rotation,
                lyricsBackdropBrush = lyricsBackdropBrush,
                lyricsGlowBrush = lyricsGlowBrush,
                position = position,
                duration = duration,
                isPlaying = isPlaying,
                playbackState = playbackState,
                repeatMode = repeatMode,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                isLiked = currentSong?.song?.liked == true,
                imageScale = imageScale,
                playerConnection = playerConnection,
                onNavigateBack = onNavigateBack,
                onSeekTo = { playerConnection.player.seekTo(it); position = it },
            )
        }
        LyricsListView(
            lazyListState = state.lazyListState,
            lines = lines,
            displayedCurrentLineIndex = if (state.isSeeking || state.isSelectionModeActive) state.deferredCurrentLineIndex else state.currentLineIndex,
            isLoadingLyrics = state.isLoadingLyrics,
            isSynced = isSynced,
            changeLyrics = prefs.changeLyrics,
            animateLyrics = prefs.animateLyrics,
            isSelectionModeActive = state.isSelectionModeActive,
            selectedIndices = state.selectedIndices,
            showTranslation = state.showTranslation,
            translatedLines = state.translatedLines,
            lyricsTextPosition = prefs.lyricsTextPosition,
            lyricsFontSize = prefs.lyricsFontSize,
            isFullscreen = isFullscreen,
            showLyrics = showLyrics,
            showControls = state.showControls,
            textColor = textColor,
            lyrics = lyrics,
            onSeekToLine = { item, index -> state.seekToLine(item, index) },
            onSelectLine = { index -> state.selectLine(index) },
            onToggleSelectionMode = { index -> state.toggleSelectionMode(index) },
            onUserScrollStateChange = { scrolling -> state.isUserScrolling = scrolling; state.lastPreviewTime = System.currentTimeMillis() },
            onRetryLyrics = { state.retryLyrics(currentSongId) },
        )
        if (!isFullscreen) {
            LyricsFloatingControls(
                isSelectionModeActive = state.isSelectionModeActive,
                selectedCount = state.selectedIndices.size,
                textColor = textColor,
                onCloseSelection = { state.isSelectionModeActive = false; state.selectedIndices.clear() },
                onShareSelected = {
                    val txt = state.selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
                    if (txt.isNotBlank()) {
                        state.shareDialogData = Triple(txt, mediaMetadata?.title.orEmpty(), mediaMetadata?.artists?.joinToString { it.name }.orEmpty())
                        state.showShareDialog = true
                    }
                    state.isSelectionModeActive = false
                    state.selectedIndices.clear()
                },
                onToggleShowLyrics = { showLyrics = !showLyrics },
                onOpenTranslate = { state.showLanguageDialog = true },
                onOpenMenu = { state.showLyricsMenuDialog = true },
            )
        }

        LyricsDialogs(
            state = state,
            lines = lines,
            mediaMetadata = mediaMetadata,
            currentSongId = currentSongId,
            playerConnection = playerConnection,
        )
    }
}
