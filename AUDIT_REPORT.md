# ECHOFY ARCHITECTURE & STABILITY AUDIT REPORT
**Target:** Google Play Store Stability (Spotify / YouTube Music Engineering Standard)  
**Date:** 2026-11-14  
**Audited Stack:** Google Play Developer Reporting Vitals, Android Lint, Detekt, Dependency Analysis, Media3, Compose.

---

## 1. Real Play Store Crashes & ANRs (From Google Play Vitals API)

### 🔴 Critical ANRs (Affecting 37.5% of crashing users)
1. **`Context.startForegroundService() did not then call Service.startForeground()`**
   - **Target File:** `app/src/main/java/com/Chenkham/Echofy/playback/MusicService.kt`
   - **Root Cause:** Android requires `startForeground()` within a strict 5–10s window. Asynchronous operations or queue loading before `startForeground()` leads to an immediate ANR kill by the system server.
2. **Main-Thread DataStore Blocking (`DataStoreKt.get`)**
   - **Target Files:** `app/src/main/java/com/Chenkham/Echofy/utils/DataStore.kt`, `MusicService.kt`
   - **Root Cause:** Calling `runBlocking` or synchronous `.get()` on DataStore on `Dispatchers.Main` freezes input dispatching and causes ANRs.
3. **Synchronous Queue Serialization on Main Thread (`saveQueueToDisk`)**
   - **Target File:** `app/src/main/java/com/Chenkham/Echofy/playback/MusicService.kt`
   - **Root Cause:** Serializing hundreds of queue songs to JSON synchronously on the main thread during playback state changes blocks UI rendering.

### 🔴 Top Fatal Crashes (Affecting 20.8% of users)
1. **`ForegroundServiceStartNotAllowedException` (Android 12–15 / API 31+)**
   - **Target File:** `app/src/main/java/com/Chenkham/Echofy/playback/PlayerConnection.kt`, `MusicService.kt`
   - **Root Cause:** Background service launch without verifying valid foreground service exemption conditions.
2. **`ReceiverCallNotAllowedException` in Widget**
   - **Target File:** `app/src/main/java/com/Chenkham/Echofy/CompactMusicWidget.kt`
   - **Root Cause:** Calling restricted context actions (e.g. synchronous service binds/registrations) directly inside a BroadcastReceiver coroutine without `goAsync()`.
3. **`IllegalArgumentException` in ImageLoader Setup**
   - **Target File:** `app/src/main/java/com/Chenkham/Echofy/App.kt`
   - **Root Cause:** Coil `newImageLoader` config error when sizing cache or configuring OkHttp dispatchers.
4. **`IllegalArgumentException` in ExploreScreen Navigation**
   - **Target File:** `app/src/main/java/com/Chenkham/Echofy/ui/screens/ExploreScreen.kt`
   - **Root Cause:** Missing or mismatched route arguments passed into Compose navigation.

---

## 2. Manifest & Android OS Compatibility (`NewApi` / OEM Killers)

1. **Manifest Phantom Service (`MissingClass` Error)**
   - **File:** `app/src/main/AndroidManifest.xml:370`
   - **Issue:** Registered `.playback.DynamicEchoService` does not exist in any module. Will crash immediately if invoked by system or intent.
2. **Display Cutout Crash on Android 8.0/8.1 (`minSdk 26`)**
   - **File:** `app/src/main/java/com/Chenkham/Echofy/ui/player/VideoPlayerView.kt:240-251`
   - **Issue:** `WindowManager.LayoutParams.layoutInDisplayCutoutMode` called directly without `Build.VERSION.SDK_INT >= Build.VERSION_CODES.P` check.
3. **Exact Alarm Permission Vulnerability**
   - **File:** `app/src/main/java/com/Chenkham/Echofy/utils/SmartAlarmManager.kt:52`
   - **Issue:** `setAlarmClock` called without guarding against `canScheduleExactAlarms()` on Android 12+ (API 31+).
4. **Foreground Service Type Declaration for Android 14+ (API 34+)**
   - **File:** `app/src/main/AndroidManifest.xml`
   - **Issue:** Must ensure `foregroundServiceType="mediaPlayback"` is declared with `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission to survive Samsung/Xiaomi/Oppo aggressive background killers.

---

## 3. Architecture Bloat & God-Classes (>250 Lines)

1. **`MainActivity.kt`**: Over 2,620 lines. The `onCreate` method alone is **1,381 lines**, handling deep links, in-app updates, permissions, navigation observers, and dialog states in a single block. Must be decomposed into modular lifecycle observers and composables.
2. **`EchofyAiManager.kt`**: 831 lines with 35 methods and cyclomatic complexity of 38. Contains repetitive `cmd == "play" || cmd == "resume"` string parsing that belongs in an intent parser strategy.
3. **`AppwriteTogetherSessionManager.kt`**: 37 functions with duplicated raw string keys ("stateVersion", "thumbnailUrl", "closed") prone to typos.

---

## 4. UI Stutter, Compose Smells & Tablet Responsiveness

1. **Flow Operator Inside Composition (`FlowOperatorInvokedInComposition`)**
   - **File:** `app/src/main/java/com/Chenkham/Echofy/ui/component/CreatePlaylistDialog.kt:54`
   - **Issue:** Flow transformations created repeatedly on every recomposition instead of retained in ViewModel or `remember`.
2. **Unused Material3 Scaffold Padding**
   - **File:** `app/src/main/java/com/Chenkham/Echofy/ui/screens/settings/DiscordSettings.kt:140`
   - **Issue:** Layout overlaps system bars and navigation bar because padding isn't consumed.
3. **Locale-Insensitive `String.format`**
   - **Files:** `AudioBookmarkManager.kt`, `DiscordRPC.kt`, `ShareStatsTracker.kt`, `DebugSettings.kt`
   - **Issue:** Uses implicit default locale, formatting decimals with commas (e.g. `0,75`) which breaks network headers and storage formats.
4. **Hardcoded Layout Widths & Tablet Breakage**
   - **Files:** Player sheets, queue drawers, and navigation grids lack `WindowSizeClass` adaptation, causing stretched phone UI on tablets and foldables.

---

## 5. Dead Code & Unreferenced Resources

1. Unused private properties: `BASE_URL_API` in `JossRedClient.kt`, `context` in `TopPlaylistViewModel.kt`.
2. Invalid format strings in `HistoryScreen.kt:146`, `LocalPlaylistScreen.kt:518, 657`, and `MusicService.kt:422`.
3. Legacy XML tint attributes: `android:tint` in `widget_lyrics.xml` instead of AppCompat/Material `app:tint`.
