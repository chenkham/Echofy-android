package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.lyrics.LyricsUtils.findCurrentLineIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LyricsScrollHandler(
    state: LyricsState,
    lines: List<LyricsEntry>,
    lyrics: String?,
    isSynced: Boolean,
    scrollLyrics: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (state.lazyListState.layoutInfo.visibleItemsInfo.any { it.index == state.currentLineIndex }) state.initialScrollDone = false
                state.isAppMinimized = true
            } else if (event == Lifecycle.Event.ON_START) state.isAppMinimized = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lyrics, state.syncOffsetMs) {
        if (lyrics.isNullOrEmpty() || !lyrics.startsWith("[")) { state.currentLineIndex = -1; return@LaunchedEffect }
        while (isActive) {
            delay(50)
            state.isSeeking = false
            state.currentLineIndex = findCurrentLineIndex(lines, state.playerConnection.player.currentPosition, state.syncOffsetMs)
        }
    }

    LaunchedEffect(state.currentLineIndex, state.lastPreviewTime, state.initialScrollDone) {
        if (!isSynced || state.isUserScrolling) return@LaunchedEffect
        suspend fun smoothScroll(targetIndex: Int, dur: Int = 1500) {
            if (state.isAnimating) return
            state.isAnimating = true
            try {
                val itemInfo = state.lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                if (itemInfo != null) {
                    val vh = state.lazyListState.layoutInfo.viewportEndOffset - state.lazyListState.layoutInfo.viewportStartOffset
                    val offset = (itemInfo.offset + itemInfo.size / 2) - (state.lazyListState.layoutInfo.viewportStartOffset + vh / 2)
                    if (kotlin.math.abs(offset) > 10) state.lazyListState.animateScrollBy(offset.toFloat(), tween(dur))
                } else state.lazyListState.scrollToItem(targetIndex)
            } finally { state.isAnimating = false }
        }
        if ((state.currentLineIndex == 0 && state.shouldScrollToFirstLine) || !state.initialScrollDone) {
            state.shouldScrollToFirstLine = false
            smoothScroll(kotlin.math.max(0, state.currentLineIndex), 800)
            if (!state.isAppMinimized) state.initialScrollDone = true
        } else if (state.currentLineIndex != -1) {
            state.deferredCurrentLineIndex = state.currentLineIndex
            if ((state.lastPreviewTime == 0L || state.currentLineIndex != state.previousLineIndex) && scrollLyrics && state.currentLineIndex != state.previousLineIndex) {
                smoothScroll(state.currentLineIndex, 1500)
            }
        }
        if (state.currentLineIndex > 0) state.shouldScrollToFirstLine = true
        state.previousLineIndex = state.currentLineIndex
    }
}
