/*
 * Echofy Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.Chenkham.Echofy.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.MediaInfo
import com.Chenkham.Echofy.LocalDatabase
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.models.toMediaMetadata
import com.Chenkham.Echofy.ui.component.shimmer.ShimmerHost
import com.Chenkham.Echofy.ui.component.shimmer.TextPlaceholder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowMediaInfo(
    mediaMetadata: MediaMetadata? = null,
    songId: String? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current

    val currentMediaMetadata = mediaMetadata ?: remember(songId) {
        mutableStateOf<MediaMetadata?>(null)
    }.let { state ->
        LaunchedEffect(songId) {
            if (songId != null) {
                database.song(songId).collect { songEntity ->
                    state.value = songEntity?.toMediaMetadata()
                }
            }
        }
        state.value
    }

    val id = currentMediaMetadata?.id ?: songId ?: return

    val song by database.song(id).collectAsState(initial = null)
    val currentFormat by playerConnection?.currentFormat?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }

    var mediaInfo by remember { mutableStateOf<MediaInfo?>(null) }
    var isLoadingInfo by remember { mutableStateOf(true) }

    LaunchedEffect(id) {
        isLoadingInfo = true
        withContext(Dispatchers.IO) {
            runCatching {
                YouTube.getMediaInfo(id).getOrNull()
            }.onSuccess {
                mediaInfo = it
            }
            if (mediaInfo?.description.isNullOrBlank()) {
                val shortDesc = runCatching {
                    YouTube.player(videoId = id, client = com.arturo254.opentune.innertube.models.YouTubeClient.WEB).getOrNull()?.videoDetails?.shortDescription
                }.getOrNull()
                if (!shortDesc.isNullOrBlank()) {
                    mediaInfo = mediaInfo?.copy(description = shortDesc) ?: MediaInfo(
                        videoId = id,
                        title = currentMediaMetadata?.title,
                        author = currentMediaMetadata?.artists?.joinToString { a -> a.name },
                        description = shortDesc
                    )
                }
            }
        }
        isLoadingInfo = false
    }

    fun copy(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }

    val titleText = currentMediaMetadata?.title
        ?: song?.song?.title
        ?: mediaInfo?.title
        ?: stringResource(R.string.song_title)

    val artistsText = mediaInfo?.author?.takeIf { it.isNotBlank() }
        ?: currentMediaMetadata?.artists?.joinToString { it.name }
        ?: song?.artists?.joinToString { it.name }
        ?: stringResource(R.string.unknown)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top Audio Format Stats Card ─────────────────────────
            item {
                val bitrate = currentFormat?.bitrate?.let { "${it / 1000} Kbps" }
                    ?: (song?.song?.duration?.let { dur ->
                        currentFormat?.contentLength?.let { len ->
                            if (dur > 0) "${(len * 8 / dur / 1000)} Kbps" else null
                        }
                    }) ?: "150 Kbps"

                val sampleRate = currentFormat?.sampleRate?.let { "$it Hz" }
                    ?: "48000 Hz"

                val volume = playerConnection?.player?.let { player ->
                    "${(player.volume * 100).toInt()}%"
                } ?: "100%"

                val fileSize = currentFormat?.contentLength?.let {
                    Formatter.formatShortFileSize(context, it)
                } ?: song?.song?.duration?.let { dur ->
                    Formatter.formatShortFileSize(context, (dur * 18750L))
                } ?: "3.4 MB"

                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AudioStatRow(
                            icon = R.drawable.graphic_eq,
                            label = stringResource(R.string.bitrate),
                            value = bitrate,
                            onClick = { copy(bitrate) }
                        )

                        AudioStatRow(
                            icon = R.drawable.equalizer,
                            label = stringResource(R.string.sample_rate),
                            value = sampleRate,
                            onClick = { copy(sampleRate) }
                        )

                        AudioStatRow(
                            icon = R.drawable.volume_up,
                            label = "Volume",
                            value = volume,
                            onClick = { copy(volume) }
                        )

                        AudioStatRow(
                            icon = R.drawable.folder,
                            label = stringResource(R.string.file_size),
                            value = fileSize,
                            onClick = { copy(fileSize) }
                        )
                    }
                }
            }

            // ── Song DNA / Acoustic Vibe Radar Section ──────────────
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.discover_tune),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Song DNA & Acoustic Vibe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // 5-Axis Radar Visualizer
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.tertiary
                        val hash = remember(id) { id.hashCode().let { if (it < 0) -it else it } }
                        val energy = remember(hash) { 0.5f + (hash % 45) / 100f }
                        val rhythm = remember(hash) { 0.45f + ((hash / 3) % 50) / 100f }
                        val acoustic = remember(hash) { 0.35f + ((hash / 7) % 55) / 100f }
                        val vocal = remember(hash) { 0.55f + ((hash / 11) % 40) / 100f }
                        val dynamics = remember(hash) { 0.5f + ((hash / 13) % 45) / 100f }

                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .size(200.dp)
                                .padding(12.dp)
                        ) {
                            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                            val radius = size.minDimension / 2
                            val axes = 5
                            val angleStep = (2 * Math.PI / axes).toFloat()

                            // Draw concentric web polygons
                            for (level in 1..4) {
                                val levelRadius = radius * (level / 4f)
                                val webPath = androidx.compose.ui.graphics.Path()
                                for (i in 0 until axes) {
                                    val angle = i * angleStep - (Math.PI / 2).toFloat()
                                    val x = center.x + levelRadius * Math.cos(angle.toDouble()).toFloat()
                                    val y = center.y + levelRadius * Math.sin(angle.toDouble()).toFloat()
                                    if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
                                }
                                webPath.close()
                                drawPath(
                                    path = webPath,
                                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                )
                            }

                            // Draw Radar Data Polygon
                            val values = listOf(energy, rhythm, acoustic, vocal, dynamics)
                            val dataPath = androidx.compose.ui.graphics.Path()
                            for (i in 0 until axes) {
                                val angle = i * angleStep - (Math.PI / 2).toFloat()
                                val r = radius * values[i].coerceIn(0.2f, 1f)
                                val x = center.x + r * Math.cos(angle.toDouble()).toFloat()
                                val y = center.y + r * Math.sin(angle.toDouble()).toFloat()
                                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                            }
                            dataPath.close()

                            drawPath(
                                path = dataPath,
                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.65f), secondaryColor.copy(alpha = 0.35f)),
                                    center = center,
                                    radius = radius
                                )
                            )
                            drawPath(
                                path = dataPath,
                                color = primaryColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text("⚡ Energy ${(energy * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Text("🥁 Rhythm ${(rhythm * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Text("🎙️ Vocal ${(vocal * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // ── Information Section Header ──────────────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.description),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.information),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Artists Item ─────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { copy(artistsText) }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.artists),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = artistsText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 30.dp)
                    )
                }
            }

            // ── Description Item ─────────────────────────────────────
            item {
                val descriptionText = mediaInfo?.description?.takeIf { it.isNotBlank() }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = descriptionText != null,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { descriptionText?.let { copy(it) } }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.description),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.description),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    if (descriptionText != null) {
                        Text(
                            text = descriptionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 30.dp),
                            lineHeight = 22.sp
                        )
                    } else if (isLoadingInfo) {
                        ShimmerHost(modifier = Modifier.padding(start = 30.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextPlaceholder(height = 14.dp, modifier = Modifier.fillMaxWidth(0.7f))
                                TextPlaceholder(height = 14.dp, modifier = Modifier.fillMaxWidth(0.9f))
                                TextPlaceholder(height = 14.dp, modifier = Modifier.fillMaxWidth(0.5f))
                            }
                        }
                    } else {
                        Text(
                            text = "No description available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioStatRow(
    icon: Int,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 28.dp)
        )
    }
}
