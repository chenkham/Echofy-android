package com.Chenkham.Echofy.ui.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.Chenkham.Echofy.constants.AnimateLyricsKey
import com.Chenkham.Echofy.constants.CacheTranslationsKey
import com.Chenkham.Echofy.constants.DarkModeKey
import com.Chenkham.Echofy.constants.HighContrastLyricsKey
import com.Chenkham.Echofy.constants.LyricsClickKey
import com.Chenkham.Echofy.constants.LyricsScrollKey
import com.Chenkham.Echofy.constants.LyricsTextPositionKey
import com.Chenkham.Echofy.constants.PlayerBackgroundStyle
import com.Chenkham.Echofy.constants.PlayerBackgroundStyleKey
import com.Chenkham.Echofy.constants.PlayerLayoutStyle
import com.Chenkham.Echofy.constants.PlayerLayoutStyleKey
import com.Chenkham.Echofy.constants.ReduceMotionKey
import com.Chenkham.Echofy.ui.screens.settings.DarkMode
import com.Chenkham.Echofy.ui.screens.settings.LyricsPosition
import com.Chenkham.Echofy.utils.LyricsTranslationService
import com.Chenkham.Echofy.utils.rememberEnumPreference
import com.Chenkham.Echofy.utils.rememberPreference

class LyricsPreferencesState(
    val changeLyrics: Boolean,
    val scrollLyrics: Boolean,
    val animateLyrics: Boolean,
    val lyricsFontSize: TextUnit,
    val lyricsTextPosition: LyricsPosition,
    val playerBackground: PlayerBackgroundStyle,
    val useDarkTheme: Boolean,
)

@Composable
fun rememberLyricsPreferences(): LyricsPreferencesState {
    val savedLyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val animateLyricsPref by rememberPreference(AnimateLyricsKey, true)
    val reduceMotion by rememberPreference(ReduceMotionKey, false)
    val animateLyrics = animateLyricsPref && !reduceMotion
    val highContrastLyrics by rememberPreference(HighContrastLyricsKey, false)
    val cacheTranslations by rememberPreference(CacheTranslationsKey, true)

    LaunchedEffect(cacheTranslations) {
        LyricsTranslationService.cachingEnabled = cacheTranslations
        if (!cacheTranslations) LyricsTranslationService.clearCache()
    }
    val lyricsFontSize = if (highContrastLyrics) 32.sp else 25.sp

    val savedBackground by rememberEnumPreference(PlayerBackgroundStyleKey, PlayerBackgroundStyle.DEFAULT)
    val playerLayoutStyle by rememberEnumPreference(PlayerLayoutStyleKey, PlayerLayoutStyle.CLASSIC)
    val isAppleStyle = playerLayoutStyle == PlayerLayoutStyle.APPLE_MUSIC
    val lyricsTextPosition = if (isAppleStyle) LyricsPosition.LEFT else savedLyricsTextPosition
    val playerBackground = if (isAppleStyle) PlayerBackgroundStyle.BLUR else savedBackground

    val darkTheme by rememberEnumPreference(DarkModeKey, DarkMode.AUTO)
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemDark) { if (darkTheme == DarkMode.AUTO) isSystemDark else darkTheme == DarkMode.ON }

    return remember(changeLyrics, scrollLyrics, animateLyrics, lyricsFontSize, lyricsTextPosition, playerBackground, useDarkTheme) {
        LyricsPreferencesState(changeLyrics, scrollLyrics, animateLyrics, lyricsFontSize, lyricsTextPosition, playerBackground, useDarkTheme)
    }
}
