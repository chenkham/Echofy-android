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

## Playback stream resolution (restored 2026-08-25)

An unverified experimental iteration had broken all playback. The following were reverted to the
known-good behaviour, and the fixes kept:

- `InnerTube.ytClient()` sends `X-Origin` / `Referer` for **every** client again (gating them to
  WEB/TV/MWEB only broke the Android/iOS fallbacks).
- `executePlayerRequest` posts to the relative `"player"` path again, so Ktor's
  `defaultRequest { url(API_URL_YOUTUBE_MUSIC) }` base URL applies. `YouTubeClient.apiUrl()` /
  `playerEndpoint()` / `isMusicClient()` were removed along with `API_URL_YOUTUBE`.
- `YouTubeClient.requestOrigin()` / `requestReferer()` back to the TV-vs-music-origin switch;
  `USER_AGENT_WEB` back to Chrome 141; `TVHTML5` back to Tizen UA + `7.20260114.00.00`;
  `TVHTML5_SIMPLY_EMBEDDED_PLAYER` back to `loginSupported/loginRequired = true`; `IOS` and
  `MOBILE` keep their new device metadata but `loginSupported` / `useSignatureTimestamp` are
  restored to `true`.
- `YTPlayerUtils.shouldSkipCipheredWebCandidate` starts with
  `if (!YouTube.webClientPoTokenEnabled) return false` again — the inverted cookie check was
  dropping every ciphered web stream for logged-out users.
- `STREAM_FALLBACK_CLIENTS` and `candidateMetadataClients` restored the dropped clients
  (`VISIONOS`, `ANDROID_TESTSUITE`, `ANDROID_UNPLUGGED`, `TVHTML5*`, `IPADOS`, `IOS`, `WEB`) so
  recovery paths are not narrowed.
- `App.kt` visitorData validation now accepts `Cgt` **and** `Cgs` prefixes, length `10..120`,
  matching InnerTube's own `Regex("^Cg[t|s]")`. The old `startsWith("Cgt")` + `10..80` check wiped
  valid tokens on every launch.

Kept from the experiment: `PlaybackData.userAgent` plumbed into `MusicService`'s
`songUrlUserAgentCache` so the replayed request's UA matches the client that minted the URL, and the
`ForegroundServiceStartNotAllowedException` suppression.

## Extractor fork swap (2026-08-26) — actual playback root cause

Reverting the experiment was necessary but not sufficient. Logcat showed all 17 fallback clients
failing with `OBTAINED: 0 / FAILED: 15 / SELECTED: 0`. The clients that *did* return `OK` (IOS,
MOBILE, ANDROID_CREATOR) then died in format selection with:

```
org.schabi.newpipe.extractor.exceptions.ParsingException:
  Could not find deobfuscation function with any of the known patterns
  at YoutubeSignatureUtils.getDeobfuscationFunctionNameAndParams
```

i.e. **NewPipeExtractor v0.26.5 can no longer decipher the current YouTube player**, so no stream URL
can ever be produced. Fix (user-approved, chosen for minimal blast radius — no UI touched):

- `gradle/libs.versions.toml`: `extractor = "6305155"` and
  `newpipe-extractor = { module = "com.github.MetrolistGroup.MetrolistExtractor:extractor", ... }`
  (was `com.github.TeamNewPipe:NewPipeExtractor` @ `v0.26.5`). JitPack multi-module coordinate —
  commit `6305155` publishes `MetrolistExtractor`, `extractor`, `timeago-parser`. `jitpack.io` was
  already in `settings.gradle.kts`. Sole consumer is `innertube/build.gradle.kts` →
  `implementation(libs.newpipe.extractor)`.
