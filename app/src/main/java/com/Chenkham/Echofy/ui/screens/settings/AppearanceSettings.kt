package com.Chenkham.Echofy.ui.screens.settings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AnimateLyricsKey
import com.Chenkham.Echofy.constants.ChipSortTypeKey
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.DefaultMiniPlayerThumbnailShape
import com.Chenkham.Echofy.constants.DefaultOpenTabKey
import com.Chenkham.Echofy.constants.DefaultPlayPauseButtonShape
import com.Chenkham.Echofy.constants.DefaultSmallButtonsShape
import com.Chenkham.Echofy.constants.DynamicThemeKey
import com.Chenkham.Echofy.constants.GridItemSize
import com.Chenkham.Echofy.constants.GridItemsSizeKey
import com.Chenkham.Echofy.constants.LibraryFilter
import com.Chenkham.Echofy.constants.LyricsClickKey
import com.Chenkham.Echofy.constants.LyricsTextPositionKey
import com.Chenkham.Echofy.constants.MiniPlayerThumbnailShapeKey
import com.Chenkham.Echofy.constants.PlayPauseButtonShapeKey
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.constants.PlayerBackgroundStyleKey
import com.Chenkham.Echofy.constants.PlayerButtonsStyle
import com.Chenkham.Echofy.constants.PlayerButtonsStyleKey
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.constants.PlayerLayoutStyleKey
import com.Chenkham.Echofy.constants.SeasonalWallpaper
import com.Chenkham.Echofy.constants.SeasonalWallpaperKey
import com.Chenkham.Echofy.constants.PlayerTextAlignmentKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.constants.RotateBackgroundKey
import com.Chenkham.Echofy.constants.SliderStyle
import com.Chenkham.Echofy.constants.SliderStyleKey
import com.Chenkham.Echofy.constants.SlimNavBarKey
import com.Chenkham.Echofy.constants.SmallButtonsShapeKey
import com.Chenkham.Echofy.constants.SwipeThumbnailKey
import com.Chenkham.Echofy.constants.PlaybackMode
import com.Chenkham.Echofy.constants.PlaybackModeKey
import com.Chenkham.Echofy.ui.component.AvatarSelector
import com.Chenkham.Echofy.ui.component.DefaultDialog
import com.Chenkham.Echofy.ui.component.EnumListPreference
import com.Chenkham.Echofy.ui.component.LanguagePreference
import com.Chenkham.Echofy.ui.component.ListPreference
import com.Chenkham.Echofy.ui.component.PlayerSliderTrack
import com.Chenkham.Echofy.ui.component.PreferenceEntry
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.component.ThumbnailCornerRadiusSelectorButton
import com.Chenkham.Echofy.ui.component.UnifiedShapeSelectorButton
import com.Chenkham.Echofy.ui.component.displayName
import com.Chenkham.Echofy.ui.component.prefersArtworkColors
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import timber.log.Timber

