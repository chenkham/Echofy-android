package com.Chenkham.Echofy.ui.component

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.MusicDatabase
import com.Chenkham.Echofy.db.entities.LyricsEntity
import com.Chenkham.Echofy.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.Chenkham.Echofy.db.getLyrics
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.lyrics.LyricsResult
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LyricsState(
    val context: Context,
    val scope: CoroutineScope,
    val database: MusicDatabase,
    val playerConnection: PlayerConnection,
    val mediaMetadata: MediaMetadata?,
    val currentSongId: String?,
) {
    var currentLineIndex by mutableIntStateOf(-1)
    var deferredCurrentLineIndex by mutableIntStateOf(0)
    var previousLineIndex by mutableIntStateOf(0)
    var lastPreviewTime by mutableLongStateOf(0L)
    var isSeeking by mutableStateOf(false)
    var initialScrollDone by mutableStateOf(false)
    var shouldScrollToFirstLine by mutableStateOf(true)
    var isAppMinimized by mutableStateOf(false)
    var showImageOverlay by mutableStateOf(false)
    var showControls by mutableStateOf(true)
    var cornerRadius by mutableFloatStateOf(16f)

    var isSelectionModeActive by mutableStateOf(false)
    var selectedIndices = mutableStateListOf<Int>()
    var showMaxSelectionToast by mutableStateOf(false)
    var showShareDialog by mutableStateOf(false)
    var shareDialogData by mutableStateOf<Triple<String, String, String>?>(null)

    var translatedLyrics by mutableStateOf<String?>(null)
    var translatedLines by mutableStateOf<Map<Int, String>>(emptyMap())
    var showTranslation by mutableStateOf(false)
    var isTranslating by mutableStateOf(false)
    var showLanguageDialog by mutableStateOf(false)
    var selectedTranslationLang by mutableStateOf("en")

    lateinit var lazyListState: LazyListState
    var isAnimating by mutableStateOf(false)
    var isUserScrolling by mutableStateOf(false)
    val maxSelectionLimit = 5

    var lyricsCache by mutableStateOf<Map<String, LyricsEntity>>(emptyMap())
    var currentLyricsEntity by mutableStateOf<LyricsEntity?>(null)
    var isLoadingLyrics by mutableStateOf(false)
    var refetchTrigger by mutableIntStateOf(0)
    var reloadTrigger by mutableIntStateOf(0)
    var showProviderDialog by mutableStateOf(false)
    var showLyricsMenuDialog by mutableStateOf(false)
    var showSyncDialog by mutableStateOf(false)
    var syncOffsetMs by mutableLongStateOf(0L)
    var showChordDialogInLyrics by mutableStateOf(false)

    var streamingResults by mutableStateOf<List<LyricsResult>>(emptyList())
    var currentProvider by mutableStateOf<String?>(null)

    fun seekToLine(item: LyricsEntry, index: Int) {
        playerConnection.player.seekTo(item.time)
        scope.launch {
            lazyListState.scrollToItem(index)
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            if (itemInfo != null) {
                val vh = lazyListState.layoutInfo.viewportEndOffset - lazyListState.layoutInfo.viewportStartOffset
                val offset = (itemInfo.offset + itemInfo.size / 2) - (lazyListState.layoutInfo.viewportStartOffset + vh / 2)
                if (kotlin.math.abs(offset) > 10) lazyListState.animateScrollBy(offset.toFloat(), tween(1500))
            }
        }
        lastPreviewTime = 0L
    }

    fun selectLine(index: Int) {
        if (selectedIndices.contains(index)) {
            selectedIndices.remove(index)
            if (selectedIndices.isEmpty()) isSelectionModeActive = false
        } else if (selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
        else Toast.makeText(context, context.getString(R.string.max_selection_limit, maxSelectionLimit), Toast.LENGTH_SHORT).show()
    }

    fun toggleSelectionMode(index: Int) {
        if (!isSelectionModeActive) { isSelectionModeActive = true; selectedIndices.add(index) }
        else if (!selectedIndices.contains(index) && selectedIndices.size < maxSelectionLimit) selectedIndices.add(index)
    }

    fun retryLyrics(songId: String?) {
        scope.launch(Dispatchers.IO) {
            songId?.let { id -> runCatching { database.query { delete(LyricsEntity(id, "")) } }; refetchTrigger++ }
        }
    }

    fun fetchLyricsStreaming(songId: String, forceRefresh: Boolean) {
        isLoadingLyrics = true
        streamingResults = emptyList()

        scope.launch(Dispatchers.IO) {
            try {
                val existing = if (!forceRefresh) runCatching { database.getLyrics(songId) }.getOrNull() else null
                if (existing != null && existing.lyrics != LYRICS_NOT_FOUND) {
                    lyricsCache = lyricsCache.toMutableMap().apply { put(songId, existing) }
                    currentLyricsEntity = existing
                    currentProvider = existing.providerName ?: "Saved"
                    isLoadingLyrics = false
                } else {
                    val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.Chenkham.Echofy.di.LyricsHelperEntryPoint::class.java)
                    val lyricsHelper = entryPoint.lyricsHelper()
                    var firstResult = true
                    val allResults = mutableListOf<LyricsResult>()

                    mediaMetadata?.let { metadata ->
                        lyricsHelper.getLyricsStreaming(metadata, forceRefresh) { result ->
                            allResults.add(result)
                            streamingResults = allResults.toList()
                            if (firstResult && result.lyrics != LYRICS_NOT_FOUND) {
                                firstResult = false
                                currentProvider = result.providerName
                                val entity = LyricsEntity(songId, result.lyrics)
                                lyricsCache = lyricsCache.toMutableMap().apply { put(songId, entity) }
                                currentLyricsEntity = entity
                                isLoadingLyrics = false
                                scope.launch(Dispatchers.IO) { runCatching { database.query { upsert(entity) } } }
                            } else if (!firstResult && result.lyrics.startsWith("[") && currentLyricsEntity?.lyrics?.startsWith("[") != true) {
                                currentProvider = result.providerName
                                val entity = LyricsEntity(songId, result.lyrics)
                                lyricsCache = lyricsCache.toMutableMap().apply { put(songId, entity) }
                                currentLyricsEntity = entity
                                scope.launch(Dispatchers.IO) { runCatching { database.query { upsert(entity) } } }
                            }
                        }
                    }
                    if (allResults.isEmpty()) {
                        val errorEntity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                        lyricsCache = lyricsCache.toMutableMap().apply { put(songId, errorEntity) }
                        currentLyricsEntity = errorEntity
                    }
                }
            } catch (e: Exception) {
                val errorEntity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                lyricsCache = lyricsCache.toMutableMap().apply { put(songId, errorEntity) }
                currentLyricsEntity = errorEntity
            } finally {
                isLoadingLyrics = false
            }
        }
    }
}

@Composable
fun rememberLyricsState(
    currentSongId: String?,
    mediaMetadata: MediaMetadata?,
): LyricsState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: error("PlayerConnection missing")

    val state = remember(currentSongId) {
        LyricsState(context, scope, database, playerConnection, mediaMetadata, currentSongId)
    }
    state.lazyListState = rememberLazyListState()
    return state
}
