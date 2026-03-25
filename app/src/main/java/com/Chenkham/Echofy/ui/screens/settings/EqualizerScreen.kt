package com.Chenkham.Echofy.ui.screens.settings

import android.media.audiofx.Equalizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.*
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.serialization.json.Json

// Frequency bands for 5-band EQ
data class FrequencyBand(
    val frequency: String,
    val unit: String
)

val frequencyBands = listOf(
    FrequencyBand("60", "Hz"),
    FrequencyBand("230", "Hz"),
    FrequencyBand("910", "Hz"),
    FrequencyBand("3.6", "kHz"),
    FrequencyBand("14", "kHz")
)

// Preset data
data class EqualizerPreset(
    val name: String,
    val levels: List<Float> // normalized 0-1 values
)

val customPresets = listOf(
    EqualizerPreset("Normal", listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)),
    EqualizerPreset("Classical", listOf(0.7f, 0.5f, 0.3f, 0.5f, 0.7f)),
    EqualizerPreset("Dance", listOf(0.8f, 0.6f, 0.4f, 0.6f, 0.8f)),
    EqualizerPreset("Flat", listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)),
    EqualizerPreset("Folk", listOf(0.6f, 0.5f, 0.4f, 0.6f, 0.5f)),
    EqualizerPreset("Heavy Metal", listOf(0.8f, 0.3f, 0.5f, 0.3f, 0.8f)),
    EqualizerPreset("Hip Hop", listOf(0.8f, 0.7f, 0.5f, 0.6f, 0.7f)),
    EqualizerPreset("Jazz", listOf(0.6f, 0.5f, 0.4f, 0.5f, 0.6f)),
    EqualizerPreset("Pop", listOf(0.5f, 0.6f, 0.7f, 0.6f, 0.5f)),
    EqualizerPreset("Rock", listOf(0.7f, 0.5f, 0.3f, 0.5f, 0.7f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    navController: NavController,
) {
    val playerConnection = LocalPlayerConnection.current

    // Preferences
    var eqEnabled by rememberPreference(EqualizerEnabledKey, false)
    var presetIndex by rememberPreference(EqualizerPresetKey, 0)
    var bassEnabled by rememberPreference(BassBoostEnabledKey, false)
    var bassStrength by rememberPreference(BassBoostStrengthKey, 500)
    var bandLevelsJson by rememberPreference(EqualizerBandLevelsKey, "")

    // Band levels (5 bands) - normalized 0 to 1
    var bandLevels by remember { mutableStateOf(listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)) }

    // EQ info from device
    val audioSessionId = playerConnection?.player?.audioSessionId ?: 0
    var devicePresetNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var minLevel by remember { mutableIntStateOf(-1500) }
    var maxLevel by remember { mutableIntStateOf(1500) }

    // Initialize from device EQ
    LaunchedEffect(audioSessionId) {
        if (audioSessionId > 0) {
            try {
                val eq = Equalizer(0, audioSessionId)
                
                val presets = mutableListOf<String>()
                for (i in 0 until eq.numberOfPresets) {
                    presets.add(eq.getPresetName(i.toShort()))
                }
                devicePresetNames = presets

                val range = eq.bandLevelRange
                minLevel = range[0].toInt()
                maxLevel = range[1].toInt()

                // Load current levels
                if (eq.numberOfBands >= 5) {
                    val levels = mutableListOf<Float>()
                    for (i in 0 until 5) {
                        val level = eq.getBandLevel(i.toShort()).toFloat()
                        val normalized = (level - minLevel) / (maxLevel - minLevel)
                        levels.add(normalized.coerceIn(0f, 1f))
                    }
                    bandLevels = levels
                }

                eq.release()
            } catch (e: Exception) { }
        }
    }

    // Save band levels when changed
    LaunchedEffect(bandLevels) {
        val actualLevels = bandLevels.map { normalized ->
            (normalized * (maxLevel - minLevel) + minLevel).toInt()
        }
        bandLevelsJson = actualLevels.joinToString(",")
    }

    // Apply preset when selected
    fun applyPreset(index: Int) {
        if (index >= 0 && index < customPresets.size) {
            bandLevels = customPresets[index].levels
            presetIndex = index
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Equalizer",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Enable/Disable Switch
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Equalizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { eqEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor
                        )
                    )
                }
            }

            // Frequency Response Graph
            if (eqEnabled) {
                FrequencyResponseGraph(
                    bandLevels = bandLevels,
                    onLevelChange = { index, level ->
                        bandLevels = bandLevels.toMutableList().also { it[index] = level }
                        presetIndex = -1 // Custom when manually adjusted
                    },
                    accentColor = accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                // Presets List
                Text(
                    "Presets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    customPresets.forEachIndexed { index, preset ->
                        PresetItem(
                            name = preset.name,
                            isSelected = presetIndex == index,
                            onClick = { applyPreset(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bass Boost Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Bass Boost",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = bassEnabled,
                                onCheckedChange = { bassEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }

                        if (bassEnabled) {
                            Spacer(Modifier.height(12.dp))
                            Slider(
                                value = bassStrength.toFloat(),
                                onValueChange = { bassStrength = it.toInt() },
                                valueRange = 0f..1000f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Low",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "High",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun FrequencyResponseGraph(
    bandLevels: List<Float>,
    onLevelChange: (Int, Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var draggedIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        // dB labels
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("+10 db", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0 db", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-10 db", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Graph area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, bottom = 24.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val width = size.width.toFloat()
                                val spacing = width / (bandLevels.size - 1)
                                val index = ((offset.x + spacing / 2) / spacing).toInt()
                                    .coerceIn(0, bandLevels.size - 1)
                                draggedIndex = index
                            },
                            onDrag = { change, _ ->
                                if (draggedIndex >= 0) {
                                    val height = size.height.toFloat()
                                    val newLevel = 1f - (change.position.y / height).coerceIn(0f, 1f)
                                    onLevelChange(draggedIndex, newLevel)
                                }
                            },
                            onDragEnd = { draggedIndex = -1 }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val spacing = width / (bandLevels.size - 1)

                // Draw grid lines
                val gridColor = Color.Gray.copy(alpha = 0.2f)
                for (i in 0..4) {
                    val y = height * i / 4
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Calculate points
                val points = bandLevels.mapIndexed { index, level ->
                    Offset(
                        x = index * spacing,
                        y = height * (1f - level)
                    )
                }

                // Draw filled area under curve
                val fillPath = Path().apply {
                    moveTo(0f, height)
                    points.forEachIndexed { index, point ->
                        if (index == 0) {
                            lineTo(point.x, point.y)
                        } else {
                            val prevPoint = points[index - 1]
                            val controlX1 = prevPoint.x + spacing / 3
                            val controlX2 = point.x - spacing / 3
                            cubicTo(controlX1, prevPoint.y, controlX2, point.y, point.x, point.y)
                        }
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.4f),
                            accentColor.copy(alpha = 0.1f)
                        )
                    )
                )

                // Draw curve line
                val curvePath = Path().apply {
                    points.forEachIndexed { index, point ->
                        if (index == 0) {
                            moveTo(point.x, point.y)
                        } else {
                            val prevPoint = points[index - 1]
                            val controlX1 = prevPoint.x + spacing / 3
                            val controlX2 = point.x - spacing / 3
                            cubicTo(controlX1, prevPoint.y, controlX2, point.y, point.x, point.y)
                        }
                    }
                }

                drawPath(
                    path = curvePath,
                    color = accentColor,
                    style = Stroke(width = 3f)
                )

                // Draw control points
                points.forEach { point ->
                    drawCircle(
                        color = accentColor,
                        radius = 12f,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = point
                    )
                }
            }

            // Frequency labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                frequencyBands.forEach { band ->
                    Text(
                        "${band.frequency} ${band.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PresetItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
