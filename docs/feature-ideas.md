# Echofy — Feature Idea Bank

A large menu of ideas to pick from. Everything here was checked against the codebase
first. Already-shipped things (equalizer, crossfade, skip silence, normalisation,
visualizer, backpaper, AI chat, wake word, driving mode, Together Sessions, Discord RPC,
Last.fm, sleep timer, widgets, ringtone export, playlist import) are **not** repeated.

Complexity: **S** = small, **M** = medium, **L** = large, **XL** = major project.

---

## Approved & On Hold

| ID | Feature | Where | Cx |
|----|---------|-------|-----|
| A1 | Per-track tempo/pitch memory | Advanced panel | S |
| A2 | A–B loop repeat | Advanced panel | M |
| A3 | Mono audio + L/R balance | Player & controls settings | M |
| B1 | Local / offline music playback | Library | L |
| B2 | Chromecast / speaker casting | Player | M |
| B3 | Full-text lyrics search | Search | M |
| B5 | Wrapped / shareable recap | Stats | M |
| B6 | Android Auto content tree | Service | M |
| B7 | Sleep timer fade-out + end-of-track | Player | S |

Dropped: B4 smart auto-download.

---

# NEW IDEAS

## 1. Killer Features — the ones that make people switch apps

### 1.1 Karaoke Mode — **XL**
Real-time vocal suppression using centre-channel cancellation, paired with the synced
lyrics already in the app. Word-by-word highlight, pitch meter showing whether the user is
on key, optional recording of their take.
**Why:** The app already has synced lyrics and an audio pipeline — this is the highest
"wow" feature available, and it is genuinely social and shareable. Nothing else on this
list gets screenshotted and sent to friends more.
**Risk:** Centre-channel cancellation quality varies by mix; on some tracks it sounds bad.

### 1.2 Song Recognition ("what's playing") — **L**
Listen through the mic and identify the song, then jump straight into playing it.
**Why:** Closes the loop between hearing music in the world and playing it in Echofy. The
mic plumbing, permissions and `OpenWakeWordDetector` are already there — much of the hard
audio-capture work is done.
**Note:** Needs a fingerprint backend (AudD, ACRCloud, or self-hosted Dejavu).

### 1.3 AI DJ — **L**
The AI assistant that already exists starts speaking between tracks: introduces the next
song, gives context about the artist, reacts to the time of day and your history.
**Why:** You already have TTS, an AI manager and driving mode. This turns an existing
chat feature into a *personality*, which is the kind of thing people tell friends about.
Pairs perfectly with driving mode.

### 1.4 Mood / Vibe Auto-Playlists — **M**
Analyse tempo, energy and valence to auto-build "Late Night", "Workout", "Focus",
"Heartbreak" mixes from the user's own library.
**Why:** The genre artwork for these categories already ships in `drawable-nodpi`. Mood is
how people actually choose music — far more than by genre.

### 1.5 Instant Mix from Any Song — **M**
Long-press any song → "Start radio from this" → an endless queue tuned to it, blending
YouTube Music radio with the user's own play history.
**Why:** The single most-used feature in every major streaming app. Low effort given the
YouTube Music radio endpoints already integrated.

---

## 2. Social & Sharing — free organic growth

### 2.1 Music Profile Cards — **M**
A public-ish profile: top artists, current favourite, listening personality, as a shareable
card. Extends the Wrapped idea (B5) into an always-available thing.

### 2.2 Collaborative Playlists — **L**
Multiple users add to one playlist in real time. Appwrite is already wired for Together
Sessions, so the realtime backend exists.

### 2.3 Song Dedication / Send a Track — **S**
Send a song to a friend with a personal note and a timestamp ("start at 1:12, this part").
**Why:** Tiny feature, disproportionately emotional. Every send is an app invite.

### 2.4 Lyric Card Sharing Upgrade — **S**
`LyricsImageGenerator` already exists. Add album-art backgrounds, colour extraction, and
multiple templates.
**Why:** Cheapest possible growth lever — improve something already built.

### 2.5 Listening Streaks & Milestones — **S**
"7-day streak", "100 hours with this artist", "your 1000th song". Light, non-gamey badges.
**Why:** Retention. Streaks are the single most effective habit mechanic in consumer apps.

---

## 3. Player & Playback

### 3.1 Gesture Controls — **S**
Swipe the album art for next/previous, double-tap to like, swipe down to minimise.
`SwipeThumbnailKey` exists — extend it into a full gesture set.

### 3.2 Queue Reordering by Drag with Undo — **S**
Drag to reorder plus a snackbar undo on removal.

### 3.3 "Play Next" vs "Add to Queue" Distinction — **S**
Two separate actions instead of one. Small, but power users notice its absence immediately.

