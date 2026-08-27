package com.Chenkham.Echofy.utils

import androidx.media3.common.MediaItem
import kotlin.random.Random

object SmartShuffleManager {

    /**
     * Generates a smart shuffled order where items by the same artist/album
     * are spaced apart to prevent annoying repetition clumping.
     */
    fun generateSmartShuffleOrder(
        items: List<MediaItem>,
        currentIndex: Int,
        randomSeed: Long = System.currentTimeMillis()
    ): IntArray {
        if (items.isEmpty()) return IntArray(0)
        if (items.size <= 2) return IntArray(items.size) { it }

        val rng = Random(randomSeed)
        val indexedItems = items.mapIndexed { index, mediaItem ->
            val artist = mediaItem.mediaMetadata.artist?.toString()?.lowercase() ?: ""
            val album = mediaItem.mediaMetadata.albumTitle?.toString()?.lowercase() ?: ""
            Triple(index, artist, album)
        }.toMutableList()

        // Keep current item at position 0
        val currentItem = indexedItems.removeAt(currentIndex.coerceIn(0, indexedItems.size - 1))
        indexedItems.shuffle(rng)

        val result = mutableListOf(currentItem)
        val remaining = indexedItems.toMutableList()

        while (remaining.isNotEmpty()) {
            val lastArtist = result.last().second
            val lastAlbum = result.last().third

            // Find candidates that don't match the last artist/album
            val candidateIndex = remaining.indexOfFirst {
                (lastArtist.isBlank() || it.second != lastArtist) &&
                (lastAlbum.isBlank() || it.third != lastAlbum)
            }

            if (candidateIndex != -1) {
                result.add(remaining.removeAt(candidateIndex))
            } else {
                // If all remaining candidates have matching artist, pick the furthest one
                result.add(remaining.removeAt(0))
            }
        }

        return result.map { it.first }.toIntArray()
    }
}
