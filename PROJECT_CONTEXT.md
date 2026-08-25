# Echofy Android — Project Context

Context log for session recovery. Read this first when starting a new session.

## Project basics

- Android music app, package `com.Chenkham.Echofy`, Jetpack Compose UI.
- Version catalog: `gradle/libs.versions.toml`. Compose artifacts come from the `compose` version ref.
- Persistence: Room. DAO lives in `app/src/main/java/com/Chenkham/Echofy/db/DatabaseDao.kt`.
- Preferences: DataStore, accessed with `rememberPreference(key, defaultValue)`; keys declared in `com.Chenkham.Echofy.constants`.
- All user-facing text goes through `app/src/main/res/values/strings.xml`. No hardcoded strings in Compose.

## Architectural decisions

- **Home discovery rows are opt-in.** `HomeViewModel` reads the DataStore snapshot once
  (`appContext.dataStore.data.first()`) and only launches a row's query when its key is `true`.
  Disabled rows cost zero database work rather than being filtered at the UI layer.
- **Parallel loading.** Each row query runs in its own `async(Dispatchers.IO)` and is awaited
  together, so adding a row does not extend home load time serially.
- **Event timestamps bind as `Long`.** `Event.timestamp` is a `LocalDateTime` with a Room
  converter, but the existing analytics queries (e.g. `mostPlayedArtists`) bind epoch millis as
  `Long`. New queries against `event.timestamp` must use `Long` to match. Passing a
  `LocalDateTime` is a compile error.

- **`event.timestamp` is epoch MILLISECONDS in SQL — never pass it raw to a date function.**
  `Converters` stores it via `toEpochMilli()`. SQLite reads a bare number in `date()` /
  `strftime()` as a *Julian day*, so `date(timestamp)` silently returns dates millions of
  years out. It compiles, throws nothing, and quietly returns wrong rows. Two shipped
  features were broken by this before it was found: the listening streak (`listeningDays`)
  and the Time Machine row (`songsPlayedBetween`). Always write
  `date(timestamp / 1000 + :offsetSeconds, 'unixepoch')`.

- **Timestamps are written in UTC, but the UI reasons in local time.** The converter uses
  `ZoneOffset.UTC` while callers use `LocalDate.now()` / `LocalTime.now()`. Every date or
  hour query therefore takes an `offsetSeconds` parameter, supplied by
  `currentUtcOffsetSeconds()` in `utils/TimeZoneOffset.kt`. Without it a user at UTC+5:30
  has plays before 05:30 filed under the previous day, and mood buckets match the wrong
  hours. Read the offset at call time, never cache it, so DST and travel stay correct.

- **Settings UI shape.** Settings screens compose `SettingsGeneralCategory(title, items = listOf { ... })`,
  where each item is a lambda wrapping a `SwitchPreference` / `EnumListPreference` / `ListPreference`.

## Active feature state

Home discovery rows, each behind its own settings toggle in `ContentSettings.kt`:

- Forgotten favorites, Keep listening, Quick picks — always loaded.
- Listening streak — `ListeningStreakEnabledKey`
- Time machine — `TimeMachineEnabledKey`, queries `songsPlayedBetween` around this date last year
- Hidden gems — `HiddenGemsEnabledKey`
- **Because you listened to — `BecauseYouListenedEnabledKey` (added this session)**

### "Because you listened to" wiring

- DAO: `topRecentArtistName(after: Long): String?` picks the top artist by summed play time;
  `songsByArtistName(artistName, limit = 12)` returns least-played library songs by that artist.
- ViewModel: `becauseYouListenedArtist` / `becauseYouListenedSongs` state flows, populated from a
  single gated `async` block using a two-week lookback (`fromTimeStamp`).
- Settings: `SwitchPreference` with the `artist` drawable, placed after Hidden gems.
- Strings: `because_you_listened`, `because_you_listened_desc`. The row title carries an artist
  name placeholder.

### Accessibility / audio settings

Grouped under `accessibility_audio` in `AppearanceSettings.kt`: reduce motion, high contrast
lyrics, listening reminder (with a minutes slider revealed by `AnimatedVisibility`), double tap
to seek plus its seek amount.

## Verification

- Compile check: `./gradlew :app:compileDebugKotlin` — currently passing. This also runs Room's
  annotation processor, so it validates new SQL and DAO signatures.
- Heavy builds and long-running commands should be run by the user in a local terminal.

## Lint & test landscape

- `app/build.gradle.kts` sets `abortOnError = false`, `checkReleaseBuilds = false`, and
  `disable += "MissingTranslation"`. English-only new strings will not fail lint.
- There is **no test source set in the `app` module**. The only test in the repo is
  `innertube/src/test/.../TestPlaylistContinuation.kt`, unrelated to UI or database code.
- Practical verification for app changes is therefore `./gradlew :app:compileDebugKotlin`, which
  also runs KSP and so validates Room SQL and DAO signatures.

## Pending / next steps

- Optional: run `./gradlew :app:lintDebug` locally for a fuller check (heavy; run in your terminal).
- No automated test covers the new DAO queries. Adding a Room instrumented test would need an
  `androidTest` source set in `app`, which does not exist yet.