### 3.4 Smart Resume — **M**
Remember playback position for long tracks (mixes, podcasts, live sets) and offer "resume
from 42:15" instead of restarting.
**Why:** The app has podcasts and Mixcloud DJ sets — those are long-form and currently
restart from zero.

### 3.5 Playback Speed per Content Type — **S**
Remember 1.5x for podcasts, 1.0x for music, automatically.

### 3.6 Headphone Auto-Actions — **M**
Auto-play on connect, pause on disconnect, and resume the last queue when a specific
Bluetooth device connects (e.g. car stereo).

### 3.7 Volume Fade on Pause/Resume — **S**
A 300ms ramp instead of an abrupt cut. Small polish that makes playback feel expensive.

### 3.8 Silent Outro Skip — **S**
Auto-skip trailing silence and long dead-air outros.

---

## 4. Library & Organisation

### 4.1 Tag Editor — **M**
Edit title, artist, album and artwork for local and cached tracks.
**Why:** Essential once local music (B1) ships — badly tagged files are the #1 local-music
complaint.

### 4.2 Folder Browsing — **M**
Browse local music by directory, not just by metadata. Power-user favourite.

### 4.3 Duplicate Detection — **S**
Find and merge duplicate songs across playlists.

### 4.4 Smart Playlists with Rules — **L**
"Songs I liked this year, over 4 minutes, not played in 30 days" — user-defined rules that
auto-update.

### 4.5 Recently Added / Never Played Views — **S**
Two auto-views that surface neglected library corners.

### 4.6 Playlist Folders — **M**
Nest playlists into folders once users have dozens.

### 4.7 Bulk Actions — **S**
Multi-select across library for delete, download, add-to-playlist. Some selection UI
exists in `SelectionSongsMenu` — extend it consistently.

---

## 5. Discovery

### 5.1 "Because You Listened To…" Rows — **M**
Explainable recommendation rows on Home instead of opaque suggestions.

### 5.2 Time Machine — **M**
"What you were playing this week last year." Uses history data already collected.
**Why:** Nostalgia is powerful and the data is free.

### 5.3 Artist Deep Dive — **M**
One screen combining MusicBrainz metadata, TheAudioDB bio, Bandsintown concerts, Discogs
releases and TasteDive similar artists — all five APIs are already integrated but scattered.
**Why:** High value, near-zero new integration work. Just presentation.

### 5.4 New Release Radar — **S**
Notify when a followed artist drops something. `NewReleaseScreen` exists; add following +
notifications.

### 5.5 Hidden Gems — **S**
Surface low-play-count tracks from the user's own library.

### 5.6 Genre / Decade Explorer — **M**
Browse by era — 90s Bollywood, 80s rock. The genre artwork already ships.

### 5.7 Concert Alerts — **S**
Bandsintown is integrated; push a notification when a followed artist plays nearby.

---

## 6. Lyrics

### 6.1 Word-by-Word (Karaoke-Style) Highlighting — **M**
Enhanced LRC / word-level timing rather than line-level.

### 6.2 Lyrics Translation Caching — **S**
Cache translations so they are instant and offline on repeat plays.

### 6.3 Romanisation Toggle — **S**
Devanagari/Arabic/Korean/Japanese → Latin script for singing along.
**Why:** Big deal for the Indian market and for K-pop/J-pop listeners.

### 6.4 Lyrics Meaning Explainer — **M**
Tap a line → the existing AI explains slang, references, cultural context.
**Why:** Uses the AI manager already built; genuinely useful for non-native listeners.

### 6.5 Offline Lyrics Prefetch — **S**
`BatchOfflineLyricsKey` already exists as a flag — build the batch fetch behind it.

### 6.6 Lyrics Contribution — **L**
Let users fix bad timings and upload back to LRCLIB.

---

## 7. Visual & Personalisation

### 7.1 Now-Playing Themes — **M**
Multiple player skins: vinyl record with rotation, cassette tape, minimal, retro Winamp.
**Why:** Screenshot-bait. Highly shareable, purely cosmetic, zero risk to playback.

### 7.2 Album Art Colour Theming — **M**
Extend dynamic theming so the whole app tints to the current album art, not just the player.

### 7.3 Animated Album Art — **M**
Canvas-style looping video backgrounds for supported tracks.

### 7.4 Custom Fonts — **S**
`CustomFontKey` exists as a key; ship an actual picker.

### 7.5 Icon Pack / Themed Icons — **S**
Material You themed icon plus a few alternates. `CustomAppIconKey` already exists.

### 7.6 Lock Screen Art Full-Bleed — **S**
Full-screen album art on the lock screen.

