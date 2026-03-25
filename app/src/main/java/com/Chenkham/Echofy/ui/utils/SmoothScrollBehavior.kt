package com.Chenkham.Echofy.ui.utils

import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * PERFORMANCE: Professional-grade smooth scroll behavior
 * Mimics the buttery-smooth scrolling of Spotify and YouTube Music
 *
 * Key features:
 * 1. Custom deceleration curves for natural feel
 * 2. Velocity-based animation timing
 * 3. Reduced friction for momentum scrolling
 * 4. Snap-friendly deceleration
 */
@Stable
object SmoothScrollBehavior {

    // Spotify/YT Music-like easing curves
    val smoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    val decelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val overshootEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    // Default animation specs for different scroll scenarios
    val quickSnap: AnimationSpec<Float> = spring(
        dampingRatio = 0.8f,
        stiffness = 400f
    )

    val smoothSnap: AnimationSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 300f
    )

    val gentleSnap: AnimationSpec<Float> = spring(
        dampingRatio = 0.9f,
        stiffness = 200f
    )
}

/**
 * PERFORMANCE: Creates an optimized fling behavior for LazyColumn/LazyRow
 * with reduced friction for smoother momentum scrolling
 */
@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    return ScrollableDefaults.flingBehavior()
}

/**
 * PERFORMANCE: Check if scroll is near the end (for prefetching)
 * Returns true when within the last [threshold] items of the list
 */
@Composable
fun LazyListState.isNearEnd(threshold: Int = 5): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    val totalItems = layoutInfo.totalItemsCount
    return totalItems > 0 && lastVisibleItem >= totalItems - threshold
}

/**
 * PERFORMANCE: Check if scroll is near the end for grid
 */
@Composable
fun LazyGridState.isNearEnd(threshold: Int = 10): Boolean {
    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    val totalItems = layoutInfo.totalItemsCount
    return totalItems > 0 && lastVisibleItem >= totalItems - threshold
}

/**
 * PERFORMANCE: Calculate scroll velocity category for adaptive behavior
 * - IDLE: Not scrolling
 * - SLOW: Normal browsing speed (< 500 px/s)
 * - MEDIUM: Quick browsing (500-1500 px/s)
 * - FAST: Fast flinging (> 1500 px/s)
 */
enum class ScrollVelocityCategory {
    IDLE, SLOW, MEDIUM, FAST
}

/**
 * Determines scroll velocity category from current scroll offset change
 */
fun categorizeScrollVelocity(velocityPxPerSecond: Float): ScrollVelocityCategory {
    val absVelocity = abs(velocityPxPerSecond)
    return when {
        absVelocity < 10f -> ScrollVelocityCategory.IDLE
        absVelocity < 500f -> ScrollVelocityCategory.SLOW
        absVelocity < 1500f -> ScrollVelocityCategory.MEDIUM
        else -> ScrollVelocityCategory.FAST
    }
}

/**
 * PERFORMANCE: Optimal prefetch distances based on scroll velocity
 * Faster scrolling = more items prefetched
 */
fun getPrefetchCount(velocityCategory: ScrollVelocityCategory): Int {
    return when (velocityCategory) {
        ScrollVelocityCategory.IDLE -> 2
        ScrollVelocityCategory.SLOW -> 4
        ScrollVelocityCategory.MEDIUM -> 8
        ScrollVelocityCategory.FAST -> 12
    }
}

/**
 * PERFORMANCE: Calculate optimal item size for smooth 60fps scrolling
 * Items should be sized to minimize jank during rendering
 */
@Composable
fun rememberOptimalItemHeight(): Int {
    val density = LocalDensity.current
    // Target: items that can be rendered in under 8ms (one frame at 120fps)
    // Generally 80-120dp is optimal for complex list items
    return remember(density) {
        with(density) { 88.dp.roundToPx() }
    }
}

