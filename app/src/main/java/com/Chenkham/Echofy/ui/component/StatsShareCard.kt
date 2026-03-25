package com.Chenkham.Echofy.ui.component

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.SongWithStats
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.ui.theme.StatsColorPalette
import com.Chenkham.Echofy.ui.theme.StatsPalettes
import com.Chenkham.Echofy.ui.theme.StatsTemplate
import com.Chenkham.Echofy.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * CaptureController to trigger bitmap capture
 */
class StatsCaptureController {
    var captureBitmap: (() -> Bitmap?)? = null
    
    fun capture(): Bitmap? {
        return captureBitmap?.invoke()
    }
}

@Composable
fun rememberStatsCaptureController(): StatsCaptureController {
    return remember { StatsCaptureController() }
}

@Composable
fun StatsShareCard(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>, // Now using Artist entity for thumbnails
    totalListenTime: Long,
    periodText: String,
    template: StatsTemplate,
    palette: StatsColorPalette,
    showTopSongs: Boolean,
    showTopArtists: Boolean,
    controller: StatsCaptureController? = null,
    modifier: Modifier = Modifier
) {
    // We use AndroidView to wrap a ComposeView so we can capture it easily if needed
    // However, for pure display in Compose we can just use the content directly.
    // To support "capture", we need a View.
    // For the Dialog preview, we just want the composable.
    // For the Share generation, we'll instantiate this separately in an off-screen view or use the controller.
    
    // In this refactor, we just define the UI structure. The Capturing logic will reside in the wrapper or utility.
    
    Card(
        modifier = modifier
            .width(360.dp) // Fixed width for consistency
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.backgroundBrush)
        ) {
            when (template) {
                is StatsTemplate.Modern -> ModernStatsLayout(
                    topSongs, topArtists, totalListenTime, periodText, palette, showTopSongs, showTopArtists
                )
                is StatsTemplate.Bold -> BoldStatsLayout(
                    topSongs, topArtists, totalListenTime, periodText, palette, showTopSongs, showTopArtists
                )
                is StatsTemplate.Minimal -> MinimalStatsLayout(
                    topSongs, topArtists, totalListenTime, periodText, palette, showTopSongs, showTopArtists
                )
                is StatsTemplate.Retro -> RetroStatsLayout(
                    topSongs, topArtists, totalListenTime, periodText, palette, showTopSongs, showTopArtists
                )
            }
        }
    }
}

// --- Layout Implementations ---

@Composable
private fun ModernStatsLayout(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTime: Long,
    periodText: String,
    palette: StatsColorPalette,
    showTopSongs: Boolean,
    showTopArtists: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandHeader(palette)
            PeriodBadge(periodText, palette)
        }

        Spacer(Modifier.height(24.dp))

        // Listen Time
        BigListenTime(totalListenTime, palette)

        Spacer(Modifier.height(24.dp))

        if (showTopSongs && topSongs.isNotEmpty()) {
            SectionHeader("Top Songs", palette.primaryText)
            Spacer(Modifier.height(8.dp))
            topSongs.take(3).forEachIndexed { index, song ->
                ModernListItem(
                    rank = index + 1,
                    title = song.title,
                    subtitle = "${song.songCountListened} acts",
                    thumbnailUrl = song.thumbnailUrl,
                    palette = palette
                )
                if (index < 2) Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showTopArtists && topArtists.isNotEmpty()) {
            SectionHeader("Top Artists", palette.primaryText)
            Spacer(Modifier.height(8.dp))
            topArtists.take(3).forEachIndexed { index, artist ->
                ModernListItem(
                    rank = index + 1,
                    title = artist.title,
                    subtitle = null, // Artists don't always have easy "plays" count handy in this context without extra data
                    thumbnailUrl = artist.thumbnailUrl,
                    palette = palette
                )
                if (index < 2) Spacer(Modifier.height(8.dp))
            }
        }
        
        Spacer(Modifier.height(20.dp))
        Footer(palette)
    }
}

