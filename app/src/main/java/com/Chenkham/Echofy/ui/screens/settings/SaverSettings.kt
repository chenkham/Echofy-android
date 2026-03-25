package com.Chenkham.Echofy.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Chenkham.Echofy.LocalPlayerAwareWindowInsets
import com.Chenkham.Echofy.R
import com.Chenkham.Echofy.constants.AudioQuality
import com.Chenkham.Echofy.constants.AudioQualityKey
import com.Chenkham.Echofy.constants.BatterySaverEnabledKey
import com.Chenkham.Echofy.constants.DataSaverEnabledKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.constants.VideoCacheEnabledKey
import com.Chenkham.Echofy.constants.VideoPlaybackEnabledKey
import com.Chenkham.Echofy.constants.VideoQualityKey
import com.Chenkham.Echofy.ui.component.EnumListPreference
import com.Chenkham.Echofy.ui.component.IconButton
import com.Chenkham.Echofy.ui.component.ListPreference
import com.Chenkham.Echofy.ui.component.PreferenceGroupTitle
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.ui.utils.backToMain
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaverSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // Data Saver state
    val (dataSaverEnabled, onDataSaverEnabledChange) = rememberPreference(
        DataSaverEnabledKey,
        defaultValue = false
    )
    // Note: These keys are "Enabled" keys, so we invert for "Disable" toggles
    val (videoCacheEnabled, onVideoCacheEnabledChange) = rememberPreference(
        VideoCacheEnabledKey,
        defaultValue = true
    )
    val (videoPlaybackEnabled, onVideoPlaybackEnabledChange) = rememberPreference(
        VideoPlaybackEnabledKey,
        defaultValue = true
    )
    // Battery Saver state
    val (batterySaverEnabled, onBatterySaverEnabledChange) = rememberPreference(
        BatterySaverEnabledKey,
        defaultValue = false
    )
    val (pureBlack, onPureBlackChange) = rememberPreference(
        PureBlackKey,
        defaultValue = false
    )
    
    // Expandable sections state
    var dataSaverExpanded by remember { mutableStateOf(dataSaverEnabled) }
    var batterySaverExpanded by remember { mutableStateOf(batterySaverEnabled) }

    Column(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))
        
        // Data Saver Section
        SaverCard(
            title = "Data Saver",
            description = "Reduce mobile data usage",
            icon = R.drawable.data_saver_on,
            isEnabled = dataSaverEnabled,
            isExpanded = dataSaverExpanded,
            onToggle = { enabled ->
                onDataSaverEnabledChange(enabled)
                if (enabled) {
                    dataSaverExpanded = true
                    // Apply data saver optimizations - DISABLE video cache and playback
                    onVideoCacheEnabledChange(false)  // false = disabled
                    onVideoPlaybackEnabledChange(false)  // false = disabled
                }
            },
            onExpandClick = { dataSaverExpanded = !dataSaverExpanded }
        ) {
            // Data Saver options
            // "Disable" toggle: checked = NOT enabled
            SwitchPreference(
                title = { Text("Disable Video Cache") },
                description = "Don't cache video files (saves storage)",
                icon = { Icon(painterResource(R.drawable.videocam_off), null) },
                checked = !videoCacheEnabled,  // Inverted: checked means disabled
                onCheckedChange = { disabled -> onVideoCacheEnabledChange(!disabled) }
            )
            
            SwitchPreference(
                title = { Text("Disable Video Playback") },
                description = "Only play audio, never video",
                icon = { Icon(painterResource(R.drawable.videocam_off), null) },
                checked = !videoPlaybackEnabled,
                onCheckedChange = { disabled -> onVideoPlaybackEnabledChange(!disabled) }
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Battery Saver Section
        SaverCard(
            title = "Battery Saver",
            description = "Extend battery life",
            icon = R.drawable.battery_saver,
            isEnabled = batterySaverEnabled,
            isExpanded = batterySaverExpanded,
            onToggle = { enabled ->
                onBatterySaverEnabledChange(enabled)
                if (enabled) {
                    batterySaverExpanded = true
                    // Apply battery saver optimizations
                    onPureBlackChange(true)
                    onVideoPlaybackEnabledChange(false)  // false = disabled
                }
            },
            onExpandClick = { batterySaverExpanded = !batterySaverExpanded }
        ) {
            // Battery Saver options
            SwitchPreference(
                title = { Text("Pure Black Mode (AMOLED)") },
                description = "Use true black for AMOLED screens",
                icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                checked = pureBlack,
                onCheckedChange = onPureBlackChange
            )
            
            SwitchPreference(
                title = { Text("Disable Video Playback") },
                description = "Reduces CPU usage significantly",
                icon = { Icon(painterResource(R.drawable.videocam_off), null) },
                checked = !videoPlaybackEnabled,  // Inverted: checked means disabled
                onCheckedChange = { disabled -> onVideoPlaybackEnabledChange(!disabled) }
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Tips section
        PreferenceGroupTitle(title = "Tips")
        Text(
            text = "• Data Saver reduces streaming quality and disables video\n" +
                   "• Battery Saver uses dark mode and disables intensive features\n" +
                   "• Both modes work together for maximum savings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }

    TopAppBar(
        title = { Text("Saver") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun SaverCard(
    title: String,
    description: String,
    icon: Int,
    isEnabled: Boolean,
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpandClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Toggle switch
                androidx.compose.material3.Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
                
                // Expand arrow
                Icon(
                    painter = painterResource(
                        if (isExpanded) R.drawable.expand_less else R.drawable.expand_more
                    ),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}