- The fork's `Downloader` API differs, so `innertube/.../pages/NewPipe.kt`'s
  `NewPipeDownloaderImpl` was adapted (modelled on Metrolist's own impl):
  - `Response(...)` is now 6-arg: `(code, message, headers, body, body.toByteArray(), latestUrl)`.
  - Added the required `executeAsync(request, callback): CancellableCall` override using
    `call.enqueue`, raising `ReCaptchaException` on HTTP 429.
  - New import: `org.schabi.newpipe.extractor.downloader.CancellableCall`.
- `NewPipeUtils.getSignatureTimestamp` / `getStreamUrl` logic is unchanged.
- `TVHTML5_SIMPLY_EMBEDDED_PLAYER.loginRequired = false` (it was being skipped for anonymous users)
  and it now leads `STREAM_FALLBACK_CLIENTS`, matching Metrolist.

`:innertube:compileKotlin` → BUILD SUCCESSFUL. Streams now resolve: logcat shows
`Stream selected successfully with client: WEB_REMIX` and `Successfully obtained playback data`.

## PoToken wiring (2026-08-26) — the 403-at-30-seconds fix

After the extractor swap, playback started but **every song died at exactly ~30s** with:

```
HttpDataSource$InvalidResponseCodeException: Response code: 403
MusicService: Max retry limit reached (4) after ERROR_CODE_IO_BAD_HTTP_STATUS
```

30s is YouTube's grace window for a ciphered `WEB_REMIX` stream requested without a PoToken — the
first range request succeeds, then GVS starts rejecting. The logs confirmed WEB_REMIX was the only
client producing a URL (all Android/iOS clients returned `LOGIN_REQUIRED` / "Sign in to confirm
you're not a bot" / "Please sign in"), and its formats were all `(cipher, ...)`.

Echofy already had a complete `PoTokenWebView` + `PoTokenGenerator` under
`app/.../utils/potoken/`, but **nothing ever called it** — `YouTube.webClientPoTokenEnabled` stayed
`false`, so `PlaybackAuthState.resolvePlayerPoToken` / `resolveGvsPoToken` always returned null.
Wired it up in `YTPlayerUtils`, following Metrolist:

- New `poTokenGenerator = PoTokenGenerator()` field and a `suspend ensureWebPoTokens(videoId,
  sessionId)` helper that runs the WebView generation on `Dispatchers.IO`, then publishes
  `YouTube.poTokenPlayer`, `YouTube.poTokenGvs` and sets `webClientPoTokenEnabled = true`.
- Called from `playerResponseForPlaybackOnce` right after `sessionId` is computed (`dataSyncId` when
  logged in, else `visitorData`), i.e. **before** the metadata `/player` request, so that request
  carries the player token too.
- `shouldSkipCipheredWebCandidate` gained `if (YouTube.poTokenGvs != null) return false`. That guard
  existed to avoid ciphered web streams when PoTokens were "enabled" but absent; now that a real GVS
  token is attached, ciphered web streams are valid and must not be skipped — otherwise enabling
  PoTokens would paradoxically discard the only working client.
- The existing `appendGvsPoToken` call inside `NewPipeUtils.getStreamUrl` needed no change; it now
  actually resolves a token and appends `pot=`.

Note: [PlayerConnection.kt](file:///c:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/app/src/main/java/com/Chenkham/Echofy/playback/PlayerConnection.kt#L663-L685)
wipes `visitorData` on any 403. Left as-is — `PoTokenGenerator` keys its cache on `sessionId`, so a
refreshed `visitorData` transparently forces regeneration.

No UI/UX files were touched.

## Aligned with OpenTune upstream (2026-08-26) — why 30s persisted

PoTokens generated successfully (`PoTokens ready for <videoId>`) yet playback still died at ~30s, and
`PoTokenWebView` warned `Token size 598 bytes may be outside expected range (110-128)` for the GVS
token and `88 bytes` for the player token. Rather than keep chasing the token format, compared
against [OpenTune master](https://github.com/Arturo254/OpenTune/tree/master), which the user reports
works instantly with no sign-in and **has no PoToken generator at all**.

The insight: OpenTune never needs one because its fallback order puts `IOS, MOBILE, ANDROID_MUSIC,
IOS_MUSIC, ANDROID_VR_NO_AUTH` first, and those clients return **unciphered direct URLs** that GVS
serves without a PoToken. Echofy had reordered the list to put `TVHTML5_SIMPLY_EMBEDDED_PLAYER` and
the VR clients first and pushed the mobile clients back, so it kept landing on ciphered `WEB_REMIX` —
the one path that mandates a PoToken. The cipher/PoToken work was treating a symptom of the ordering.

Changes to realign with upstream:

- `YouTubeClient.kt`: reverted Echofy's local drift so it now matches OpenTune verbatim — `IOS` and
  `MOBILE` lost the added `osName`/`deviceMake`/`deviceModel`/`packageName`/`friendlyName` metadata
  (extra context fields change how YouTube fingerprints the client), `ANDROID_VR_NO_AUTH` went back
  to version `1.37` from `1.61.48`, and `TVHTML5_SIMPLY_EMBEDDED_PLAYER.loginRequired` returned to
  `true`.
- `YTPlayerUtils.STREAM_FALLBACK_CLIENTS`: replaced with OpenTune's exact order.
- `ensureWebPoTokens` is no longer called eagerly per track. It now runs lazily, only when a ciphered
  web candidate is actually the best remaining option, and short-circuits if a GVS token already
  exists. This also removes a WebView round-trip from every song's start (part of the "slow to
  start" complaint).

Deliberately **not** reverted: the extractor stays on the Metrolist fork (`6305155`) rather than
OpenTune's `TeamNewPipe:NewPipeExtractor v0.25.2`. v0.25.2 throws `Could not find deobfuscation
function with any of the known patterns` against the current YouTube player, so it would leave the
ciphered-web last resort permanently broken. The fork is a superset here — with the ordering fixed,
that path should rarely be reached anyway.

Verified: no diagnostics in `YTPlayerUtils.kt` or `YouTubeClient.kt`. `InnerTube.kt` (`ytClient`
headers, `player`/`executePlayerRequest`) and `NewPipe.kt`'s `getStreamUrl` were compared line by
line against OpenTune and already match; only the `Downloader` impl differs, as required by the
fork's API.

## Music recognition (Shazam) — wired up 2026-08-25

The feature already existed but was unreachable dead code. Now:

- `NavigationBuilder.kt` registers `composable(MusicRecognitionRoute) { MusicRecognitionScreen(navController) }`
  inside `TouchBlockingWrapper`, after the `notifications` route.
- `MainActivity.kt` top app bar has a mic `IconButton` (before the notifications badge) calling
  `navController.openMusicRecognition()`.
- Backing pieces were already in place: `:shazamkit` module (`Shazam.recognize`,
  `ShazamSignatureGenerator`), `music_recognition*` strings in `values/opentune_strings.xml`,
  `RECORD_AUDIO` in the manifest.
- `FloatingNavigationToolbar` still exposes `onMusicRecognitionClick` but the composable itself is
  never used anywhere, so it remains dead.

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

- **Verify the PoToken fix on device**: `.\gradlew.bat installDebug`, play a song, and confirm it
  goes past 30s. Success signals in logcat: `PoTokens ready for <videoId>` (YTPlayerUtils) and no
  `Response code: 403`. If PoToken generation fails, look for `PoToken generation failed` or
  `PoTokenGenerator` / `PoTokenWebView` errors — the device WebView may be too old.
- Verify the mic button in the top app bar opens the Shazam screen.
- Note: `:innertube` is a plain JVM module — its compile task is `:innertube:compileKotlin`, not
  `:innertube:compileDebugKotlin`.
- Optional: run `./gradlew :app:lintDebug` locally for a fuller check (heavy; run in your terminal).
- No automated test covers the new DAO queries. Adding a Room instrumented test would need an
  `androidTest` source set in `app`, which does not exist yet.