@Composable
private fun BoldStatsLayout(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTime: Long,
    periodText: String,
    palette: StatsColorPalette,
    showTopSongs: Boolean,
    showTopArtists: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        BrandHeader(palette)
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "RECAP",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = palette.primaryText
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = makeTimeString(totalListenTime) ?: "0m",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = palette.accentColor
        )
        Text(
            text = periodText.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (showTopSongs && topSongs.isNotEmpty()) {
            Text("TOP SONGS", fontWeight = FontWeight.Black, color = palette.secondaryText)
            Spacer(Modifier.height(8.dp))
            topSongs.take(5).forEachIndexed { index, song ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(16.dp))
        }

         if (showTopArtists && topArtists.isNotEmpty()) {
            Text("TOP ARTISTS", fontWeight = FontWeight.Black, color = palette.secondaryText)
            Spacer(Modifier.height(8.dp))
            topArtists.take(5).forEachIndexed { index, artist ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor,
                        modifier = Modifier.width(24.dp)
                    )
                    Text(
                        text = artist.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.primaryText,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun MinimalStatsLayout(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTime: Long,
    periodText: String,
    palette: StatsColorPalette,
    showTopSongs: Boolean,
    showTopArtists: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.echofy),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
             text = makeTimeString(totalListenTime) ?: "0m",
             style = MaterialTheme.typography.displayMedium,
             fontWeight = FontWeight.Light,
             color = palette.primaryText
        )
        Text(
            text = "Total Listening Time • $periodText",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText
        )
        
        Spacer(Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (showTopSongs && topSongs.isNotEmpty()) {
                // Show cover art grid
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Top Song", color = palette.secondaryText, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(topSongs.first().thumbnailUrl)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        topSongs.first().title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.primaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
            
            if (showTopArtists && topArtists.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Top Artist", color = palette.secondaryText, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(topArtists.first().thumbnailUrl)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        topArtists.first().title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = palette.primaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RetroStatsLayout(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTime: Long,
    periodText: String,
    palette: StatsColorPalette,
    showTopSongs: Boolean,
    showTopArtists: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, palette.primaryText, RoundedCornerShape(16.dp))
            .padding(16.dp), // Inner padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "************",
            color = palette.primaryText,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(Modifier.height(8.dp))
        Image(
            painter = painterResource(R.drawable.echofy),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = "ECHOFY MIX",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
         Text(
            text = "************",
            color = palette.primaryText,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "TIME: ${makeTimeString(totalListenTime)}",
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = palette.accentColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(Modifier.height(24.dp))
        
        if (showTopSongs && topSongs.isNotEmpty()) {
            Column(Modifier.fillMaxWidth()) {
                topSongs.take(3).forEachIndexed { index, song ->
                     Text(
                        text = "${index+1}. ${song.title.uppercase()}",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = palette.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        
         if (showTopArtists && topArtists.isNotEmpty()) {
             Column(Modifier.fillMaxWidth()) {
                 Text("FEATURING:", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = palette.secondaryText)
                 topArtists.take(3).forEach { artist ->
                      Text(
                        text = ">> ${artist.title.uppercase()}",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = palette.primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                 }
             }
        }
        
        Spacer(Modifier.height(24.dp))
        Text(
            text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = palette.secondaryText
        )
    }
}

// --- Common Components ---

@Composable
private fun BrandHeader(palette: StatsColorPalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.surfaceColor),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.echofy),
                contentDescription = "Echofy Logo",
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Echofy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Text(
                text = "My Music Stats",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText
            )
        }
    }
}

@Composable
private fun PeriodBadge(text: String, palette: StatsColorPalette) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surfaceColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = palette.primaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BigListenTime(time: Long, palette: StatsColorPalette) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = makeTimeString(time) ?: "0h",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Text(
            text = "Total Listening Time",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText
        )
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun Footer(palette: StatsColorPalette) {
    Text(
        text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
        style = MaterialTheme.typography.labelSmall,
        color = palette.secondaryText
    )
}

@Composable
private fun ModernListItem(
    rank: Int,
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    palette: StatsColorPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        // Thumbnail
        if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
        }
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
            }
        }
    }
}

// Extension to generate image
suspend fun generateStatsImage(
    context: Context,
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTime: Long,
    periodText: String,
    template: StatsTemplate,
    palette: StatsColorPalette,
    showTopSongs: Boolean,
    showTopArtists: Boolean
): File? {
    return withContext(Dispatchers.Main) {
        // We create an invisible ComposeView, add it to the window (or a fake container),
        // wait for it to layout, and then draw it to a bitmap.
        // Since we are not in an Activity context easily here without hacks, 
        // a common strategy is to use the existing dialog's view hierarchy or simply 
        // use the CaptureController pattern inside the active composition.
        
        // For simplicity and reliability in this "function" calling context, we will assume
        // the ComposeView is actively rendered on screen in the Dialog and we just capture THAT.
        // But if we want "background generation", we need a different approach.
        // Given the goal "User Selects -> Clicks Share", we can just capture the Preview View!
        
        null // We will handle logic in the Dialog
    }
}


// End of file

