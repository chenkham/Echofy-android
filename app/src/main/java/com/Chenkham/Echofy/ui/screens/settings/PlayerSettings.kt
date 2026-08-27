package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AudioNormalizationKey
import com.Chenkham.Echofy.constants.AudioQuality
import com.Chenkham.Echofy.constants.AudioQualityKey
import com.Chenkham.Echofy.constants.AutoLoadMoreKey
import com.Chenkham.Echofy.constants.AutoSkipNextOnErrorKey
import com.Chenkham.Echofy.constants.PersistentQueueKey
import com.Chenkham.Echofy.constants.PipEnabledKey
import com.Chenkham.Echofy.constants.SimilarContent
import com.Chenkham.Echofy.constants.SkipSilenceKey
import com.Chenkham.Echofy.constants.MonoAudioKey
import com.Chenkham.Echofy.constants.VocalSuppressionKey
import com.Chenkham.Echofy.constants.VolumeFadeOnPauseKey
import com.Chenkham.Echofy.constants.DynamicShortcutsEnabledKey
import com.Chenkham.Echofy.constants.AudioBalanceKey
import com.Chenkham.Echofy.constants.VolumeLimitEnabledKey
import com.Chenkham.Echofy.constants.VolumeLimitPercentKey
import com.Chenkham.Echofy.constants.LongFormMinMinutesKey
import com.Chenkham.Echofy.constants.LongFormPlaybackSpeedKey
import com.Chenkham.Echofy.constants.RememberPlaybackSettingsKey
import com.Chenkham.Echofy.constants.SilentOutroSecondsKey
import com.Chenkham.Echofy.constants.SkipSilentOutroKey
import com.Chenkham.Echofy.constants.SpeedPerContentTypeKey
import com.Chenkham.Echofy.constants.AbLoopEnabledKey
import com.Chenkham.Echofy.constants.SleepTimerFadeOutKey
import com.Chenkham.Echofy.constants.SleepTimerFadeDurationKey
import com.Chenkham.Echofy.constants.QuickSettingsTileEnabledKey
import com.Chenkham.Echofy.constants.SmartResumeEnabledKey
import com.Chenkham.Echofy.constants.SmartResumeMinMinutesKey
import com.Chenkham.Echofy.constants.ResumeOnHeadphonesKey
import com.Chenkham.Echofy.constants.StopMusicOnTaskClearKey
import com.Chenkham.Echofy.ui.component.EnumListPreference
import com.Chenkham.Echofy.ui.component.ListPreference
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.PreferenceGroupTitle
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (pipEnabled, onPipEnabledChange) = rememberPreference(
        PipEnabledKey,
        defaultValue = true
    )
    val (monoAudio, onMonoAudioChange) = rememberPreference(
        MonoAudioKey,
        defaultValue = false
    )
    val (volumeFadeOnPause, onVolumeFadeOnPauseChange) = rememberPreference(
        VolumeFadeOnPauseKey,
        defaultValue = false
    )
    val (dynamicShortcuts, onDynamicShortcutsChange) = rememberPreference(
        DynamicShortcutsEnabledKey,
        defaultValue = false
    )
    val (audioBalance, onAudioBalanceChange) = rememberPreference(
        AudioBalanceKey,
        defaultValue = 0f
    )
    val (vocalSuppression, onVocalSuppressionChange) = rememberPreference(
        VocalSuppressionKey,
        defaultValue = 0f
    )
    val (volumeLimitEnabled, onVolumeLimitEnabledChange) = rememberPreference(
        VolumeLimitEnabledKey,
        defaultValue = false
    )
    val (volumeLimitPercent, onVolumeLimitPercentChange) = rememberPreference(
        VolumeLimitPercentKey,
        defaultValue = 85
    )
    val (rememberPlaybackSettings, onRememberPlaybackSettingsChange) = rememberPreference(
        RememberPlaybackSettingsKey,
        defaultValue = false
    )
    val (abLoopEnabled, onAbLoopEnabledChange) = rememberPreference(
        AbLoopEnabledKey,
        defaultValue = false
    )
    val (sleepTimerFadeOut, onSleepTimerFadeOutChange) = rememberPreference(
        SleepTimerFadeOutKey,
        defaultValue = true
    )
    val (sleepTimerFadeDuration, onSleepTimerFadeDurationChange) = rememberPreference(
        SleepTimerFadeDurationKey,
        defaultValue = 30
    )
    val (quickTileEnabled, onQuickTileEnabledChange) = rememberPreference(
        QuickSettingsTileEnabledKey,
        defaultValue = true
    )
    val (smartResume, onSmartResumeChange) = rememberPreference(
        SmartResumeEnabledKey,
        defaultValue = false
    )
    val (smartResumeMinMinutes, onSmartResumeMinMinutesChange) = rememberPreference(
        SmartResumeMinMinutesKey,
        defaultValue = 15
    )
    val (resumeOnHeadphones, onResumeOnHeadphonesChange) = rememberPreference(
        ResumeOnHeadphonesKey,
        defaultValue = false
    )
    val (speedPerContentType, onSpeedPerContentTypeChange) = rememberPreference(
        SpeedPerContentTypeKey,
        defaultValue = false
    )
    val (longFormSpeed, onLongFormSpeedChange) = rememberPreference(
        LongFormPlaybackSpeedKey,
        defaultValue = 1.5f
    )
    val (longFormMinMinutes, onLongFormMinMinutesChange) = rememberPreference(
        LongFormMinMinutesKey,
        defaultValue = 20
    )
    val (skipSilentOutro, onSkipSilentOutroChange) = rememberPreference(
        SkipSilentOutroKey,
        defaultValue = false
    )
    val (silentOutroSeconds, onSilentOutroSecondsChange) = rememberPreference(
        SilentOutroSecondsKey,
        defaultValue = 5
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    val adManager = com.Chenkham.Echofy.ui.component.LocalAdManager.current

    SettingsPage(
        title = stringResource(R.string.player_and_audio),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.audio_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = audioQuality,
                    onValueSelected = onAudioQualityChange,
                    valueText = {
                        when (it) {
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.HIGHEST -> stringResource(R.string.audio_quality_highest)
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                        }
                    }
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.skip_silence)) },
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = skipSilence,
                    onCheckedChange = onSkipSilenceChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = audioNormalization,
                    onCheckedChange = onAudioNormalizationChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.playback_fade)) },
                    description = stringResource(R.string.playback_fade_desc),
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = volumeFadeOnPause,
                    onCheckedChange = onVolumeFadeOnPauseChange
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.queue),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = stringResource(R.string.persistent_queue_desc),
                    icon = { Icon(painterResource(R.drawable.queue_music), null) },
                    checked = persistentQueue,
                    onCheckedChange = onPersistentQueueChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    description = stringResource(R.string.auto_load_more_desc),
                    icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    checked = autoLoadMore,
                    onCheckedChange = onAutoLoadMoreChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    description = stringResource(R.string.similar_content_desc),
                    icon = { Icon(painterResource(R.drawable.similar), null) },
                    checked = similarContentEnabled,
                    onCheckedChange = similarContentEnabledChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    description = stringResource(R.string.auto_skip_next_on_error_desc),
                    icon = { Icon(painterResource(R.drawable.skip_next), null) },
                    checked = autoSkipNextOnError,
                    onCheckedChange = onAutoSkipNextOnErrorChange
                )},
                {
                    val (showAudioQualityBadge, onShowAudioQualityBadgeChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.ShowAudioQualityBadgeKey,
                        defaultValue = false
                    )
                    SwitchPreference(
                        title = { Text("Stream Quality Diagnostics Badge") },
                        description = "Display live Codec, Bitrate, and Sample Rate badge on the player",
                        icon = { Icon(painterResource(R.drawable.info), null) },
                        checked = showAudioQualityBadge,
                        onCheckedChange = onShowAudioQualityBadgeChange
                    )
                },
                {
                    val (streamCanvas, onStreamCanvasChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.StreamCanvasEnabledKey,
                        defaultValue = true
                    )
                    SwitchPreference(
                        title = { Text("Looping Video Canvas") },
                        description = "Stream animated canvas video backgrounds when available",
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = streamCanvas,
                        onCheckedChange = onStreamCanvasChange
                    )
                },
                {
                    val (transitionFinder, onTransitionFinderChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.SongTransitionFinderEnabledKey,
                        defaultValue = false
                    )
                    SwitchPreference(
                        title = { Text("DJ Transition Finder") },
                        description = "Recommend harmonically matched tracks with identical BPM & Key in player menu",
                        icon = { Icon(painterResource(R.drawable.discover_tune), null) },
                        checked = transitionFinder,
                        onCheckedChange = onTransitionFinderChange
                    )
                },
                {
                    val (waveformHeatmap, onWaveformHeatmapChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.WaveformHeatmapScrubberEnabledKey,
                        defaultValue = false
                    )
                    SwitchPreference(
                        title = { Text("Audio Frequency Heatmap Scrubber") },
                        description = "Display dynamic frequency waveform heatmap bars on progress scrubber",
                        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                        checked = waveformHeatmap,
                        onCheckedChange = onWaveformHeatmapChange
                    )
                },
                {
                    val (flipToPause, onFlipToPauseChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.FlipToPauseEnabledKey,
                        defaultValue = false
                    )
                    SwitchPreference(
                        title = { Text("Flip Phone to Pause") },
                        description = "Automatically pause playback when placing phone face-down on a surface",
                        icon = { Icon(painterResource(R.drawable.tune), null) },
                        checked = flipToPause,
                        onCheckedChange = onFlipToPauseChange
                    )
                },
                {
                    val (queueCleaner, onQueueCleanerChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.QueueDeduplicatorEnabledKey,
                        defaultValue = false
                    )
                    SwitchPreference(
                        title = { Text("Queue Deduplicator & Cleaner") },
                        description = "Show 1-tap button in queue header to remove duplicate and remastered tracks",
                        icon = { Icon(painterResource(R.drawable.clear_all), null) },
                        checked = queueCleaner,
                        onCheckedChange = onQueueCleanerChange
                    )
                },
                {
                    val (subwayPreCache, onSubwayPreCacheChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.SubwayPreCacheEnabledKey,
                        defaultValue = false
                    )
                    SwitchPreference(
                        title = { Text("Subway & Elevator Pre-Cache") },
                        description = "Automatically buffer upcoming songs in queue to prevent pauses in dead zones",
                        icon = { Icon(painterResource(R.drawable.download), null) },
                        checked = subwayPreCache,
                        onCheckedChange = onSubwayPreCacheChange
                    )
                },
                {
                    val (retroWidgetStyle, onRetroWidgetStyleChange) = rememberPreference(
                        key = com.Chenkham.Echofy.constants.RetroWidgetStyleKey,
                        defaultValue = "Default"
                    )
                    ListPreference(
                        title = { Text("Retro Widget Style") },
                        icon = { Icon(painterResource(R.drawable.album), null) },
                        selectedValue = retroWidgetStyle,
                        values = listOf("Default", "SpinningVinyl", "VintageCassette"),
                        valueText = { styleValue ->
                            when (styleValue) {
                                "SpinningVinyl" -> "💿 Spinning Vinyl Record"
                                "VintageCassette" -> "📼 Vintage Cassette Tape"
                                else -> "Standard Card"
                            }
                        },
                        onValueSelected = onRetroWidgetStyleChange
                    )
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.accessibility_audio),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.mono_audio)) },
                    description = stringResource(R.string.mono_audio_desc),
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = monoAudio,
                    onCheckedChange = onMonoAudioChange
                )},
                {Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.audio_balance),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.audio_balance_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = audioBalance,
                        onValueChange = onAudioBalanceChange,
                        valueRange = -1f..1f,
                        steps = 19,
                    )
                }},

                {Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.karaoke_mode),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.karaoke_mode_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = vocalSuppression,
                        onValueChange = onVocalSuppressionChange,
                        valueRange = 0f..1f,
                        steps = 9,
                    )
                }},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.volume_limit)) },
                    description = stringResource(R.string.volume_limit_desc),
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = volumeLimitEnabled,
                    onCheckedChange = onVolumeLimitEnabledChange
                )},

                {if (volumeLimitEnabled) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "$volumeLimitPercent%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = volumeLimitPercent.toFloat(),
                            onValueChange = { onVolumeLimitPercentChange(it.toInt()) },
                            valueRange = 10f..100f,
                        )
                    }
                }},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.tempo_and_pitch),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.per_track_audio_settings)) },
                    description = stringResource(R.string.per_track_audio_settings_desc),
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    checked = rememberPlaybackSettings,
                    onCheckedChange = onRememberPlaybackSettingsChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.ab_loop_enabled)) },
                    description = stringResource(R.string.ab_loop_enabled_desc),
                    icon = { Icon(painterResource(R.drawable.repeat), null) },
                    checked = abLoopEnabled,
                    onCheckedChange = onAbLoopEnabledChange
                )},
            )
        )

        val smartResumeItems = buildList<@Composable () -> Unit> {
            add {
                SwitchPreference(
                    title = { Text(stringResource(R.string.smart_resume)) },
                    description = stringResource(R.string.smart_resume_desc),
                    icon = { Icon(painterResource(R.drawable.play), null) },
                    checked = smartResume,
                    onCheckedChange = onSmartResumeChange
                )
            }
            if (smartResume) {
                add {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${stringResource(R.string.smart_resume_length)} " +
                                stringResource(R.string.smart_resume_minutes, smartResumeMinMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = smartResumeMinMinutes.toFloat(),
                            onValueChange = { onSmartResumeMinMinutesChange(it.toInt()) },
                            valueRange = 5f..60f,
                        )
                    }
                }
            }
            add {
                SwitchPreference(
                    title = { Text(stringResource(R.string.resume_on_headphones)) },
                    description = stringResource(R.string.resume_on_headphones_desc),
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = resumeOnHeadphones,
                    onCheckedChange = onResumeOnHeadphonesChange
                )
            }
            add {
                SwitchPreference(
                    title = { Text(stringResource(R.string.speed_per_content_type)) },
                    description = stringResource(R.string.speed_per_content_type_desc),
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = speedPerContentType,
                    onCheckedChange = onSpeedPerContentTypeChange
                )
            }
            if (speedPerContentType) {
                add {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${stringResource(R.string.long_form_speed)}: ${"%.2f".format(longFormSpeed)}x",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = longFormSpeed,
                            onValueChange = { onLongFormSpeedChange((it * 20f).toInt() / 20f) },
                            valueRange = 0.5f..3f,
                        )
                        Text(
                            text = "${stringResource(R.string.long_form_length)} " +
                                stringResource(R.string.smart_resume_minutes, longFormMinMinutes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = longFormMinMinutes.toFloat(),
                            onValueChange = { onLongFormMinMinutesChange(it.toInt()) },
                            valueRange = 5f..90f,
                        )
                    }
                }
            }
            add {
                SwitchPreference(
                    title = { Text(stringResource(R.string.skip_silent_outro)) },
                    description = stringResource(R.string.skip_silent_outro_desc),
                    icon = { Icon(painterResource(R.drawable.skip_next), null) },
                    checked = skipSilentOutro,
                    onCheckedChange = onSkipSilentOutroChange
                )
            }
            if (skipSilentOutro) {
                add {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${stringResource(R.string.silent_outro_length)}: ${silentOutroSeconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = silentOutroSeconds.toFloat(),
                            onValueChange = { onSilentOutroSecondsChange(it.toInt()) },
                            valueRange = 1f..30f,
                        )
                    }
                }
            }
        }

        SettingsGeneralCategory(
            title = stringResource(R.string.smart_resume),
            items = smartResumeItems
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.sleep_timer),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.sleep_timer_fade_out)) },
                    description = stringResource(R.string.sleep_timer_fade_out_desc),
                    icon = { Icon(painterResource(R.drawable.bedtime), null) },
                    checked = sleepTimerFadeOut,
                    onCheckedChange = onSleepTimerFadeOutChange
                )},

                {if (sleepTimerFadeOut) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "${stringResource(R.string.sleep_timer_fade_duration)}: ${sleepTimerFadeDuration}s",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Slider(
                            value = sleepTimerFadeDuration.toFloat(),
                            onValueChange = { onSleepTimerFadeDurationChange(it.toInt()) },
                            valueRange = 5f..120f,
                        )
                    }
                }},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    checked = stopMusicOnTaskClear,
                    onCheckedChange = onStopMusicOnTaskClearChange
                )},
                {SwitchPreference(
                    title = { Text("Picture-in-Picture Mode") },
                    description = "Show mini player when app is minimized",
                    icon = { Icon(painterResource(R.drawable.picture_in_picture_alt), null) },
                    checked = pipEnabled,
                    onCheckedChange = onPipEnabledChange
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.quick_settings_tile)) },
                    description = stringResource(R.string.quick_settings_tile_desc),
                    icon = { Icon(painterResource(R.drawable.play), null) },
                    checked = quickTileEnabled,
                    onCheckedChange = onQuickTileEnabledChange
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.dynamic_shortcuts)) },
                    description = stringResource(R.string.dynamic_shortcuts_desc),
                    icon = { Icon(painterResource(R.drawable.favorite), null) },
                    checked = dynamicShortcuts,
                    onCheckedChange = onDynamicShortcutsChange
                )},
            )
        )
    }
}