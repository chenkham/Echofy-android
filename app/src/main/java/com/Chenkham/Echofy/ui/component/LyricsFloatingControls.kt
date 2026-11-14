package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.R

@Composable
fun BoxScope.LyricsFloatingControls(
    isSelectionModeActive: Boolean,
    selectedCount: Int,
    textColor: Color,
    onCloseSelection: () -> Unit,
    onShareSelected: () -> Unit,
    onToggleShowLyrics: () -> Unit,
    onOpenTranslate: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    if (isSelectionModeActive) {
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(color = Color.Black.copy(alpha = 0.3f), shape = CircleShape)
                            .clickable(onClick = onCloseSelection),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Row(
                    modifier =
                        Modifier
                            .background(
                                color = if (selectedCount > 0) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp),
                            )
                            .clickable(enabled = selectedCount > 0, onClick = onShareSelected)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.share),
                        contentDescription = stringResource(R.string.share_selected),
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.share),
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShowLyrics) {
                Icon(
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = null,
                    tint = textColor,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onOpenTranslate) {
                Icon(
                    painter = painterResource(id = R.drawable.translate),
                    contentDescription = "Translate Lyrics",
                    tint = textColor,
                )
            }

            IconButton(onClick = onOpenMenu) {
                Icon(
                    painter = painterResource(id = R.drawable.more_horiz),
                    contentDescription = stringResource(R.string.more_options),
                    tint = textColor,
                )
            }
        }
    }
}
