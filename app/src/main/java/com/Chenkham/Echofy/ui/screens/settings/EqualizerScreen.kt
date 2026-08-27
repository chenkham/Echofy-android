package com.Chenkham.Echofy.ui.screens.settings

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.LocalPlayerConnection
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.*
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.PreferenceGroupTitle
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.rememberPreference

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

// Comprehensive presets
data class EqualizerPreset(
    val name: String,
    val levels: List<Float> // normalized 0-1 values (0.5 = flat/0dB)
)

val customPresets = listOf(
    EqualizerPreset("Flat", listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)),
    EqualizerPreset("Classical", listOf(0.7f, 0.55f, 0.35f, 0.55f, 0.7f)),
    EqualizerPreset("Dance", listOf(0.85f, 0.65f, 0.45f, 0.65f, 0.85f)),
    EqualizerPreset("Folk", listOf(0.6f, 0.5f, 0.45f, 0.55f, 0.5f)),
    EqualizerPreset("Heavy Metal", listOf(0.8f, 0.4f, 0.55f, 0.4f, 0.85f)),
    EqualizerPreset("Hip Hop", listOf(0.85f, 0.75f, 0.5f, 0.65f, 0.75f)),
    EqualizerPreset("Jazz", listOf(0.65f, 0.55f, 0.45f, 0.55f, 0.65f)),
    EqualizerPreset("Pop", listOf(0.55f, 0.65f, 0.75f, 0.65f, 0.55f)),
    EqualizerPreset("Rock", listOf(0.75f, 0.55f, 0.35f, 0.55f, 0.75f)),
    EqualizerPreset("Acoustic", listOf(0.65f, 0.6f, 0.5f, 0.6f, 0.7f)),
    EqualizerPreset("Bass Booster", listOf(0.9f, 0.8f, 0.6f, 0.5f, 0.5f)),
    EqualizerPreset("Bass Reducer", listOf(0.2f, 0.3f, 0.5f, 0.5f, 0.5f)),
    EqualizerPreset("Electronic", listOf(0.8f, 0.7f, 0.4f, 0.6f, 0.8f)),
    EqualizerPreset("Vocal Booster", listOf(0.4f, 0.55f, 0.8f, 0.7f, 0.5f)),
    EqualizerPreset("Treble Booster", listOf(0.5f, 0.5f, 0.55f, 0.8f, 0.9f)),
)

val reverbPresets = listOf(
    "None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate"
)

data class AutoEqProfile(
    val brand: String,
    val model: String,
    val levels: List<Float>
)

