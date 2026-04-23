package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.CrossfadeEnabledKey
import com.Chenkham.Echofy.constants.HapticBassBeatsKey
import com.Chenkham.Echofy.constants.HardwareVolButtonSkipKey
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.constants.PlayerLayoutStyleKey
import com.Chenkham.Echofy.constants.ShakeToSkipKey
import com.Chenkham.Echofy.constants.VisualizerEnabledKey
import com.Chenkham.Echofy.ui.component.EnumListPreference
import com.Chenkham.Echofy.ui.component.LocalAdManager
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeaturesSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val adManager = LocalAdManager.current
    val isPremium = adManager?.isPremium?.collectAsState()?.value == true

    val (enableListenTogether, onEnableListenTogetherChange) = rememberPreference(
        com.Chenkham.Echofy.constants.EnableListenTogetherKey,
        defaultValue = false,
    )
    val (hapticBass, onHapticBassChange) = rememberPreference(HapticBassBeatsKey, defaultValue = false)
    val (shakeToSkip, onShakeToSkipChange) = rememberPreference(ShakeToSkipKey, defaultValue = false)
    val (hwVolSkip, onHwVolSkipChange) = rememberPreference(HardwareVolButtonSkipKey, defaultValue = false)
    val (crossfade, onCrossfadeChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (visualizer, onVisualizerChange) = rememberPreference(VisualizerEnabledKey, defaultValue = true)
    val (playerLayout, onPlayerLayoutChange) = rememberEnumPreference(
        PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.CLASSIC,
    )

    SettingsPage(
        title = "Together & Controls",
        navController = navController,
        scrollBehavior = scrollBehavior,
    ) {
        if (!isPremium) {
            androidx.compose.material3.Card(
                modifier = Modifier.padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.workspace_premium),
                        contentDescription = "Pro",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    androidx.compose.material3.Button(onClick = { navController.navigate("premium") }) {
                        Text("Unlock Pro")
                    }
                }
            }
            return@SettingsPage
        }

        SettingsGeneralCategory(
            title = "Together",
            items = listOf(
                {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Together (Beta)")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Realtime shared playback powered by Appwrite",
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        checked = enableListenTogether,
                        onCheckedChange = onEnableListenTogetherChange,
                    )
                },
            ),
        )

        Spacer(Modifier.padding(8.dp))

        SettingsGeneralCategory(
            title = "Gesture & Sensor Controls",
            items = listOf(
                {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Haptic Bass Beats")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Phone vibrates gently on bass hits while playing music",
                        icon = { Icon(painterResource(R.drawable.waves), null) },
                        checked = hapticBass,
                        onCheckedChange = onHapticBassChange,
                    )
                },
                {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Shake to Skip")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Shake your phone to skip to the next song",
                        icon = { Icon(painterResource(R.drawable.skip_next), null) },
                        checked = shakeToSkip,
                        onCheckedChange = onShakeToSkipChange,
                    )
                },
                {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Volume Button Skip")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Long-press hardware volume buttons to skip tracks",
                        icon = { Icon(painterResource(R.drawable.tune), null) },
                        checked = hwVolSkip,
                        onCheckedChange = onHwVolSkipChange,
                    )
                },
            ),
        )

        Spacer(Modifier.padding(8.dp))

        SettingsGeneralCategory(
            title = "Playback Enhancements",
            items = listOf(
                {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Seamless DJ Crossfade")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Smoothly blend the end of one song into the beginning of another",
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        checked = crossfade,
                        onCheckedChange = onCrossfadeChange,
                    )
                },
                {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Real-Time Audio Visualizer")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Show dynamic pulsing sound waves when music is playing",
                        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                        checked = visualizer,
                        onCheckedChange = onVisualizerChange,
                    )
                },
            ),
        )

        Spacer(Modifier.padding(8.dp))

        SettingsGeneralCategory(
            title = "Premium Interface",
            items = listOf(
                {
                    EnumListPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Player Layout Style")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        selectedValue = playerLayout,
                        onValueSelected = onPlayerLayoutChange,
                        valueText = {
                            when (it) {
                                PlayerLayoutStyle.CLASSIC -> "Classic (Default)"
                                PlayerLayoutStyle.APPLE_MUSIC -> "Apple Music Style"
                            }
                        },
                        values = enumValues<PlayerLayoutStyle>().toList(),
                    )
                },
            ),
        )
    }
}
