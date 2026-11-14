package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.media3.common.Player
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection

@Composable
fun BoxScope.LyricsFullscreenContainer(
    state: LyricsState,
    lines: List<LyricsEntry>,
    lyrics: String?,
    mediaMetadata: MediaMetadata?,
    playerBackground: PlayerBackgroundStyle,
    rotateBackground: Boolean,
    rotation: Float,
    lyricsBackdropBrush: Brush,
    lyricsGlowBrush: Brush,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    playbackState: Int,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isLiked: Boolean,
    imageScale: Float,
    playerConnection: PlayerConnection,
    onNavigateBack: (() -> Unit)?,
    onSeekTo: (Long) -> Unit,
) {
    LyricsFullscreenBackground(
        mediaMetadata = mediaMetadata,
        playerBackground = playerBackground,
        rotateBackground = rotateBackground,
        rotation = rotation,
        lyricsBackdropBrush = lyricsBackdropBrush,
        lyricsGlowBrush = lyricsGlowBrush,
    )
    LyricsFullscreenHeader(
        visible = state.showControls,
        mediaMetadata = mediaMetadata,
        currentProvider = state.currentProvider,
        hasLyrics = !lyrics.isNullOrEmpty(),
        isTranslating = state.isTranslating,
        hasTranslation = state.translatedLyrics != null,
        isSelectionModeActive = state.isSelectionModeActive,
        selectedIndicesNotEmpty = state.selectedIndices.isNotEmpty(),
        onNavigateBack = onNavigateBack,
        onOpenTranslate = { state.showLanguageDialog = true },
        onOpenChords = { state.showChordDialogInLyrics = true },
        onShareSelected = {
            val txt = state.selectedIndices.sorted().mapNotNull { lines.getOrNull(it)?.text }.joinToString("\n")
            if (txt.isNotBlank()) {
                state.shareDialogData = Triple(txt, mediaMetadata?.title.orEmpty(), mediaMetadata?.artists?.joinToString { it.name }.orEmpty())
                state.showShareDialog = true
            }
            state.isSelectionModeActive = false
            state.selectedIndices.clear()
        },
        onOpenMenu = { state.showLyricsMenuDialog = true },
    )
    LyricsPlaybackControls(
        visible = state.showControls,
        position = position,
        duration = duration,
        isPlaying = isPlaying,
        playbackState = playbackState,
        repeatMode = repeatMode,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        isLiked = isLiked,
        onSeekTo = onSeekTo,
        onToggleRepeatMode = { playerConnection.player.repeatMode = if (playerConnection.player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF },
        onSeekToPrevious = { playerConnection.seekToPrevious() },
        onTogglePlayPause = {
            if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
            }
        },
        onSeekToNext = { playerConnection.seekToNext() },
        onToggleLike = { playerConnection.toggleLike() },
    )
    LyricsOverlayViews(
        showImageOverlay = state.showImageOverlay,
        onDismissImageOverlay = { state.showImageOverlay = false },
        mediaMetadata = mediaMetadata,
        imageScale = imageScale,
        cornerRadius = state.cornerRadius,
        rotateBackground = rotateBackground,
        rotation = rotation,
        isSelectionModeActive = state.isSelectionModeActive,
        selectedCount = state.selectedIndices.size,
        showControls = state.showControls,
    )
}
