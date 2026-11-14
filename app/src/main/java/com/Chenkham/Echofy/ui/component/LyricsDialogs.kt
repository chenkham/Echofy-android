package com.Chenkham.Echofy.ui.component

import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.Chenkham.Echofy.audio.ChordsManager
import com.Chenkham.Echofy.constants.lyricsSyncOffsetKey
import com.Chenkham.Echofy.db.entities.LyricsEntity
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.menu.LyricsMenu
import com.Chenkham.Echofy.utils.LyricsTranslationService
import com.Chenkham.Echofy.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsDialogs(
    state: LyricsState,
    lines: List<LyricsEntry>,
    mediaMetadata: MediaMetadata?,
    currentSongId: String?,
    playerConnection: PlayerConnection,
) {
    if (state.showLyricsMenuDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { state.showLyricsMenuDialog = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            mediaMetadata?.let { metadata ->
                LyricsMenu(
                    lyricsProvider = { state.currentLyricsEntity },
                    mediaMetadataProvider = { metadata },
                    onDismiss = { state.showLyricsMenuDialog = false },
                    onLyricsUpdated = {
                        currentSongId?.let { songId -> state.lyricsCache = state.lyricsCache.toMutableMap().also { it.remove(songId) } }
                        state.reloadTrigger++
                    },
                    onAdjustSync = { state.showLyricsMenuDialog = false; state.showSyncDialog = true },
                )
            }
        }
    }

    if (state.showSyncDialog) {
        LyricsSyncDialog(
            offsetMs = state.syncOffsetMs,
            onOffsetChange = { newOffset ->
                state.syncOffsetMs = newOffset
                currentSongId?.let { songId ->
                    state.scope.launch(Dispatchers.IO) {
                        state.context.dataStore.edit { p -> if (newOffset == 0L) p.remove(lyricsSyncOffsetKey(songId)) else p[lyricsSyncOffsetKey(songId)] = newOffset }
                    }
                }
            },
            onDismiss = { state.showSyncDialog = false },
        )
    }

    if (state.showChordDialogInLyrics) {
        mediaMetadata?.let { metadata ->
            val durationMs = playerConnection.player.duration.coerceAtLeast(60000L)
            val timeline = remember(metadata.id) { ChordsManager.generateTimeline(metadata.id, metadata.title, metadata.artists.joinToString { it.name }, durationMs) }
            val currentPos = playerConnection.player.currentPosition
            val activeChord = timeline.chords.find { currentPos in it.startMs..it.endMs }?.chord ?: timeline.key
            ChordDiagramDialog(initialChord = activeChord, progression = timeline.chords.map { it.chord }, onDismiss = { state.showChordDialogInLyrics = false })
        }
    }

    if (state.showLanguageDialog) {
        LyricsLanguageDialog(
            selectedTranslationLang = state.selectedTranslationLang,
            translatedLines = state.translatedLines,
            showTranslation = state.showTranslation,
            onRomanize = {
                state.showLanguageDialog = false
                state.isTranslating = true
                state.scope.launch {
                    runCatching {
                        LyricsTranslationService.transliterateLines(lines.map { it.text })
                            .onSuccess { lineMap -> state.translatedLines = lineMap; state.translatedLyrics = lineMap.values.joinToString("\n"); state.showTranslation = true }
                            .onFailure { e -> Toast.makeText(state.context, "Romanization failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                    state.isTranslating = false
                }
            },
            onSelectLanguage = { code ->
                state.selectedTranslationLang = code
                state.showLanguageDialog = false
                state.isTranslating = true
                state.scope.launch {
                    runCatching {
                        val indexed = lines.map { it.text }.withIndex().filter { it.value.isNotBlank() }
                        LyricsTranslationService.translateCached(currentSongId.orEmpty(), indexed.joinToString("\n") { it.value }, code)
                            .onSuccess { trans ->
                                val transList = trans.lines()
                                if (transList.size == indexed.size) {
                                    val lineMap = mutableMapOf<Int, String>()
                                    indexed.forEachIndexed { pos, entry -> lineMap[entry.index] = transList[pos] }
                                    state.translatedLines = lineMap
                                    state.translatedLyrics = trans
                                    state.showTranslation = true
                                } else Toast.makeText(state.context, "Translation alignment failed", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { e -> Toast.makeText(state.context, "Translation failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                    state.isTranslating = false
                }
            },
            onToggleTranslation = { state.showTranslation = !state.showTranslation; state.showLanguageDialog = false },
            onClearTranslation = { state.translatedLines = emptyMap(); state.translatedLyrics = null; state.showTranslation = false; state.showLanguageDialog = false },
            onDismiss = { state.showLanguageDialog = false },
        )
    }

    if (state.showProviderDialog) {
        LyricsProviderDialog(
            currentProvider = state.currentProvider,
            providerNames = remember { listOf("Musixmatch", "LrcLib", "KuGou", "YouTube") },
            streamingResults = state.streamingResults,
            isLoadingLyrics = state.isLoadingLyrics,
            onSelectProvider = { _, name ->
                val result = state.streamingResults.find { it.providerName == name }
                if (result != null) {
                    currentSongId?.let { songId ->
                        val entity = LyricsEntity(songId, result.lyrics)
                        state.currentLyricsEntity = entity
                        state.currentProvider = name
                        state.scope.launch(Dispatchers.IO) { runCatching { state.database.query { upsert(entity) } } }
                    }
                    state.showProviderDialog = false
                }
            },
            onRefetchAll = { state.showProviderDialog = false; state.refetchTrigger++ },
            onDismiss = { state.showProviderDialog = false },
        )
    }

    if (state.showShareDialog && state.shareDialogData != null) {
        LyricsShareMenuDialog(
            lyricsText = state.shareDialogData!!.first,
            songTitle = state.shareDialogData!!.second,
            artists = state.shareDialogData!!.third,
            mediaMetadata = mediaMetadata,
            onDismiss = { state.showShareDialog = false; state.shareDialogData = null },
        )
    }
}
