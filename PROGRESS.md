# Echofy Autonomous Overhaul Progress Tracker

## Phase 1: Real Play Store Crashes & ANRs (Highest Priority)
- [ ] 1.1 Fix ANR: `Context.startForegroundService() did not then call Service.startForeground()` in `MusicService.kt` (ensure immediate foreground notification attachment).
- [ ] 1.2 Fix Crash: `ForegroundServiceStartNotAllowedException` in `PlayerConnection.kt` and `MusicService.kt` with lifecycle state verification.
- [ ] 1.3 Fix ANR: Eliminate main-thread `runBlocking` on DataStore in `DataStore.kt` and `MusicService.kt` (move all preference reads to `Dispatchers.IO`).
- [ ] 1.4 Fix ANR: Move `MusicService.saveQueueToDisk` off `Dispatchers.Main` onto a background IO coroutine.
- [ ] 1.5 Fix Crash: `ReceiverCallNotAllowedException` in `CompactMusicWidget.kt` using `goAsync()`.
- [ ] 1.6 Fix Crash: `IllegalArgumentException` in Coil image loader initialization in `App.kt`.
- [ ] 1.7 Fix Crash: `IllegalArgumentException` in `ExploreScreen.kt` navigation argument handling.

## Phase 2: Dead Files, Dead Resources, Commented-out Code & Manifest Fixes
- [ ] 2.1 Remove phantom service `.playback.DynamicEchoService` from `app/src/main/AndroidManifest.xml:370` (`MissingClass` error).
- [ ] 2.2 Fix invalid format strings in `HistoryScreen.kt:146`, `LocalPlaylistScreen.kt:518, 657`, and `MusicService.kt:422`.
- [ ] 2.3 Fix `UseAppTint` in `app/src/main/res/layout/widget_lyrics.xml:115, 125` (replace `android:tint` with `app:tint`).
- [ ] 2.4 Remove dead private properties (e.g. `BASE_URL_API` in `JossRedClient.kt`, `context` in `TopPlaylistViewModel.kt`).
- [ ] 2.5 Clean up unused/bloated dependencies identified in `build-health-report.json`.

## Phase 3: Android OS Compatibility, OEM Background Resilience & API Upgrades
- [ ] 3.1 Fix `NewApi` display cutout crash in `VideoPlayerView.kt:240-251` (add `Build.VERSION.SDK_INT >= Build.VERSION_CODES.P` check).
- [ ] 3.2 Guard `SmartAlarmManager.kt:52` with `canScheduleExactAlarms()` check on Android 12+ (API 31+).
- [ ] 3.3 Verify and harden `foregroundServiceType="mediaPlayback"` in `AndroidManifest.xml` against Samsung/Xiaomi/Vivo/Oppo battery killers.
- [ ] 3.4 Replace unlocalized `String.format("%.2f", ...)` with `Locale.US` across `AudioBookmarkManager.kt`, `DiscordRPC.kt`, `ShareStatsTracker.kt`, and `DebugSettings.kt`.
- [ ] 3.5 Fix `RestrictedApi` usage on `dispatchKeyEvent` in `MainActivity.kt`.

## Phase 4: Spotify-Grade Clean MVVM Decomposition (<250 Lines/File)
- [ ] 4.1 Decompose `MainActivity.kt` (currently 2,620+ lines, `onCreate` 1,381 lines) into dedicated lifecycle observers, navigation hosts, and view state binders.
- [ ] 4.2 Modularize `EchofyAiManager.kt` (currently 831 lines, 35 functions) by extracting command classifiers, action executors, and speech listeners.
- [ ] 4.3 Refactor `AppwriteTogetherSessionManager.kt` by extracting strongly-typed session models and removing duplicated string literals.
- [ ] 4.4 Clean noisy AI comments and replace with minimal, senior-grade KDoc documentation.

## Phase 5: Tablet Responsiveness & 120fps Smoothness Verification
- [ ] 5.1 Fix `FlowOperatorInvokedInComposition` in `CreatePlaylistDialog.kt:54`.
- [ ] 5.2 Fix missing scaffold inner padding in `DiscordSettings.kt:140`.
- [ ] 5.3 Implement responsive layout rules (`WindowSizeClass`) for player sheets and drawer components for tablets and foldables.
- [ ] 5.4 Run final end-to-end quality gate: `.\gradlew.bat ktlintFormat assembleDebug lintDebug` with zero errors.
