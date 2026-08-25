package com.Chenkham.Echofy.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.WidgetBackgroundMode

class EchofyPlayerWidget : GlanceAppWidget() {

    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val preferences = currentState<androidx.datastore.preferences.core.Preferences>()
                val state = PlayerWidgetState.fromPreferences(preferences)

                val backgroundMode by WidgetPreferences.backgroundModeFlow(LocalContext.current)
                    .collectAsState(initial = WidgetBackgroundMode.BLUR)
                val scrimOpacity by WidgetPreferences.scrimOpacityFlow(LocalContext.current)
                    .collectAsState(initial = 0.32f)
                val cornerRadius by WidgetPreferences.cornerRadiusFlow(LocalContext.current)
                    .collectAsState(initial = 24f)
                val showProgressBar by WidgetPreferences.showProgressBarFlow(LocalContext.current)
                    .collectAsState(initial = true)

                PlayerWidgetContent(
                    state = state,
                    uiPrefs = WidgetUiPreferences(
                        backgroundMode = backgroundMode,
                        scrimOpacity = scrimOpacity,
                        cornerRadius = cornerRadius,
                        showProgressBar = showProgressBar,
                    ),
                )
            }
        }
    }
}

data class WidgetUiPreferences(
    val backgroundMode: WidgetBackgroundMode,
    val scrimOpacity: Float,
    val cornerRadius: Float,
    val showProgressBar: Boolean,
)

@SuppressLint("RestrictedApi")
@Composable
private fun PlayerWidgetContent(
    state: PlayerWidgetState,
    uiPrefs: WidgetUiPreferences,
) {
    val context = LocalContext.current
    val title = state.title.ifBlank { context.getString(R.string.app_name) }
    val artist = state.artist.ifBlank { context.getString(R.string.tap_to_open) }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(uiPrefs.cornerRadius.dp)
            .clickable(actionStartActivity(PlayerWidgetActions.openAppIntent(context))),
        contentAlignment = Alignment.Center,
    ) {
        WidgetBackground(state = state, mode = uiPrefs.backgroundMode)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black.copy(alpha = uiPrefs.scrimOpacity))),
        ) {}

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.graphic_eq),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(Color.White.copy(alpha = 0.9f))),
                    modifier = GlanceModifier.size(18.dp),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    Text(
                        text = artist,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White.copy(alpha = 0.78f)),
                            fontSize = 13.sp,
                        ),
                    )
                }

                Spacer(modifier = GlanceModifier.width(12.dp))

                PlayPauseButton(isPlaying = state.isPlaying)
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (uiPrefs.showProgressBar) {
                ProgressBar(progress = state.progress)
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(
                    icon = R.drawable.skip_previous,
                    contentDescription = "Previous",
                    enabled = state.hasPrevious,
                    action = actionRunCallback<PreviousWidgetAction>(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                ControlButton(
                    icon = R.drawable.skip_next,
                    contentDescription = "Next",
                    enabled = state.hasNext,
                    action = actionRunCallback<NextWidgetAction>(),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun WidgetBackground(state: PlayerWidgetState, mode: WidgetBackgroundMode) {
    val artwork = state.artworkBitmap
    val blur = state.backgroundBlurBitmap
    val dominant = state.dominantColor

    when (mode) {
        WidgetBackgroundMode.BLUR -> {
            when {
                blur != null -> {
                    Image(
                        provider = ImageProvider(blur),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                }
                artwork != null -> {
                    Image(
                        provider = ImageProvider(artwork),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                }
                dominant != null -> SolidBackground(ColorProvider(Color(dominant)))
                else -> SolidBackground(GlanceTheme.colors.surface)
            }
        }
        WidgetBackgroundMode.DOMINANT_COLOR -> {
            if (dominant != null) {
                SolidBackground(ColorProvider(Color(dominant)))
            } else {
                SolidBackground(GlanceTheme.colors.surface)
            }
        }
        WidgetBackgroundMode.SOLID -> {
            SolidBackground(GlanceTheme.colors.surface)
        }
    }
}

@Composable
private fun SolidBackground(color: ColorProvider) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(color),
    ) {}
}

@SuppressLint("RestrictedApi")
@Composable
private fun ProgressBar(progress: Float) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(3.dp)
            .cornerRadius(1.5.dp)
            .background(ColorProvider(Color.White.copy(alpha = 0.25f))),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(3.dp)
                .cornerRadius(1.5.dp)
                .background(ColorProvider(Color.White)),
        ) {}
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun PlayPauseButton(isPlaying: Boolean) {
    Box(
        modifier = GlanceModifier
            .size(48.dp)
            .background(ColorProvider(Color.White))
            .cornerRadius(24.dp)
            .clickable(actionRunCallback<PlayPauseWidgetAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            colorFilter = ColorFilter.tint(ColorProvider(Color.Black)),
            modifier = GlanceModifier.size(24.dp),
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun ControlButton(
    icon: Int,
    contentDescription: String,
    enabled: Boolean,
    action: Action,
) {
    val tint = if (enabled) {
        ColorProvider(Color.White)
    } else {
        ColorProvider(Color.White.copy(alpha = 0.35f))
    }
    Box(
        modifier = if (enabled) {
            GlanceModifier
                .size(32.dp)
                .clickable(action)
        } else {
            GlanceModifier.size(32.dp)
        },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(20.dp),
        )
    }
}
