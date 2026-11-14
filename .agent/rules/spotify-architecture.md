# ECHOFY SPOTIFY-GRADE ARCHITECTURE & AUTO-LOOP RULES

## Ponytail Anti-Bloat Mandate
- Before creating any new class, helper, or dependency, search the entire codebase first to reuse existing functions or built-in Kotlin/Android APIs.
- Never over-engineer or write duplicate code.
- Max 250 lines per file. Split giant classes/files into small, clean, single-responsibility components with KDoc comments.
- Delete dead Kotlin files, unused functions, and unreferenced XML/drawables ONLY after verifying zero references across the whole repository.

## Deterministic Quality Gates
- Always run `.\gradlew.bat ktlintFormat detekt lintDebug assembleDebug` after every batch of changes (or every 2–3 file edits).
- Fix every error and warning immediately before committing to Git.
- Always call `sequential-thinking` MCP before refactoring more than 2 files.
- Always query `google-developer-knowledge` or `context7` MCP for Media3/ExoPlayer, Room, Coil/Glide, and Android 10–16 APIs.

## Play Store & Multi-Device Safety
- NEVER alter `applicationId`, signing configs, or existing Play Store user storage keys.
- NEVER alter Room database version or schemas without a tested Migration.
- Guard newer Android APIs with `Build.VERSION.SDK_INT` checks down to `minSdk`. Fix Android 13+ (`POST_NOTIFICATIONS`, `READ_MEDIA_AUDIO`) and Android 14+ (`foregroundServiceType="mediaPlayback"`) permissions/services so Samsung, Xiaomi, Vivo, and Oppo battery killers never stop background audio.
- Replace hardcoded widths with responsive layouts (`WindowSizeClass` / weights) for tablets and landscape mode.
- Forbid DB, network, file I/O, or bitmap decoding on `Dispatchers.Main` (always use `Dispatchers.IO`).
- Organize into clean MVVM packages: `core/network/`, `core/database/`, `core/player/` (isolated Media3 MediaSessionService), `core/ui/`, and `feature/<name>/` (`data/`, `domain/`, `ui/`).
