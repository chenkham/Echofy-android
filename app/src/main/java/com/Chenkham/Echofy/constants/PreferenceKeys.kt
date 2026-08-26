package com.Chenkham.Echofy.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.Chenkham.Echofy.R
import java.time.LocalDateTime
import java.time.ZoneOffset

val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val DarkModeKey = stringPreferencesKey("darkMode")
val PureBlackKey = booleanPreferencesKey("pureBlack")
val CustomThemeColorKey = stringPreferencesKey("customThemeColor")
val WebClientPoTokenEnabledKey = booleanPreferencesKey("webClientPoTokenEnabled")
val UseVisitorDataKey = booleanPreferencesKey("useVisitorData")
val PoTokenSourceUrlKey = stringPreferencesKey("poTokenSourceUrl")
val PoTokenGvsKey = stringPreferencesKey("poTokenGvs")
val PoTokenPlayerKey = stringPreferencesKey("poTokenPlayer")
val PlayerCustomImageUriKey = stringPreferencesKey("playerCustomImageUri")
val PlayerCustomBlurKey = floatPreferencesKey("playerCustomBlur")
val PlayerCustomContrastKey = floatPreferencesKey("playerCustomContrast")
val PlayerCustomBrightnessKey = floatPreferencesKey("playerCustomBrightness")
val DefaultOpenTabKey = stringPreferencesKey("defaultOpenTab")
val SlimNavBarKey = booleanPreferencesKey("slimNavBar")
val GridItemsSizeKey = stringPreferencesKey("gridItemSize")
val SliderStyleKey = stringPreferencesKey("sliderStyle")
val PipEnabledKey = booleanPreferencesKey("pipEnabled")
val EnableListenTogetherKey = booleanPreferencesKey("enableListenTogether")
val PlaybackModeKey = stringPreferencesKey("playbackMode")
val SeasonalWallpaperKey = stringPreferencesKey("seasonalWallpaper")
val DataSaverEnabledKey = booleanPreferencesKey("dataSaverEnabled")
val BatterySaverEnabledKey = booleanPreferencesKey("batterySaverEnabled")
val VideoCacheEnabledKey = booleanPreferencesKey("videoCacheEnabled")
val VideoPlaybackEnabledKey = booleanPreferencesKey("videoPlaybackEnabled")
val AodArtShapeKey = stringPreferencesKey("aodArtShape")
val AodStyleKey = stringPreferencesKey("aodStyle")
val AodControlStyleKey = stringPreferencesKey("aodControlStyle")
val AodDarknessKey = floatPreferencesKey("aodDarkness")
val AodArtSizeKey = floatPreferencesKey("aodArtSize")
val AodShowTitleKey = booleanPreferencesKey("aodShowTitle")
val AodShowArtistKey = booleanPreferencesKey("aodShowArtist")
val AodShowTimeKey = booleanPreferencesKey("aodShowTime")
val AodShowProgressKey = booleanPreferencesKey("aodShowProgress")
val AodShowControlsKey = booleanPreferencesKey("aodShowControls")
val AodFullscreenKey = booleanPreferencesKey("aodFullscreen")
val AodAutoActivationKey = intPreferencesKey("aodAutoActivation")
val AodSpotlightIntensityKey = floatPreferencesKey("aodSpotlightIntensity")
val AodSpotlightPulseKey = booleanPreferencesKey("aodSpotlightPulse")
val AodTransitionDurationKey = intPreferencesKey("aodTransitionDuration")
val AodTextScaleKey = floatPreferencesKey("aodTextScale")
val AodShowClockKey = booleanPreferencesKey("aodShowClock")
val AodClockFormatKey = booleanPreferencesKey("aodClockFormat")
val DisableBlurKey = booleanPreferencesKey("disableBlur")
val SelectedLocalFoldersKey = stringSetPreferencesKey("selected_local_folders")
val CanvasSourceKey = stringPreferencesKey("canvasSource")

enum class CanvasSource {
    AUTO,
    APPLE_MUSIC,
    TIDAL,
    CUSTOM,
}

enum class WidgetBackgroundMode {
    BLUR,
    DOMINANT_COLOR,
    SOLID,
}

val WidgetBackgroundModeKey = stringPreferencesKey("widget_background_mode")
val WidgetScrimOpacityKey = floatPreferencesKey("widget_scrim_opacity")
val WidgetCornerRadiusKey = floatPreferencesKey("widget_corner_radius")
val WidgetShowProgressBarKey = booleanPreferencesKey("widget_show_progress_bar")


enum class AodArtShape {
    ROUNDED,
    CIRCLE,
    SQUIRCLE,
    DIAMOND,
    HEXAGON,
    STAR,
    ARCH,
    PETAL
}

enum class AodStyle {
    CLASSIC,
    BACKGROUND,
    MINIMAL,
    LARGE,
    SPOTLIGHT
}

enum class AodControlStyle {
    ROUNDED,
    SQUARE,
    ACCENT,
    MINIMAL_FLAT
}

enum class PlaybackMode {
    AUDIO,  // Default - shows album art thumbnail
    VIDEO   // Shows music video
}

enum class SeasonalWallpaper {
    OFF,
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN,
}

enum class SliderStyle {
    DEFAULT,
    SQUIGGLY,
    SLIM,
    YOUTUBE_MUSIC,
    VINTAGE_CABLE
}

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrclib")
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
val HideVideoKey = booleanPreferencesKey("hideVideo")
val LastNewReleaseCheckKey = longPreferencesKey("last_new_release_check")
val minPlaybackDurKey = intPreferencesKey("minPlaybackDur")
val ProxyEnabledKey = booleanPreferencesKey("proxyEnabled")
val ProxyUrlKey = stringPreferencesKey("proxyUrl")
val ProxyTypeKey = stringPreferencesKey("proxyType")
val YtmSyncKey = booleanPreferencesKey("ytmSync")

val AudioQualityKey = stringPreferencesKey("audioQuality")

