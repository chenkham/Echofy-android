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
import com.Chenkham.Echofy.constants.ShakeToSkipKey
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.constants.PlayerLayoutStyleKey
import com.Chenkham.Echofy.ui.component.LocalAdManager
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeaturesSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val isPremium = true

    val (enableListenTogether, onEnableListenTogetherChange) = rememberPreference(
        com.Chenkham.Echofy.constants.EnableListenTogetherKey,
        defaultValue = false,
    )
    val (hapticBass, onHapticBassChange) = rememberPreference(HapticBassBeatsKey, defaultValue = false)
    val (shakeToSkip, onShakeToSkipChange) = rememberPreference(ShakeToSkipKey, defaultValue = false)
    val (hwVolSkip, onHwVolSkipChange) = rememberPreference(HardwareVolButtonSkipKey, defaultValue = false)
    val (crossfade, onCrossfadeChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (playerLayoutStyle, onPlayerLayoutStyleChange) = com.Chenkham.Echofy.utils.rememberEnumPreference(
        PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.CLASSIC
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
                            PremiumTitle("Together (Beta)")
                        },
                        description = "Realtime shared playback powered by Appwrite",
                        icon = { Icon(painterResource(R.drawable.group), null) },
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
                            PremiumTitle("Haptic Bass Beats")
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
                            PremiumTitle("Shake to Skip")
                        },
                        description = "Shake the phone to skip to the next song",
                        icon = { Icon(painterResource(R.drawable.skip_next), null) },
                        checked = shakeToSkip,
                        onCheckedChange = onShakeToSkipChange,
                    )
                },
                {
                    SwitchPreference(
                        title = {
                            PremiumTitle("Volume Button Skip")
                        },
                        description = "Long-press hardware volume buttons to skip tracks",
                        icon = { Icon(painterResource(R.drawable.tune), null) },
                        checked = hwVolSkip,
                        onCheckedChange = onHwVolSkipChange,
                    )
                },
                {
                    com.Chenkham.Echofy.ui.component.EnumListPreference(
                        title = { Text("Player Component Style") },
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        selectedValue = playerLayoutStyle,
                        onValueSelected = { selected ->
                            onPlayerLayoutStyleChange(selected)
                        },
                        valueText = {
                            when (it) {
                                PlayerLayoutStyle.CLASSIC -> "Default Classic Player"
                                PlayerLayoutStyle.APPLE_MUSIC -> "Modern Fluid UI Player"
                            }
                        },
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
                            PremiumTitle("Seamless DJ Crossfade")
                        },
                        description = "Smoothly blend one song into the next",
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        checked = crossfade,
                        onCheckedChange = onCrossfadeChange,
                    )
                },
            ),
        )
    }
}

@Composable
private fun PremiumTitle(text: String) {
    Text(text)
}
