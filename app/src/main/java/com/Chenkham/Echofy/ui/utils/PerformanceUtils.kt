package com.Chenkham.Echofy.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * PERFORMANCE: Professional-grade utility functions for Spotify/YouTube Music level smoothness
 *
 * Key Guidelines for commercial-level UI performance:
 * 1. Use @Stable/@Immutable annotations for data classes passed to composables
 * 2. Use remember {} with minimal keys for expensive computations
 * 3. Use derivedStateOf for computed values that depend on other state
 * 4. NEVER create lambdas inside composables without remember {}
 * 5. Use key() for list items to enable efficient diffing
 * 6. Batch state updates to reduce recomposition count
 * 7. Use LaunchedEffect with proper keys to avoid re-triggering
 * 8. Hoist state to parent composables when possible
 * 9. Use debounced scroll listeners to avoid excessive updates during scrolling
 * 10. Prefetch data before it's needed using scroll position prediction
 */

/**
 * Stable wrapper for callbacks to prevent unnecessary recomposition.
 * Use this when passing lambdas to child composables.
 *
 * Usage:
 * ```kotlin
 * val onClick = rememberStableCallback {
 *     // Your callback logic
 * }
 * ```
 */
@Stable
class StableCallback<T>(private val callback: T) {
    fun get(): T = callback

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StableCallback<*>) return false
        return callback == other.callback
    }

    override fun hashCode(): Int = callback.hashCode()
}

/**
 * Immutable data class for playback state to minimize recomposition in lists.
 * All list items can compare against this single object instead of multiple state values.
 *
 * PERFORMANCE: Using @Immutable allows Compose to skip recomposition when the same instance is passed
 */
@Immutable
data class ImmutablePlaybackInfo(
    val currentMediaId: String?,
    val currentAlbumId: String?,
    val isPlaying: Boolean
) {
    companion object {
        val EMPTY = ImmutablePlaybackInfo(null, null, false)
    }

    fun isActiveItem(itemId: String?): Boolean =
        itemId != null && (itemId == currentMediaId || itemId == currentAlbumId)
}

/**
 * Immutable library state for efficient list rendering.
 * Bundles multiple Set states into one object to reduce remember{} key count.
 *
 * PERFORMANCE: Reduces recomposition triggers from 3 (one per set) to 1 (the entire object)
 */
@Immutable
data class ImmutableLibraryInfo(
    val likedSongIds: Set<String>,
    val librarySongIds: Set<String>,
    val bookmarkedAlbumIds: Set<String>
) {
    companion object {
        val EMPTY = ImmutableLibraryInfo(emptySet(), emptySet(), emptySet())
    }

    fun isLiked(songId: String): Boolean = songId in likedSongIds
    fun inLibrary(songId: String): Boolean = songId in librarySongIds
    fun isBookmarked(albumId: String): Boolean = albumId in bookmarkedAlbumIds
}

/**
 * Remember a stable callback wrapper to prevent recomposition
 */
@Composable
fun <T> rememberStableCallback(callback: T): StableCallback<T> {
    return remember { StableCallback(callback) }
}

/**
 * Stable holder for a Set to prevent recomposition when the contents don't change
 */
@Stable
data class StableSet<T>(val set: Set<T>) {
    operator fun contains(element: T): Boolean = element in set
}

/**
 * Convert a Set to a StableSet for use in Composables
 */
fun <T> Set<T>.toStable(): StableSet<T> = StableSet(this)

/**
 * PERFORMANCE: Debounced scroll listener for LazyList
 * Prevents excessive updates during fast scrolling by debouncing scroll events.
 * Use this instead of direct scroll state observation for scroll-dependent operations.
 *
 * @param listState The LazyListState to monitor
 * @param debounceMs Debounce interval in milliseconds (default 100ms)
 * @param onScrollPositionChanged Callback with first visible item index and offset
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberDebouncedScrollPosition(
    listState: LazyListState,
    debounceMs: Long = 100L,
    onScrollPositionChanged: (firstVisibleIndex: Int, offset: Int) -> Unit
) {
    val currentCallback by rememberUpdatedState(onScrollPositionChanged)

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
        .debounce(debounceMs)
        .distinctUntilChanged()
        .collect { (index, offset) ->
            currentCallback(index, offset)
        }
    }
}

/**
 * PERFORMANCE: Debounced scroll listener for LazyGrid
 */
