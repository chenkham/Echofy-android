package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.ui.screens.settings.LyricsPosition

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsLineItem(
    index: Int,
    item: LyricsEntry,
    displayedCurrentLineIndex: Int,
    isSynced: Boolean,
    animateLyrics: Boolean,
    isSelectionModeActive: Boolean,
    isSelected: Boolean,
    showTranslation: Boolean,
    translatedText: String?,
    lyricsTextPosition: LyricsPosition,
    lyricsFontSize: TextUnit,
    isFullscreen: Boolean,
    textColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val distance = kotlin.math.abs(index - displayedCurrentLineIndex)
    val animatedScale by animateFloatAsState(
        when {
            !isSynced || index == displayedCurrentLineIndex -> 1f
            distance == 1 -> 0.95f
            distance >= 2 -> 0.9f
            else -> 1f
        },
        tween(if (animateLyrics) 250 else 0),
    )
    val animatedAlpha by animateFloatAsState(
        when {
            !isSynced || (isSelectionModeActive && isSelected) -> 1f
            index == displayedCurrentLineIndex -> 1f
            distance == 1 -> 0.7f
            distance == 2 -> 0.4f
            else -> 0.2f
        },
        tween(if (animateLyrics) 250 else 0),
    )

    val itemModifier =
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                enabled = true,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .background(
                if (isSelected && isSelectionModeActive) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                } else {
                    Color.Transparent
                },
            )
            .padding(
                horizontal = if (isFullscreen) 16.dp else 24.dp,
                vertical = 8.dp,
            )
            .alpha(animatedAlpha)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }

    Column(modifier = itemModifier) {
        if (showTranslation && !translatedText.isNullOrBlank()) {
            Text(
                text = translatedText,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign =
                    when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            )
        }

        val isActiveLine = index == displayedCurrentLineIndex && isSynced
        val isElapsedLine = isSynced && displayedCurrentLineIndex >= 0 && index < displayedCurrentLineIndex
        val activeLyricsBrush =
            Brush.linearGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFF8DFDFF), Color(0xFFE7A8FF), Color(0xFFFFF0A8)),
            )
        val elapsedLyricsBrush =
            Brush.linearGradient(
                colors = listOf(Color(0xFFB879FF).copy(alpha = 0.92f), Color(0xFF00E5FF).copy(alpha = 0.86f), Color(0xFFFF7AD9).copy(alpha = 0.74f)),
            )
        val inactiveColor =
            animateColorAsState(
                if (isFullscreen) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else textColor.copy(alpha = 0.8f),
                animationSpec = tween(if (animateLyrics) 250 else 0),
            ).value

        val textAlign =
            when (lyricsTextPosition) {
                LyricsPosition.LEFT -> TextAlign.Left
                LyricsPosition.CENTER -> TextAlign.Center
                LyricsPosition.RIGHT -> TextAlign.Right
            }

        Text(
            text = item.text,
            fontSize = lyricsFontSize,
            style =
                if (isActiveLine) {
                    TextStyle(brush = activeLyricsBrush, fontSize = lyricsFontSize, fontWeight = FontWeight.ExtraBold, textAlign = textAlign)
                } else if (isElapsedLine) {
                    TextStyle(brush = elapsedLyricsBrush, fontSize = lyricsFontSize, fontWeight = FontWeight.ExtraBold, textAlign = textAlign)
                } else {
                    TextStyle(color = inactiveColor, fontSize = lyricsFontSize, fontWeight = FontWeight.Bold, textAlign = textAlign)
                },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
