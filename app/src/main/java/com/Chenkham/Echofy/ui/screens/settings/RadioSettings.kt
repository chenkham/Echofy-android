package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.CountryCodeToName
import com.Chenkham.Echofy.constants.RadioDefaultCountryKey
import com.Chenkham.Echofy.constants.RadioEnabledKey
import com.Chenkham.Echofy.constants.RadioHideBrokenKey
import com.Chenkham.Echofy.constants.RadioMinBitrateKey
import com.Chenkham.Echofy.ui.component.ListPreference
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (radioEnabled, onRadioEnabledChange) = rememberPreference(
        key = RadioEnabledKey,
        defaultValue = true
    )
    val (defaultCountry, onDefaultCountryChange) = rememberPreference(
        key = RadioDefaultCountryKey,
        defaultValue = "all"
    )
    val (minBitrate, onMinBitrateChange) = rememberPreference(
        key = RadioMinBitrateKey,
        defaultValue = "any"
    )
    val (hideBroken, onHideBrokenChange) = rememberPreference(
        key = RadioHideBrokenKey,
        defaultValue = false
    )

    SettingsPage(
        title = stringResource(R.string.radio),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.general),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Enable Radio Tab") },
                        icon = { Icon(painterResource(R.drawable.radio), null) },
                        checked = radioEnabled,
                        onCheckedChange = onRadioEnabledChange,
                    )
                },
                {
                    ListPreference(
                        title = { Text("Default Country") },
                        icon = { Icon(painterResource(R.drawable.location_on), null) },
                        selectedValue = defaultCountry,
                        values = listOf("all") + CountryCodeToName.keys.toList(),
                        valueText = {
                            if (it == "all") "All Countries" else CountryCodeToName.getOrElse(it) { it }
                        },
                        onValueSelected = onDefaultCountryChange,
                    )
                },
                {
                    ListPreference(
                        title = { Text("Minimum Bitrate") },
                        icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                        selectedValue = minBitrate,
                        values = listOf("any", "64", "128", "256"),
                        valueText = {
                            when (it) {
                                "any" -> "Any Quality"
                                else -> "$it kbps"
                            }
                        },
                        onValueSelected = onMinBitrateChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Hide Broken Stations") },
                        icon = { Icon(painterResource(R.drawable.error), null) },
                        checked = hideBroken,
                        onCheckedChange = onHideBrokenChange,
                    )
                },
            )
        )
    }
}
