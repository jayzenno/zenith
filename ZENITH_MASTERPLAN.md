# ZENITH MASTERPLAN v2

## Product North Star

Zenith is not merely an IPTV player. It is a premium, native, remote-first Android TV media platform that can optionally become the user's full TV home/launcher.

The product must feel like a commercial TV platform, not a phone app stretched to a television. Every feature must be operable with D-pad/OK/Back without touch. Stability, speed, focus behavior, playback and data correctness beat visual polish. Visual polish must then reach a premium Liquid Glass standard.

The main Home screen keeps the horizontal top-level tiles: **Live TV · VOD · Music · Settings**. This is a fixed product decision.

Zenith is a player/organizer for sources the user is authorized to use. Do not ship or facilitate unauthorized content.

## 1. Non-negotiable product rules

1. TV-first, remote-first. Every focus transition is intentional. No mobile-style controls enlarged for TV.
2. No fake data in production. Placeholder EPG, demo channels, mock metadata and preview-only limits must never leak into real provider flows.
3. Data integrity before UI. If a provider returns N channels, Zenith must account for all N from API/parser -> database -> categories -> UI.
4. No silent truncation. Audit hardcoded limits, paging mistakes, slicing and preview-only code.
5. Fast but safe. Avoid unnecessary player recreation, full-screen blur over video, huge bitmap allocations and loading all provider content into memory.
6. Build success is not feature acceptance. Real-device/real-stream gaps must be documented.
7. Incremental architecture. Preserve working code; do not rewrite modules without evidence.
8. Every substantial architecture decision is researched twice where practical. Prefer primary docs and active open-source projects. Record findings, license and adaptation notes in .ai-collab/RESEARCH.md.
9. Never blindly copy code. Concepts/patterns may be adapted. Reuse source only when licensing is understood and compatible.
10. No feature churn. Every round must solve a concrete user problem or satisfy a defined acceptance criterion.

Reference projects/products to study as appropriate:
- OwnTV: Android-TV-first navigation, large-list architecture, EPG, profiles, QR setup, playback/fallback, backup.
- OpenTV: persistent EPG, DVR/recording, reminders, profiles, sync, catch-up.
- GridStreamr: multi-source collections, EPG workflows, timeshift, MultiView, enrichment, secure sharing concepts.
- Projectivy and Monet: launcher customization and TV-home UX.
- TiviMate, Sky, waipu.tv, MagentaTV: product/UX references only; never copy proprietary assets/private code.
- OmnioTV/Nuvio: later VOD/plugin architecture.
- Eclipse/EclipseMusic: later music integration.
- Android Developers / Media3 / Compose for TV: platform behavior and implementation guidance.

## 2. Current emergency priority: CORE TV

Until the Core-TV acceptance gate is passed, do not spend implementation rounds on Nuvio, Eclipse, launcher polish, wallpaper toys, theme marketplace, or speculative extras.

Priority:
1. crashes / data loss / security / playback failures
2. complete channel/category import
3. real EPG correctness and navigation
4. D-pad zapping and live player UX
5. favorites/history/groups/search
6. performance and lifecycle stability
7. unified TV design system
8. broader roadmap

### Core-TV acceptance gate
Core-TV is accepted only when:
- EPG no longer crashes.
- Real provider/XMLTV/Xtream EPG is displayed; no production placeholders.
- Current time is correct and Guide opens around now.
- A live Now marker exists.
- D-pad navigates channel rows and programme cells in all four directions.
- Focus is always visible and scrolls the grid.
- All provider categories/groups are reachable from Live TV and Guide.
- Favorites, All Channels and Recently Watched exist as first-class virtual groups.
- Full channel counts are accounted for; no unexplained missing channels.
- D-pad Up/Down zaps in Live TV.
- Live player has premium zap overlay: logo, logical channel, Now/Next, progress, useful badges.
- ExoPlayer/VLC fallback is deterministic and does not ping-pong.
- Builds/tests pass.
- Real-device gaps are documented; manual remote smoke test runs when device is available.

When Claude independently verifies the gate, write CORE_TV_ACCEPTED on its own line in .ai-collab/CORE_STATUS.md.

## 3. Provider, playlist and data integrity

Support authorized Xtream, M3U/M3U8 and XMLTV cleanly; Stalker/Ministra later if architecture permits.

For every sync account for provider category/channel counts, parsed items, DB writes, category membership, hidden/filtered items and UI-reachable items.

Use streaming/chunked parsing and Room/Paging for large sources. Preserve useful metadata such as tvg-id, tvg-name, tvg-logo, group-title, stream id, provider category id, language, country, quality hints and archive attributes.

Refresh should not wipe previously good data because of a temporary provider/EPG failure.

## 4. Logical Channel Graph

Zenith presents channels, not URLs.

