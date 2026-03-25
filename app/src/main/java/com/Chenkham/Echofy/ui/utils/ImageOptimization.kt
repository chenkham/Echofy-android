package com.Chenkham.Echofy.ui.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

/**
 * PERFORMANCE: Professional-grade image loading utilities for Spotify/YT Music level smoothness
 *
 * Key optimizations:
 * 1. Aggressive memory and disk caching
 * 2. Crossfade disabled for instant display from cache
 * 3. Hardware bitmaps for GPU rendering
 * 4. Size hints for efficient decoding
 * 5. Stable cache keys for maximum cache hits
 * 6. Request deduplication through Coil's built-in mechanisms
 */
@Stable
object ImageOptimization {

    // Pre-computed cache key prefixes for different contexts
    private const val THUMBNAIL_PREFIX = "thumb_"
    private const val PLAYER_PREFIX = "player_"
    private const val LIST_PREFIX = "list_"
    private const val GRID_PREFIX = "grid_"

    /**
     * Creates an optimized ImageRequest for thumbnail images in lists/grids
     * Designed for fast scrolling performance - NO crossfade for instant display
     */
    @Composable
    fun rememberOptimizedThumbnailRequest(
        url: String?,
        size: Int = ThumbnailSizes.MEDIUM, // Default thumbnail size
        crossfade: Boolean = false // Disable crossfade for instant display
    ): ImageRequest {
        val context = LocalContext.current
        return remember(url, size) {
            createOptimizedThumbnailRequest(context, url, size, crossfade)
        }
    }

    /**
     * Non-composable version for use in callbacks/lambdas
     */
    fun createOptimizedThumbnailRequest(
        context: Context,
        url: String?,
        size: Int = ThumbnailSizes.MEDIUM,
        crossfade: Boolean = false
    ): ImageRequest {
        val cacheKey = "$THUMBNAIL_PREFIX${size}_$url"
        return ImageRequest.Builder(context)
            .data(url)
            .size(size, size)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfade)
            .allowHardware(true)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build()
    }

    /**
     * Creates an optimized ImageRequest for list items (smaller size, faster loading)
     */
    @Composable
    fun rememberOptimizedListRequest(
        url: String?,
        crossfade: Boolean = false
    ): ImageRequest {
        val context = LocalContext.current
        return remember(url) {
            createOptimizedListRequest(context, url, crossfade)
        }
    }

    fun createOptimizedListRequest(
        context: Context,
        url: String?,
        crossfade: Boolean = false
    ): ImageRequest {
        val cacheKey = "$LIST_PREFIX$url"
        return ImageRequest.Builder(context)
            .data(url)
            .size(ThumbnailSizes.SMALL, ThumbnailSizes.SMALL)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfade)
            .allowHardware(true)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build()
    }

    /**
     * Creates an optimized ImageRequest for grid items
     */
    @Composable
    fun rememberOptimizedGridRequest(
        url: String?,
        crossfade: Boolean = false
    ): ImageRequest {
        val context = LocalContext.current
        return remember(url) {
            createOptimizedGridRequest(context, url, crossfade)
        }
    }

    fun createOptimizedGridRequest(
        context: Context,
        url: String?,
        crossfade: Boolean = false
    ): ImageRequest {
        val cacheKey = "$GRID_PREFIX$url"
        return ImageRequest.Builder(context)
            .data(url)
            .size(ThumbnailSizes.LARGE, ThumbnailSizes.LARGE)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfade)
            .allowHardware(true)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build()
    }

    /**
     * Creates an optimized ImageRequest for album art in player
     * Higher quality with optional crossfade for smooth transitions
     */
    @Composable
    fun rememberOptimizedPlayerArtRequest(
        url: String?,
        crossfade: Boolean = true
    ): ImageRequest {
        val context = LocalContext.current
        return remember(url) {
            createOptimizedPlayerArtRequest(context, url, crossfade)
        }
    }

    fun createOptimizedPlayerArtRequest(
        context: Context,
        url: String?,
        crossfade: Boolean = true
    ): ImageRequest {
        val cacheKey = "$PLAYER_PREFIX$url"
        return ImageRequest.Builder(context)
            .data(url)
            .size(Size.ORIGINAL) // Full resolution for player
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(if (crossfade) 150 else 0) // Slightly faster crossfade
            .allowHardware(true)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .build()
    }

    /**
     * Creates an optimized ImageRequest builder for any image
     * Use this when you need more control
     */
    fun createOptimizedRequestBuilder(
        context: Context,
        url: String?
    ): ImageRequest.Builder {
        return ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true)
    }
}

/**
 * Standard thumbnail sizes used throughout the app
 * Using consistent sizes improves cache hit rates significantly
 *
 * IMPORTANT: Always use these constants for image loading to maximize cache efficiency
 */
@Stable
object ThumbnailSizes {
    const val TINY = 64    // For very small icons
    const val SMALL = 120  // For small list items (ListThumbnailSize)
    const val MEDIUM = 256 // For medium items
    const val LARGE = 544  // For grid items (GridThumbnailHeight)
    const val PLAYER = 720 // For player artwork
    const val FULL = 1080  // For full-screen images
}

/**
 * PERFORMANCE: Optimized AsyncImage wrapper with all caching best practices
 * Use this instead of raw AsyncImage for consistent performance
 */
@Composable
fun OptimizedAsyncImage(
    url: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Int = ThumbnailSizes.MEDIUM,
    crossfade: Boolean = false, // Default: no crossfade for instant display
    placeholderPainter: Painter? = null,
    errorPainter: Painter? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val request = remember(url, size) {
        ImageOptimization.createOptimizedThumbnailRequest(
            context = context,
            url = url,
            size = size,
            crossfade = crossfade
        )
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = placeholderPainter,
        error = errorPainter,
        contentScale = contentScale
    )
}

/**
 * PERFORMANCE: Pre-warm the image cache for a list of URLs
 * Call this when you know images will be needed soon (e.g., on screen entry)
 */
suspend fun prewarmImageCache(
    context: Context,
    imageLoader: coil.ImageLoader,
    urls: List<String?>,
    size: Int = ThumbnailSizes.MEDIUM
) {
    urls.filterNotNull().take(10).forEach { url ->
        try {
            val request = ImageOptimization.createOptimizedThumbnailRequest(context, url, size)
            imageLoader.enqueue(request)
        } catch (e: Exception) {
            // Silently fail - prewarming is best-effort
        }
    }
}

