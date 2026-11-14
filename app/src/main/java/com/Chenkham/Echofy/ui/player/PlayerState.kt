package com.Chenkham.Echofy.ui.player

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import coil.ImageLoader
import coil.request.ImageRequest
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.LiveFluidBackgroundKey
import com.Chenkham.Echofy.constants.LiveFluidColorPalette
import com.Chenkham.Echofy.constants.LiveFluidColorPaletteKey
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.constants.PlayerBackgroundStyleKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.constants.ShowLyricsKey
import com.Chenkham.Echofy.models.MediaMetadata
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.component.prefersArtworkColors
import com.Chenkham.Echofy.ui.screens.settings.DarkMode
import com.Chenkham.Echofy.ui.theme.extractGradientColors
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Stable
class PlayerDialogState {
    var showSleepTimer by mutableStateOf(false)
    var sleepTimerValue by mutableFloatStateOf(30f)
    var showChoosePlaylist by mutableStateOf(false)
    var showDetails by mutableStateOf(false)
    var showJamSheet by mutableStateOf(false)
    var showSonglink by mutableStateOf(false)
    var showChordDiagram by mutableStateOf(false)
}

@Composable
fun rememberPlayerDialogState(): PlayerDialogState = remember { PlayerDialogState() }

@Stable
class SleepTimerState(
    val enabled: Boolean,
    val timeLeft: Long,
)

@Composable
fun rememberSleepTimerState(playerConnection: PlayerConnection): SleepTimerState {
    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd,
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    return remember(sleepTimerEnabled, sleepTimerTimeLeft) {
        SleepTimerState(enabled = sleepTimerEnabled, timeLeft = sleepTimerTimeLeft)
    }
}

@Stable
class PlayerVisualState(
    val playerBackground: PlayerBackgroundStyle,
    val backgroundImageUrl: String?,
    val blurRadius: Dp,
    val backgroundAlpha: Float,
    val overlayAlpha: Float,
    val liveFluidAlpha: Float,
    val gradientColors: List<Color>,
    val onBackgroundColor: Color,
    val textBackgroundColor: Color,
    val useArtworkFluidColors: Boolean,
    val liveFluidPalette: LiveFluidColorPalette,
    val showLyrics: Boolean,
    val useBlackBackground: Boolean,
)

@Composable
fun rememberPlayerVisualState(
    mediaMetadata: MediaMetadata?,
    isExpanded: Boolean,
    context: Context = LocalContext.current,
): PlayerVisualState {
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val darkTheme by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, false)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) { if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON }
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) { useDarkTheme && pureBlack }

    val onBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
            else -> if (useDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
        }

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)
    val liveFluidBackground by rememberPreference(LiveFluidBackgroundKey, defaultValue = false)
    val liveFluidPalette by rememberEnumPreference(LiveFluidColorPaletteKey, LiveFluidColorPalette.ALBUM)
    val useArtworkFluidColors = liveFluidBackground && liveFluidPalette.prefersArtworkColors()
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }

    val blurRadius by animateDpAsState(
        targetValue = if (isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "blurRadius",
    )
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "backgroundAlpha",
    )
    val overlayAlpha by animateFloatAsState(
        targetValue =
            when {
                !isExpanded -> 0f
                playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
                playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
                else -> 0f
            },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "overlayAlpha",
    )
    val liveFluidAlpha by animateFloatAsState(
        targetValue =
            if (isExpanded && liveFluidBackground) {
                when (playerBackground) {
                    PlayerBackgroundStyle.DEFAULT -> 0.92f
                    PlayerBackgroundStyle.BLUR -> 0.42f
                    else -> 0.58f
                }
            } else {
                0f
            },
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "liveFluidAlpha",
    )

    LaunchedEffect(mediaMetadata?.thumbnailUrl, playerBackground, useBlackBackground, useArtworkFluidColors) {
        backgroundImageUrl = mediaMetadata?.thumbnailUrl
        if (useBlackBackground && !useArtworkFluidColors) {
            gradientColors = listOf(Color.Black, Color.Black)
            return@LaunchedEffect
        }
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || useArtworkFluidColors) {
            val currentUrl = mediaMetadata?.thumbnailUrl
            if (currentUrl != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val result =
                            (
                                ImageLoader(context).execute(
                                    ImageRequest.Builder(context)
                                        .data(currentUrl)
                                        .allowHardware(false)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build(),
                                ).drawable as? BitmapDrawable
                            )?.bitmap?.extractGradientColors()
                        result?.let { gradientColors = it }
                    } catch (e: Exception) {
                        // Fail silently on color extraction error
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val textBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast = if (gradientColors.size >= 2) ColorUtils.calculateContrast(gradientColors.first().toArgb(), Color.White.toArgb()) else 2.0
                val blackContrast = if (gradientColors.size >= 2) ColorUtils.calculateContrast(gradientColors.last().toArgb(), Color.Black.toArgb()) else 2.0
                if (whiteContrast < 2f && blackContrast > 2f) Color.Black else Color.White
            }
        }

    return remember(
        playerBackground,
        backgroundImageUrl,
        blurRadius,
        backgroundAlpha,
        overlayAlpha,
        liveFluidAlpha,
        gradientColors,
        onBackgroundColor,
        textBackgroundColor,
        useArtworkFluidColors,
        liveFluidPalette,
        showLyrics,
        useBlackBackground,
    ) {
        PlayerVisualState(
            playerBackground = playerBackground,
            backgroundImageUrl = backgroundImageUrl,
            blurRadius = blurRadius,
            backgroundAlpha = backgroundAlpha,
            overlayAlpha = overlayAlpha,
            liveFluidAlpha = liveFluidAlpha,
            gradientColors = gradientColors,
            onBackgroundColor = onBackgroundColor,
            textBackgroundColor = textBackgroundColor,
            useArtworkFluidColors = useArtworkFluidColors,
            liveFluidPalette = liveFluidPalette,
            showLyrics = showLyrics,
            useBlackBackground = useBlackBackground,
        )
    }
}
