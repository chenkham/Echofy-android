/*
 * Echofy Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.Chenkham.Echofy.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.WidgetBackgroundMode
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.widget.WidgetPreferences
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backgroundMode by WidgetPreferences.backgroundModeFlow(context)
        .collectAsStateWithLifecycle(initialValue = WidgetBackgroundMode.BLUR)
    val scrimOpacity by WidgetPreferences.scrimOpacityFlow(context)
        .collectAsStateWithLifecycle(initialValue = 0.32f)
    val cornerRadius by WidgetPreferences.cornerRadiusFlow(context)
        .collectAsStateWithLifecycle(initialValue = 24f)
    val showProgressBar by WidgetPreferences.showProgressBarFlow(context)
        .collectAsStateWithLifecycle(initialValue = true)

    val onBackgroundModeChange: (WidgetBackgroundMode) -> Unit = { mode ->
        scope.launch { WidgetPreferences.setBackgroundMode(context, mode) }
    }
    val onScrimOpacityChange: (Float) -> Unit = { value ->
        scope.launch { WidgetPreferences.setScrimOpacity(context, value) }
    }
    val onCornerRadiusChange: (Float) -> Unit = { value ->
        scope.launch { WidgetPreferences.setCornerRadius(context, value) }
    }
    val onShowProgressBarChange: (Boolean) -> Unit = { value ->
        scope.launch { WidgetPreferences.setShowProgressBar(context, value) }
    }

    val availableBackgroundModes = WidgetBackgroundMode.entries.filter {
        it != WidgetBackgroundMode.BLUR || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState()),
    ) {
        PreferenceGroupTitle(
            title = stringResource(R.string.widget_preview),
        )

        WidgetLivePreview(
            backgroundMode = backgroundMode,
            scrimOpacity = scrimOpacity,
            cornerRadius = cornerRadius.dp,
            showProgressBar = showProgressBar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.widget_background),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (mode in availableBackgroundModes) {
                WidgetBackgroundModeCard(
                    mode = mode,
                    selected = backgroundMode == mode,
                    onClick = { onBackgroundModeChange(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Text(
                text = stringResource(R.string.widget_blur_unavailable_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        WidgetSliderRow(
            title = stringResource(R.string.widget_scrim_intensity),
            value = scrimOpacity,
            valueRange = 0f..0.7f,
            valueText = { "${(it * 100).roundToInt()}%" },
            onValueChangeFinished = onScrimOpacityChange,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.widget_shape),
        )

        WidgetSliderRow(
            title = stringResource(R.string.widget_corner_radius),
            value = cornerRadius,
            valueRange = 0f..32f,
            valueText = { "${it.roundToInt()}dp" },
            onValueChangeFinished = onCornerRadiusChange,
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.widget_content),
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.widget_show_progress_bar)) },
            description = stringResource(R.string.widget_show_progress_bar_desc),
            icon = { Icon(painterResource(R.drawable.buttons), null) },
            checked = showProgressBar,
            onCheckedChange = onShowProgressBarChange,
        )
    }

    TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
        title = { Text(stringResource(R.string.widget_settings_title)) },
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
    )
}

/**
 * Vista previa en vivo del widget dentro de Settings con el nuevo diseno de la foto.
 */
@Composable
private fun WidgetLivePreview(
    backgroundMode: WidgetBackgroundMode,
    scrimOpacity: Float,
    cornerRadius: Dp,
    showProgressBar: Boolean,
    modifier: Modifier = Modifier,
) {
    val previewBackground = when (backgroundMode) {
        WidgetBackgroundMode.BLUR -> Brush.radialGradient(
            colors = listOf(Color(0xFF8B2500), Color(0xFF1E100A), Color(0xFF0F0805))
        )
        WidgetBackgroundMode.DOMINANT_COLOR -> Brush.linearGradient(
            colors = listOf(Color(0xFF6B2D1B), Color(0xFF1F120E))
        )
        WidgetBackgroundMode.SOLID -> Brush.linearGradient(
            colors = listOf(Color(0xFF22222A), Color(0xFF16161C))
        )
    }

    Box(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(previewBackground),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimOpacity)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Row: Waveform glyph
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.graphic_eq),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Middle Row: Title/Artist on Left, White Circle Play Button on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Con Altura ft. El Guincho",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "ROSALÍA",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Solid White Circular Play Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = null,
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom Row: Controls + Thin Progress Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                if (showProgressBar) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.28f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(Color.White),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    painter = painterResource(R.drawable.heart),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WidgetBackgroundModeCard(
    mode: WidgetBackgroundMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (mode) {
        WidgetBackgroundMode.BLUR -> "Blur"
        WidgetBackgroundMode.DOMINANT_COLOR -> "Dominant"
        WidgetBackgroundMode.SOLID -> "Solid"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WidgetSliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    onValueChangeFinished: (Float) -> Unit,
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(valueText(sliderValue), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChangeFinished(sliderValue) },
            valueRange = valueRange,
        )
    }
}
