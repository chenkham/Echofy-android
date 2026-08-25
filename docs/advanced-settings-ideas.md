# Feature Proposals

## Part A — Approved (holding for implementation)

These three are confirmed and parked until you give the go-ahead.

### A1. Per-Track Settings Memory
Remembers tempo + pitch per song and re-applies on the next play. Reuses the exact
pattern already proven by `lyricsSyncOffsetKey(songId)` in
[PreferenceKeys.kt](file:///c:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/app/src/main/java/com/Chenkham/Echofy/constants/PreferenceKeys.kt#L173):
a per-song DataStore key, read on `onMediaItemTransition`, written on change.
**Location:** Advanced panel. **Complexity: Low.**

### A2. A–B Loop Repeat
Mark point A and point B, loop that section until cleared. Combined with the existing
tempo slider this becomes a practice loop. Needs `loopStartMs`/`loopEndMs` state, a
position callback that seeks back to A past B, and a highlighted range on the seek bar.
**Location:** Advanced panel. **Complexity: Medium.**

### A3. Mono Audio & Channel Balance
Mono downmix switch plus L/R balance slider. Genuine accessibility feature — single-ear
hearing loss, one earbud, broken earbud. Custom `AudioProcessor` in the Media3 chain.
**Location:** Player & controls settings, *not* Advanced (per your call). **Complexity: Medium.**

Dropped as already shipped: skip silence (`SkipSilenceKey`), crossfade
(`CrossfadeEnabledKey` / `SeamlessDJCrossfadeKey`), normalisation
(`AudioNormalizationKey`), lyrics tool grouping.

---

## Part B — New Ideas

Each of these was checked against the codebase first. None exist today.

### B1. Local / Offline Music Playback

**The gap.** There is no `MediaStore.Audio` query anywhere in the app. `RingtoneUtils`
writes *to* MediaStore, but nothing reads the user's own music files. Echofy is
streaming-only.

**What it does.** Scans the device for audio files, folds them into Library alongside
streamed content, and plays them through the same player — same queue, same lyrics, same
equalizer, same widget.

**Why it matters most.** This is the biggest structural gap. Users with ripped albums, DJ
sets, regional music that isn't on YouTube, or their own recordings currently need a
second app. It also makes Echofy fully useful with no internet, which matters a lot in
the Indian market the app targets. Competitors like Poweramp and Musicolet are
local-first — this is table stakes for being someone's *only* music app.

**Implementation.** `MediaStore.Audio` query behind `READ_MEDIA_AUDIO`, map rows into the
existing `Song` entity with an `isLocal` flag, add a Library filter. ExoPlayer plays local
URIs natively, so playback needs no change. Folder browsing and tag editing can follow.

**Complexity: Medium-High.** Mostly data-layer and permissions work; no new playback stack.

---

### B2. Chromecast / Speaker Casting

**The gap.** No `CastPlayer` or `MediaRouter` anywhere. A `cast.xml` drawable exists with
no implementation behind it.

**What it does.** Casts to Chromecast, Google TV, and Cast-enabled speakers, with the
phone as remote and the queue transferring intact.

**Why it matters.** Music is social — parties, family rooms, gatherings. Today the audio
is trapped on the handset. Casting is also the feature users most often assume exists,
and it complements the Together Session feature already shipped.

**Implementation.** Media3 ships `CastPlayer` and `MediaRouteButton`; the work is swapping
the active `Player` instance when a route connects and mirroring queue state. Needs the
Cast SDK dependency and a receiver app ID.

**Complexity: Medium.** Well-trodden path with first-party Media3 support.

---

### B3. Full-Text Lyrics Search ("find the song from one line")

**The gap.** Lyrics are fetched and cached per song, but nothing searches *across* them.

**What it does.** Type a half-remembered line — "tose naina", "we could be heroes" — and
get back songs whose lyrics contain it, searched across everything already cached, with an
online fallback.

**Why it's genuinely cool.** This is the "I only remember one line" problem, which is how
people actually hunt for songs. Almost no competitor does it well. Because lyrics already
live in Room, an FTS index makes this instant and fully offline — a feature that is cheap
here but expensive for anyone without a local lyrics store.

**Implementation.** Add an FTS4/FTS5 virtual table over the existing lyrics table, a Room
`@Query` with `MATCH`, and a "Lyrics" tab in the existing search UI with the matched line
shown as a snippet.

**Complexity: Low-Medium.** Room supports FTS directly; the data is already on device.

---

### B4. Smart Offline Mode / Auto-Download

**The gap.** Downloads exist, but they are entirely manual. `DataSaverEnabledKey` exists
without predictive caching behind it.

**What it does.** Automatically keeps the songs you actually play available offline —
"top 50 most played", "liked songs", "last 7 days of history" — refreshed on Wi-Fi and
charging, with a user-set storage cap that evicts least-played tracks first.

**Why it matters.** Solves the commute problem without asking the user to plan ahead.
Metro tunnels, flights, patchy rural data — the music is just there. It is also the
clearest justification for a premium tier, and it directly reduces streaming bandwidth
costs.

**Implementation.** A `WorkManager` periodic job with Wi-Fi + charging constraints,
querying the existing play-count and history tables, feeding the download manager already
in place. Cache eviction reuses `MaxSongCacheSizeKey`.

**Complexity: Medium.** All the pieces exist; this orchestrates them.

---

### B5. Listening Stats Recap / Shareable Wrapped

**The gap.** [StatsScreen.kt](file:///c:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/app/src/main/java/com/Chenkham/Echofy/ui/screens/StatsScreen.kt)
shows raw numbers. There is no narrative, no visual summary, nothing shareable — even
though `InstagramShareCardsKey` and `LyricsImageGenerator` already exist for lyrics.

**What it does.** A monthly/yearly story-format recap — top artists, top songs, total
minutes, a "your music personality" line — rendered as swipeable cards and exportable as
an image sized for Instagram Stories.

**Why it matters for growth.** This is the single best organic-marketing feature a music
app can ship. Every shared card is a free ad with the app's branding on it, and Spotify
Wrapped proved the format drives enormous seasonal engagement. The data is already being
collected; only the presentation is missing.

**Implementation.** Aggregate queries over the existing history/play-count tables, a
Compose card stack, and image export reusing the `LyricsImageGenerator` approach already
written for lyrics sharing.

**Complexity: Low-Medium.** No new data collection — presentation layer plus export.

---

### B6. Android Auto / Car Mode Polish

**The gap.** The manifest declares `automotive_app_desc` and a `MediaBrowserService`, so
the foundation is there — but there is no evidence of a browsable content tree, and
`DrivingModeBanner` suggests car use is already an intended scenario.

**What it does.** Proper Android Auto support: a browsable hierarchy (Liked, Recent,
Playlists, Downloads), voice-command playback, and large-target in-app driving mode with
oversized controls.

**Why it matters.** Commutes are peak listening time. A half-working Auto integration is
worse than none — users drop an app permanently if it fails in the car. Since the wiring
is already declared, this is finishing work rather than a new feature.

**Implementation.** Implement `onGetLibraryRoot` / `onGetChildren` in the existing
`MediaLibraryService`, add media-item hierarchy, and validate against Google's Auto
quality checklist.

**Complexity: Medium.** Foundation exists; the content tree and testing are the work.

---

### B7. Sleep Timer Upgrade — "End of Track" & Fade Out

**The gap.** [SleepTimer.kt](file:///c:/Users/chenk/.gemini/antigravity/scratch/ultimate_projects/Echofy-android/app/src/main/java/com/Chenkham/Echofy/playback/SleepTimer.kt)
exists but is duration-only, and cuts audio abruptly.

**What it does.** Adds "stop at end of current track", a gentle volume fade over the final
30–60 seconds, and an optional shake-to-extend.

**Why it matters.** An abrupt cut mid-song wakes people up — which defeats the entire
purpose of a sleep timer. Fade-out is the detail that makes bedtime listening feel
premium, and it is a very small change relative to how much it improves the experience.

**Implementation.** Extend the existing timer with an end-of-track mode
(`player.duration - player.currentPosition`) and a `ValueAnimator` ramping `player.volume`
to zero before pausing. Shake detection can reuse the `ShakeToSkipKey` sensor plumbing.

**Complexity: Low.** Small, self-contained extension of shipped code.

---

## Recommended Priority

| Rank | Feature | Complexity | Why this rank |
|------|---------|-----------|---------------|
| 1 | B5 Wrapped / Recap | Low-Med | Free organic growth; data already collected |
| 2 | B7 Sleep Timer Fade | Low | Cheapest real polish win |
| 3 | B3 Lyrics Full-Text Search | Low-Med | Distinctive; almost no competitor does it |
| 4 | B1 Local Music | Med-High | Biggest structural gap; makes Echofy an only-app |
| 5 | B4 Smart Offline | Medium | Strongest premium justification |
| 6 | B2 Chromecast | Medium | High assumed-to-exist factor |
| 7 | B6 Android Auto | Medium | Finishing work on existing foundation |

**If picking three:** B5 for growth, B3 for differentiation, B1 for retention.
