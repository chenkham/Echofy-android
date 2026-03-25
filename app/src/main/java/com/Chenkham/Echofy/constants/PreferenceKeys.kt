package com.Chenkham.Echofy.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDateTime
import java.time.ZoneOffset

val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val DarkModeKey = stringPreferencesKey("darkMode")
val PureBlackKey = booleanPreferencesKey("pureBlack")
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
}

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrclib")
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
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
    LOW,
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
val AudioOffload = booleanPreferencesKey("audioOffload")

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
    DOWNLOADED
}

enum class ArtistFilter {
    LIBRARY,
    LIKED
}

enum class AlbumFilter {
    LIBRARY,
    LIKED
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
}

enum class PlayerBackgroundStyle {
    DEFAULT,
    GRADIENT,
    BLUR,
}


enum class PlayerButtonsStyle {
    DEFAULT,
    SECONDARY,
}

enum class MiniPlayerStyle {
    Floating,
    Slim
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
