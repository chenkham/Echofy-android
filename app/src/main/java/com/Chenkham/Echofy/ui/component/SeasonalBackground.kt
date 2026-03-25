package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.Chenkham.Echofy.constants.SeasonalWallpaper
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Snowflake data class for the Winter theme
 */
private data class Snowflake(
    var x: Float,           // X position (0-1 normalized)
    var y: Float,           // Y position (0-1 normalized)
    val size: Float,        // Size in dp
    val speed: Float,       // Fall speed multiplier
    val wobbleFreq: Float,  // Horizontal wobble frequency
    val wobbleAmp: Float,   // Horizontal wobble amplitude
    val alpha: Float,       // Transparency (0-1)
    val rotation: Float,    // Initial rotation
    var phase: Float = 0f   // Animation phase
)

/**
 * Creates a list of snowflakes with random properties
 */
private fun createSnowflakes(count: Int): List<Snowflake> {
    return List(count) {
        Snowflake(
            x = Random.nextFloat(),
            y = Random.nextFloat() * 2f - 1f, // Start some above screen
            size = Random.nextFloat() * 4f + 2f, // 2-6 dp
            speed = Random.nextFloat() * 0.3f + 0.1f, // Variable speeds
            wobbleFreq = Random.nextFloat() * 2f + 1f,
            wobbleAmp = Random.nextFloat() * 0.02f + 0.005f,
            alpha = Random.nextFloat() * 0.5f + 0.5f, // 0.5-1.0
            rotation = Random.nextFloat() * 360f,
            phase = Random.nextFloat() * 6.28f
        )
    }
}

/**
 * A beautiful animated snow background effect
 * Based on realistic snow physics with gentle falling and horizontal drift
 */
@Composable
fun SeasonalBackground(
    season: SeasonalWallpaper,
    modifier: Modifier = Modifier
) {
    // Only show for Winter season
    if (season != SeasonalWallpaper.WINTER) {
        return
    }
    
    val density = LocalDensity.current
    
    // Create snowflakes once
    val snowflakes = remember { createSnowflakes(80) }
    
    // Frame time for animation - this triggers recomposition
    var frameTimeNanos by remember { mutableLongStateOf(0L) }
    
    // Continuous animation loop
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            val currentTime = withFrameNanos { it }
            val deltaSeconds = (currentTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = currentTime
            
            // Update snowflake positions
            snowflakes.forEach { flake ->
                // Update phase for wobble
                flake.phase += deltaSeconds * flake.wobbleFreq * 2f
                
                // Move down
                flake.y += flake.speed * deltaSeconds * 0.5f
                
                // Horizontal wobble
                flake.x += sin(flake.phase) * flake.wobbleAmp
                
                // Reset if off screen
                if (flake.y > 1.1f) {
                    flake.y = -0.1f
                    flake.x = Random.nextFloat()
                }
                
                // Wrap horizontally
                if (flake.x < 0f) flake.x = 1f
                if (flake.x > 1f) flake.x = 0f
            }
            
            // Trigger recomposition by updating state
            frameTimeNanos = currentTime
        }
    }
    
    // Draw the snowflakes
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        // Use frameTimeNanos to ensure recomposition (compiler won't optimize it away)
        @Suppress("UNUSED_EXPRESSION")
        frameTimeNanos
        
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Draw gradient background overlay (subtle dark gradient for winter feel)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0x15001530), // Very subtle dark blue at top
                    Color.Transparent
                )
            )
        )
        
        // Draw each snowflake
        snowflakes.forEach { flake ->
            val x = flake.x * canvasWidth
            val y = flake.y * canvasHeight
            val radius = flake.size * density.density
            
            // Main snowflake (white circle with glow)
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha * 0.3f),
                radius = radius * 2f, // Outer glow
                center = Offset(x, y)
            )
            
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha * 0.6f),
                radius = radius * 1.3f, // Middle glow
                center = Offset(x, y)
            )
            
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha),
                radius = radius, // Core
                center = Offset(x, y)
            )
        }
    }
}
