---
description: Complete autonomous cleanup, bug-fix, and Spotify-grade architecture overhaul for Echofy
---
# Echofy Autonomous Self-Healing Loop
Work continuously in a loop until every item in `PROGRESS.md` is checked `[x]` and `.\gradlew.bat assembleDebug lintDebug` passes with zero errors.
## Step 0: Initialize Safety & Task Board
1. Run `git checkout -b echofy-clean-v2` and commit a backup snapshot (`git add . ; git commit -m "Backup before autonomous loop"`).
2. Add `detekt` and `ktlint` Gradle plugins if missing, and run `.\gradlew.bat detekt lintDebug`.
3. Create `PROGRESS.md` in the root folder listing every dead file, duplicate block, Lint `NewApi`/OEM issue, bloated file (>250 lines), and rigid non-tablet layout found across the repo.
## Continuous Loop (Repeat Until `PROGRESS.md` Is 100% Complete)
For each unchecked `[ ]` item in `PROGRESS.md`:
1. Plan with `sequential-thinking` and verify APIs with `google-developer-knowledge` / `context7` MCPs.
2. Clean up, split, or fix 1 to 3 files at a time following `.agent/rules/spotify-architecture.md`.
3. Run `.\gradlew.bat ktlintFormat assembleDebug lintDebug`. Fix any errors immediately.
4. Mark `[x]` in `PROGRESS.md` and run `git add . ; git commit -m "chore/fix: <task summary>"`.
5. Immediately move to the next `[ ]` item without stopping.
