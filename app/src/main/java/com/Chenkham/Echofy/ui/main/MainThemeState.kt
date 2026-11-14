package com.Chenkham.Echofy.ui.main

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.Chenkham.Echofy.constants.CustomFontKey
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.DynamicThemeKey
import com.Chenkham.Echofy.constants.PureBlackKey
import com.Chenkham.Echofy.constants.UseSystemFontKey
import com.Chenkham.Echofy.playback.PlayerConnection
import com.Chenkham.Echofy.ui.screens.settings.DarkMode
import com.Chenkham.Echofy.ui.theme.ColorSaver
import com.Chenkham.Echofy.ui.theme.DefaultThemeColor
import com.Chenkham.Echofy.ui.theme.extractThemeColor
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

data class MainThemeConfig(
    val useDarkTheme: Boolean,
    val pureBlack: Boolean,
    val themeColor: Color,
    val useSystemFont: Boolean,
    val appFont: com.Chenkham.Echofy.constants.AppFont,
)

/**
 * Manages theme mode (dark/light/auto), pure black, dynamic album art color extraction, and system bars.
 */
@Composable
fun rememberMainThemeConfig(
    activity: Activity,
    playerConnection: PlayerConnection?,
): MainThemeConfig {
    val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    LaunchedEffect(useDarkTheme) {
        setSystemBarAppearance(activity.window, useDarkTheme)
    }

    var themeColor by rememberSaveable(stateSaver = ColorSaver) {
        mutableStateOf(DefaultThemeColor)
    }

    LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
        if (!enableDynamicTheme || playerConnection == null) {
            themeColor = DefaultThemeColor
            return@LaunchedEffect
        }
        playerConnection.service.currentMediaMetadata.collectLatest { song ->
            themeColor = if (song != null) {
                withContext(Dispatchers.IO) {
                    val result = activity.imageLoader.execute(
                        ImageRequest.Builder(activity)
                            .data(song.thumbnailUrl)
                            .allowHardware(false)
                            .build(),
                    )
                    (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor() ?: DefaultThemeColor
                }
            } else {
                DefaultThemeColor
            }
        }
    }

    val useSystemFont by rememberPreference(UseSystemFontKey, defaultValue = false)
    val appFont by rememberEnumPreference(CustomFontKey, defaultValue = com.Chenkham.Echofy.constants.AppFont.SYSTEM)

    return MainThemeConfig(
        useDarkTheme = useDarkTheme,
        pureBlack = pureBlack,
        themeColor = themeColor,
        useSystemFont = useSystemFont,
        appFont = appFont,
    )
}

fun setSystemBarAppearance(window: Window, isDark: Boolean) {
    WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        window.navigationBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
    }
}