enum class AudioQuality {
    AUTO,
    HIGH,
    HIGHEST,
    LOW,
}

val PlayerStreamClientKey = stringPreferencesKey("playerStreamClient")

enum class PlayerStreamClient {
    ANDROID_VR,
    WEB_REMIX,
    IOS,
    TVHTML5,
    ANDROID_MUSIC,
}

val PersistentQueueKey = booleanPreferencesKey("persistentQueue")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val AudioNormalizationKey = booleanPreferencesKey("audioNormalization")
val AutoLoadMoreKey = booleanPreferencesKey("autoLoadMore")
val SimilarContent = booleanPreferencesKey("similarContent")
val AutoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val StopMusicOnTaskClearKey = booleanPreferencesKey("stopMusicOnTaskClear")

val MaxImageCacheSizeKey = intPreferencesKey("maxImageCacheSize")
val MaxSongCacheSizeKey = intPreferencesKey("maxSongCacheSize")
val AutoClearCacheOnCloseKey = booleanPreferencesKey("autoClearCacheOnClose")

val DisableLoadMoreWhenRepeatAllKey = booleanPreferencesKey("disableLoadMoreWhenRepeatAll")
val ScrobbleDelayPercentKey = floatPreferencesKey("scrobbleDelayPercent")
val ScrobbleMinSongDurationKey = intPreferencesKey("scrobbleMinSongDuration")
val ScrobbleDelaySecondsKey = intPreferencesKey("scrobbleDelaySeconds")
val EnableLastFMScrobblingKey = booleanPreferencesKey("enableLastFMScrobbling")
val LastFMUseNowPlaying = booleanPreferencesKey("lastFMUseNowPlaying")
val LastFmApiKeyKey = stringPreferencesKey("lastFmApiKey")
val LastFmApiSecretKey = stringPreferencesKey("lastFmApiSecret")
val LastFmSessionKeyKey = stringPreferencesKey("lastFmSessionKey")
val LastFmUsernameKey = stringPreferencesKey("lastFmUsername")
val LastFMSessionKey = LastFmSessionKeyKey
val LastFMUsernameKey = LastFmUsernameKey
val LastFmTokenKey = stringPreferencesKey("lastFmToken")
val AudioOffload = booleanPreferencesKey("audioOffload")

// Premium feature preference keys
val HapticBassBeatsKey = booleanPreferencesKey("hapticBassBeats")
val ShakeToSkipKey = booleanPreferencesKey("shakeToSkip")
val HardwareVolButtonSkipKey = booleanPreferencesKey("hardwareVolButtonSkip")
val CrossfadeEnabledKey = booleanPreferencesKey("crossfadeEnabled")
val VisualizerEnabledKey = booleanPreferencesKey("visualizerEnabled")
val CustomAppIconKey = intPreferencesKey("customAppIcon")

// Built-in Equalizer settings
val EqualizerEnabledKey = booleanPreferencesKey("equalizerEnabled")
val EqualizerPresetKey = intPreferencesKey("equalizerPreset") // -1 = Custom, 0+ = preset index
val EqualizerBandLevelsKey = stringPreferencesKey("equalizerBandLevels") // JSON array
val BassBoostEnabledKey = booleanPreferencesKey("bassBoostEnabled")
val BassBoostStrengthKey = intPreferencesKey("bassBoostStrength") // 0-1000

// Video Quality setting for video playback
val VideoQualityKey = stringPreferencesKey("videoQuality") // "Auto", "1080p", "720p", "480p", "360p", "144p"

val PlayerTextAlignmentKey = stringPreferencesKey("playerTextAlignment")

val RotateBackgroundKey = booleanPreferencesKey("rotate_background")


val SmallButtonsShapeKey = stringPreferencesKey("small_buttons_shape")
const val DefaultSmallButtonsShape = "Circle"

val PauseListenHistoryKey = booleanPreferencesKey("pauseListenHistory")
val PauseSearchHistoryKey = booleanPreferencesKey("pauseSearchHistory")
val DisableScreenshotKey = booleanPreferencesKey("disableScreenshot")

val DiscordTokenKey = stringPreferencesKey("discordToken")
val DiscordInfoDismissedKey = booleanPreferencesKey("discordInfoDismissed")
val DiscordUsernameKey = stringPreferencesKey("discordUsername")
val DiscordNameKey = stringPreferencesKey("discordName")
val EnableDiscordRPCKey = booleanPreferencesKey("discordRPCEnable")
val DiscordLargeImageTypeKey = stringPreferencesKey("discordLargeImageType")
val DiscordLargeTextSourceKey = stringPreferencesKey("discordLargeTextSource")
val DiscordLargeTextCustomKey = stringPreferencesKey("discordLargeTextCustom")
val DiscordLargeImageCustomUrlKey = stringPreferencesKey("discordLargeImageCustomUrl")
val DiscordSmallImageTypeKey = stringPreferencesKey("discordSmallImageType")
val DiscordSmallImageCustomUrlKey = stringPreferencesKey("discordSmallImageCustomUrl")
val SelectedYtmPlaylistsKey = stringPreferencesKey("selected_ytm_playlists")

val EnableUpdateNotificationKey = booleanPreferencesKey("enableUpdateNotification")
val UpdateChannelKey = stringPreferencesKey("updateChannel")
val LastUpdateCheckKey = longPreferencesKey("lastUpdateCheck")
val LastNotifiedVersionKey = stringPreferencesKey("lastNotifiedVersion")

enum class UpdateChannel {
    STABLE,
    NIGHTLY,
}

