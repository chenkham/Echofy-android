# Echofy Jam Security Notes

This is the honest security status of the current Firebase Jam implementation.

## Current State

Today the app writes directly to Firebase Realtime Database using public Firebase config.

That means:

- the app can sync rooms without your own server
- room creation and voting work now
- but strict production-grade Firebase rules still depend on Firebase Anonymous Auth being enabled on every shard

## What Is Already Better

- room codes are shard-aware
- the app now uses a stable per-install Jam participant id instead of a fresh random id every join
- the app silently signs into each Firebase shard anonymously when possible
- room activation now waits for shard auth when it is available, instead of immediately writing host metadata and hoping backfill lands later
- host, presence, and playback records now carry auth-aware fields
- rooms now publish explicit `active` / `closed` lifecycle state
- presence and room activity timestamps are refreshed periodically while connected
- only the host now refreshes room-level activity heartbeats
- the host also republishes playback snapshots on a short heartbeat so guests can recover drift over time
- guest joins now wait for live room metadata and fail closed if the room never resolves as joinable
- guests now also treat stale room heartbeats as a lost-host condition, so dead rooms do not linger forever as joinable
- the database shape is normalized enough to validate room, playback, presence, and queue objects

## What You Can Deploy Right Now

Use [database.rules.dev.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.dev.json) for bring-up and internal testing.

It does:

- deny everything outside the Jam tree
- allow Jam reads and writes
- validate the core data structure so bad payloads are reduced

It does not do:

- real user authentication
- room-owner-only writes
- rate limiting
- anti-spam protection

There is now also a stronger candidate ruleset:

- [database.rules.auth.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.auth.json)

That file is a next-step ruleset for:

- authenticated room reads
- host-owned room metadata and explicit room close
- host-only playback writes
- auth-bound presence writes
- queue writes allowed only while the room is `active`

It still leaves queue writes broader than ideal, because queue reorder and vote transactions currently rewrite whole queue item payloads instead of enforcing voter ownership at a smaller child path. Votes now prefer `auth.uid`, with local participant id only as a fallback if auth is not ready yet.

## Production Hardening Path

The next security step should be:

1. Enable Firebase Anonymous Auth.
2. Keep Firebase Anonymous Auth enabled for every shard project.
3. Store `auth.uid` on presence and host records.
4. Change Firebase rules so:
   - anyone authenticated can read a room they know
   - only the host uid can update room meta or close the room
   - only the host uid can write playback
   - each uid can only write its own presence node
   - queue writes stop once the room is closed
5. Finish moving queue voting to auth-uid-first enforcement all the way down into Firebase rules.
6. Optionally have Cloudflare mint short-lived room-scoped tokens later.

## Why I Did Not Fake “Secure” Rules

Without Firebase Auth or a custom token flow, any rule that pretends to protect host-only writes would be misleading. The current rule file is intentionally labeled `dev` so you do not accidentally think it is production-hard.
