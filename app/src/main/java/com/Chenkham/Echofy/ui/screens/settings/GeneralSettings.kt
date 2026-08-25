package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.CrossfadeEnabledKey
import com.Chenkham.Echofy.constants.EnableListenTogetherKey
import com.Chenkham.Echofy.constants.HapticBassBeatsKey
import com.Chenkham.Echofy.constants.HardwareVolButtonSkipKey
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.constants.PlayerLayoutStyleKey
import com.Chenkham.Echofy.constants.ShakeToSkipKey
import com.Chenkham.Echofy.ui.component.EnumListPreference
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (enableListenTogether, onEnableListenTogetherChange) = rememberPreference(
        EnableListenTogetherKey,
        defaultValue = false,
    )
    val (hapticBass, onHapticBassChange) = rememberPreference(HapticBassBeatsKey, defaultValue = false)
    val (shakeToSkip, onShakeToSkipChange) = rememberPreference(ShakeToSkipKey, defaultValue = false)
    val (hwVolSkip, onHwVolSkipChange) = rememberPreference(HardwareVolButtonSkipKey, defaultValue = false)
    val (crossfade, onCrossfadeChange) = rememberPreference(CrossfadeEnabledKey, defaultValue = false)
    val (playerLayoutStyle, onPlayerLayoutStyleChange) = rememberEnumPreference(
        PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.CLASSIC,
    )

    SettingsPage(
        title = "General",
        navController = navController,
        scrollBehavior = scrollBehavior,
    ) {
        SettingsGeneralCategory(
            title = "Together & Controls",
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Together (Beta)") },
                        description = "Realtime shared playback powered by Appwrite",
                        icon = { Icon(painterResource(R.drawable.group), null) },
                        checked = enableListenTogether,
                        onCheckedChange = onEnableListenTogetherChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Haptic Bass Beats") },
                        description = "Phone vibrates gently on bass hits while playing music",
                        icon = { Icon(painterResource(R.drawable.waves), null) },
                        checked = hapticBass,
                        onCheckedChange = onHapticBassChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Shake to Skip") },
                        description = "Shake the phone to skip to the next song",
                        icon = { Icon(painterResource(R.drawable.skip_next), null) },
                        checked = shakeToSkip,
                        onCheckedChange = onShakeToSkipChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Volume Button Skip") },
                        description = "Long-press hardware volume buttons to skip tracks",
                        icon = { Icon(painterResource(R.drawable.tune), null) },
                        checked = hwVolSkip,
                        onCheckedChange = onHwVolSkipChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Seamless DJ Crossfade") },
                        description = "Smoothly blend one song into the next",
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        checked = crossfade,
                        onCheckedChange = onCrossfadeChange,
                    )
                },
                {
                    EnumListPreference(
                        title = { Text("Player Component Style") },
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        selectedValue = playerLayoutStyle,
                        onValueSelected = onPlayerLayoutStyleChange,
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
    }
}
