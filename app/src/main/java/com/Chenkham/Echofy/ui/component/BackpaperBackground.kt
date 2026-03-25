package com.Chenkham.Echofy.ui.component

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Chenkham.Echofy.constants.BackpaperApplyToExploreKey
import com.Chenkham.Echofy.constants.BackpaperApplyToHomeKey
import com.Chenkham.Echofy.constants.BackpaperApplyToLibraryKey
import com.Chenkham.Echofy.constants.BackpaperApplyToLyricsKey
import com.Chenkham.Echofy.constants.BackpaperApplyToPlayerKey
import com.Chenkham.Echofy.constants.BackpaperApplyToSearchKey
import com.Chenkham.Echofy.constants.BackpaperApplyToSettingsKey
import com.Chenkham.Echofy.constants.BackpaperBlurKey
import com.Chenkham.Echofy.constants.BackpaperBuiltInIdKey
import com.Chenkham.Echofy.constants.BackpaperCustomPathKey
import com.Chenkham.Echofy.constants.BackpaperEnabledKey
import com.Chenkham.Echofy.constants.BackpaperOpacityKey
import com.Chenkham.Echofy.constants.BackpaperScreen
import com.Chenkham.Echofy.constants.BackpaperType
import com.Chenkham.Echofy.constants.BackpaperTypeKey
import com.Chenkham.Echofy.models.BuiltInWallpapers
import com.Chenkham.Echofy.utils.dataStore
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.flow.map

/**
 * A composable that wraps content with an optional background wallpaper.
 * The wallpaper is applied based on user preferences for the specific screen.
 * 
 * @param screen The screen type to check if wallpaper should be applied
 * @param modifier Modifier for the container
 * @param content The content to display on top of the wallpaper
 */
@Composable
fun BackpaperBackground(
    screen: BackpaperScreen,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Read all preferences
    val backpaperEnabled by rememberPreference(BackpaperEnabledKey, defaultValue = false)
    val backpaperType by rememberEnumPreference(BackpaperTypeKey, BackpaperType.NONE)
    val builtInId by rememberPreference(BackpaperBuiltInIdKey, defaultValue = "")
    val customPath by rememberPreference(BackpaperCustomPathKey, defaultValue = "")
    val opacity by rememberPreference(BackpaperOpacityKey, defaultValue = 0.5f)
    val blurAmount by rememberPreference(BackpaperBlurKey, defaultValue = 0f)
    
    // Per-screen toggles
    val applyToHome by rememberPreference(BackpaperApplyToHomeKey, defaultValue = true)
    val applyToExplore by rememberPreference(BackpaperApplyToExploreKey, defaultValue = true)
    val applyToLibrary by rememberPreference(BackpaperApplyToLibraryKey, defaultValue = true)
    val applyToPlayer by rememberPreference(BackpaperApplyToPlayerKey, defaultValue = false)
    val applyToSettings by rememberPreference(BackpaperApplyToSettingsKey, defaultValue = true)
    val applyToSearch by rememberPreference(BackpaperApplyToSearchKey, defaultValue = true)
    val applyToLyrics by rememberPreference(BackpaperApplyToLyricsKey, defaultValue = false)
    
    // Check if wallpaper should be shown on this screen
    val shouldShowWallpaper = remember(
        backpaperEnabled, backpaperType, screen,
        applyToHome, applyToExplore, applyToLibrary, applyToPlayer, applyToSettings, applyToSearch
    ) {
        if (!backpaperEnabled || backpaperType == BackpaperType.NONE) {
            false
        } else {
            when (screen) {
                BackpaperScreen.HOME -> applyToHome
                BackpaperScreen.EXPLORE -> applyToExplore
                BackpaperScreen.LIBRARY -> applyToLibrary
                BackpaperScreen.PLAYER -> applyToPlayer
                BackpaperScreen.SETTINGS -> applyToSettings
                BackpaperScreen.SEARCH -> applyToSearch
                BackpaperScreen.LYRICS -> applyToLyrics
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Render wallpaper if enabled
        if (shouldShowWallpaper) {
            when (backpaperType) {
                BackpaperType.BUILT_IN -> {
                    val wallpaper = remember(builtInId) {
                        BuiltInWallpapers.getById(builtInId)
                    }
                    wallpaper?.let {
                        Image(
                            painter = painterResource(id = it.resourceId),
                            contentDescription = "Background wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    // Optimized: cap blur to 15dp max for performance
                                    if (blurAmount > 0f) Modifier.blur(minOf(blurAmount, 15f).dp)
                                    else Modifier
                                )
                                .graphicsLayer {
                                    alpha = opacity
                                }
                        )
                    }
                }
                BackpaperType.CUSTOM -> {
                    if (customPath.isNotEmpty()) {
                        AsyncImage(
                            model = Uri.parse(customPath),
                            contentDescription = "Custom background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    // Optimized: cap blur to 15dp max for performance
                                    if (blurAmount > 0f) Modifier.blur(minOf(blurAmount, 15f).dp)
                                    else Modifier
                                )
                                .graphicsLayer {
                                    alpha = opacity
                                }
                        )
                    }
                }
                BackpaperType.NONE -> { /* No wallpaper */ }
            }
        }
        
        // Content on top
        content()
    }
}
