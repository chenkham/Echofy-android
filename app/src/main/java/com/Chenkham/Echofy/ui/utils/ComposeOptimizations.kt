package com.Chenkham.Echofy.ui.utils

import android.os.Build
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * PERFORMANCE: Professional-grade Compose optimizations for commercial app quality
 *
 * These utilities help achieve Spotify/YouTube Music level smoothness by:
 * 1. Reducing unnecessary recompositions
 * 2. Optimizing scroll performance
 * 3. Deferring expensive operations
 * 4. Caching computed values efficiently
 */

/**
 * Deferred rendering during scroll - prevents jank by deferring complex content
 * when user is actively scrolling the list.
 *
 * Usage:
 * ```
 * val deferredState = rememberDeferredScrollContent(listState)
 *
 * // In your item:
 * if (deferredState.shouldRenderFullContent) {
 *     ExpensiveContent()
 * } else {
 *     SimplePlaceholder()
 * }
 * ```
 */
@Stable
class DeferredScrollState(
    initialShouldRender: Boolean = true
) {
    var shouldRenderFullContent by mutableStateOf(initialShouldRender)
        internal set
}

@OptIn(FlowPreview::class)
@Composable
fun rememberDeferredScrollContent(
    listState: LazyListState,
    deferDelayMs: Long = 150L
): DeferredScrollState {
    val state = remember { DeferredScrollState() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { isScrolling ->
                if (isScrolling) {
                    state.shouldRenderFullContent = false
                } else {
                    delay(deferDelayMs)
                    state.shouldRenderFullContent = true
                }
            }
    }

    return state
}

@OptIn(FlowPreview::class)
@Composable
fun rememberDeferredGridScrollContent(
    gridState: LazyGridState,
    deferDelayMs: Long = 150L
): DeferredScrollState {
    val state = remember { DeferredScrollState() }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { isScrolling ->
                if (isScrolling) {
                    state.shouldRenderFullContent = false
                } else {
                    delay(deferDelayMs)
                    state.shouldRenderFullContent = true
                }
            }
    }

    return state
}

/**
 * Optimized scroll-to-top with smart animation
 * For long lists, jumps closer to top first to avoid long scroll animations
 */
suspend fun LazyListState.smoothScrollToTop(
    jumpThreshold: Int = 15,
    jumpToIndex: Int = 5
) {
    if (firstVisibleItemIndex > jumpThreshold) {
        scrollToItem(jumpToIndex)
    }
    animateScrollToItem(0)
}

suspend fun LazyGridState.smoothScrollToTop(
    jumpThreshold: Int = 20,
    jumpToIndex: Int = 8
) {
    if (firstVisibleItemIndex > jumpThreshold) {
        scrollToItem(jumpToIndex)
    }
    animateScrollToItem(0)
}

/**
 * Check if list is scrolled past first item - useful for FAB visibility
 */
@Composable
fun rememberIsScrolledPastFirst(listState: LazyListState): Boolean {
    return remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }.value
}

/**
 * Debounced callback for scroll position changes
 * Prevents excessive updates during fast scrolling
 */
@OptIn(FlowPreview::class)
@Composable
fun ObserveScrollPosition(
    listState: LazyListState,
    debounceMs: Long = 100L,
    onPositionChange: (firstVisibleIndex: Int, offset: Int) -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
        .debounce(debounceMs)
        .distinctUntilChanged()
        .collect { (index, offset) ->
            onPositionChange(index, offset)
        }
    }
}

/**
 * Stable callback wrapper to prevent recomposition from lambda changes
 */
@Stable
class StableOnClick(private val onClick: () -> Unit) {
    fun invoke() = onClick()
}

@Composable
fun rememberStableOnClick(onClick: () -> Unit): StableOnClick {
    return remember { StableOnClick(onClick) }
}

/**
 * Performance timing utility for debugging slow composables
 */
@Composable
inline fun MeasureCompositionTime(
    tag: String,
    crossinline content: @Composable () -> Unit
) {
    val startTime = remember { System.nanoTime() }
    content()
    DisposableEffect(Unit) {
        val endTime = System.nanoTime()
        val durationMs = (endTime - startTime) / 1_000_000.0
        if (durationMs > 16.0) { // Log if takes more than one frame
            android.util.Log.w("PerfMeasure", "$tag composition took ${durationMs}ms")
        }
        onDispose { }
    }
}

/**
 * Batch state updates to reduce recomposition count
 * Use when you need to update multiple states at once
 */
@Stable
class BatchStateUpdater {
    private val updates = mutableListOf<() -> Unit>()

    fun queue(update: () -> Unit) {
        updates.add(update)
    }

    fun execute() {
        // Execute all updates in a single composition pass
        updates.forEach { it() }
        updates.clear()
    }
}

@Composable
fun rememberBatchStateUpdater(): BatchStateUpdater {
    return remember { BatchStateUpdater() }
}

/**
 * Utility to check device capability for advanced effects
 */
object DeviceCapabilities {
    val supportsBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val supportsHardwareAcceleration: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val isHighEndDevice: Boolean
        get() = Runtime.getRuntime().maxMemory() > 256 * 1024 * 1024 // > 256MB heap
}

/**
 * Conditional modifier application based on condition
 * Useful for performance-sensitive conditional styling
 */
inline fun Modifier.thenIf(
    condition: Boolean,
    crossinline block: Modifier.() -> Modifier
): Modifier = if (condition) block() else this

/**
 * Apply modifier only on high-end devices
 * Useful for effects that may cause jank on lower-end devices
 */
inline fun Modifier.ifHighEnd(
    crossinline block: Modifier.() -> Modifier
): Modifier = if (DeviceCapabilities.isHighEndDevice) block() else this

