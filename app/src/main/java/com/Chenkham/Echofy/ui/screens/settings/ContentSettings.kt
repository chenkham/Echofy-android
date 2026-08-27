package com.Chenkham.Echofy.ui.screens.settings

import com.Chenkham.Echofy.ui.component.EnumListPreference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Chenkham.Echofy.constants.HomeRow
import com.Chenkham.Echofy.constants.HomeRowOrderKey
import com.Chenkham.Echofy.constants.ListItemHeight
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.navigation.NavController
import com.Chenkham.Echofy.NotificationPermissionPreference
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.ContentCountryKey
import com.Chenkham.Echofy.constants.ContentLanguageKey
import com.Chenkham.Echofy.constants.CountryCodeToName
import com.Chenkham.Echofy.constants.EnableKugouKey
import com.Chenkham.Echofy.constants.CacheTranslationsKey
import com.Chenkham.Echofy.constants.ListeningStreakEnabledKey
import com.Chenkham.Echofy.constants.TimeMachineEnabledKey
import com.Chenkham.Echofy.constants.BecauseYouListenedEnabledKey
import com.Chenkham.Echofy.constants.MoodPlaylistsEnabledKey
import com.Chenkham.Echofy.constants.ReleaseRadarEnabledKey
import com.Chenkham.Echofy.utils.ReleaseRadarWorker
import com.Chenkham.Echofy.constants.HiddenGemsEnabledKey
import com.Chenkham.Echofy.constants.EnableLrcLibKey
import com.Chenkham.Echofy.constants.EnableLrcLibKey
import com.Chenkham.Echofy.constants.HideExplicitKey

