package com.Chenkham.Echofy.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.models.MediaMetadata

@Composable
fun LyricsFullscreenHeader(
    visible: Boolean,
    mediaMetadata: MediaMetadata?,
    currentProvider: String?,
    hasLyrics: Boolean,
    isTranslating: Boolean,
    hasTranslation: Boolean,
    isSelectionModeActive: Boolean,
    selectedIndicesNotEmpty: Boolean,
    onNavigateBack: (() -> Unit)?,
    onOpenTranslate: () -> Unit,
    onOpenChords: () -> Unit,
    onShareSelected: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -it },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it },
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .zIndex(2f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onNavigateBack?.invoke() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White,
                    )
                }

                mediaMetadata?.let { metadata ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = metadata.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = metadata.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        currentProvider?.let { provider ->
                            Text(
                                text = "Provided by $provider",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                            )
                        }
                    }
                }

                if (hasLyrics) {
                    IconButton(
                        onClick = onOpenTranslate,
                        enabled = !isTranslating,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.translate),
                                contentDescription = "Translate lyrics",
                                tint = if (hasTranslation) MaterialTheme.colorScheme.primary else Color.White,
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenChords,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                ) {
                    Text(text = "🎸", fontSize = 18.sp)
                }

                if (isSelectionModeActive) {
                    IconButton(
                        onClick = onShareSelected,
                        enabled = selectedIndicesNotEmpty,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = stringResource(R.string.share_selected),
                            tint = if (selectedIndicesNotEmpty) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                        )
                    }
                } else {
                    IconButton(
                        onClick = onOpenMenu,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = stringResource(R.string.more_options),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