val ChipSortTypeKey = stringPreferencesKey("chipSortType")
val SongSortTypeKey = stringPreferencesKey("songSortType")
val SongSortDescendingKey = booleanPreferencesKey("songSortDescending")
val PlaylistSongSortTypeKey = stringPreferencesKey("playlistSongSortType")
val PlaylistSongSortDescendingKey = booleanPreferencesKey("playlistSongSortDescending")
val AutoPlaylistSongSortTypeKey = stringPreferencesKey("autoPlaylistSongSortType")
val AutoPlaylistSongSortDescendingKey = booleanPreferencesKey("autoPlaylistSongSortDescending")
val ArtistSortTypeKey = stringPreferencesKey("artistSortType")
val ArtistSortDescendingKey = booleanPreferencesKey("artistSortDescending")
val AlbumSortTypeKey = stringPreferencesKey("albumSortType")
val AlbumSortDescendingKey = booleanPreferencesKey("albumSortDescending")
val PlaylistSortTypeKey = stringPreferencesKey("playlistSortType")
val PlaylistSortDescendingKey = booleanPreferencesKey("playlistSortDescending")
val ArtistSongSortTypeKey = stringPreferencesKey("artistSongSortType")
val ArtistSongSortDescendingKey = booleanPreferencesKey("artistSongSortDescending")
val MixSortTypeKey = stringPreferencesKey("mixSortType")
val MixSortDescendingKey = booleanPreferencesKey("albumSortDescending")

val SongFilterKey = stringPreferencesKey("songFilter")
val ArtistFilterKey = stringPreferencesKey("artistFilter")
val AlbumFilterKey = stringPreferencesKey("albumFilter")


val LyricsScrollKey = booleanPreferencesKey("lyricsScrollKey")

val DiscordUseDetailsKey = booleanPreferencesKey("discordUseDetails")


val ArtistViewTypeKey = stringPreferencesKey("artistViewType")
val AlbumViewTypeKey = stringPreferencesKey("albumViewType")
val PlaylistViewTypeKey = stringPreferencesKey("playlistViewType")

val PlaylistEditLockKey = booleanPreferencesKey("playlistEditLock")
val QuickPicksKey = stringPreferencesKey("discover")
val PreferredLyricsProviderKey = stringPreferencesKey("lyricsProvider")
val CurrentLyricsProviderKey = stringPreferencesKey("currentLyricsProvider") // Manual provider override
val QueueEditLockKey = booleanPreferencesKey("queueEditLock")
val LyricFontSizeKey = intPreferencesKey("lyricFontSize")
val fullScreenLyricsKey = booleanPreferencesKey("fullScreenLyrics")
val AnimateLyricsKey = booleanPreferencesKey("animate_lyrics")

fun lyricsSyncOffsetKey(songId: String) = longPreferencesKey("lyricsSyncOffset_$songId")

val PlayPauseButtonShapeKey = stringPreferencesKey("playPauseButtonShape")
const val DefaultPlayPauseButtonShape = "Circle"

val MiniPlayerThumbnailShapeKey = stringPreferencesKey("miniPlayerThumbnailShape")
const val DefaultMiniPlayerThumbnailShape = "Circle"

enum class LibraryViewType {
    LIST,
    GRID,
    ;

    fun toggle() =
        when (this) {
            LIST -> GRID
            GRID -> LIST
        }
}

enum class SongFilter {
    LIBRARY,
    LIKED,
    DOWNLOADED,
}

enum class ArtistFilter {
    LIBRARY,
    LIKED,
}

enum class AlbumFilter {
    LIBRARY,
    LIKED,
    DOWNLOADED,
    DOWNLOADED_FULL,
}

enum class SongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class PlaylistSongSortType {
    CUSTOM,
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class AutoPlaylistSongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class ArtistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    PLAY_TIME,
}

enum class ArtistSongSortType {
    CREATE_DATE,
    NAME,
    PLAY_TIME,
}

enum class AlbumSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    YEAR,
    SONG_COUNT,
    LENGTH,
    PLAY_TIME,
}

enum class PlaylistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    LAST_UPDATED,
    CUSTOM,
}

enum class MixSortType {
    CREATE_DATE,
    NAME,
    LAST_UPDATED,
}

enum class GridItemSize {
    SMALL,
    BIG,
}

enum class MyTopFilter {
    ALL_TIME,
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ;

