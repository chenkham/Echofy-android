package com.Chenkham.Echofy.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

sealed class StatsTemplate(val id: String, val name: String) {
    object Modern : StatsTemplate("modern", "Modern")
    object Bold : StatsTemplate("bold", "Bold")
    object Minimal : StatsTemplate("minimal", "Minimal")
    object Retro : StatsTemplate("retro", "Retro")
    
    companion object {
        fun getAll() = listOf(Modern, Bold, Minimal, Retro)
    }
}

data class StatsColorPalette(
    val name: String,
    val backgroundBrush: Brush,
    val primaryText: Color,
    val secondaryText: Color,
    val accentColor: Color,
    val surfaceColor: Color // Semi-transparent overlay
)

object StatsPalettes {
    val EchofyPurple = StatsColorPalette(
        name = "Echofy",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF6B4EFF),
                Color(0xFF9B4DCA),
                Color(0xFFE040FB)
            )
        ),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.8f),
        accentColor = Color(0xFFE040FB),
        surfaceColor = Color.White.copy(alpha = 0.15f)
    )

    val Midnight = StatsColorPalette(
        name = "Midnight",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364)
            )
        ),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.7f),
        accentColor = Color(0xFF4CA1AF),
        surfaceColor = Color.White.copy(alpha = 0.1f)
    )

    val Sunset = StatsColorPalette(
        name = "Sunset",
        backgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF512F),
                Color(0xFFDD2476)
            )
        ),
        primaryText = Color.White,
        secondaryText = Color.White.copy(alpha = 0.9f),
        accentColor = Color(0xFFFFD700),
        surfaceColor = Color.Black.copy(alpha = 0.2f)
    )
    
    val Ocean = StatsColorPalette(
        name = "Ocean",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2193b0),
                Color(0xFF6dd5ed)
            )
        ),
        primaryText = Color.White,
        secondaryText = Color(0xFFE0F7FA),
        accentColor = Color.White,
        surfaceColor = Color.White.copy(alpha = 0.2f)
    )
    
    val Mono = StatsColorPalette(
        name = "Mono",
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF000000),
                Color(0xFF434343)
            )
        ),
        primaryText = Color.White,
        secondaryText = Color.LightGray,
        accentColor = Color.White,
        surfaceColor = Color.White.copy(alpha = 0.1f)
    )

    fun getAll() = listOf(EchofyPurple, Midnight, Sunset, Ocean, Mono)
}
