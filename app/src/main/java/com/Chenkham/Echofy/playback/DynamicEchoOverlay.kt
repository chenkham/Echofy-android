package com.Chenkham.Echofy.playback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.constants.DynamicEchoStyle
import kotlin.math.PI
import kotlin.math.sin

// Neon color palette - vibrant and colorful
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonPink = Color(0xFFFF1493)
private val NeonPurple = Color(0xFF9400D3)
private val NeonBlue = Color(0xFF4169E1)
private val NeonGreen = Color(0xFF00FF7F)
private val NeonOrange = Color(0xFFFF4500)
private val NeonYellow = Color(0xFFFFD700)
private val NeonRed = Color(0xFFFF0044)

/**
 * Dynamic Echo edge visualization overlay with neon glow effects.
 * Wave style: flowing neon waves on edges
 * Equalizer style: 4-edge glowing border with gradient
 */
@Composable
fun DynamicEchoOverlay(
    amplitudes: FloatArray,
    isPlaying: Boolean,
    style: DynamicEchoStyle,
    isDarkTheme: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) return
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    // Edge thickness and glow - THICKER lines
    val edgeThickness = with(density) { 5.dp.toPx() }
    val glowThickness = with(density) { 18.dp.toPx() }
    val maxAmplitude = with(density) { 40.dp.toPx() }
    
    // Animated rainbow gradient colors
    val infiniteTransition = rememberInfiniteTransition(label = "echoColors")
    
    val colorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorShift"
    )
    
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Dynamic gradient colors that shift over time
    val gradientColors = remember(colorShift) {
        listOf(
            shiftColor(NeonCyan, colorShift),
            shiftColor(NeonMagenta, colorShift + 0.15f),
            shiftColor(NeonPink, colorShift + 0.3f),
            shiftColor(NeonPurple, colorShift + 0.45f),
            shiftColor(NeonBlue, colorShift + 0.6f),
            shiftColor(NeonGreen, colorShift + 0.75f),
            shiftColor(NeonCyan, colorShift + 0.9f)
        )
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (style) {
                DynamicEchoStyle.WAVE -> {
                    // Wave style - neon waves on top, left, right edges
                    drawNeonWaveEdges(
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        amplitudes = amplitudes,
                        wavePhase = wavePhase,
                        pulseScale = pulseScale,
                        maxAmplitude = maxAmplitude,
                        edgeThickness = edgeThickness,
                        glowThickness = glowThickness,
                        gradientColors = gradientColors
                    )
                }
                DynamicEchoStyle.GLOW -> {
                    // Glow style - 4-edge glowing border
                    drawGlowingBorder(
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        amplitudes = amplitudes,
                        pulseScale = pulseScale,
                        gradientColors = gradientColors
                    )
                }
            }
        }
    }
}

/**
 * Shift color hue for rainbow effect
 */
private fun shiftColor(color: Color, shift: Float): Color {
    val normalizedShift = shift % 1f
    val colors = listOf(NeonCyan, NeonMagenta, NeonPink, NeonPurple, NeonBlue, NeonGreen, NeonOrange, NeonYellow)
    val index = (normalizedShift * colors.size).toInt() % colors.size
    val nextIndex = (index + 1) % colors.size
    val blend = (normalizedShift * colors.size) % 1f
    
    return Color(
        red = colors[index].red * (1 - blend) + colors[nextIndex].red * blend,
        green = colors[index].green * (1 - blend) + colors[nextIndex].green * blend,
        blue = colors[index].blue * (1 - blend) + colors[nextIndex].blue * blend,
        alpha = 1f
    )
}