import com.Chenkham.Echofy.constants.HistoryDuration
import com.Chenkham.Echofy.constants.LanguageCodeToName
import com.Chenkham.Echofy.constants.PreferredLyricsProvider
import com.Chenkham.Echofy.constants.PreferredLyricsProviderKey
import com.Chenkham.Echofy.constants.ProxyEnabledKey
import com.Chenkham.Echofy.constants.ProxyTypeKey
import com.Chenkham.Echofy.constants.ProxyUrlKey
import com.Chenkham.Echofy.constants.QuickPicks
import com.Chenkham.Echofy.constants.QuickPicksKey
import com.Chenkham.Echofy.constants.SYSTEM_DEFAULT
import com.Chenkham.Echofy.constants.TopSize
import com.Chenkham.Echofy.ui.component.EditTextPreference
import com.Chenkham.Echofy.ui.component.ListPreference
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SliderPreference
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import java.net.Proxy


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val (releaseRadarEnabled, onReleaseRadarEnabledChange) = rememberPreference(
        key = ReleaseRadarEnabledKey,
        defaultValue = true
    )
    val (contentLanguage, onContentLanguageChange) = rememberPreference(
        key = ContentLanguageKey,
        defaultValue = "system"
    )
    val (contentCountry, onContentCountryChange) = rememberPreference(
        key = ContentCountryKey,
        defaultValue = "system"
    )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(
        key = HideExplicitKey,
        defaultValue = false
    )
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(
        key = ProxyEnabledKey,
        defaultValue = false
    )
    val (streamClient, onStreamClientChange) = rememberEnumPreference(
        key = com.Chenkham.Echofy.constants.PlayerStreamClientKey,
        defaultValue = com.Chenkham.Echofy.constants.PlayerStreamClient.ANDROID_VR
    )
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(
        key = ProxyTypeKey,
        defaultValue = Proxy.Type.HTTP
    )
    val (proxyUrl, onProxyUrlChange) = rememberPreference(
        key = ProxyUrlKey,
        defaultValue = "host:port"
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(
        key = TopSize,
        defaultValue = "50"
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        key = HistoryDuration,
        defaultValue = 30f
    )
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(
        key = QuickPicksKey,
        defaultValue = QuickPicks.QUICK_PICKS
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(
        key = EnableKugouKey,
        defaultValue = true
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = EnableLrcLibKey,
        defaultValue = true
    )

    val (preferredProvider, onPreferredProviderChange) = rememberEnumPreference(
        key = PreferredLyricsProviderKey,
        defaultValue = PreferredLyricsProvider.LRCLIB
    )

    val (cacheTranslations, onCacheTranslationsChange) = rememberPreference(
        key = CacheTranslationsKey,
        defaultValue = true
    )
    val (listeningStreakEnabled, onListeningStreakEnabledChange) = rememberPreference(
        key = ListeningStreakEnabledKey,
        defaultValue = true
    )
    val (timeMachineEnabled, onTimeMachineEnabledChange) = rememberPreference(
        key = TimeMachineEnabledKey,
        defaultValue = true
    )
    val (hiddenGemsEnabled, onHiddenGemsEnabledChange) = rememberPreference(
        key = HiddenGemsEnabledKey,
        defaultValue = true
    )
    val (becauseYouListenedEnabled, onBecauseYouListenedEnabledChange) = rememberPreference(
        key = BecauseYouListenedEnabledKey,
        defaultValue = true
    )
    val (moodPlaylistsEnabled, onMoodPlaylistsEnabledChange) = rememberPreference(
        key = MoodPlaylistsEnabledKey,
        defaultValue = true
    )
    val (showViral50, onShowViral50Change) = rememberPreference(
        key = com.Chenkham.Echofy.constants.ShowViral50HomeKey,
        defaultValue = true
    )
    val (showTopCharts, onShowTopChartsChange) = rememberPreference(
        key = com.Chenkham.Echofy.constants.ShowTopChartsHomeKey,
        defaultValue = true
    )
    val (chartsCountry, onChartsCountryChange) = rememberPreference(
        key = com.Chenkham.Echofy.constants.ChartsCountryKey,
        defaultValue = "GLOBAL"
    )

    SettingsPage(
        title = stringResource(R.string.content),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        // Charts & Trending Feed Category
        SettingsGeneralCategory(
            title = "Charts & Trending Feed",
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Echofy Viral 50 on Home") },
                        description = { Text("Display viral & trending songs feed on Home screen") },
                        icon = { Icon(painterResource(R.drawable.trending_up), null) },
                        checked = showViral50,
                        onCheckedChange = onShowViral50Change,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Top 10 Charts on Home") },
                        description = { Text("Display official Top 10 Charts on Home screen") },
                        icon = { Icon(painterResource(R.drawable.leaderboard), null) },
                        checked = showTopCharts,
                        onCheckedChange = onShowTopChartsChange,
                    )
                },
                {
                    ListPreference(
                        title = { Text("Charts Region") },
                        description = { Text("Select Global or specific country for Charts") },
                        icon = { Icon(painterResource(R.drawable.language), null) },
                        selectedValue = chartsCountry,
                        values = listOf("GLOBAL") + CountryCodeToName.keys.toList(),
                        valueText = { if (it == "GLOBAL") "Global (Worldwide)" else (CountryCodeToName[it] ?: it) },
                        onValueSelected = onChartsCountryChange,
                    )
                }
            )
        )
        // General settings
        SettingsGeneralCategory(
            title = stringResource(R.string.general),
            items = listOf(
                {ListPreference(
                    title = { Text(stringResource(R.string.content_language)) },
                    icon = { Icon(painterResource(R.drawable.language), null) },
                    selectedValue = contentLanguage,
                    values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                    valueText = {
                        LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentLanguageChange,
                )},
                {ListPreference(
                    title = { Text(stringResource(R.string.content_country)) },
                    icon = { Icon(painterResource(R.drawable.location_on), null) },
                    selectedValue = contentCountry,
                    values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                    valueText = {
                        CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentCountryChange,
                )},

                // Hide explicit content
                {SwitchPreference(
                    title = { Text(stringResource(R.string.hide_explicit)) },
                    icon = { Icon(painterResource(R.drawable.explicit), null) },
                    checked = hideExplicit,
                    onCheckedChange = onHideExplicitChange,
                )},

                // Listen Together Feature


                {NotificationPermissionPreference()},
            )
        )

        // Streaming Client
        SettingsGeneralCategory(
            title = "Streaming Engine",
            items = listOf(
                {
                    EnumListPreference(
                        title = { Text("Playback Stream Client") },
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        selectedValue = streamClient,
                        onValueSelected = onStreamClientChange,
                        valueText = { it.name }
                    )
                }
            )
        )

        // Proxy settings
        SettingsGeneralCategory(
            title = stringResource(R.string.proxy),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_proxy)) },
                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                    checked = proxyEnabled,
                    onCheckedChange = onProxyEnabledChange,
                )},
                {if (proxyEnabled) {
                    Column {
                        ListPreference(
                            title = { Text(stringResource(R.string.proxy_type)) },
                            selectedValue = proxyType,
                            values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                            valueText = { it.name },
                            onValueSelected = onProxyTypeChange,
                        )
                        EditTextPreference(
                            title = { Text(stringResource(R.string.proxy_url)) },
                            value = proxyUrl,
                            onValueChange = onProxyUrlChange,
                        )
                    }
                }}
            )
        )

        // Lyrics settings
        SettingsGeneralCategory(
            title = stringResource(R.string.lyrics),
            items = listOf(

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_lrclib)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableLrclib,
                    onCheckedChange = onEnableLrclibChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_kugou)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableKugou,
                    onCheckedChange = onEnableKugouChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.cache_translations)) },
                    description = stringResource(R.string.cache_translations_desc),
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = cacheTranslations,
                    onCheckedChange = onCacheTranslationsChange,
                )},
            )
        )

        // Discovery rows and listening habits
        SettingsGeneralCategory(
            title = stringResource(R.string.explore),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.listening_streak)) },
                    description = stringResource(R.string.listening_streak_desc),
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    checked = listeningStreakEnabled,
                    onCheckedChange = onListeningStreakEnabledChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.time_machine)) },
                    description = stringResource(R.string.time_machine_desc),
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    checked = timeMachineEnabled,
                    onCheckedChange = onTimeMachineEnabledChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.hidden_gems)) },
                    description = stringResource(R.string.hidden_gems_desc),
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    checked = hiddenGemsEnabled,
                    onCheckedChange = onHiddenGemsEnabledChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.because_you_listened)) },
                    description = stringResource(R.string.because_you_listened_desc),
                    icon = { Icon(painterResource(R.drawable.artist), null) },
                    checked = becauseYouListenedEnabled,
                    onCheckedChange = onBecauseYouListenedEnabledChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.mood_playlists)) },
                    description = stringResource(R.string.mood_playlists_desc),
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    checked = moodPlaylistsEnabled,
                    onCheckedChange = onMoodPlaylistsEnabledChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.release_radar)) },
                    description = stringResource(R.string.release_radar_desc),
                    icon = { Icon(painterResource(R.drawable.notification_on), null) },
                    checked = releaseRadarEnabled,
                    onCheckedChange = { enabled ->
                        onReleaseRadarEnabledChange(enabled)
                        // Scheduling follows the toggle directly so turning the feature off
                        // stops the daily work rather than leaving it running silently.
                        if (enabled) {
                            ReleaseRadarWorker.schedule(context)
                        } else {
                            ReleaseRadarWorker.cancel(context)
                        }
                    },
                )},
                { HomeRowOrderPreference() },
            )
        )

        // Misc settings
        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {EditTextPreference(
                    title = { Text(stringResource(R.string.top_length)) },
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    value = lengthTop,
                    isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                    onValueChange = onLengthTopChange,
                )},
                {ListPreference(
                    title = { Text(stringResource(R.string.set_quick_picks)) },
                    icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                    selectedValue = quickPicks,
                    values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
                    valueText = {
                        when (it) {
                            QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                            QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                        }
                    },
                    onValueSelected = onQuickPicksChange,
                )},
                {SliderPreference(
                    title = { Text(stringResource(R.string.history_duration)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    value = historyDuration,
                    onValueChange = onHistoryDurationChange,
                )},
            )
        )
    }
}

