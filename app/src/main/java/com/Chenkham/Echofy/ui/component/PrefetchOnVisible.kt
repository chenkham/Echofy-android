package com.Chenkham.Echofy.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.Chenkham.Echofy.LocalPlayerConnection

/**
 * Prefetches the stream URL for a song when this composable enters composition.
 * This enables YouTube Music-like instant playback by resolving the URL
 * before the user taps the item.
 *
 * Usage: Place inside any composable that displays a song:
 * ```
 * SongListItem(...) {
 *     PrefetchOnVisible(mediaId = song.id)
 *     // ...
 * }
 * ```
 *
 * Or use in a LazyColumn item:
 * ```
 * item {
 *     PrefetchOnVisible(mediaId = song.id)
 *     SongListItem(...)
 * }
 * ```
 */
@Composable
fun PrefetchOnVisible(mediaId: String) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    LaunchedEffect(mediaId) {
        playerConnection.prefetch(mediaId)
    }
}

/**
 * Prefetches multiple songs at once. Useful for grids or lists
 * where multiple items are visible simultaneously.
 */
@Composable
fun PrefetchOnVisible(mediaIds: List<String>) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    LaunchedEffect(mediaIds) {
        mediaIds.forEach { mediaId ->
            playerConnection.prefetch(mediaId)
        }
    }
}