    fun toTimeMillis(): Long =
        when (this) {
            DAY ->
                LocalDateTime
                    .now()
                    .minusDays(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            WEEK ->
                LocalDateTime
                    .now()
                    .minusWeeks(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            MONTH ->
                LocalDateTime
                    .now()
                    .minusMonths(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            YEAR ->
                LocalDateTime
                    .now()
                    .minusMonths(12)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            ALL_TIME -> 0
        }
}

enum class QuickPicks {
    QUICK_PICKS,
    LAST_LISTEN,
}

enum class PreferredLyricsProvider {
    LRCLIB,
    KUGOU,
    BETTER_LYRICS,
    GENIUS,
    YOUTUBE,
    YOUTUBE_SUBTITLES,
}

enum class PlayerBackgroundStyle {
    DEFAULT,
    GRADIENT,
    CUSTOM,
    BLUR,
    COLORING,
    BLUR_GRADIENT,
    GLOW,
    GLOW_ANIMATED,
}


enum class PlayerButtonsStyle {
    DEFAULT,
    SECONDARY,
}

enum class MiniPlayerStyle {
    Floating,
    Slim
}

enum class PlayerLayoutStyle {
    CLASSIC,
    APPLE_MUSIC
}

val TopSize = stringPreferencesKey("topSize")
val HistoryDuration = floatPreferencesKey("historyDuration")

val PlayerBackgroundStyleKey = stringPreferencesKey("playerBackgroundStyle")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val LyricsTextPositionKey = stringPreferencesKey("lyricsTextPosition")
val LyricsClickKey = booleanPreferencesKey("lyricsClick")
val TranslateLyricsKey = booleanPreferencesKey("translateLyrics")

val PlayerVolumeKey = floatPreferencesKey("playerVolume")
val RepeatModeKey = intPreferencesKey("repeatMode")
val PlayerButtonsStyleKey = stringPreferencesKey("player_buttons_style")
val MiniPlayerStyleKey = stringPreferencesKey("mini_player_style")
val PlayerLayoutStyleKey = stringPreferencesKey("player_layout_style")

val SearchSourceKey = stringPreferencesKey("searchSource")
val SwipeThumbnailKey = booleanPreferencesKey("swipeThumbnail")

enum class SearchSource {
    LOCAL,
    ONLINE,
    ;

    fun toggle() =
        when (this) {
            LOCAL -> ONLINE
            ONLINE -> LOCAL
        }
}

val VisitorDataKey = stringPreferencesKey("visitorData")
val VisitorDataTimestampKey = longPreferencesKey("visitorDataTimestamp")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val AccountPhotoUrlKey = stringPreferencesKey("account_photo_url")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val AccountNameKey = stringPreferencesKey("accountName")
val AccountEmailKey = stringPreferencesKey("accountEmail")
val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")

val LanguageCodeToName =
    mapOf(
        "af" to "Afrikaans",
        "az" to "AzÉ™rbaycan",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Malaysia",
        "ca" to "CatalÃ ",
        "cs" to "ÄŒeÅ¡tina",
        "da" to "Dansk",
        "de" to "Deutsch",
        "et" to "Eesti",
        "en-GB" to "English (UK)",
        "en" to "English (US)",
        "es" to "EspaÃ±ol (EspaÃ±a)",
        "es-419" to "EspaÃ±ol (LatinoamÃ©rica)",
        "eu" to "Euskara",
        "fil" to "Filipino",
        "fr" to "FranÃ§ais",
        "fr-CA" to "FranÃ§ais (Canada)",
        "gl" to "Galego",
        "hr" to "Hrvatski",
        "zu" to "IsiZulu",
        "is" to "Ãslenska",
        "it" to "Italiano",
        "sw" to "Kiswahili",
        "lt" to "LietuviÅ³",
        "hu" to "Magyar",
        "nl" to "Nederlands",
        "no" to "Norsk",
        "or" to "Odia",
        "uz" to "Oâ€˜zbe",
        "pl" to "Polski",
        "pt-PT" to "PortuguÃªs",
        "pt" to "PortuguÃªs (Brasil)",
        "ro" to "RomÃ¢nÄƒ",
        "sq" to "Shqip",
        "sk" to "SlovenÄina",
        "sl" to "SlovenÅ¡Äina",
        "fi" to "Suomi",
        "sv" to "Svenska",
        "bo" to "Tibetan à½–à½¼à½‘à¼‹à½¦à¾à½‘à¼",
        "vi" to "Tiáº¿ng Viá»‡t",
        "tr" to "TÃ¼rkÃ§e",
        "bg" to "Ð‘ÑŠÐ»Ð³Ð°Ñ€ÑÐºÐ¸",
        "ky" to "ÐšÑ‹Ñ€Ð³Ñ‹Ð·Ñ‡Ð°",
        "kk" to "ÒšÐ°Ð·Ð°Ò› Ð¢Ñ–Ð»Ñ–",
        "mk" to "ÐœÐ°ÐºÐµÐ´Ð¾Ð½ÑÐºÐ¸",
        "mn" to "ÐœÐ¾Ð½Ð³Ð¾Ð»",
        "ru" to "Ð ÑƒÑÑÐºÐ¸Ð¹",
        "sr" to "Ð¡Ñ€Ð¿ÑÐºÐ¸",
        "uk" to "Ð£ÐºÑ€Ð°Ñ—Ð½ÑÑŒÐºÐ°",
        "el" to "Î•Î»Î»Î·Î½Î¹ÎºÎ¬",
        "hy" to "Õ€Õ¡ÕµÕ¥Ö€Õ¥Õ¶",
        "iw" to "×¢×‘×¨×™×ª",
        "ur" to "Ø§Ø±Ø¯Ùˆ",
        "ar" to "Ø§Ù„Ø¹Ø±Ø¨ÙŠØ©",
        "fa" to "ÙØ§Ø±Ø³ÛŒ",
        "ne" to "à¤¨à¥‡à¤ªà¤¾à¤²à¥€",
        "mr" to "à¤®à¤°à¤¾à¤ à¥€",
        "hi" to "à¤¹à¤¿à¤¨à¥à¤¦à¥€",
        "bn" to "à¦¬à¦¾à¦‚à¦²à¦¾",
        "pa" to "à¨ªà©°à¨œà¨¾à¨¬à©€",
        "gu" to "àª—à«àªœàª°àª¾àª¤à«€",
        "ta" to "à®¤à®®à®¿à®´à¯",
        "te" to "à°¤à±†à°²à±à°—à±",
        "kn" to "à²•à²¨à³à²¨à²¡",
        "ml" to "à´®à´²à´¯à´¾à´³à´‚",
        "si" to "à·ƒà·’à¶‚à·„à¶½",
        "th" to "à¸ à¸²à¸©à¸²à¹„à¸—à¸¢",
        "lo" to "àº¥àº²àº§",
        "my" to "á€—á€™á€¬",
        "ka" to "áƒ¥áƒáƒ áƒ—áƒ£áƒšáƒ˜",
        "am" to "áŠ áˆ›áˆ­áŠ›",
        "km" to "ážáŸ’áž˜áŸ‚ážš",
        "zh-CN" to "ä¸­æ–‡ (ç®€ä½“)",
        "zh-TW" to "ä¸­æ–‡ (ç¹é«”)",
        "zh-HK" to "ä¸­æ–‡ (é¦™æ¸¯)",
        "ja" to "æ—¥æœ¬èªž",
        "ko" to "í•œêµ­ì–´",
    )

val CountryCodeToName =
    mapOf(
        "DZ" to "Algeria",
        "AR" to "Argentina",
        "AU" to "Australia",
        "AT" to "Austria",
        "AZ" to "Azerbaijan",
        "BH" to "Bahrain",
        "BD" to "Bangladesh",
        "BY" to "Belarus",
        "BE" to "Belgium",
        "BO" to "Bolivia",
        "BA" to "Bosnia and Herzegovina",
        "BR" to "Brazil",
        "BG" to "Bulgaria",
        "KH" to "Cambodia",
        "CA" to "Canada",
        "CL" to "Chile",
        "HK" to "Hong Kong",
        "CO" to "Colombia",
        "CR" to "Costa Rica",
        "HR" to "Croatia",
        "CY" to "Cyprus",
        "CZ" to "Czech Republic",
        "DK" to "Denmark",
        "DO" to "Dominican Republic",
        "EC" to "Ecuador",
        "EG" to "Egypt",
        "SV" to "El Salvador",
        "EE" to "Estonia",
        "FI" to "Finland",
        "FR" to "France",
        "GE" to "Georgia",
        "DE" to "Germany",
        "GH" to "Ghana",
        "GR" to "Greece",
        "GT" to "Guatemala",
        "HN" to "Honduras",
        "HU" to "Hungary",
        "IS" to "Iceland",
        "IN" to "India",
        "ID" to "Indonesia",
        "IQ" to "Iraq",
        "IE" to "Ireland",
        "IL" to "Israel",
        "IT" to "Italy",
        "JM" to "Jamaica",
        "JP" to "Japan",
        "JO" to "Jordan",
        "KZ" to "Kazakhstan",
        "KE" to "Kenya",
        "KR" to "South Korea",
        "KW" to "Kuwait",
        "LA" to "Lao",
        "LV" to "Latvia",
        "LB" to "Lebanon",
        "LY" to "Libya",
        "LI" to "Liechtenstein",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "MK" to "Macedonia",
        "MY" to "Malaysia",
        "MT" to "Malta",
        "MX" to "Mexico",
        "ME" to "Montenegro",
        "MA" to "Morocco",
        "NP" to "Nepal",
        "NL" to "Netherlands",
        "NZ" to "New Zealand",
        "NI" to "Nicaragua",
        "NG" to "Nigeria",
        "NO" to "Norway",
        "OM" to "Oman",
        "PK" to "Pakistan",
        "PA" to "Panama",
        "PG" to "Papua New Guinea",
        "PY" to "Paraguay",
        "PE" to "Peru",
        "PH" to "Philippines",
        "PL" to "Poland",
        "PT" to "Portugal",
        "PR" to "Puerto Rico",
        "QA" to "Qatar",
        "RO" to "Romania",
        "RU" to "Russian Federation",
        "SA" to "Saudi Arabia",
        "SN" to "Senegal",
        "RS" to "Serbia",
        "SG" to "Singapore",
        "SK" to "Slovakia",
        "SI" to "Slovenia",
        "ZA" to "South Africa",
        "ES" to "Spain",
        "LK" to "Sri Lanka",
        "SE" to "Sweden",
        "CH" to "Switzerland",
        "TW" to "Taiwan",
        "TZ" to "Tanzania",
        "TH" to "Thailand",
        "TN" to "Tunisia",
        "TR" to "Turkey",
        "UG" to "Uganda",
        "UA" to "Ukraine",
        "AE" to "United Arab Emirates",
        "GB" to "United Kingdom",
        "US" to "United States",
        "UY" to "Uruguay",
        "VE" to "Venezuela (Bolivarian Republic)",
        "VN" to "Vietnam",
        "YE" to "Yemen",
        "ZW" to "Zimbabwe",
    )

// Onboarding / First Launch
val OnboardingCompletedKey = booleanPreferencesKey("onboardingCompleted")
val OnboardingSelectedCountryKey = stringPreferencesKey("onboardingSelectedCountry")
val OnboardingSelectedArtistsKey = stringPreferencesKey("onboardingSelectedArtists") // JSON array
val OnboardingSelectedLanguageKey = stringPreferencesKey("onboardingSelectedLanguage")


// Backpaper (App Background Wallpaper) Settings
val BackpaperEnabledKey = booleanPreferencesKey("backpaperEnabled")
val BackpaperTypeKey = stringPreferencesKey("backpaperType")
val BackpaperBuiltInIdKey = stringPreferencesKey("backpaperBuiltInId")
val BackpaperCustomPathKey = stringPreferencesKey("backpaperCustomPath")
val BackpaperOpacityKey = floatPreferencesKey("backpaperOpacity")
val BackpaperBlurKey = floatPreferencesKey("backpaperBlur")
val BackpaperApplyToHomeKey = booleanPreferencesKey("backpaperApplyToHome")
val BackpaperApplyToExploreKey = booleanPreferencesKey("backpaperApplyToExplore")
val BackpaperApplyToLibraryKey = booleanPreferencesKey("backpaperApplyToLibrary")
val BackpaperApplyToPlayerKey = booleanPreferencesKey("backpaperApplyToPlayer")
val BackpaperApplyToSettingsKey = booleanPreferencesKey("backpaperApplyToSettings")
val BackpaperApplyToSearchKey = booleanPreferencesKey("backpaperApplyToSearch")
val BackpaperApplyToLyricsKey = booleanPreferencesKey("backpaperApplyToLyrics")

enum class BackpaperType {
    NONE,       // No wallpaper
    BUILT_IN,   // Use bundled wallpaper
    CUSTOM      // Use user's custom photo
}

enum class WallpaperCategory {
    WINTER,
    SPRING,
    SUMMER,
    AUTUMN,
    NIGHT,
    NATURE,
    ABSTRACT
}

enum class BackpaperScreen {
    HOME,
    EXPLORE,
    LIBRARY,
    PLAYER,
    SETTINGS,
    SEARCH,
    LYRICS
}

val IsGuestModeKey = booleanPreferencesKey("is_guest_mode")

// Charts Country Selection
val ChartCountryKey = stringPreferencesKey("chartCountry")

val VoiceControlEnabledKey = booleanPreferencesKey("voiceControlEnabled")

// Pro Feature Toggles
val SeamlessDJCrossfadeKey = booleanPreferencesKey("seamlessDJCrossfade")
val AudioSpeedPitchKey = booleanPreferencesKey("audioSpeedPitch")
val InstagramShareCardsKey = booleanPreferencesKey("instagramShareCards")
val BatchOfflineLyricsKey = booleanPreferencesKey("batchOfflineLyrics")
val IncognitoModeKey = booleanPreferencesKey("incognitoMode")
val CustomFontKey = stringPreferencesKey("customFont")
val UseSystemFontKey = booleanPreferencesKey("use_system_font")
val LiveFluidBackgroundKey = booleanPreferencesKey("liveFluidBackground")
val LiveFluidColorPaletteKey = stringPreferencesKey("liveFluidColorPalette")
val RealTimeVisualizerKey = booleanPreferencesKey("realTimeVisualizer")

enum class LiveFluidColorPalette {
    ALBUM,
    ECHOFY,
    OCEAN,
    SUNSET,
    ROSE,
    EMERALD,
    MONO,
}

val PremiumCustomIconEnabledKey = booleanPreferencesKey("premiumAppIconEnabled")
val PremiumCustomFontEnabledKey = booleanPreferencesKey("premiumCustomFontEnabled")

// Internet Radio (Radio Browser)
val RadioEnabledKey = booleanPreferencesKey("radioEnabled")
val RadioDefaultCountryKey = stringPreferencesKey("radioDefaultCountry")
val RadioMinBitrateKey = stringPreferencesKey("radioMinBitrate")
val RadioHideBrokenKey = booleanPreferencesKey("radioHideBroken")

// Artist metadata enrichment (MusicBrainz)
val ArtistInfoEnabledKey = booleanPreferencesKey("artistInfoEnabled")
val ArtistInfoShowGenresKey = booleanPreferencesKey("artistInfoShowGenres")
val ArtistInfoShowLinksKey = booleanPreferencesKey("artistInfoShowLinks")

// Artist biography and artwork (TheAudioDB)
val ArtistBioEnabledKey = booleanPreferencesKey("artistBioEnabled")
val TheAudioDbApiKeyKey = stringPreferencesKey("theAudioDbApiKey")

// Concerts (Bandsintown)
val ConcertsEnabledKey = booleanPreferencesKey("concertsEnabled")
val BandsintownAppIdKey = stringPreferencesKey("bandsintownAppId")

// Similar artists and cross-domain recommendations (TasteDive)
val SimilarArtistsEnabledKey = booleanPreferencesKey("similarArtistsEnabled")
val TasteDiveApiKeyKey = stringPreferencesKey("tasteDiveApiKey")

// Genius lyrics provider
val EnableGeniusKey = booleanPreferencesKey("enableGenius")
val GeniusAccessTokenKey = stringPreferencesKey("geniusAccessToken")

// Cross-platform share links (Songlink / Odesli)
val SonglinkEnabledKey = booleanPreferencesKey("songlinkEnabled")
val SonglinkApiKeyKey = stringPreferencesKey("songlinkApiKey")
val CustomShareDomainKey = stringPreferencesKey("customShareDomain")
val GoogleDriveApkUrlKey = stringPreferencesKey("googleDriveApkUrl")

// Physical release details (Discogs)
val DiscogsEnabledKey = booleanPreferencesKey("discogsEnabled")
val DiscogsTokenKey = stringPreferencesKey("discogsToken")

// Ambient sounds (Freesound)
val AmbientSoundsEnabledKey = booleanPreferencesKey("ambientSoundsEnabled")
val FreesoundApiKeyKey = stringPreferencesKey("freesoundApiKey")

// DJ mixes and radio shows (Mixcloud)
val MixcloudEnabledKey = booleanPreferencesKey("mixcloudEnabled")

// ---------------------------------------------------------------------------
// Sleep timer comfort options
// ---------------------------------------------------------------------------
/** Fades the volume down before the sleep timer pauses instead of cutting abruptly. */
val SleepTimerFadeOutKey = booleanPreferencesKey("sleepTimerFadeOut")
/** Length of the sleep timer fade, in seconds. */
val SleepTimerFadeDurationKey = intPreferencesKey("sleepTimerFadeDuration")

// ---------------------------------------------------------------------------
// Playback comfort
// ---------------------------------------------------------------------------
/** Short volume ramp when playback is paused or resumed instead of an abrupt cut. */
val VolumeFadeOnPauseKey = booleanPreferencesKey("volumeFadeOnPause")

// ---------------------------------------------------------------------------
// Safe listening
// ---------------------------------------------------------------------------
/** Caps the in-app playback volume to protect hearing. */
val VolumeLimitEnabledKey = booleanPreferencesKey("volumeLimitEnabled")
/** Maximum volume as a percentage (10..100) when the limit is enabled. */
val VolumeLimitPercentKey = intPreferencesKey("volumeLimitPercent")

// ---------------------------------------------------------------------------
// Per-track playback settings memory (tempo / pitch)
// ---------------------------------------------------------------------------
/** Master switch for remembering tempo and pitch per song. */
val RememberPlaybackSettingsKey = booleanPreferencesKey("rememberPlaybackSettings")

/** Saved tempo for a specific song. */
fun playbackTempoKey(songId: String) = floatPreferencesKey("playbackTempo_$songId")

/** Saved pitch (in semitones) for a specific song. */
fun playbackPitchKey(songId: String) = floatPreferencesKey("playbackPitch_$songId")

// ---------------------------------------------------------------------------
// Listening streaks and milestones
// ---------------------------------------------------------------------------
/** Master switch for streak tracking and milestone notices. */
val ListeningStreakEnabledKey = booleanPreferencesKey("listeningStreakEnabled")
/** Current consecutive-day listening streak. */
val ListeningStreakCountKey = intPreferencesKey("listeningStreakCount")
/** Longest streak ever reached. */
val ListeningStreakBestKey = intPreferencesKey("listeningStreakBest")
/** Epoch day of the most recent counted listening day. */
val ListeningStreakLastDayKey = longPreferencesKey("listeningStreakLastDay")

// ---------------------------------------------------------------------------
// Quick Settings tile
// ---------------------------------------------------------------------------
/** Allows the notification-shade tile to control playback. */
val QuickSettingsTileEnabledKey = booleanPreferencesKey("quickSettingsTileEnabled")
/** Keeps launcher long-press shortcuts up to date with recent listening. */
val DynamicShortcutsEnabledKey = booleanPreferencesKey("dynamicShortcutsEnabled")

// ---------------------------------------------------------------------------
// A-B loop
// ---------------------------------------------------------------------------
/** Shows the A-B loop control in the player menu. */
val AbLoopEnabledKey = booleanPreferencesKey("abLoopEnabled")

// ---------------------------------------------------------------------------
// Mono audio and channel balance (accessibility)
// ---------------------------------------------------------------------------
/** Downmixes stereo to mono for single-ear listening. */
val MonoAudioKey = booleanPreferencesKey("monoAudio")
/** Left/right balance from -1 (full left) to 1 (full right). */
val AudioBalanceKey = floatPreferencesKey("audioBalance")

// ---------------------------------------------------------------------------
// Karaoke
// ---------------------------------------------------------------------------
/**
 * Strength of karaoke vocal suppression, 0 = off through 1 = full centre cancellation.
 * Adjustable rather than a plain switch because how much of the vocal disappears, and how
 * thin the backing ends up, varies from mix to mix.
 */
val VocalSuppressionKey = floatPreferencesKey("vocalSuppression")

// ---------------------------------------------------------------------------
// New release radar
// ---------------------------------------------------------------------------
/** Checks daily for new albums by followed artists and posts a notification. */
val ReleaseRadarEnabledKey = booleanPreferencesKey("releaseRadarEnabled")

/**
 * Album ids already announced, comma separated, so the same release is never notified
 * twice. Trimmed to the newest [RELEASE_RADAR_SEEN_LIMIT] ids to stop it growing forever.
 */
val ReleaseRadarSeenIdsKey = stringPreferencesKey("releaseRadarSeenIds")

/** Upper bound on remembered album ids; comfortably more than a year of releases. */
const val RELEASE_RADAR_SEEN_LIMIT = 300

// ---------------------------------------------------------------------------
// Mood / activity playlists
// ---------------------------------------------------------------------------
/** Shows the behaviour-derived mood mixes on Home. */
val MoodPlaylistsEnabledKey = booleanPreferencesKey("moodPlaylistsEnabled")

/**
 * Auto-generated mood mixes built from listening behaviour rather than acoustic analysis.
 *
 * YouTube Music exposes no tempo/energy/valence data, so a mood here means "the music you
 * actually reach for at this time of day", taken from the `event` table. [FOCUS] is the
 * exception: it uses track length and sustained play time instead of the clock.
 *
 * Hours are inclusive and expressed in the device's local zone.
 */
enum class MoodPlaylist(
    val titleRes: Int,
    val startHour: Int,
    val endHour: Int,
) {
    MORNING(R.string.mood_morning, 5, 11),
    AFTERNOON(R.string.mood_afternoon, 12, 17),
    EVENING(R.string.mood_evening, 18, 21),
    LATE_NIGHT(R.string.mood_late_night, 22, 4),
    FOCUS(R.string.mood_focus, -1, -1),
    ;

    /** True when the window crosses midnight and needs the wrapping query. */
    val wrapsMidnight: Boolean get() = startHour > endHour
}

// ---------------------------------------------------------------------------
// Smart resume for long-form content
// ---------------------------------------------------------------------------
/** Offers to resume long tracks (mixes, podcasts, live sets) where they left off. */
val SmartResumeEnabledKey = booleanPreferencesKey("smartResumeEnabled")
/** Minimum track length, in minutes, before a position is remembered. */
val SmartResumeMinMinutesKey = intPreferencesKey("smartResumeMinMinutes")
/** Saved playback position for a specific song. */
fun resumePositionKey(songId: String) = longPreferencesKey("resumePosition_$songId")

// ---------------------------------------------------------------------------
// Headphone automation
// ---------------------------------------------------------------------------
/** Resumes playback automatically when headphones are connected. */
val ResumeOnHeadphonesKey = booleanPreferencesKey("resumeOnHeadphones")

// ---------------------------------------------------------------------------
// Gestures
// ---------------------------------------------------------------------------
/** Double tapping the artwork seeks backward or forward. */
val DoubleTapSeekKey = booleanPreferencesKey("doubleTapSeek")
/** How many seconds a single double-tap seek jumps. */
val DoubleTapSeekSecondsKey = intPreferencesKey("doubleTapSeekSeconds")

// ---------------------------------------------------------------------------
// Accessibility and wellbeing
// ---------------------------------------------------------------------------
/** Disables decorative animations across the app. */
val ReduceMotionKey = booleanPreferencesKey("reduceMotion")
/** Reminds the user after a long continuous listening session. */
val ListeningReminderEnabledKey = booleanPreferencesKey("listeningReminderEnabled")
/** How many minutes of listening before a reminder appears. */
val ListeningReminderMinutesKey = intPreferencesKey("listeningReminderMinutes")
/** Larger, higher-contrast lyrics for readability. */
val HighContrastLyricsKey = booleanPreferencesKey("highContrastLyrics")

// ---------------------------------------------------------------------------
// Lyrics extras
// ---------------------------------------------------------------------------
/** Caches translated lyrics so repeat plays are instant and work offline. */
val CacheTranslationsKey = booleanPreferencesKey("cacheTranslations")

// ---------------------------------------------------------------------------
// Discovery
// ---------------------------------------------------------------------------
/** Shows a "this week in previous years" row built from listening history. */
val TimeMachineEnabledKey = booleanPreferencesKey("timeMachineEnabled")
/** Surfaces rarely played songs from the user's own library. */
val HiddenGemsEnabledKey = booleanPreferencesKey("hiddenGemsEnabled")
/** Shows an explainable "because you listened to [artist]" row on Home. */
val BecauseYouListenedEnabledKey = booleanPreferencesKey("becauseYouListenedEnabled")

/**
 * Home row order and visibility, stored as a comma-separated list of [HomeRow] names.
 * Rows missing from the stored value fall back to the end of the list in declaration
 * order, so adding a new row in a future release does not strand it.
 */
val HomeRowOrderKey = stringPreferencesKey("homeRowOrder")

/**
 * The Home screen rows the user can reorder or hide. [Quick picks] is deliberately not
 * included: it is the primary landing content and always renders first.
 */
enum class HomeRow(val titleRes: Int) {
    FORGOTTEN_FAVORITES(R.string.forgotten_favorites),
    KEEP_LISTENING(R.string.keep_listening),
    HIDDEN_GEMS(R.string.hidden_gems),
    TIME_MACHINE(R.string.time_machine),
    BECAUSE_YOU_LISTENED(R.string.because_you_listened),
    MOOD(R.string.mood_playlists),
}

// ---------------------------------------------------------------------------
// Playback speed per content type
// ---------------------------------------------------------------------------
/** Applies a separate default speed to long-form content such as podcasts and DJ sets. */
val SpeedPerContentTypeKey = booleanPreferencesKey("speedPerContentType")
/** Default speed used for long-form content when [SpeedPerContentTypeKey] is on. */
val LongFormPlaybackSpeedKey = floatPreferencesKey("longFormPlaybackSpeed")
/** Track length, in minutes, at or above which a track counts as long-form. */
val LongFormMinMinutesKey = intPreferencesKey("longFormMinMinutes")

// ---------------------------------------------------------------------------
// Silent outro skip
// ---------------------------------------------------------------------------
/** Skips to the next track once a trailing silent outro is reached. */
val SkipSilentOutroKey = booleanPreferencesKey("skipSilentOutro")
/** How many seconds of trailing audio are treated as a skippable outro. */
val SilentOutroSecondsKey = intPreferencesKey("silentOutroSeconds")

// ---------------------------------------------------------------------------
// Personalisation: font and launcher icon
// ---------------------------------------------------------------------------
/** Font family applied app-wide. Stored via [CustomFontKey]. */
enum class AppFont {
    SYSTEM,
    LINOTTE,
    POPPINS,
    SF_PRO,
    ANYBODY,
    SANS_SERIF,
    SERIF,
    MONOSPACE,
    CURSIVE,
}

/**
 * Launcher icon variants. [aliasName] must match an `activity-alias` in the manifest,
 * since [com.Chenkham.Echofy.utils.AppIconManager] toggles them by component name.
 */
enum class AppIcon(val aliasName: String) {
    DEFAULT("com.Chenkham.Echofy.icon.Default"),
    CLASSIC("com.Chenkham.Echofy.icon.Classic"),
    MONOCHROME("com.Chenkham.Echofy.icon.Monochrome"),
}

val GitHubReleasesEtagKey = stringPreferencesKey("github_releases_etag")
val GitHubReleasesJsonKey = stringPreferencesKey("github_releases_json")
val GitHubReleasesLastCheckedAtKey = longPreferencesKey("github_releases_last_checked_at")
val GitHubReleasesFingerprintKey = stringPreferencesKey("github_releases_fingerprint")





val SpotifySpDcKey = stringPreferencesKey("spotify_sp_dc")
val SpotifySpKeyKey = stringPreferencesKey("spotify_sp_key")
val SpotifyAccessTokenKey = stringPreferencesKey("spotify_access_token")
val SpotifyAccessTokenExpiresAtKey = longPreferencesKey("spotify_access_token_expires_at")
val SpotifyAccountNameKey = stringPreferencesKey("spotify_account_name")
val SpotifyAccountAvatarUrlKey = stringPreferencesKey("spotify_account_avatar_url")
val ShowSpotifyPlaylistsKey = booleanPreferencesKey("show_spotify_playlists")
val SpotifyLibraryPlaylistsCacheKey = stringPreferencesKey("spotify_library_playlists_cache")

val ProviderOrderKey = stringPreferencesKey("provider_order")

val EnableBetterLyricsKey = booleanPreferencesKey("enable_better_lyrics")

val DiscordActivityNameKey = stringPreferencesKey("discordActivityName")
val DiscordActivityDetailsKey = stringPreferencesKey("discordActivityDetails")
val DiscordActivityStateKey = stringPreferencesKey("discordActivityState")
val DiscordActivityButton1LabelKey = stringPreferencesKey("discordActivityButton1Label")
val DiscordActivityButton1UrlSourceKey = stringPreferencesKey("discordActivityButton1UrlSource")
val DiscordActivityButton1CustomUrlKey = stringPreferencesKey("discordActivityButton1CustomUrl")
val DiscordActivityButton2LabelKey = stringPreferencesKey("discordActivityButton2Label")
val DiscordActivityButton2UrlSourceKey = stringPreferencesKey("discordActivityButton2UrlSource")
val DiscordActivityButton2CustomUrlKey = stringPreferencesKey("discordActivityButton2CustomUrl")
val DiscordActivityButton1EnabledKey = booleanPreferencesKey("discordActivityButton1Enabled")
val DiscordActivityButton2EnabledKey = booleanPreferencesKey("discordActivityButton2Enabled")
val DiscordShowWhenPausedKey = booleanPreferencesKey("discordShowWhenPaused")
val DiscordActivityTypeKey = stringPreferencesKey("discordActivityType")
val DiscordPresenceIntervalValueKey = intPreferencesKey("discordPresenceIntervalValue")
val DiscordPresenceIntervalUnitKey = stringPreferencesKey("discordPresenceIntervalUnit")
val DiscordPresenceStatusKey = stringPreferencesKey("discordPresenceStatus")
val DiscordActivityPlatformKey = stringPreferencesKey("discordActivityPlatform")