---

## 8. System Integration

### 8.1 Quick Settings Tile — **S**
Play/pause and skip from the notification shade pulldown. No `TileService` exists today.

### 8.2 Alarm Clock Integration — **M**
Wake to a chosen song or playlist with a gradual volume ramp.
**Why:** Creates a guaranteed daily open. Very strong retention hook.

### 8.3 Wear OS Companion — **XL**
Watch controls plus offline playback on-wrist.

### 8.4 Bluetooth Device Profiles — **M**
Per-device EQ and volume — car vs earbuds vs speaker.

### 8.5 Live Activity / Ongoing Notification Polish — **S**
Richer media notification with seek bar and album-art background.

### 8.6 App Shortcuts Expansion — **S**
Long-press launcher icon → "Play liked songs", "Continue listening". Shortcut XML already
exists; add dynamic ones.

### 8.7 Assistant / Voice App Integration — **M**
"Hey Google, play X on Echofy" via `MediaBrowserService` (already declared).

---

## 9. Utility & Power User

### 9.1 Audio Trimmer / Clip Export — **M**
Cut a 30-second clip and export as audio or as a video with lyrics for social.

### 9.2 Set as Ringtone Upgrade — **S**
`RingtoneUtils` exists; add a trim UI and per-contact assignment.

### 9.3 Playback History Export — **S**
CSV/JSON export of listening history.

### 9.4 Statistics Deep Dive — **M**
Listening clock (hours of day), weekday patterns, genre pie, discovery-vs-repeat ratio.

### 9.5 Data Usage Monitor — **S**
Show streamed MB per session and per month.

### 9.6 Backup to Cloud — **M**
Local backup exists; add Google Drive sync.

### 9.7 Multi-Device Queue Handoff — **L**
Start on phone, continue on tablet.

---

## 10. Accessibility & Wellbeing

### 10.1 Hearing Profile / Personalised EQ — **M**
A short in-app hearing test that generates a custom EQ curve per ear.
**Why:** Genuinely differentiating, and a natural extension of A3 mono/balance.

### 10.2 Volume Limit / Safe Listening — **S**
Cap output and warn on prolonged loud listening.

### 10.3 Listening Time Reminders — **S**
Optional "you've been listening 3 hours" nudge.

### 10.4 High-Contrast & Large-Text Lyrics — **S**
Accessibility mode for the lyrics view.

### 10.5 Reduce Motion — **S**
Respect the system reduce-motion setting across the app's many animations.

---

## 11. Monetisation-Friendly

### 11.1 Rewarded Ad → Temporary Premium — **S**
Watch an ad, get 30 minutes ad-free. Rewarded ads are already implemented.

### 11.2 Referral Program — **M**
Invite a friend, both get premium days.

### 11.3 Artist Tip Jar — **M**
Donation infrastructure already exists — point some of it at artists.

### 11.4 Premium Trial on First Launch — **S**
7-day trial during onboarding, which already exists.

---

## 12. Wildcards

### 12.1 Music Quiz / Guess the Song — **M**
Play 5 seconds from the user's own library and guess. Uses existing data, high replay value.

### 12.2 Group Listening Rooms with Chat — **L**
Together Sessions plus a text/emoji layer.

### 12.3 Mood Journal — **M**
Log how music made you feel; correlate mood with listening over time.

### 12.4 Focus Timer with Music — **M**
Pomodoro + curated focus audio. Ambient sounds via Freesound are already integrated.

### 12.5 Song Stories — **S**
AI-generated trivia card about the track currently playing.

### 12.6 Playlist Cover Generator — **M**
AI-generated artwork for user playlists.

### 12.7 Cross-App Handoff Links — **S**
Songlink is already integrated — expose "open this in Spotify/Apple" on share.

---

# If You Want My Pick

**Ship these five first** — highest impact per unit of effort, and each builds on
something already in the codebase:

| # | Feature | Cx | Why |
|---|---------|-----|-----|
| 1 | 5.3 Artist Deep Dive | M | Five APIs already integrated, just unpresented |
| 2 | 8.1 Quick Settings Tile + 8.6 Shortcuts | S | Daily-use convenience, tiny effort |
| 3 | 1.5 Instant Mix from Any Song | M | Most-used feature in every rival app |
| 4 | 2.5 Streaks + 2.4 Lyric Cards | S | Retention plus free organic growth |
| 5 | 1.3 AI DJ | L | Your genuine differentiator — nobody else has this |

**The one big swing:** Karaoke Mode (1.1). It is the only idea here that could make Echofy
*the* app people recommend rather than one of many. It is also the riskiest — worth
prototyping the vocal removal on 10 real tracks before committing to it.