A logical/canonical channel can have multiple variants, e.g.:
- Das Erste FHD — preferred live, no catch-up
- Das Erste HD — 7-day catch-up
- Provider B HD — backup
- Provider C SD — low-bandwidth fallback

A logical channel owns display name/aliases, logo candidates, EPG identity, user channel number, favorites/profile state, preferred live variant, backups, catch-up variants and source health.

This enables duplicate cleanup, live failover, Shared Catchup, provider-independent favorites, cleaner EPG mapping, smarter recording and quality/source preferences.

Automatic merges must use confidence and remain reversible. Never silently merge ambiguous channels.

## 5. Universal Source Resolver

The user expresses an intent; Zenith chooses the best valid source.

Intents:
- LIVE -> best live variant
- RESTART -> best valid catch-up/archive variant
- RECORD -> most reliable compatible variant
- MULTIVIEW -> resource-efficient variant respecting connection limits
- MAX_QUALITY -> highest supported healthy variant
- DATA_SAVER -> lower bitrate/resolution
- FAILURE -> next healthy backup

Selection considers availability, reliability, startup latency, resolution/codec/HDR, tracks, archive window, connection limits, device decoder ability, profile preference and network quality.

Resolver decisions must be deterministic and explainable in diagnostics.

## 6. Shared Catchup

Shared Catchup is first-class.

If Das Erste FHD has no archive but Das Erste HD is the same logical channel and has catch-up, Long OK -> Start from beginning must automatically use the HD archive.

Requirements:
- native catch-up first
- otherwise sibling variants of same logical channel
- optionally other authorized providers
- archive window must cover programme
- avoid ambiguous EPG/channel matching
- respect stream limits
- show actual archive quality/source in technical info
- allow manual override and persist it
- never label HD archive as FHD

Fallback hierarchy:
1. Native Catchup
2. Shared Catchup
3. Local timeshift/ring buffer if enabled

## 7. EPG / Guide

Build a true 2D remote-first timeline:
- sticky channel column
- horizontal time axis
- programme cells sized by duration
- correct timezone/DST handling
- live Now line
- open near current time
- jump to Now
- previous/next day
- configurable zoom/density
- synchronized scrolling

Remote:
- Up/Down -> channel row
- Left/Right -> programme
- OK current -> watch
- OK future -> details/actions
- Long OK -> context menu
- Back -> close layer, then leave
- shortcuts for Now/day/search/groups where available

Groups:
Favorites, Recently Watched, All Channels, provider groups, smart/custom groups.

EPG mapping:
- exact tvg-id first
- controlled normalized aliases as fallback
- never silently accept ambiguous matches
- EPG Repair Center with match state/confidence/reason
- manual mapping
- per-channel/source offset
- multiple EPG sources
- incremental refresh
- persistent cache across restarts
- preserve previous good EPG on transient failure

Search:
programmes across channels/days, On Now, Tonight, genres, reminders, recurring reminders, keyword/team alerts, repeat airings.

## 8. Live TV player and zapping

Live TV must not resemble a mobile VOD player.

Defaults:
- D-pad Up -> previous channel
- D-pad Down -> next channel
- OK -> HUD / mini guide
- Back -> hide HUD first, then exit
- Long OK -> context menu
- configurable remote shortcuts
- optional numeric channel entry
- Last Channel / recall

Premium zap overlay:
logo, channel number, logical channel name, current programme/time, progress, next programme, quality/HDR, catch-up badge, optional advanced source/engine info.

Long-OK context menu:
- Start from beginning
- Back to live
- Record
- Programme details
- Favorite
- Full Guide
- Stream/quality
- Audio
- Subtitles
- Aspect
- Engine override
- Channel options / EPG mapping / hide / number / group

Fast zapping should avoid unnecessary player teardown. Safe preload/prewarm only if provider connection limits permit. Measure zap time.

Later/where supported: AFR, language preferences, surround/stereo fallback, subtitle profiles, PiP, mini-player, external-player handoff, aspect modes, diagnostics, local timeshift.

## 9. Favorites, groups, history and collections

Virtual groups independent of provider:
Favorites, Recently Watched, All Channels, Continue Watching, Recordings, custom smart groups.

Smart group rules may use source, country, language, quality, EPG availability, genre, favorites, keywords, hidden state and catch-up ability.

Allow rename, reorder, hide, combine, pin, bulk edit and logical duplicate assignment.

## 10. Stream Health Engine

Maintain local privacy-conscious health per stream:
last success, startup time, errors, buffering, preferred engine, failure category, last resolution/codec, catch-up success.

Differentiate internet, DNS, provider, auth, HTTP, unsupported codec, decoder, timeout and ended-source failures.

Always sanitize credentials/tokens from logs.

Optional power-user network controls: custom DNS/DoH, User-Agent, per-source headers, connection-limit awareness, network health test.

