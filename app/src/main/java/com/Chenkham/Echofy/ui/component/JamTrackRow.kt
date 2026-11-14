package com.Chenkham.Echofy.ui.component

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.utils.ShareUtils

@Composable
internal fun CurrentJamTrackRow(
    currentSong: MediaMetadata?,
    canControlPlayback: Boolean,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!currentSong?.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = currentSong?.thumbnailUrl,
                contentDescription = currentSong?.title,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp)),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF282828)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.66f),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currentSong?.title ?: "Nothing is playing yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (currentSong != null) Color(0xFF1DB954) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = currentSong?.artists?.joinToString { it.name } ?: "Start playback to sync the room",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "More options",
                    tint = Color.White.copy(alpha = 0.72f),
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                if (currentSong != null) {
                    DropdownMenuItem(
                        text = { Text("View artist") },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, currentSong.artists.firstOrNull()?.name ?: "Unknown artist", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.artist), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("View album") },
                        onClick = {
                            showMenu = false
                            Toast.makeText(context, currentSong.album?.title ?: "Unknown album", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.album), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share song") },
                        onClick = {
                            showMenu = false
                            val shareText =
                                ShareUtils.buildTrackShareText(
                                    context = context,
                                    songId = currentSong.id,
                                    title = currentSong.title,
                                    artist = currentSong.artists.joinToString { it.name },
                                )
                            val shareIntent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        },
                        leadingIcon = {
                            Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                    )
                }
            }
        }
    }
}