@OptIn(FlowPreview::class)
@Composable
fun rememberDebouncedGridScrollPosition(
    gridState: LazyGridState,
    debounceMs: Long = 100L,
    onScrollPositionChanged: (firstVisibleIndex: Int, offset: Int) -> Unit
) {
    val currentCallback by rememberUpdatedState(onScrollPositionChanged)

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }
        .debounce(debounceMs)
        .distinctUntilChanged()
        .collect { (index, offset) ->
            currentCallback(index, offset)
        }
    }
}

/**
 * PERFORMANCE: Check if user is scrolling - useful for deferring expensive operations
 * Returns true when the list is being actively scrolled.
 */
@Composable
fun rememberIsScrolling(listState: LazyListState): Boolean {
    val isScrollInProgress by remember {
        derivedStateOf { listState.isScrollInProgress }
    }
    return isScrollInProgress
}

/**
 * PERFORMANCE: Check if grid is scrolling
 */
@Composable
fun rememberIsGridScrolling(gridState: LazyGridState): Boolean {
    val isScrollInProgress by remember {
        derivedStateOf { gridState.isScrollInProgress }
    }
    return isScrollInProgress
}

/**
 * PERFORMANCE: Deferred content - only renders when not scrolling
 * Use this for expensive content that doesn't need to render during scroll.
 * Shows placeholder during scroll, actual content when stopped.
 *
 * @param isScrolling Whether the parent list is scrolling
 * @param placeholder Content to show while scrolling
 * @param content Actual content to show when not scrolling
 */
@Composable
fun DeferredContent(
    isScrolling: Boolean,
    placeholder: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    // Use a delayed state change to prevent flickering
    var showContent by remember { mutableStateOf(!isScrolling) }

    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            // Immediately hide content when scrolling starts
            showContent = false
        } else {
            // Delay showing content to ensure scroll has fully stopped
            kotlinx.coroutines.delay(50)
            showContent = true
        }
    }

    if (showContent) {
        content()
    } else {
        placeholder()
    }
}

/**
 * PERFORMANCE: Calculate visible item range with buffer for prefetching
 * Returns a range of indices that should have their data ready.
 *
 * @param listState The LazyListState to analyze
 * @param bufferSize Number of items to prefetch before/after visible range
 */
@Composable
fun rememberVisibleItemRange(
    listState: LazyListState,
    bufferSize: Int = 5
): IntRange {
    return remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount

            val start = (firstVisible - bufferSize).coerceAtLeast(0)
            val end = (lastVisible + bufferSize).coerceAtMost(totalItems - 1)
            start..end
        }
    }.value
}

/**
 * PERFORMANCE: Stable empty lambda for onClick handlers
 * Use this instead of {} to prevent recomposition
 */
val NoOpClick: () -> Unit = {}

/**
 * PERFORMANCE: Stable empty lambda with single parameter
 */
fun <T> noOpSingleParam(): (T) -> Unit = { _ -> }

/**
 * Immutable wrapper for list data to prevent unnecessary recomposition
 */
@Immutable
data class ImmutableList<T>(val items: List<T>) {
    val size: Int get() = items.size
    fun isEmpty(): Boolean = items.isEmpty()
    fun isNotEmpty(): Boolean = items.isNotEmpty()
    operator fun get(index: Int): T = items[index]

    companion object {
        private val EMPTY = ImmutableList<Any>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): ImmutableList<T> = EMPTY as ImmutableList<T>
    }
}

/**
 * Convert a list to ImmutableList for use in Composables
 */
fun <T> List<T>.toImmutableList(): ImmutableList<T> = ImmutableList(this)

