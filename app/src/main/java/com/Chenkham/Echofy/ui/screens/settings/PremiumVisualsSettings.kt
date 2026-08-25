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
        SettingsGeneralCategory(
            title = "Aesthetics",
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
    }
}