/**
 * Drag-to-reorder list controlling the order discovery rows appear on Home.
 *
 * The order is persisted as a comma-separated list of [HomeRow] names rather than
 * indices, so renaming or inserting an enum constant cannot silently reshuffle a
 * user's saved layout. Whether a row appears at all stays with its own toggle above;
 * this only decides sequence.
 */
@Composable
private fun HomeRowOrderPreference() {
    val (rawOrder, onRawOrderChange) = rememberPreference(HomeRowOrderKey, "")

    val order = remember(rawOrder) {
        val stored = rawOrder.split(",")
            .mapNotNull { name -> HomeRow.entries.find { it.name == name.trim() } }
        (stored + HomeRow.entries.filterNot { it in stored }).toMutableStateList()
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        order.add(to.index, order.removeAt(from.index))
        onRawOrderChange(order.joinToString(",") { it.name })
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(R.string.home_row_order),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.home_row_order_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        // Height is bounded so the list never fights the settings page for scroll.
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight * HomeRow.entries.size),
        ) {
            items(order, key = { it.name }) { row ->
                ReorderableItem(reorderableState, key = row.name) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight),
                    ) {
                        Text(
                            text = stringResource(row.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {},
                            modifier = Modifier.draggableHandle(),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.drag_handle),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