## 11. DVR, recording, timeshift and MultiView

After Core-TV:
- one-shot/current/Guide recording
- series recording
- pre/post padding
- conflict detection
- local/USB/NAS targets where feasible
- storage cleanup
- play while recording where possible
- recording library/resume/diagnostics

Timeshift:
provider catch-up, local ring buffer, Back to Live, configurable buffer.

MultiView:
2-4 panes depending capability, focus selects audio, swap/maximize, respect provider and decoder limits, saved sports/news layouts.

## 12. Profiles and privacy

Per profile:
name, avatar, PIN, Kids mode, favorites, history, resume, watchlist, hidden channels/categories, source visibility, startup destination/channel, theme, layout density, shortcuts, player prefs, language prefs, content rows/order, reminders.

Also:
Guest profile with optional auto-reset, quick profile switch, profile-scoped search/history.

Avatar packs:
- original Zenith-created goofy/meme-inspired internet characters
- clean/minimal/neon/abstract packs
- do not ship copyrighted character artwork without permission
- later custom photo import/cropping

## 13. Premium first-run onboarding

Fresh install opens a modern animated TV-native onboarding:
1. logo/intro
2. language
3. create profile
4. avatar
5. theme/layout preset
6. add sources
7. EPG basics
8. favorites/categories optional
9. playback preferences
10. interactive remote tutorial
11. startup destination
12. App Mode vs optional Zenith Home
13. summary

Requirements: D-pad-perfect, optional steps skippable, QR/PIN companion setup for long credentials, credentials protected, progress indicator, resumable after interruption, rerunnable from Settings, no walls of text.

## 14. Zenith Design System / Liquid Glass

Central tokens/components only:
colors, backgrounds, glass/frost/transparency, blur, glow, border, radius, focus scale/ring/glow, typography, animation intensity, overlay opacity, EPG density, tile/navigation/player styles, OLED, reduced motion, high contrast.

Focus must be obvious from sofa distance.

Performance:
selective blur only; avoid stacked full-screen blur over active video; prefer cheaper gradients/tints/frost/noise where appropriate; set frame-time/memory budgets.

Theme Studio:
live preview, undo/reset, preset inheritance, per-profile theme, import/export, compact share codes, versioned schema/migrations.

Preset families:
Zenith Aqua Glass, Midnight, OLED Cinema, Neon Glacier, Solar Ember, Emerald, Crimson, Deep Ocean, Nebula, Clean Pro, Grid Pro, Cinematic, Live Focus, Compact Power User.

References may inspire layout language, never proprietary branding/assets.

## 15. Layout Engine

Theme and layout are separate.

Presets:
- Zenith Glass
- Grid Pro
- Cinematic
- Live Focus
- Compact

Allow panel widths, sidebar position/visibility, row heights, EPG zoom, logo/card size, row order, focus animation and information density.

Home top-level tiles remain Live TV / VOD / Music / Settings.

## 16. Optional Zenith Home launcher

Zenith may optionally become the TV Home/launcher where Android/device policy permits. Never forced.

Study Projectivy and Monet plus Android TV launcher constraints before implementation.

Customization target:
- arbitrary rows/sections
- add/remove/reorder rows
- app grid/list/hero modes
- configurable size/spacing/radius/focus
- folders
- hidden apps
- custom labels/icons/artwork/icon packs
- pinned favorites
- PIN per app
- Kids launcher
- row visibility per profile
- dock on/off
- sidebar left/right/off
- status indicators
- clock/date/weather optional
- HDMI/input shortcuts where permitted
- widgets where permitted
- custom/animated/video wallpapers when safe
- ambient scenes/screensaver
- OLED mode
- day/night profiles
- backup/restore
- app launch animations
- reduced motion
- overscan calibration

Zenith-specific rows:
Continue Watching, Live Now, Favorite Channels, Tonight, Sports Live, News Live, reminders, Recordings, Recent Apps, Nuvio later, Eclipse later, MultiView presets, Inputs, custom smart rows.

Global launcher search across apps, live channels, EPG, recordings and later VOD/music.

Launcher implementation begins after Core-TV acceptance, except architecture/research docs.

## 17. Companion / local management

Reduce TV keyboard pain with QR + short PIN local setup page:
add/edit sources, reorder groups, fix EPG mapping, favorites, upload avatars/wallpapers, theme editor, backup/restore.

Keep credentials local by default. If sync exists, privacy model must be explicit.

Optional: device-to-device clone, LAN/Tailscale/self-hosted sync, encrypted backup, no mandatory cloud account for basic use.

## 18. Android TV integration

Where supported:
MediaSession/transport keys, audio focus, PiP, Android TV preview channels/Continue Watching, deep links, voice search optional, notifications/reminders, frame-rate matching, HDMI-CEC-friendly behavior.

