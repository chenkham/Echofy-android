# Echofy Jam Smoke Test

Use this after you connect the first real Firebase shard and Cloudflare Worker.

## Device Setup

- `Device A`: host
- `Device B`: guest
- both devices should be signed into the same app build
- both devices should have internet access

## Test 1: Control Plane Connect

1. Open `https://your-worker.workers.dev/connect` on `Device A`.
2. Confirm the app shows `Echofy Jam control plane connected`.
3. Repeat on `Device B`.

Expected:

- no crash
- no malformed deep-link error
- room creation should still work even if the Worker allocation endpoint is temporarily unavailable

## Test 2: Room Create + Join

1. On `Device A`, open `Echofy Jam (Internet)`.
2. Create a room.
3. Verify the room code starts with your shard prefix, for example `01-`.
4. On `Device B`, join using the room code.

Expected:

- host enters the active Jam screen
- guest joins without creating a ghost room
- participant list shows both devices

## Test 3: Playback Sync

1. Start a song on `Device A`.
2. Let `Device B` join while the song is already in progress.
3. Wait 10 to 15 seconds.

Expected:

- guest should land close to the host position
- playback should keep correcting over time from heartbeat updates
- pause/play on host should affect guest

## Test 4: Shared Queue

1. On `Device A`, make sure there is a current song playing.
2. On `Device B`, add songs into the shared queue.
3. Add at least 3 songs total.
4. Upvote one of the next few votable tracks.

Expected:

- queue updates appear on both devices
- voting only affects the front voting window
- if no votes exist, normal queue order still works

## Test 5: Host Skip Behavior

1. On `Device A`, press next from the Jam controls.
2. Let a song end naturally once.

Expected:

- next song comes from the shared Firebase queue
- host local playback queue stays aligned with the shared queue
- guest follows the new song

## Test 6: Host Disconnect

1. Start a Jam with both devices connected.
2. Force close the host app or disconnect host internet long enough for the room heartbeat to go stale.

Expected:

- guest should eventually see the room as unavailable
- dead rooms should not remain joinable forever

## Before Public Testing

- switch from `database.rules.dev.json` to `database.rules.auth.json`
- verify Firebase Anonymous Auth is enabled on every shard
- test `workers.dev/connect` again after any Worker change
- add a second shard in `CANARY` before sending the app to more users
