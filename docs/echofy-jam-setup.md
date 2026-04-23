# Echofy Jam Setup

This is the deployment checklist for the internet-wide Firebase Jam system.

## What You Need

- `1` Cloudflare Worker project for the control plane
- `1+` Firebase projects on Blaze
- `1+` Realtime Database instances or shard projects
- Android `google-services.json` for the bootstrap Firebase project

## Architecture

- App:
  - playback
  - Jam UI
  - host and guest sync behavior
- Firebase:
  - room playback state
  - shared queue
  - votes
  - presence
- Cloudflare:
- shard registry JSON
- deep-link routing
- optional room allocation endpoint for controller-picked room codes

## Firebase

For each Jam shard you need:

- `projectId`
- `appId`
- `apiKey`
- `databaseUrl`
- optional `messagingSenderId`
- optional `storageBucket`

Suggested Realtime Database path layout:

```json
jam/v1/rooms/{roomId}/meta
jam/v1/rooms/{roomId}/host
jam/v1/rooms/{roomId}/presence
jam/v1/rooms/{roomId}/playback
jam/v1/rooms/{roomId}/queue/items
```

For initial bring-up, the repo now includes:

- [firebase/firebase.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/firebase.json)
- [firebase/database.rules.dev.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.dev.json)
- [firebase/database.rules.auth.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.auth.json)
- [jam-registry.example.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/jam-registry.example.json)
- [wrangler.toml.example](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/wrangler.toml.example)
- [echofy-jam-smoke-test.md](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/echofy-jam-smoke-test.md)

Important:

- the current rule file is for development and internal testing
- it validates Jam data shape, but it is not production-hard
- read [echofy-jam-security.md](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/echofy-jam-security.md) before public rollout
- enable Firebase Anonymous Auth in every Jam shard project
- switch to [database.rules.auth.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.auth.json) only after anonymous auth is enabled and your shard sessions can sign in successfully

## Cloudflare Registry

Serve a JSON document like this:

```json
{
  "version": 1,
  "defaultTtlSeconds": 86400,
    "inviteBaseUrl": "https://your-worker.workers.dev/r/",
  "shards": [
    {
      "id": "01",
      "status": "ACTIVE",
      "region": "asia-south1",
      "weight": 100,
      "capacity": {
        "softRooms": 50000,
        "hardRooms": 70000
      },
      "firebase": {
        "apiKey": "AIza...",
        "appId": "1:123:android:abc",
        "projectId": "echofy-jam-01",
        "databaseUrl": "https://echofy-jam-01-default-rtdb.firebaseio.com",
        "messagingSenderId": "1234567890",
        "storageBucket": "echofy-jam-01.appspot.com"
      },
      "features": {
        "canCreateRooms": true,
        "canJoinRooms": true
      }
    }
  ]
}
```

You do not need a custom domain on day one.

- If you have no domain yet, use a temporary `workers.dev` URL.
- If you want to keep it even simpler, leave `inviteBaseUrl` empty and share only room codes.

Recommended no-domain flow:

1. Deploy the Worker and note your `https://your-worker.workers.dev` URL.
2. Open `https://your-worker.workers.dev/connect` once on the same Android device.
3. The app stores:
   - `jamRegistryUrl`
   - `jamInviteBaseUrl`
   - `jamRoomAllocateUrl`
4. After that, the app can fetch the shard registry from your Worker without a rebuild.

Optional app resource overrides:

```xml
<string name="jam_registry_url">https://your-worker.workers.dev/jam-registry.json</string>
<string name="jam_invite_base_url">https://your-worker.workers.dev/r</string>
<string name="jam_room_allocate_url">https://your-worker.workers.dev/v1/rooms/allocate</string>
```

## First Live Bring-Up

Use this order for your very first working shard:

1. In Firebase Console, create a new project for shard `01`.
2. Add your Android app package `com.Chenkham.Echofy`.
3. Download `google-services.json`.
4. Place it at [app/google-services.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/app/google-services.json).
5. In Firebase Console, enable Realtime Database.
6. In Firebase Console, enable Anonymous Authentication.
7. Apply [database.rules.dev.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.dev.json) first for bring-up.
8. Copy your Firebase values into [jam-registry.example.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/jam-registry.example.json) and save a real registry JSON.
9. In Cloudflare, create a Worker and a KV namespace bound as `JAM_REGISTRY`.
10. Start from [cloudflare-worker-jam-example.js](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/cloudflare-worker-jam-example.js) and [wrangler.toml.example](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/docs/wrangler.toml.example).
11. Put your real registry JSON into KV under key `active`.
12. Deploy the Worker.
13. Open `https://your-worker.workers.dev/connect` on the same Android device.
14. Create a Jam room from the app and verify that the room code starts with your shard id, for example `01-XXXXXX`.

After that works:

1. Switch Firebase rules from `dev` to [database.rules.auth.json](/C:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/firebase/database.rules.auth.json).
2. Verify anonymous auth is succeeding on that shard.
3. Add shard `02` in the registry as `CANARY`.
4. Only after canary looks healthy, raise shard `02` weight or mark it `ACTIVE`.

## Cloudflare KV Example

If you use Wrangler, the core flow is:

```bash
wrangler kv namespace create JAM_REGISTRY
wrangler kv key put --binding JAM_REGISTRY active ./docs/jam-registry.example.json
wrangler deploy
```

Replace the example registry JSON with your real shard values first.

## Expansion Flow

When one shard gets hot:

1. Create a new Firebase shard project or database instance.
2. Apply the same database rules.
3. Add the shard to the Cloudflare registry JSON.
4. Set status to `ACTIVE`.
5. New rooms start using it without rebuilding the Android app.

## Current App State

Already implemented in the app:

- bootstrap Firebase shard fallback
- remote registry fetch with local cache
- runtime Jam config via `echofy://config?...`
- optional control-plane room allocation with local shard fallback if the Worker is unavailable
- auth-aware shard session activation with anonymous Firebase auth
- host room creation
- guest join by room code
- guest room validation against live Firebase room meta before the session is treated as healthy
- deep link room join via `echofy://jam/{roomCode}`
- playback sync
- periodic host playback heartbeat for drift correction
- shared queue
- queue voting
- top-voted playback trigger
- host queue reconciliation that preserves collaborative queue items and votes while keeping local playback aligned
- host local upcoming playback queue now follows shared Firebase queue changes when guests add or vote songs
- Jam sheet host next/previous controls now route through the shared queue instead of only the local player queue
- participant presence UI
- room close propagation when the host ends the Jam
- periodic presence heartbeat
- host-owned room activity heartbeat
- guest-side stale-room detection using `lastActivityAtEpochMs`

Still to do:

- tighter per-voter Firebase rule enforcement for queue mutations
- better host queue reconciliation
- QR invite flow
