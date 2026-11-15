package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.Chenkham.Echofy.lyrics.LyricsEntry
import com.Chenkham.Echofy.ui.component.shimmer.ShimmerHost
import com.Chenkham.Echofy.ui.component.shimmer.TextPlaceholder
import com.Chenkham.Echofy.ui.screens.settings.LyricsPosition
import com.Chenkham.Echofy.ui.utils.fadingEdge

@Composable
fun LyricsListView(
    lazyListState: LazyListState,
    lines: List<LyricsEntry>,
    displayedCurrentLineIndex: Int,
    isLoadingLyrics: Boolean,
    isSynced: Boolean,
    changeLyrics: Boolean,
    animateLyrics: Boolean,
    isSelectionModeActive: Boolean,
    selectedIndices: SnapshotStateList<Int>,
    showTranslation: Boolean,
    translatedLines: Map<Int, String>,
    lyricsTextPosition: LyricsPosition,
    lyricsFontSize: TextUnit,
    isFullscreen: Boolean,
    showLyrics: Boolean,
    showControls: Boolean,
    textColor: Color,
    lyrics: String?,
    onSeekToLine: (LyricsEntry, Int) -> Unit,
    onSelectLine: (Int) -> Unit,
    onToggleSelectionMode: (Int) -> Unit,
    onUserScrollStateChange: (Boolean) -> Unit,
    onRetryLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        contentAlignment = if (isFullscreen) Alignment.TopStart else Alignment.Center,
        modifier =
            modifier
                .fillMaxSize()
                .then(if (isFullscreen) Modifier else Modifier.padding(bottom = 12.dp)),
    ) {
        if (isFullscreen || showLyrics) {
            val topPadding =
                if (isFullscreen) {
                    with(LocalDensity.current) {
                        100.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    }
                } else {
                    0.dp
                }

            LazyColumn(
                state = lazyListState,
                contentPadding =
                    if (isFullscreen) {
                        PaddingValues(
                            top = topPadding,
                            bottom = if (showControls) 104.dp else 24.dp,
                            start = 8.dp,
                            end = 8.dp,
                        )
                    } else {
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Top)
                            .add(WindowInsets(top = maxHeight / 2, bottom = maxHeight / 2))
                            .asPaddingValues()
                    },
                modifier =
                    Modifier
                        .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier)
                        .fadingEdge(vertical = if (isFullscreen) 32.dp else 64.dp)
                        .nestedScroll(
                            remember {
                                object : NestedScrollConnection {
                                    override fun onPreScroll(
                                        available: Offset,
                                        source: NestedScrollSource,
                                    ): Offset {
                                        if (source == NestedScrollSource.UserInput && !isSelectionModeActive) {
                                            onUserScrollStateChange(true)
                                        }
                                        return Offset.Zero
                                    }

                                    override suspend fun onPostFling(
                                        consumed: Velocity,
                                        available: Velocity,
                                    ): Velocity {
                                        if (!isSelectionModeActive) {
                                            kotlinx.coroutines.delay(4_000)
                                            onUserScrollStateChange(false)
                                        }
                                        return super.onPostFling(consumed, available)
                                    }
                                }
                            },
                        ),
            ) {
                if (isLoadingLyrics) {
                    item {
                        ShimmerHost {
                            repeat(if (isFullscreen) 6 else 10) {
                                Box(
                                    contentAlignment =
                                        when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Alignment.CenterStart
                                            LyricsPosition.CENTER -> Alignment.Center
                                            LyricsPosition.RIGHT -> Alignment.CenterEnd
                                        },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = if (isFullscreen) 16.dp else 24.dp,
                                                vertical = if (isFullscreen) 6.dp else 4.dp,
                                            ),
                                ) {
                                    TextPlaceholder()
                                }
                            }
                        }
                    }
                } else if (lines.isEmpty()) {
                    item {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(R.drawable.lyrics),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = textColor.copy(alpha = 0.5f),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.no_lyrics_available),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = lines,
                        key = { index, item -> "$index-${item.time}" },
                    ) { index, item ->
                        LyricsLineItem(
                            index = index,
                            item = item,
                            displayedCurrentLineIndex = displayedCurrentLineIndex,
                            isSynced = isSynced,
                            animateLyrics = animateLyrics,
                            isSelectionModeActive = isSelectionModeActive,
                            isSelected = selectedIndices.contains(index),
                            showTranslation = showTranslation,
                            translatedText = translatedLines[index],
                            lyricsTextPosition = lyricsTextPosition,
                            lyricsFontSize = lyricsFontSize,
                            isFullscreen = isFullscreen,
                            textColor = textColor,
                            onClick = {
                                if (isSelectionModeActive) {
                                    onSelectLine(index)
                                } else if (isSynced && changeLyrics) {
                                    onSeekToLine(item, index)
                                }
                            },
                            onLongClick = {
                                onToggleSelectionMode(index)
                            },
                        )
                    }
                }
            }

            if (lyrics == LYRICS_NOT_FOUND) {
                LyricsNotFoundView(
                    isFullscreen = isFullscreen,
                    isLoadingLyrics = isLoadingLyrics,
                    lyricsTextPosition = lyricsTextPosition,
                    onRetry = onRetryLyrics,
                )
            }
        }
    }
}
