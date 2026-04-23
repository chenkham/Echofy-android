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
import com.Chenkham.Echofy.constants.LiveFluidBackgroundKey
import com.Chenkham.Echofy.constants.LiveFluidColorPalette
import com.Chenkham.Echofy.constants.LiveFluidColorPaletteKey
import com.Chenkham.Echofy.constants.RealTimeVisualizerKey
import com.Chenkham.Echofy.ui.component.EnumListPreference
import com.Chenkham.Echofy.ui.component.LocalAdManager
import com.Chenkham.Echofy.ui.component.SettingsGeneralCategory
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.component.displayName
import com.Chenkham.Echofy.ui.component.prefersArtworkColors
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumVisualsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val adManager = LocalAdManager.current
    val isPremium = adManager?.isPremium?.collectAsState()?.value == true

    val (liveFluidBackground, onLiveFluidBackgroundChange) = rememberPreference(
        LiveFluidBackgroundKey,
        defaultValue = false,
    )
    val (liveFluidPalette, onLiveFluidPaletteChange) = rememberEnumPreference(
        LiveFluidColorPaletteKey,
        defaultValue = LiveFluidColorPalette.ALBUM,
    )
    val (realTimeVisualizer, onRealTimeVisualizerChange) = rememberPreference(
        RealTimeVisualizerKey,
        defaultValue = false,
    )

    SettingsPage(
        title = "Player Visuals",
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
            title = "Aesthetics",
            items = buildList {
                add {
                    SwitchPreference(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Live Fluid Background")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
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
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Real-Time Visualizer")
                                Spacer(Modifier.padding(4.dp))
                                Icon(
                                    painterResource(R.drawable.workspace_premium),
                                    contentDescription = "Premium",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        description = "Audio spectrum bars on now playing",
                        icon = { Icon(painterResource(R.drawable.equalizer), null) },
                        checked = realTimeVisualizer,
                        onCheckedChange = onRealTimeVisualizerChange,
                    )
                }
            },
        )
    }
}
