package com.Chenkham.Echofy.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.Chenkham.Echofy.R
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
import com.Chenkham.Echofy.constants.BackpaperTypeKey
import com.Chenkham.Echofy.constants.BackpaperType
import com.Chenkham.Echofy.constants.WallpaperCategory
import com.Chenkham.Echofy.models.BuiltInWallpaper
import com.Chenkham.Echofy.models.BuiltInWallpapers
import com.Chenkham.Echofy.ui.component.SettingsPage
import com.Chenkham.Echofy.ui.component.SwitchPreference
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackpaperSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    
    // Preferences
    val (backpaperEnabled, onBackpaperEnabledChange) = rememberPreference(BackpaperEnabledKey, defaultValue = false)
    val (backpaperType, onBackpaperTypeChange) = rememberEnumPreference(BackpaperTypeKey, BackpaperType.NONE)
    val (builtInId, onBuiltInIdChange) = rememberPreference(BackpaperBuiltInIdKey, defaultValue = "")
    val (customPath, onCustomPathChange) = rememberPreference(BackpaperCustomPathKey, defaultValue = "")
    val (opacity, onOpacityChange) = rememberPreference(BackpaperOpacityKey, defaultValue = 0.5f)
    val (blurAmount, onBlurAmountChange) = rememberPreference(BackpaperBlurKey, defaultValue = 0f)
    
    // Per-screen toggles
    val (applyToHome, onApplyToHomeChange) = rememberPreference(BackpaperApplyToHomeKey, defaultValue = true)
    val (applyToExplore, onApplyToExploreChange) = rememberPreference(BackpaperApplyToExploreKey, defaultValue = true)
    val (applyToLibrary, onApplyToLibraryChange) = rememberPreference(BackpaperApplyToLibraryKey, defaultValue = true)
    val (applyToPlayer, onApplyToPlayerChange) = rememberPreference(BackpaperApplyToPlayerKey, defaultValue = false)

    val (applyToLyrics, onApplyToLyricsChange) = rememberPreference(BackpaperApplyToLyricsKey, defaultValue = false)
    val (applyToSettings, onApplyToSettingsChange) = rememberPreference(BackpaperApplyToSettingsKey, defaultValue = true)
    val (applyToSearch, onApplyToSearchChange) = rememberPreference(BackpaperApplyToSearchKey, defaultValue = true)
    
    // Image picker with cropping
    val imageCropperLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            val uriFilePath = result.getUriFilePath(context) // Get path from cropper result
            
            if (uriContent != null) {
                // Save the cropped image path
                onCustomPathChange(uriContent.toString())
                onBackpaperTypeChange(BackpaperType.CUSTOM)
                if (!backpaperEnabled) {
                    onBackpaperEnabledChange(true)
                }
            }
        } else {
             // Handle error if needed (result.error)
        }
    }
    
    SettingsPage(
        title = stringResource(R.string.backpaper),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        // Enable/Disable toggle
        SwitchPreference(
            title = { Text(stringResource(R.string.enable_backpaper)) },
            description = stringResource(R.string.enable_backpaper_desc),
            icon = { Icon(painterResource(R.drawable.wallpaper), null) },
            checked = backpaperEnabled,
            onCheckedChange = onBackpaperEnabledChange
        )
        
        if (backpaperEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preview
            Text(
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when (backpaperType) {
                    BackpaperType.BUILT_IN -> {
                        BuiltInWallpapers.getById(builtInId)?.let { wallpaper ->
                            Image(
                                painter = painterResource(id = wallpaper.resourceId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (blurAmount > 0f) Modifier.blur(blurAmount.dp)
                                        else Modifier
                                    )
                                    .graphicsLayer { alpha = opacity }
                            )
                        }
                    }
                    BackpaperType.CUSTOM -> {
                        if (customPath.isNotEmpty()) {
                            AsyncImage(
                                model = Uri.parse(customPath),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (blurAmount > 0f) Modifier.blur(blurAmount.dp)
                                        else Modifier
                                    )
                                    .graphicsLayer { alpha = opacity }
                            )
                        }
                    }
                    BackpaperType.NONE -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = stringResource(R.string.no_wallpaper_selected),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Adjustments
            Text(
                text = stringResource(R.string.adjustments),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Opacity slider
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.opacity))
                    Text("${(opacity * 100).toInt()}%")
                }
                Slider(
                    value = opacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.1f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
            
            // Blur slider
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.blur))
                    Text("${blurAmount.toInt()}dp")
                }
                Slider(
                    value = blurAmount,
                    onValueChange = onBlurAmountChange,
                    valueRange = 0f..30f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Built-in wallpapers
            Text(
                text = stringResource(R.string.built_in_wallpapers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(BuiltInWallpapers.all) { wallpaper ->
                    WallpaperGridItem(
                        wallpaper = wallpaper,
                        isSelected = backpaperType == BackpaperType.BUILT_IN && builtInId == wallpaper.id,
                        onClick = {
                            onBuiltInIdChange(wallpaper.id)
                            onBackpaperTypeChange(BackpaperType.BUILT_IN)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Custom photo button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { 
                        // Launch cropper
                        val options = CropImageContractOptions(
                            uri = null, // null means pick image from gallery first
                            cropImageOptions = CropImageOptions(
                                imageSourceIncludeGallery = true,
                                imageSourceIncludeCamera = false,
                                outputCompressFormat = android.graphics.Bitmap.CompressFormat.PNG,
                                guidelines = CropImageView.Guidelines.ON,
                                fixAspectRatio = false 
                            )
                        )
                        imageCropperLauncher.launch(options)
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add_photo_alternate),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.choose_custom_photo),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Apply to screens
            Text(
                text = stringResource(R.string.apply_to_screens),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            SwitchPreference(
                title = { Text(stringResource(R.string.home)) },
                checked = applyToHome,
                onCheckedChange = onApplyToHomeChange
            )
            SwitchPreference(
                title = { Text(stringResource(R.string.explore)) },
                checked = applyToExplore,
                onCheckedChange = onApplyToExploreChange
            )
            SwitchPreference(
                title = { Text(stringResource(R.string.library)) },
                checked = applyToLibrary,
                onCheckedChange = onApplyToLibraryChange
            )
            SwitchPreference(
                title = { Text(stringResource(R.string.player)) },
                checked = applyToPlayer,
                onCheckedChange = onApplyToPlayerChange
            )
            SwitchPreference(
                title = { Text(stringResource(R.string.lyrics)) },
                checked = applyToLyrics,
                onCheckedChange = onApplyToLyricsChange
            )
            SwitchPreference(
                title = { Text(stringResource(R.string.settings)) },
                checked = applyToSettings,
                onCheckedChange = onApplyToSettingsChange
            )
            SwitchPreference(
                title = { Text(stringResource(R.string.search)) },
                checked = applyToSearch,
                onCheckedChange = onApplyToSearchChange
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
        }
    }


@Composable
private fun WallpaperGridItem(
    wallpaper: BuiltInWallpaper,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected)
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = wallpaper.resourceId),
            contentDescription = wallpaper.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
