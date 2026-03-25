package com.Chenkham.Echofy.ui.component

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.db.entities.SongWithStats
import com.Chenkham.Echofy.db.entities.Artist
import com.Chenkham.Echofy.ui.theme.StatsColorPalette
import com.Chenkham.Echofy.ui.theme.StatsPalettes
import com.Chenkham.Echofy.ui.theme.StatsTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsShareDialog(
    topSongs: List<SongWithStats>,
    topArtists: List<Artist>,
    totalListenTime: Long,
    periodText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var selectedTemplate by remember { mutableStateOf<StatsTemplate>(StatsTemplate.Modern) }
    var selectedPalette by remember { mutableStateOf<StatsColorPalette>(StatsPalettes.EchofyPurple) }
    var showTopSongs by remember { mutableStateOf(true) }
    var showTopArtists by remember { mutableStateOf(true) }
    
    // View Capture State
    var captureView by remember { mutableStateOf<View?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isViewReady by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Share Stats") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(painterResource(R.drawable.close), null)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (isCapturing) return@TextButton
                                isCapturing = true
                                scope.launch {
                                    try {
                                        val view = captureView
                                        if (view != null && isViewReady) {
                                            // Use withContext to ensure we're on main thread for view operations
                                            val bitmap = withContext(Dispatchers.Main) {
                                                // IMPORTANT: Set layer type to SOFTWARE to handle hardware bitmaps from Coil
                                                // This must be done BEFORE drawing to avoid "Software rendering doesn't support hardware bitmaps" error
                                                val originalLayerType = view.layerType
                                                view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                                
                                                try {
                                                    // Force invalidation and wait for compositions
                                                    view.invalidate()
                                                    kotlinx.coroutines.delay(500)

                                                    // Get the actual view dimensions or use defaults
                                                    val targetWidth = 1080
                                                    val targetHeight = if (view.height > 0) view.height else 1600

                                                    // Force measure and layout with EXACTLY mode for predictable sizing
                                                    val widthSpec = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
                                                    val heightSpec = View.MeasureSpec.makeMeasureSpec(targetHeight, View.MeasureSpec.AT_MOST)
                                                    view.measure(widthSpec, heightSpec)

                                                    val measuredWidth = view.measuredWidth.coerceAtLeast(targetWidth)
                                                    val measuredHeight = view.measuredHeight.coerceAtLeast(800)

                                                    view.layout(0, 0, measuredWidth, measuredHeight)

                                                    // Force another invalidation and wait for layout to complete
                                                    view.invalidate()
                                                    kotlinx.coroutines.delay(300)

                                                    // Create bitmap using Canvas (more reliable than drawToBitmap)
                                                    val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
                                                    val canvas = Canvas(bitmap)

                                                    // Draw a background first in case of transparency
                                                    canvas.drawColor(android.graphics.Color.TRANSPARENT)

                                                    view.draw(canvas)
                                                    bitmap
                                                } finally {
                                                    // Restore original layer type
                                                    view.setLayerType(originalLayerType, null)
                                                }
                                            }

                                            shareBitmap(context, bitmap)
                                        } else {
                                            Toast.makeText(context, "Please wait for the preview to load", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to capture image: ${e.message}. Please wait for the preview to load and try again.", Toast.LENGTH_LONG).show()
                                        e.printStackTrace()
                                    } finally {
                                        isCapturing = false
                                    }
                                }
                            },
                            enabled = !isCapturing && isViewReady
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Share", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // We use AndroidView to host the Compose content so we can get a View reference to drawToBitmap
                    AndroidView(
                        factory = { ctx ->
                            ComposeView(ctx).apply {
                                setContent {
                                    StatsShareCard(
                                        topSongs = topSongs,
                                        topArtists = topArtists,
                                        totalListenTime = totalListenTime,
                                        periodText = periodText,
                                        template = selectedTemplate,
                                        palette = selectedPalette,
                                        showTopSongs = showTopSongs,
                                        showTopArtists = showTopArtists,
                                        modifier = Modifier.shadow(16.dp, RoundedCornerShape(24.dp))
                                    )
                                }
                                // Ensure view is measured before allowing capture
                                post {
                                    captureView = this
                                    isViewReady = true
                                }
                            }
                        },
                        update = { view ->
                             (view as? ComposeView)?.setContent {
                                 StatsShareCard(
                                    topSongs = topSongs,
                                    topArtists = topArtists,
                                    totalListenTime = totalListenTime,
                                    periodText = periodText,
                                    template = selectedTemplate,
                                    palette = selectedPalette,
                                    showTopSongs = showTopSongs,
                                    showTopArtists = showTopArtists,
                                    modifier = Modifier.shadow(16.dp, RoundedCornerShape(24.dp))
                                )
                             }
                             captureView = view
                        },
                        modifier = Modifier.wrapContentSize()
                    )
                }
                
                // Controls Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(24.dp)
                ) {
                     // Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilterChip(
                            selected = showTopSongs,
                            onClick = { showTopSongs = !showTopSongs },
                            label = { Text("Songs") },
                            leadingIcon = { if(showTopSongs) Icon(painterResource(R.drawable.check), null) }
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = showTopArtists,
                            onClick = { showTopArtists = !showTopArtists },
                            label = { Text("Artists") },
                            leadingIcon = { if(showTopArtists) Icon(painterResource(R.drawable.check), null) }
                        )
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Template", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StatsTemplate.getAll()) { template ->
                            SuggestionChip(
                                onClick = { selectedTemplate = template },
                                label = { Text(template.name) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (selectedTemplate == template) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = if (selectedTemplate == template) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Color Theme", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(StatsPalettes.getAll()) { palette ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(palette.backgroundBrush)
                                    .border(
                                        width = if (selectedPalette == palette) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPalette = palette }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp)) // Extra padding at bottom
                }
            }
        }
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cacheDir = File(context.cacheDir, "share")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // Clean up old share files
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("echofy_stats_share_")) {
                file.delete()
            }
        }
        
        // Save to a file
        val file = File(cacheDir, "echofy_stats_share_${System.currentTimeMillis()}.png")
        
        FileOutputStream(file).use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!compressed) {
                throw Exception("Failed to compress bitmap")
            }
            out.flush()
        }
        
        if (!file.exists() || file.length() == 0L) {
            throw Exception("Failed to save image file")
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share Stats"))
    } catch (e: Exception) {
        e.printStackTrace()
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