import com.Chenkham.Echofy.constants.MiniPlayerStyle
import com.Chenkham.Echofy.constants.MiniPlayerStyleKey
import com.Chenkham.Echofy.ui.component.LocalAdManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (playerTextAlignment, onPlayerTextAlignmentChange) =
        rememberEnumPreference(
            PlayerTextAlignmentKey,
            defaultValue = PlayerTextAlignment.SIDED,
        )

    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val (miniPlayerStyle, onMiniPlayerStyleChange) = rememberEnumPreference(
        MiniPlayerStyleKey,
        defaultValue = MiniPlayerStyle.Slim
    )


    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (liveFluidBackground, onLiveFluidBackgroundChange) = rememberPreference(
        com.Chenkham.Echofy.constants.LiveFluidBackgroundKey,
        defaultValue = false,
    )
    val (reduceMotion, onReduceMotionChange) = rememberPreference(
        com.Chenkham.Echofy.constants.ReduceMotionKey,
        defaultValue = false,
    )
    val (appFont, onAppFontChange) = rememberEnumPreference(
        com.Chenkham.Echofy.constants.CustomFontKey,
        defaultValue = com.Chenkham.Echofy.constants.AppFont.SYSTEM,
    )
    var appIcon by remember {
        mutableStateOf(com.Chenkham.Echofy.utils.AppIconManager.currentIcon(context))
    }
    val (highContrastLyrics, onHighContrastLyricsChange) = rememberPreference(
        com.Chenkham.Echofy.constants.HighContrastLyricsKey,
        defaultValue = false
    )
    val (listeningReminderEnabled, onListeningReminderEnabledChange) = rememberPreference(
        com.Chenkham.Echofy.constants.ListeningReminderEnabledKey,
        defaultValue = false
    )
    val (listeningReminderMinutes, onListeningReminderMinutesChange) = rememberPreference(
        com.Chenkham.Echofy.constants.ListeningReminderMinutesKey,
        defaultValue = 60
    )
    val (liveFluidPalette, onLiveFluidPaletteChange) = rememberEnumPreference(
        com.Chenkham.Echofy.constants.LiveFluidColorPaletteKey,
        defaultValue = com.Chenkham.Echofy.constants.LiveFluidColorPalette.ALBUM,
    )
    val (realTimeVisualizer, onRealTimeVisualizerChange) = rememberPreference(
        com.Chenkham.Echofy.constants.RealTimeVisualizerKey,
        defaultValue = false,
    )
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.SLIM
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (doubleTapSeek, onDoubleTapSeekChange) = rememberPreference(
        com.Chenkham.Echofy.constants.DoubleTapSeekKey,
        defaultValue = false
    )
    val (doubleTapSeekSeconds, onDoubleTapSeekSecondsChange) = rememberPreference(
        com.Chenkham.Echofy.constants.DoubleTapSeekSecondsKey,
        defaultValue = 10
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )
    val (animateLyrics, onAnimateLyricsChange) = rememberPreference(
        AnimateLyricsKey,
        defaultValue = true
    )

    val (rotateBackground, onRotateBackgroundChange) = rememberPreference(
        key = RotateBackgroundKey,
        defaultValue = false
    )

    // Estados de formas
    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )

    val (miniPlayerThumbnailShape, onMiniPlayerThumbnailShapeChange) = rememberPreference(
        key = MiniPlayerThumbnailShapeKey,
        defaultValue = DefaultMiniPlayerThumbnailShape
    )

    val (seasonalWallpaper, onSeasonalWallpaperChange) = rememberEnumPreference(
        key = SeasonalWallpaperKey,
        defaultValue = SeasonalWallpaper.OFF
    )

    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)

    val (playbackMode, onPlaybackModeChange) = rememberEnumPreference(
        key = PlaybackModeKey,
        defaultValue = PlaybackMode.AUDIO
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    // Automatically disable pureBlack when switching to light mode
    LaunchedEffect(useDarkTheme) {
        if (!useDarkTheme && pureBlack) {
            onPureBlackChange(false)
        }
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.YOUTUBE_MUSIC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.YOUTUBE_MUSIC)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = sliderState.value)
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = "YT Music",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.VINTAGE_CABLE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.VINTAGE_CABLE)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = {
                            com.Chenkham.Echofy.ui.player.VintageCableCarThumb(
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(width = 24.dp, height = 18.dp)
                            )
                        },
                        track = {
                            Box(
                                modifier = Modifier
                                    .height(2.5.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = sliderValue)
                                        .background(MaterialTheme.colorScheme.primary)
                                    )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = "Vintage Cable",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
    }


    SettingsPage(
        title = stringResource(R.string.appearance),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.theme),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    checked = dynamicTheme,
                    onCheckedChange = onDynamicThemeChange,
                )},
                {EnumListPreference(
                    title = { Text(stringResource(R.string.dark_theme)) },
                    icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                    selectedValue = darkMode,
                    onValueSelected = onDarkModeChange,
                    valueText = {
                        when (it) {
                            DarkMode.ON -> stringResource(R.string.dark_theme_on)
                            DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                            DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                        }
                    },
                )},
                {AnimatedVisibility(useDarkTheme) {
                    SwitchPreference(
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.pure_black))
                            }
                        },
                        icon = { Icon(painterResource(R.drawable.contrast), null) },
                        checked = pureBlack && useDarkTheme,
                        onCheckedChange = { newValue ->
                            if (useDarkTheme) {
                                onPureBlackChange(newValue)
                            }
                        },
                        isEnabled = useDarkTheme
                    )
                }},
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.backpaper)) },
                        description = stringResource(R.string.enable_backpaper_desc),
                        icon = { Icon(painterResource(R.drawable.wallpaper), null) },
                        onClick = { navController.navigate("settings/backpaper") }
                    )
                }
            )
        )

        // Language preferences
        SettingsGeneralCategory(
            title = stringResource(R.string.app_language),
            items = listOf(
                { LanguagePreference() }
            )
        )

        // Determine the options available based on the Android version
        val availableBackgroundStyles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enumValues<PlayerBackgroundStyle>().toList()
        } else {
            enumValues<PlayerBackgroundStyle>().filter {
                it != PlayerBackgroundStyle.BLUR
            }
        }

        // Also ensure that the selected value is compatible.
        val safeSelectedValue = if (playerBackground == PlayerBackgroundStyle.BLUR &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            PlayerBackgroundStyle.DEFAULT
        } else {
            playerBackground
        }

        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {
                    ListPreference(
                        title = { Text(stringResource(R.string.player_background_style)) },
                        icon = { Icon(painterResource(R.drawable.gradient), null) },
                        selectedValue = safeSelectedValue,
                        values = availableBackgroundStyles,
                        valueText = {
                            when (it) {
                                PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                PlayerBackgroundStyle.COLORING -> "Coloring"
                                PlayerBackgroundStyle.BLUR_GRADIENT -> "Blur & Gradient"
                                PlayerBackgroundStyle.GLOW -> "Aura Glow"
                                PlayerBackgroundStyle.GLOW_ANIMATED -> "Animated Glow"
                                PlayerBackgroundStyle.CUSTOM -> "Custom Artwork" 
                            }
                        },
                        onValueSelected = onPlayerBackgroundChange,
                    )
                },

                {ThumbnailCornerRadiusSelectorButton(
                    onRadiusSelected = { selectedRadius ->
                        Timber.tag("Thumbnail").d("Selected radio: $selectedRadius")
                    }
                )},

                // En tu lista de items del Player, simplemente usa:
                {
                    UnifiedShapeSelectorButton(
                        smallButtonsShape = smallButtonsShapeState.value,
                        playPauseShape = playPauseShapeState.value,
                        miniPlayerShape = miniPlayerThumbnailShape,
                        onSmallButtonsShapeSelected = { newShape: String ->
                            smallButtonsShapeState.value = newShape
                        },
                        onPlayPauseShapeSelected = { newShape: String ->
                            playPauseShapeState.value = newShape
                        },
                        onMiniPlayerShapeSelected = { newShape: String ->
                            onMiniPlayerThumbnailShapeChange(newShape)
                        }
                    )
                },


                {EnumListPreference(
                    title = { Text(stringResource(R.string.mini_player_style)) },
                    icon = { Icon(painterResource(R.drawable.picture_in_picture_alt), null) },
                    selectedValue = miniPlayerStyle,
                    onValueSelected = { onMiniPlayerStyleChange(it) },
                    valueText = {
                        when (it) {
                            MiniPlayerStyle.Floating -> stringResource(R.string.floating)
                            MiniPlayerStyle.Slim -> stringResource(R.string.slim)
                        }
                    },
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_buttons_style)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    selectedValue = playerButtonsStyle,
                    onValueSelected = onPlayerButtonsStyleChange,
                    valueText = {
                        when (it) {
                            PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                            PlayerButtonsStyle.SECONDARY -> stringResource(R.string.secondary_color_style)
                        }
                    },
                )},

                {PreferenceEntry(
                    title = { Text(stringResource(R.string.player_slider_style)) },
                    description =
                        when (sliderStyle) {
                            SliderStyle.DEFAULT -> stringResource(R.string.default_)
                            SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                            SliderStyle.SLIM -> stringResource(R.string.slim)
                            SliderStyle.YOUTUBE_MUSIC -> "YT Music"
                            SliderStyle.VINTAGE_CABLE -> "Vintage Cable Car"
                        },
                    icon = { Icon(painterResource(R.drawable.sliders), null) },
                    onClick = {
                        showSliderOptionDialog = true
                    },
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                    icon = { Icon(painterResource(R.drawable.swipe), null) },
                    checked = swipeThumbnail,
                    onCheckedChange = onSwipeThumbnailChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.double_tap_seek)) },
                    description = stringResource(R.string.double_tap_seek_desc),
                    icon = { Icon(painterResource(R.drawable.swipe), null) },
                    checked = doubleTapSeek,
                    onCheckedChange = onDoubleTapSeekChange,
                )},

                {
                    AnimatedVisibility(visible = doubleTapSeek) {
                        Column {
                            Text(
                                text = "${stringResource(R.string.double_tap_seek_amount)}: ${doubleTapSeekSeconds}s",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Slider(
                                value = doubleTapSeekSeconds.toFloat(),
                                onValueChange = { onDoubleTapSeekSecondsChange(it.toInt()) },
                                valueRange = 5f..30f,
                                steps = 4,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                },

                {EnumListPreference(
                    title = { Text(stringResource(R.string.playback_mode)) },
                    icon = { Icon(painterResource(R.drawable.slow_motion_video), null) },
                    selectedValue = playbackMode,
                    onValueSelected = onPlaybackModeChange,
                    valueText = {
                        when (it) {
                            PlaybackMode.AUDIO -> stringResource(R.string.playback_mode_audio)
                            PlaybackMode.VIDEO -> stringResource(R.string.playback_mode_video)
                        }
                    },
                )},



                {SwitchPreference(
                    title = { Text(stringResource(R.string.Rotatelyricsbackground)) },
                    description = null,
                    icon = { Icon(painterResource(R.drawable.album), null) },
                    checked = rotateBackground,
                    onCheckedChange = onRotateBackgroundChange
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_text_alignment)) },
                    icon = {
                        Icon(
                            painter =
                                painterResource(
                                    when (playerTextAlignment) {
                                        PlayerTextAlignment.CENTER -> R.drawable.format_align_center
                                        PlayerTextAlignment.SIDED -> R.drawable.format_align_left
                                    },
                                ),
                            contentDescription = null,
                        )
                    },
                    selectedValue = playerTextAlignment,
                    onValueSelected = onPlayerTextAlignmentChange,
                    valueText = {
                        when (it) {
                            PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                            PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                        }
                    },
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    selectedValue = lyricsPosition,
                    onValueSelected = onLyricsPositionChange,
                    valueText = {
                        when (it) {
                            LyricsPosition.LEFT -> stringResource(R.string.left)
                            LyricsPosition.CENTER -> stringResource(R.string.center)
                            LyricsPosition.RIGHT -> stringResource(R.string.right)
                        }
                    },
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = lyricsClick,
                    onCheckedChange = onLyricsClickChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.animate_lyrics)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    description = stringResource(R.string.animate_lyrics_desc),
                    checked = animateLyrics,
                    onCheckedChange = onAnimateLyricsChange
                )}
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    selectedValue = defaultOpenTab,
                    onValueSelected = onDefaultOpenTabChange,
                    valueText = {
                        when (it) {
                            NavigationTab.HOME -> stringResource(R.string.home)
                            NavigationTab.EXPLORE -> stringResource(R.string.explore)
                            NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                )},

                {ListPreference(
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    icon = { Icon(painterResource(R.drawable.tab), null) },
                    selectedValue = defaultChip,
                    values = listOf(
                        LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                        LibraryFilter.ALBUMS, LibraryFilter.ARTISTS
                    ),
                    valueText = {
                        when (it) {
                            LibraryFilter.SONGS -> stringResource(R.string.songs)
                            LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                            LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                            LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                            LibraryFilter.DEVICE -> "Device"
                            LibraryFilter.SPOTIFY -> "Spotify"
                            LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                    onValueSelected = onDefaultChipChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.slim_navbar)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    checked = slimNav,
                    onCheckedChange = onSlimNavChange
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    icon = { Icon(painterResource(R.drawable.grid_view), null) },
                    selectedValue = gridItemSize,
                    onValueSelected = onGridItemSizeChange,
                    valueText = {
                        when (it) {
                            GridItemSize.SMALL -> stringResource(R.string.small)
                            GridItemSize.BIG -> stringResource(R.string.big)
                        }
                    },
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.app_font)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    selectedValue = appFont,
                    onValueSelected = onAppFontChange,
                    valueText = {
                        when (it) {
                            com.Chenkham.Echofy.constants.AppFont.SYSTEM -> stringResource(R.string.font_system)
                            com.Chenkham.Echofy.constants.AppFont.LINOTTE -> "Linotte"
                            com.Chenkham.Echofy.constants.AppFont.POPPINS -> "Poppins"
                            com.Chenkham.Echofy.constants.AppFont.SF_PRO -> "SF Pro Display"
                            com.Chenkham.Echofy.constants.AppFont.ANYBODY -> "Anybody"
                            com.Chenkham.Echofy.constants.AppFont.SANS_SERIF -> stringResource(R.string.font_sans_serif)
                            com.Chenkham.Echofy.constants.AppFont.SERIF -> stringResource(R.string.font_serif)
                            com.Chenkham.Echofy.constants.AppFont.MONOSPACE -> stringResource(R.string.font_monospace)
                            com.Chenkham.Echofy.constants.AppFont.CURSIVE -> stringResource(R.string.font_cursive)
                        }
                    },
                )},

                {ListPreference(
                    title = { Text(stringResource(R.string.app_icon)) },
                    icon = { Icon(painterResource(R.drawable.image), null) },
                    selectedValue = appIcon,
                    values = com.Chenkham.Echofy.constants.AppIcon.entries,
                    valueText = {
                        when (it) {
                            com.Chenkham.Echofy.constants.AppIcon.DEFAULT -> stringResource(R.string.app_icon_default)
                            com.Chenkham.Echofy.constants.AppIcon.CLASSIC -> stringResource(R.string.app_icon_classic)
                            com.Chenkham.Echofy.constants.AppIcon.MONOCHROME -> stringResource(R.string.app_icon_monochrome)
                        }
                    },
                    onValueSelected = {
                        appIcon = it
                        com.Chenkham.Echofy.utils.AppIconManager.applyIcon(context, it)
                    },
                )},
            )
        )

        Spacer(Modifier.padding(8.dp))

        SettingsGeneralCategory(
            title = stringResource(R.string.accessibility_audio),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.reduce_motion)) },
                    description = stringResource(R.string.reduce_motion_desc),
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    checked = reduceMotion,
                    onCheckedChange = onReduceMotionChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.high_contrast_lyrics)) },
                    description = stringResource(R.string.high_contrast_lyrics_desc),
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = highContrastLyrics,
                    onCheckedChange = onHighContrastLyricsChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.listening_reminder)) },
                    description = stringResource(R.string.listening_reminder_desc),
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = listeningReminderEnabled,
                    onCheckedChange = onListeningReminderEnabledChange,
                )},
                {
                    AnimatedVisibility(visible = listeningReminderEnabled) {
                        Column {
                            Text(
                                text = "${stringResource(R.string.listening_reminder_after)}: $listeningReminderMinutes min",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                            )
                            Slider(
                                value = listeningReminderMinutes.toFloat(),
                                onValueChange = { onListeningReminderMinutesChange(it.toInt()) },
                                valueRange = 15f..180f,
                                steps = 10,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                },
            )
        )

        SettingsGeneralCategory(
            title = "Player Visuals",
            items = buildList {
                add {
                    SwitchPreference(
                        title = { Text("Live Fluid Background") },
                        description = "Animated flowing color meshes on player",
                        icon = { Icon(painterResource(R.drawable.palette), null) },
                        checked = liveFluidBackground,
                        onCheckedChange = onLiveFluidBackgroundChange,
                    )
                }
                if (liveFluidBackground) {
                    add {
                        EnumListPreference(
                            title = { Text("Fluid Color Palette") },
                            icon = { Icon(painterResource(R.drawable.palette), null) },
                            selectedValue = liveFluidPalette,
                            valueText = { palette ->
                                if (palette.prefersArtworkColors()) {
                                    "${palette.displayName()} (matches current artwork)"
                                } else {
                                    palette.displayName()
                                }
                            },
                            onValueSelected = onLiveFluidPaletteChange,
                        )
                    }
                }
                add {
                    SwitchPreference(
                        title = { Text("Real-Time Visualizer") },
                        description = "Audio spectrum bars on now playing",
                        icon = { Icon(painterResource(R.drawable.equalizer), null) },
                        checked = realTimeVisualizer,
                        onCheckedChange = onRealTimeVisualizerChange,
                    )
                }
            },
        )

        // New avatar selector
        AvatarSelector(modifier = Modifier.padding(vertical = 8.dp))
    }
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    EXPLORE,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