/**
 * Draw stunning neon wave patterns on edges (TOP, LEFT, RIGHT)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNeonWaveEdges(
    screenWidth: Float,
    screenHeight: Float,
    amplitudes: FloatArray,
    wavePhase: Float,
    pulseScale: Float,
    maxAmplitude: Float,
    edgeThickness: Float,
    glowThickness: Float,
    gradientColors: List<Color>
) {
    val avgAmplitude = amplitudes.average().toFloat() * pulseScale
    val waveHeight = maxAmplitude * avgAmplitude
    
    // === TOP EDGE - Flowing neon wave ===
    val topPath = Path().apply {
        moveTo(0f, 0f)
        var x = 0f
        val step = 3f
        while (x <= screenWidth) {
            val wave1 = sin(x * 0.025f + wavePhase) * waveHeight * 0.6f
            val wave2 = sin(x * 0.04f + wavePhase * 1.5f) * waveHeight * 0.3f
            val wave3 = sin(x * 0.06f + wavePhase * 0.7f) * waveHeight * 0.1f
            val y = (wave1 + wave2 + wave3).coerceAtLeast(0f)
            lineTo(x, y)
            x += step
        }
    }
    
    // Glow layer (wider, more transparent)
    drawPath(
        path = topPath,
        brush = Brush.horizontalGradient(
            colors = gradientColors.map { it.copy(alpha = 0.4f) }
        ),
        style = Stroke(width = glowThickness, cap = StrokeCap.Round)
    )
    
    // Core neon line (bright) - THICKER
    drawPath(
        path = topPath,
        brush = Brush.horizontalGradient(colors = gradientColors),
        style = Stroke(width = edgeThickness, cap = StrokeCap.Round)
    )
    
    // === LEFT EDGE - Vertical neon wave ===
    val leftPath = Path().apply {
        moveTo(0f, 0f)
        var y = 0f
        val step = 4f
        while (y <= screenHeight) {
            val idx = ((y / screenHeight) * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
            val amp = amplitudes[idx] * pulseScale
            
            val wave1 = sin(y * 0.02f + wavePhase) * waveHeight * amp * 0.7f
            val wave2 = sin(y * 0.035f + wavePhase * 1.3f) * waveHeight * amp * 0.3f
            val x = (wave1 + wave2).coerceAtLeast(0f)
            lineTo(x, y)
            y += step
        }
    }
    
    // Glow layer
    drawPath(
        path = leftPath,
        brush = Brush.verticalGradient(
            colors = gradientColors.map { it.copy(alpha = 0.4f) }
        ),
        style = Stroke(width = glowThickness, cap = StrokeCap.Round)
    )
    
    // Core line - THICKER
    drawPath(
        path = leftPath,
        brush = Brush.verticalGradient(colors = gradientColors),
        style = Stroke(width = edgeThickness, cap = StrokeCap.Round)
    )
    
    // === RIGHT EDGE - Vertical neon wave (mirrored) ===
    val rightPath = Path().apply {
        moveTo(screenWidth, 0f)
        var y = 0f
        val step = 4f
        while (y <= screenHeight) {
            val idx = ((y / screenHeight) * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
            val amp = amplitudes[amplitudes.size - 1 - idx] * pulseScale
            
            val wave1 = sin(y * 0.02f + wavePhase + PI.toFloat()) * waveHeight * amp * 0.7f
            val wave2 = sin(y * 0.035f + wavePhase * 1.3f + PI.toFloat()) * waveHeight * amp * 0.3f
            val x = screenWidth - (wave1 + wave2).coerceAtLeast(0f)
            lineTo(x, y)
            y += step
        }
    }
    
    // Glow layer
    drawPath(
        path = rightPath,
        brush = Brush.verticalGradient(
            colors = gradientColors.reversed().map { it.copy(alpha = 0.4f) }
        ),
        style = Stroke(width = glowThickness, cap = StrokeCap.Round)
    )
    
    // Core line - THICKER
    drawPath(
        path = rightPath,
        brush = Brush.verticalGradient(colors = gradientColors.reversed()),
        style = Stroke(width = edgeThickness, cap = StrokeCap.Round)
    )
    
    // === BOTTOM EDGE - Horizontal neon wave ===
    val bottomPath = Path().apply {
        moveTo(0f, screenHeight)
        var x = 0f
        val step = 3f
        while (x <= screenWidth) {
            val idx = ((x / screenWidth) * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
            val amp = amplitudes[amplitudes.size - 1 - idx] * pulseScale
            
            val wave1 = sin(x * 0.025f + wavePhase + PI.toFloat()) * waveHeight * amp * 0.6f
            val wave2 = sin(x * 0.04f + wavePhase * 1.5f + PI.toFloat()) * waveHeight * amp * 0.3f
            val wave3 = sin(x * 0.06f + wavePhase * 0.7f + PI.toFloat()) * waveHeight * amp * 0.1f
            val y = screenHeight - (wave1 + wave2 + wave3).coerceAtLeast(0f)
            lineTo(x, y)
            x += step
        }
    }
    
    // Glow layer
    drawPath(
        path = bottomPath,
        brush = Brush.horizontalGradient(
            colors = gradientColors.reversed().map { it.copy(alpha = 0.4f) }
        ),
        style = Stroke(width = glowThickness, cap = StrokeCap.Round)
    )
    
    // Core neon line - THICKER
    drawPath(
        path = bottomPath,
        brush = Brush.horizontalGradient(colors = gradientColors.reversed()),
        style = Stroke(width = edgeThickness, cap = StrokeCap.Round)
    )
}

/**
 * Draw a beautiful glowing border around all 4 edges
 * Pulses with music amplitude
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlowingBorder(
    screenWidth: Float,
    screenHeight: Float,
    amplitudes: FloatArray,
    pulseScale: Float,
    gradientColors: List<Color>
) {
    val avgAmplitude = amplitudes.average().toFloat().coerceIn(0.3f, 1f)
    val glowIntensity = avgAmplitude * pulseScale
    
    // Layer 1: Outer wide glow (very soft)
    val outerGlowWidth = 50f * glowIntensity
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                gradientColors[0].copy(alpha = 0.2f * glowIntensity),
                gradientColors[1].copy(alpha = 0.2f * glowIntensity),
                gradientColors[2].copy(alpha = 0.2f * glowIntensity),
                gradientColors[3].copy(alpha = 0.2f * glowIntensity)
            ),
            start = Offset(0f, 0f),
            end = Offset(screenWidth, screenHeight)
        ),
        style = Stroke(width = outerGlowWidth)
    )
    
    // Layer 2: Mid glow
    val midGlowWidth = 28f * glowIntensity
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                gradientColors[0].copy(alpha = 0.4f * glowIntensity),
                gradientColors[1].copy(alpha = 0.4f * glowIntensity),
                gradientColors[2].copy(alpha = 0.4f * glowIntensity),
                gradientColors[3].copy(alpha = 0.4f * glowIntensity)
            ),
            start = Offset(0f, 0f),
            end = Offset(screenWidth, screenHeight)
        ),
        style = Stroke(width = midGlowWidth)
    )
    
    // Layer 3: Inner bright glow
    val innerGlowWidth = 14f * glowIntensity
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                gradientColors[0].copy(alpha = 0.7f * glowIntensity),
                gradientColors[1].copy(alpha = 0.7f * glowIntensity),
                gradientColors[2].copy(alpha = 0.7f * glowIntensity),
                gradientColors[3].copy(alpha = 0.7f * glowIntensity)
            ),
            start = Offset(0f, 0f),
            end = Offset(screenWidth, screenHeight)
        ),
        style = Stroke(width = innerGlowWidth)
    )
    
    // Layer 4: Core bright line - THICKER
    val coreWidth = 6f
    drawRect(
        brush = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(0f, 0f),
            end = Offset(screenWidth, screenHeight)
        ),
        style = Stroke(width = coreWidth)
    )
    
    // Add corner accents based on amplitude
    val cornerSize = 80f * glowIntensity
    
    // Top-left corner accent
    val tlAmp = amplitudes.getOrElse(0) { 0.5f }
    drawCornerGlow(0f, 0f, cornerSize * tlAmp, gradientColors[0], glowIntensity * tlAmp)
    
    // Top-right corner accent
    val trAmp = amplitudes.getOrElse(1) { 0.5f }
    drawCornerGlow(screenWidth, 0f, cornerSize * trAmp, gradientColors[1], glowIntensity * trAmp)
    
    // Bottom-right corner accent
    val brAmp = amplitudes.getOrElse(2) { 0.5f }
    drawCornerGlow(screenWidth, screenHeight, cornerSize * brAmp, gradientColors[2], glowIntensity * brAmp)
    
    // Bottom-left corner accent
    val blAmp = amplitudes.getOrElse(3) { 0.5f }
    drawCornerGlow(0f, screenHeight, cornerSize * blAmp, gradientColors[3], glowIntensity * blAmp)
}

/**
 * Draw glowing corner accent
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerGlow(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    intensity: Float
) {
    val radius = size * 2.5f
    
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.7f * intensity),
                color.copy(alpha = 0.4f * intensity),
                color.copy(alpha = 0.15f * intensity),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = radius
        ),
        center = Offset(x, y),
        radius = radius
    )
}