val autoEqProfiles = listOf(
    AutoEqProfile("Apple", "AirPods Pro 2", listOf(0.52f, 0.48f, 0.54f, 0.62f, 0.58f)),
    AutoEqProfile("Apple", "AirPods Max", listOf(0.48f, 0.52f, 0.56f, 0.60f, 0.55f)),
    AutoEqProfile("Sony", "WH-1000XM5", listOf(0.42f, 0.48f, 0.58f, 0.64f, 0.60f)),
    AutoEqProfile("Sony", "WH-1000XM4", listOf(0.40f, 0.46f, 0.60f, 0.65f, 0.58f)),
    AutoEqProfile("Sony", "WF-1000XM5", listOf(0.46f, 0.50f, 0.55f, 0.62f, 0.58f)),
    AutoEqProfile("Samsung", "Galaxy Buds 2 Pro", listOf(0.50f, 0.52f, 0.54f, 0.58f, 0.56f)),
    AutoEqProfile("Sennheiser", "HD 600", listOf(0.60f, 0.55f, 0.50f, 0.52f, 0.55f)),
    AutoEqProfile("Sennheiser", "HD 650", listOf(0.58f, 0.54f, 0.50f, 0.54f, 0.56f)),
    AutoEqProfile("Sennheiser", "Momentum 4", listOf(0.44f, 0.48f, 0.56f, 0.60f, 0.58f)),
    AutoEqProfile("Bose", "QuietComfort Ultra", listOf(0.46f, 0.50f, 0.58f, 0.60f, 0.56f)),
    AutoEqProfile("Bose", "QC45", listOf(0.48f, 0.52f, 0.56f, 0.58f, 0.54f)),
    AutoEqProfile("Audio-Technica", "ATH-M50x", listOf(0.48f, 0.52f, 0.55f, 0.58f, 0.52f)),
    AutoEqProfile("Moondrop", "Chu II", listOf(0.52f, 0.50f, 0.54f, 0.56f, 0.55f)),
    AutoEqProfile("Moondrop", "Aria", listOf(0.50f, 0.52f, 0.52f, 0.56f, 0.54f)),
    AutoEqProfile("Beyerdynamic", "DT 770 Pro (80Ω)", listOf(0.46f, 0.52f, 0.50f, 0.52f, 0.46f)),
    AutoEqProfile("Beyerdynamic", "DT 990 Pro (250Ω)", listOf(0.48f, 0.54f, 0.52f, 0.50f, 0.44f)),
    AutoEqProfile("Shure", "SE215", listOf(0.45f, 0.50f, 0.58f, 0.62f, 0.60f)),
    AutoEqProfile("JBL", "Tune 760NC", listOf(0.46f, 0.50f, 0.56f, 0.60f, 0.58f)),
    AutoEqProfile("OnePlus", "Buds Pro 2", listOf(0.48f, 0.52f, 0.55f, 0.60f, 0.56f)),
    AutoEqProfile("Nothing", "Ear (2)", listOf(0.50f, 0.52f, 0.56f, 0.58f, 0.55f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val accentColor = MaterialTheme.colorScheme.primary

    // Persistent Settings
    var eqEnabled by rememberPreference(EqualizerEnabledKey, false)
    var presetIndex by rememberPreference(EqualizerPresetKey, 0)
    var bassEnabled by rememberPreference(BassBoostEnabledKey, false)
    var bassStrength by rememberPreference(BassBoostStrengthKey, 500)
    var virtualizerEnabled by rememberPreference(booleanPreferencesKey("virtualizerEnabled"), false)
    var virtualizerStrength by rememberPreference(intPreferencesKey("virtualizerStrength"), 500)
    var outputGain by rememberPreference(intPreferencesKey("eqOutputGainMb"), 0)
    var audioBalance by rememberPreference(floatPreferencesKey("audioBalance"), 0f)
    var selectedReverb by rememberPreference(stringPreferencesKey("reverbPreset"), "None")
    var selectedAutoEqModel by rememberPreference(AutoEqHeadphoneModelKey, "None")
    var parametricEnabled by rememberPreference(ParametricEqEnabledKey, false)
    var bitPerfectEnabled by rememberPreference(BitPerfectOutputKey, false)
    var antiClippingEnabled by rememberPreference(AntiClippingLimiterKey, true)

    // Band levels (5 bands) - normalized 0 to 1
    var bandLevels by remember { mutableStateOf(customPresets.getOrElse(presetIndex) { customPresets.first() }.levels) }

    val audioSessionId = playerConnection?.player?.audioSessionId ?: 0

    // Apply audio effects when settings change
    LaunchedEffect(audioSessionId, eqEnabled, bandLevels, bassEnabled, bassStrength, virtualizerEnabled, virtualizerStrength) {
        if (audioSessionId > 0) {
            runCatching {
                val eq = Equalizer(0, audioSessionId)
                eq.enabled = eqEnabled
                if (eqEnabled) {
                    val minMb = eq.bandLevelRange[0]
                    val maxMb = eq.bandLevelRange[1]
                    val rangeMb = maxMb - minMb
                    val numBands = minOf(bandLevels.size, eq.numberOfBands.toInt())
                    for (i in 0 until numBands) {
                        val levelMb = (minMb + (bandLevels[i] * rangeMb)).toInt().toShort()
                        eq.setBandLevel(i.toShort(), levelMb)
                    }
                }
            }
            runCatching {
                val bb = BassBoost(0, audioSessionId)
                bb.enabled = bassEnabled && eqEnabled
                if (bassEnabled && eqEnabled) {
                    bb.setStrength(bassStrength.toShort())
                }
            }
            runCatching {
                val virt = Virtualizer(0, audioSessionId)
                virt.enabled = virtualizerEnabled && eqEnabled
                if (virtualizerEnabled && eqEnabled) {
                    virt.setStrength(virtualizerStrength.toShort())
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.equalizer),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = eqEnabled,
                        onCheckedChange = { eqEnabled = it },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!eqEnabled) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Equalizer is currently turned OFF. Toggle the switch at top right to enable effects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            // ═══════════════════════════════════════════════════════════════════════════
            // Interactive Frequency Response Graph
            // ═══════════════════════════════════════════════════════════════════════════
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Frequency Response",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FrequencyResponseGraph(
                        bandLevels = bandLevels,
                        onLevelChange = { index, level -> if (eqEnabled) {
                            val updated = bandLevels.toMutableList()
                            updated[index] = level
                            bandLevels = updated
                            presetIndex = -1 // custom
                        } },
                        accentColor = accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════════════════
            // Preset Chips Row
            // ═══════════════════════════════════════════════════════════════════════════
            Text(
                text = stringResource(R.string.presets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(customPresets) { index, preset ->
                    val isSelected = presetIndex == index
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            presetIndex = index
                            bandLevels = preset.levels
                        },
                        label = {
                            Text(preset.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════════════════
            // Individual Band Level Sliders
            // ═══════════════════════════════════════════════════════════════════════════
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Band Sliders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    frequencyBands.forEachIndexed { index, band ->
                        val level = bandLevels.getOrElse(index) { 0.5f }
                        val dbVal = ((level - 0.5f) * 20f).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "${band.frequency} ${band.unit}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(64.dp)
                            )
                            Slider(
                                value = level,
                                onValueChange = { newLvl ->
                                    val updated = bandLevels.toMutableList()
                                    updated[index] = newLvl
                                    bandLevels = updated
                                    presetIndex = -1
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (dbVal > 0) "+$dbVal dB" else "$dbVal dB",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════════════
            // Advanced Audio Enhancements (Bass Boost, Virtualizer, Gain, Balance)
            // ═══════════════════════════════════════════════════════════════════════════
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sound Enhancements",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Bass Boost
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.graphic_eq),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.bass_boost),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = bassEnabled,
                                onCheckedChange = { bassEnabled = it }
                            )
                        }
                        if (bassEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = bassStrength.toFloat(),
                                    onValueChange = { if (eqEnabled) bassStrength = it.toInt() },
                    enabled = eqEnabled,
                                    valueRange = 0f..1000f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${bassStrength / 10}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(44.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Virtualizer / 3D Surround
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.volume_up),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "3D Virtualizer",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Switch(
                                checked = virtualizerEnabled,
                                onCheckedChange = { virtualizerEnabled = it }
                            )
                        }
                        if (virtualizerEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Slider(
                                    value = virtualizerStrength.toFloat(),
                                    onValueChange = { if (eqEnabled) virtualizerStrength = it.toInt() },
                    enabled = eqEnabled,
                                    valueRange = 0f..1000f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${virtualizerStrength / 10}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(44.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Pre-amp / Output Gain
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pre-Amp Output Gain",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${outputGain / 100} dB",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = outputGain.toFloat(),
                            onValueChange = { outputGain = it.toInt() },
                            valueRange = -1000f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Stereo Balance (L / R)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Stereo Audio Balance",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when {
                                    audioBalance < -0.05f -> "Left (${(audioBalance * -100).toInt()}%)"
                                    audioBalance > 0.05f -> "Right (${(audioBalance * 100).toInt()}%)"
                                    else -> "Center"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = audioBalance,
                            onValueChange = { audioBalance = it },
                            valueRange = -1f..1f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ── AutoEq Headphone Profile Section ──
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.headphones),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AutoEq Headphone Matching",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Calibrate EQ curve for your specific earbuds/headphones (Harman Target)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedAutoEqModel == "None",
                                onClick = {
                                    selectedAutoEqModel = "None"
                                },
                                label = { Text("None (Manual)") }
                            )
                        }
                        itemsIndexed(autoEqProfiles) { _, profile ->
                            val fullName = "${profile.brand} ${profile.model}"
                            FilterChip(
                                selected = selectedAutoEqModel == fullName,
                                onClick = {
                                    selectedAutoEqModel = fullName
                                    bandLevels = profile.levels
                                    presetIndex = -1
                                },
                                label = { Text(fullName) }
                            )
                        }
                    }
                }
            }

            // ── Audiophile Pro & Hi-Res Output Controls ──
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Audiophile Pro Controls",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Parametric EQ Curve
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Parametric EQ Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Higher precision frequency band interpolation with smooth Q-curves",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = parametricEnabled,
                            onCheckedChange = { parametricEnabled = it }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Bit-Perfect Output
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Bit-Perfect USB DAC Output", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Bypass Android 48kHz system mixer for external Hi-Res DACs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = bitPerfectEnabled,
                            onCheckedChange = { bitPerfectEnabled = it }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Anti-Clipping Soft Limiter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Anti-Clipping Soft Limiter", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Prevents audio distortion and harsh peaks on high gain",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = antiClippingEnabled,
                            onCheckedChange = { antiClippingEnabled = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
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
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
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
            Text("+10 dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("0 dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-10 dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Graph canvas area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 52.dp, bottom = 24.dp)
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

                // Grid lines
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

                // Points along the curve
                val points = bandLevels.mapIndexed { index, level ->
                    Offset(
                        x = index * spacing,
                        y = height * (1f - level)
                    )
                }

                // Gradient fill under curve
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
                            accentColor.copy(alpha = 0.45f),
                            accentColor.copy(alpha = 0.05f)
                        )
                    )
                )

                // Stroke line
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
                    style = Stroke(width = 3.5f)
                )

                // Control points
                points.forEach { point ->
                    drawCircle(
                        color = accentColor,
                        radius = 12f,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.5f,
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