Capability-detect OEM/device features and degrade gracefully.

## 19. Accessibility and household

Scalable text, subtitle profiles, high contrast, reduced motion, color-independent focus, sofa-distance spacing, accessibility semantics, safe overscan, parental PIN, content/channel/app locks, hidden adult groups, Kids and Guest profiles.

## 20. Power-user / uncommon differentiators

After foundations:
- Predictive Zapping when connection budget permits
- Last Channel Stack/history carousel
- EPG Confidence and visible match reasoning
- Source Health Score
- Smart Duplicate Resolver
- Intent-based quality: Max Quality / Max Stability / Low Latency / Data Saver
- Spoiler-free mode for sports/results/progress
- Focus Debug Mode
- Remote key-sequence test harness
- Safe Mode after repeated decoder/engine failures
- Stream promotion from preview to fullscreen without restart where possible
- Global content search
- Smart title/team/keyword alerts
- Theme/EPG mapping share codes without credentials
- Sanitized diagnostics bundle
- Connection Budget Manager for preview/MultiView/recording/preload
- reversible automatic category cleanup
- optional local smart-home hooks later

## 21. VOD / Nuvio — later

Only after Core-TV acceptance:
native Zenith-integrated TV UX; inspect OmnioTV/Nuvio upstream architecture/license first; clean provider/module boundaries; addons/plugins/catalogs/metadata/search/details/playback where legal/feasible; common profiles/theme/search/player quality; Continue Watching/watchlist/trailers/cast/subtitles/next episode/trickplay; authorized integrations only.

## 22. Music / Eclipse — later

Only after Core-TV acceptance:
native TV music UX; inspect Eclipse/EclipseMusic and EclipseMusicBridge first; provider abstraction; library/search/artists/albums/playlists/queue/Now Playing/covers/lyrics where authorized/background playback/MediaSession/mini-player/screensaver; future Spotify/Apple Music only through authorized integration paths.

## 23. Testing and Definition of Done

Every feature has explicit acceptance criteria.

Per implementation round:
- inspect current code/diff
- one bounded task
- build
- relevant tests
- regression check
- diff review
- update handoff/results

Navigation screens:
test Up/Down/Left/Right/OK/Back; visible focus; no traps; focus restoration; empty/loading/error/full states.

Provider/EPG:
large fixtures, missing IDs, duplicate names, timezone/DST, incremental refresh, malformed input, provider outage, restart persistence.

Player:
loading/buffering/playing/error/ended, retry, fallback, rapid zapping, lifecycle, stream switching, tracks.

Build infrastructure is part of the job: JDK/JAVA_HOME, Gradle, Android SDK, dependencies and wrapper. Diagnose and safely fix project/workspace issues, retry build, avoid blind destructive system changes.

## 24. AI collaboration rules

DeepSeek:
- primary implementer
- one bounded task per round
- read this Masterplan + current handoff/state
- research uncertain architecture
- do not repeat the same verification forever with no new finding

Claude:
- independent senior reviewer/co-developer
- verify actual diff/code/tests
- challenge mobile-looking UX, focus bugs, lifecycle races, truncation and performance
- in Core-TV mode choose next highest-impact Core task after review
- never declare the whole product done

Codex:
- targeted fallback/verification engineer
- primarily used on quota/hard technical blocker
- finish current task, do not start unrelated features

All:
- no reset
- no destructive git
- no push/merge
- preserve good work
- no credential leaks
- distinguish build from real hardware verification
- for substantial architecture, check at least two relevant sources where practical
- record research/license notes in .ai-collab/RESEARCH.md

## 25. Roadmap order

Phase A — Core rescue:
EPG crash -> full channel/category integrity -> real EPG -> EPG D-pad grid -> Favorites/History/Groups -> live zapping -> premium player HUD -> lifecycle/performance.

Phase B — Intelligent TV core:
Logical Channel Graph -> duplicate resolver -> Universal Source Resolver -> Shared Catchup -> Health Engine -> connection budget.

Phase C — Premium TV UX:
Design System -> Liquid Glass -> Layout Engine -> focus audit -> performance budgets.

Phase D — Identity:
Profiles -> original meme avatar packs -> Kids/PIN -> onboarding -> QR companion -> backup/restore.

Phase E — Advanced TV:
DVR -> timeshift -> MultiView -> reminders -> Android TV integration -> sync.

Phase F — Optional Launcher:
Zenith Home with Projectivy/Monet-level customization plus Zenith media rows.

Phase G — Integrations:
Nuvio/OmnioTV VOD -> EclipseMusic -> authorized future providers.

## Final principle

**Coding exists to solve user problems.** Do not add a checkbox because another player has it. Identify the user's real problem, design the cleanest remote-first behavior, verify against multiple references, and make Zenith feel simpler than the complexity underneath.
