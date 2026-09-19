# AI Collaboration Changelog

## 2026-09-19 — DeepSeek Core-TV-Runde (Code-Change: Premium-Zap-Overlay — echte Kanalnummer + ehrliche Qualitäts-Badges, Gate #11)
- `MODUS=core-tv`, `TO_DEEPSEEK.md` weiterhin nur Platzhalter → höchster noch nicht
  vollständig erfüllter Gate-Punkt selbst gewählt: **Gate #11 „Live player has premium zap
  overlay: logo, channel number, logical channel, Now/Next, progress, useful badges“** —
  Logo/Now-Next/Fortschritt waren da, **Kanalnummer und nützliche Badges fehlten**.
- Neu `ChannelQuality.kt` (pure, JVM-testbar): `channelQualityHint(name, url)` ⇒ 4K/FHD/
  HD/SD nur aus **echten** Kanalname/URL-Daten, sonst `null` (Regel #2: niemals erfundene
  Qualität; `_`-Normalisierung für „stream_1080p“-URLs — Bug im ersten Testlauf entdeckt).
- `PlayerScreen`: Kanalnummer (`Channel.number > 0`, sonst Zap-Position) nur bei LIVE als
  accent-Plakette in TopBar/Banner; Glass-`QualityBadge` neben LIVE-Chip nur bei ableitbarem
  Wert; VOD/Music ohne Badge.
- Guide-Nebenbefund (Priorität 3): Hinweis „◄ ► = Tag“ war falsch (Tag = PageUp/PageDown)
  und „Pause = jetzt“ behauptete einen Shortcut, den es nicht gab — `snapNow()` war
  ungebunden. Fix: `MediaPlayPause`/`MediaPlay` springen zu jetzt; Hinweis auf die echten
  Tasten korrigiert.
- Neu `ChannelQualityTest` +10 Tests; Gesamt **51 JVM-Unit-Tests grün** (`ChannelQuality`
  10, `EpgData` 18, `RecentWatch` 9, `FallbackPolicy` 4, `PlaybackStatus` 4,
  `PlayerDiagnostics` 6), `BUILD SUCCESSFUL` offline (`--rerun-tasks` real ausgeführt).
  Kein Gerät/`adb` → keine Hardware-/Badge-Optik-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert; `STATE.md` bleibt
  `ACTIVE_AGENT=CLAUDE` (war zu Rundenbeginn bereits so — Claude-Review der Vorrunden steht
  noch aus; LAST_COMPLETED_AGENT=DEEPSEEK ist korrekt).

## 2026-09-19 — DeepSeek Core-TV-Runde (Code-Change: Anbieter-Kategorien im Guide, Gate #7 + ehrliche Favoriten)
- `MODUS=core-tv`, `TO_DEEPSEEK.md` weiterhin nur Platzhalter → höchster unerfüllter
  Gate-Punkt selbst gewählt: **Gate #7 „All provider categories/groups are reachable
  from Live TV and Guide“** — Live-TV erreichte Kategorien bereits
  (`MediaListViewModel.rows` → `GridRow`s), aber der Guide zyklte über `G` nur durch
  Alle/Favoriten/Zuletzt gesehen: **keine Anbieter-Kategorie war im TV-Programm
  erreichbar**.
- Nebenbefund (Regel #2): Favoriten-Seed `setOf(0, 2, 4, 6)` in `ZenSettings`-Default,
  DataStore-Fallback, `toggleFav` und Home-Reihe fabrizierte Favoriten (♥/Reihe/Gruppe
  mit erfundenen Sendern) — entfernt, Favoriten sind ab jetzt daten-ehrlich.
- Neu in `EpgData.kt` (pure, JVM-testbar): `epgCategories` (distinct, nicht-blank,
  Playlist-Reihenfolge), `epgCategoryVis` (exakte globale Indizes), `epgGroupCycle` +
  `epgNextGroup` (Zyklus `all → favs → recent → cat0..catN → all`; `cat:`-Präfix gegen
  Kollisionen; Skip leerer virtueller Gruppen; verschwundene Kategorie fällt auf `all`
  zurück), `epgGroupCategoryName` (Roundtrip), `EpgGroupState`.
- `SettingsRepository`: `ZenSettings.epgCategoryGroup` + Key `epg_category`,
  `setEpgCategoryGroup(cat)` als exklusiver Gruppen-Setter; `setEpgActiveGroup`/Low-
  Level-Setter löschen die Kategorie → „genau EINE aktive Gruppe“ über alle Pfade.
- `EpgViewModel`: `currentVis` mit Kategorienzweig, `cycleGroup` über Kategorien mit
  Toast „Kategorie: X aktiv“, `activeGroupLabel`, `toggleFav` ohne Seed. `EpgScreen`:
  Empty-State-Untertitel für leere Kategorie. `HomeScreen`: Favoritenreihe ohne Seed.
- Neu `EpgDataTest` +7 Tests (Klasse jetzt 18); Gesamt 41 JVM-Unit-Tests grün
  (`EpgDataTest` 18, `RecentWatchTest` 9, `FallbackPolicyTest` 4,
  `PlaybackStatusTest` 4, `PlayerDiagnosticsTest` 6), `BUILD SUCCESSFUL` offline
  (Windows-JVM, 15 s, 45 Tasks, `--rerun-tasks` real ausgeführt). Kein Gerät/`adb` →
  keine Hardware-/D-Pad-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert; `STATE.md` bleibt
  `ACTIVE_AGENT=CLAUDE` (war zu Rundenbeginn bereits so — Claude-Review der Vorrunden
  steht noch aus; LAST_COMPLETED_AGENT=DEEPSEEK ist korrekt).

## 2026-09-19 — DeepSeek Core-TV-Runde (Code-Change: „Zuletzt gesehen“ als First-Class-Gruppe)
- `MODUS=core-tv`, `TO_DEEPSEEK.md` weiterhin nur Platzhalter (Claude-Review war
  permissions-blockiert) → höchster unerfüllter Gate-Punkt selbst gewählt:
  **Gate #8 „Favorites, All Channels and Recently Watched exist as first-class
  virtual groups“** — Favoriten/„Alle Sender“ existierten, „Zuletzt gesehen“ fehlte
  komplett (keine Wiedergabe-Historie), und der Guide hatte keinen Gruppenumschalter.
- Neu `data/settings/RecentWatch.kt`: pure, JVM-testbare Recency-Logik
  (`recentWatchKey` provider-gescopt, `pushRecentWatch` Dedupe/Move-to-Front/Cap 24,
  `recentWatchDecode/Encode`, `recentOnlyVis` überspringt unbekannte/gelöschte Kanäle).
- `SettingsRepository`: `ZenSettings.epgRecent`/`epgRecentOnly`, Keys `epg_recent`/
  `epg_recentonly`, `pushRecentChannel()` als atomarer Read-Modify-Write (Zap-Rennen
  sicher), `setEpgActiveGroup()` setzt exakt EINE Gruppe in einem Edit (kein Torn-State).
- `PlayerScreen`: Tune von LIVE-Kanälen (`providerId >= 0`) schreibt in die Historie —
  ein Funnel für alle Einstiege (Guide/Live-TV/Home); VOD/Music bewusst nicht.
- `EpgScreen`: `Key.G` Gruppenschleife (Alle Sender → Favoriten → Zuletzt gesehen),
  Spalten-Header zeigt aktive Gruppe + ehrliche Zählung, Empty-States pro Gruppe,
  Hint „G = Gruppe“. `EpgViewModel`: `cycleGroup()`/`snapTo()` re-anchorn synchron
  (Settings-Flow async), `toggleFavsOnly` (VM + `SettingsViewModel`) → exklusive
  Gruppen über `setEpgActiveGroup`.
- Neu `RecentWatchTest` 9 JVM-Tests; Gesamt 34 Tests grün (`RecentWatchTest` 9,
  `EpgDataTest` 11, `FallbackPolicyTest` 4, `PlaybackStatusTest` 4,
  `PlayerDiagnosticsTest` 6), `BUILD SUCCESSFUL` offline (`--rerun-tasks`, 23 s).
  Kein Gerät/`adb` → keine Hardware-/D-Pad-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` = `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Core-TV-Runde (Code-Change: EPG-Grid virtualisiert, Focus-Follow, kein Silent-Truncation)
- `MODUS=core-tv`, `TO_DEEPSEEK.md` war Platzhalter → höchste offene Gate-Punkte
  selbst gewählt, beide hingen an der fehlenden Grid-Virtualisierung:
  - Gate „Focus is always visible and scrolls the grid“ (vorher kein Auto-Scroll,
    Auswahl lief nach ~10 Schritten aus dem Bild).
  - Gate #9 / Regel #4 „No silent truncation“ (`MAX_EPG_CHANNELS=200` kappte
    stillschweigend; Kanal 201+ unerreichbar).
- `EpgScreen.kt`: Programm-Grid + Kanalspalte auf `LazyColumn` umgestellt (nur
  sichtbare Zeilen werden komponiert); Kanalspalte folgt über
  `snapshotFlow(firstVisibleItemIndex/ScrollOffset) → scrollToItem` bei identischem
  Zeilen-Pitch; horizontale Achse über einen gemeinsamen `hScroll` (sticky
  Time-Header + alle Zeilen, bewährtes Shared-`ScrollState`-Muster).
- Vertikaler Focus-Follow: `animateScrollToItem(vm.row)`; horizontaler Focus-Follow:
  `animateScrollTo(epgScrollTargetX(...))` (Grid-Breite via `onSizeChanged`).
- `EpgData.kt`: `MAX_EPG_CHANNELS` entfernt; neue pure Funktion
  `epgScrollTargetX()` (JVM-testbar). `EpgViewModel.kt`: beide `take(...)` entfernt.
- Ehrliche Kanalzahlen in der Kanalspalte („X von N“ bei Filter, sonst volle Zahl).
- Neu `EpgDataTest` +3 Tests (`epgScrollTargetX`); Gesamt 25 JVM-Unit-Tests grün
  (`EpgDataTest` 11, `FallbackPolicyTest` 4, `PlaybackStatusTest` 4,
  `PlayerDiagnosticsTest` 6), `BUILD SUCCESSFUL` offline (`--rerun-tasks` real
  ausgeführt, 20 s). Kein Gerät/`adb` → keine Hardware-/Scroll-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` = `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Core-TV-Runde (Code-Change: Guide zeigt nur noch echte Anbieterdaten)
- `MODUS=core-tv`, `TO_DEEPSEEK.md` war Platzhalter → höchster unerfüllter Gate-Punkt
  selbst gewählt: **Gate #2 / Regel #2 (keine Fake-Daten in Produktion)**.
- `EpgData.kt`: `EPG_CHANNELS` startet leer (26 Demo-Sender entfernt), `epgProgramsFor()`
  liefert nur echte gespeicherte Daten — komplette Mock-Maschinerie gelöscht (−143/+11).
- `EpgScreen.kt`: ehrliche Empty-States (kein Sender / „Keine Programmdaten für diesen
  Sender“). `buildEpgChannel` entfernt — der Guide startet jetzt über den echten
  DB-Kanal (`EpgViewModel.channelAt(vi)`).
- `PlayerScreen.kt`: „JETZT/DANACH“-Lookup über die EPG-Identität `extra ?: id`
  (konsistent zum Guide), statt URL-DB-Id → funktioniert für M3U tvg-id / Xtream
  epg_channel_id.
- Behobener Nebenbefund (Priorität-1-Playback-Fehler): OK im Guide führte vorher zu
  `player/-1/LIVE/epg_…` → leerer schwarzer Player (fabricated Channel).
- Neu `EpgDataTest.kt` (8 Tests); Gesamt 22 JVM-Unit-Tests grün, `BUILD SUCCESSFUL`
  offline (Windows-JVM, 29 s, `--rerun-tasks` real ausgeführt). Kein Gerät/`adb` →
  keine Hardware-/Stream-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` = `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Improve-Runde (Code-Change: Lifecycle-Pause bei App-Stopp)
- Offener Handoff aus `claude-r3.log` gelöst (Priorität 3, Lifecycle/Stabilität):
  Bei HOME/Input-Switch/Bildschirm-aus spielten ExoPlayer/VLC unbegrenzt im Hintergrund
  weiter — `MainActivity` überschrieb nur `onCreate`, der einzige Player-Teardown war
  `DisposableEffect.onDispose` (`session.release()`).
- Fix in `PlayerScreen.kt`: `LifecycleEventObserver` über `LocalLifecycleOwner` → bei
  `ON_STOP` `session.pause()` (kein Release, kein Auto-Resume; Observer wird in
  `onDispose` entfernt). `ON_STOP` statt `ON_PAUSE`: TV ohne Multi-Window/PIP, exakte
  Grenze „App nicht mehr sichtbar“.
- Begleit-Fix in `ZenPlayerSession.kt`: `PlaybackStatus.settledAfterStart()` zählt
  `Paused` als „gesettelt“ — verhindert falschen `Timeout` bzw. Hintergrund-Fallback-
  Start, wenn während der Start-Phase pausiert wird (HOME während des Tunings).
  Engines emittieren `Paused` nur aus nutzbarem Zustand → keine Fehler-Maskierung.
- `PlaybackStatusTest.kt`: +2 Tests (Klasse jetzt 4); Gesamt 14 JVM-Unit-Tests grün
  (`FallbackPolicyTest` 4/4, `PlaybackStatusTest` 4/4, `PlayerDiagnosticsTest` 6/6),
  `BUILD SUCCESSFUL` offline (`--rerun-tasks` real ausgeführt). Kein Gerät/`adb` →
  keine Hardware-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` → `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Improve-Runde (Code-Change: Ticker nur bei aktiver Wiedergabe)
- Offener Handoff aus `claude-r2.log` gelöst (Priorität 3, Performance): Der
  500-ms-Ticker in `ZenPlayerSession` pollte `positionMs()/durationMs()` permanent —
  auch in `Idle`/`Loading`/`Paused`/`Ended`/`Error`/`Timeout`, d. h. unnötige
  ExoPlayer-/VLC-Calls über die gesamte Session-Lebensdauer.
- Fix: neue testbare Extension `PlaybackStatus.progressLive` (`Playing || Buffering`)
  als Poll-Gate; in allen anderen States wird der letzte bekannte Fortschritt
  eingefroren (`startStream()`/`switchToFallback()` resetten weiterhin explizit auf 0).
- Zusatz ohne Rückschritt: `seekBy()` snappt einmalig den Fortschritt, damit der
  VOD-Balken bei Seek während Pause nicht stale bleibt (StateFlow dedupliziert → im
  laufenden Poll No-op).
- Neuer JVM-Unit-Test `PlaybackStatusTest` 2/2; Gesamt 12 Tests grün
  (`FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6), `BUILD SUCCESSFUL` offline
  (Windows-JVM, 23 s). Kein Gerät/`adb` → keine Hardware-Verifikation.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` → `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Review-Fix (Code-Change: systemischer Compose-Fokus-Bug)
- Offener Handoff aus `claude-r1.log` gelöst: `.onFocusChanged{}` stand in allen
  7 Fokus-Call-Sites HINTER `.focusable()` — laut Compose-Semantik (Events wandern vom
  `FocusTargetNode` nach außen, Doku: „onFocusChanged should be added BEFORE the
  focusable“) wurde es nie erreicht → kein Fokus-Indikator reagierte auf D-Pad.
- Fix in `ZenCard`, `ZenChip`, `NavItemBox`, `ChoiceBtn`, `SmallBtn`,
  `LiveChannelCard`, `PosterChannelCard`: `.onFocusChanged{}` vor `.clickable()`,
  redundantes `.focusable()` entfernt (genau EIN Focus-Target pro Element, da
  `clickable()` bereits fokussierbar macht), unbenutzte Imports bereinigt.
- Verifikation gegen offizielle API-Doku + `FocusTargetNode`-Quellcode
  (`dispatchFocusCallbacks` → `visitSelfAndAncestors(Nodes.FocusEvent, untilType =
  Nodes.FocusTarget)`). `PlayerScreen`/`EpgScreen` geprüft, korrekt, unverändert.
- Build + 10 Unit-Tests offline grün (`BUILD SUCCESSFUL`, `--rerun-tasks`:
  `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6). Kein Gerät/`adb` verfügbar.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` → `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Improve-Runde (Code-Change: TV-Fokus für Browse-Karten)
- Orchestrator `MODUS=improve`; `TO_DEEPSEEK.md` weiterhin nur Platzhalter → kein
  offener Fix-Auftrag, stattdessen genau EIN Schritt nach Priorität 5 (TV-UX/D-pad).
- `ChannelCards.kt`: `LiveChannelCard`/`PosterChannelCard` bekamen das app-weite,
  theme-getriebene Fokusmuster (`LocalFocusEffect` + `zenFocusEffect` + `focusable()` +
  `onFocusChanged`, `ZOOM`-Skalierung). Fokus-Indikation als Overlay über dem Inhalt,
  damit Ring/Gradient auch auf opaken Poster-Artworks sichtbar ist.
- Bestehender `user_agent`-Commit (`90f4f5d`) auf korrekte Verdrahtung geprüft (OK).
- Build + 10 Unit-Tests offline grün (`BUILD SUCCESSFUL`); keine echte Geräte-
  Verifikation möglich (kein `adb`).
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert, `STATE.md` → `ACTIVE_AGENT=CLAUDE`.

## Initial setup
DeepSeek = Implementer.
Claude/Haiku = unabhängiger Reviewer.
Sequenzielle Übergabe über `.ai-collab/`.

## 2026-09-19 — DeepSeek Verifikationsrunde (kein Code-Change)
- `TO_DEEPSEEK.md` ohne neuen Auftrag → Verifikation statt Implementierung.
- Build (`assembleDebug`) + 10 Unit-Tests (`--rerun-tasks`) erfolgreich verifiziert
  (`FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6).
- Working Tree sauber; Code-Stand deckt sich mit `DEEPSEEK_RESULT.md`.
- `DEEPSEEK_RESULT.md` um Abschnitt „Verifikationsrunde“ ergänzt.
- Handoff nach `TO_CLAUDE.md` geschrieben, `STATE.md` → `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek zweite Verifikationsrunde (erneut kein Code-Change)
- Weiterhin kein neuer Auftrag in `TO_DEEPSEEK.md` (nur Platzhalter), kein Diff.
- Build + Tests über Windows-JVM frisch ausgeführt: `BUILD SUCCESSFUL`, 10/10 grün.
- Stand unverändert; `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` um Bestätigung ergänzt.
- `STATE.md` bleibt `ACTIVE_AGENT=CLAUDE` (bereits korrekt gesetzt).

## 2026-09-19 — DeepSeek dritte Verifikationsrunde (erneut kein Code-Change)
- Orchestrator-Runde ohne neuen Auftrag: `TO_DEEPSEEK.md` weiterhin nur Platzhalter.
- Build + Tests frisch über Windows-JVM (`cmd.exe`): `BUILD SUCCESSFUL` in 28 s,
  45 Tasks; `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6, 0 Failures/Errors.
- Code-Abgleich aller Kern-Dateien deckungsgleich mit `DEEPSEEK_RESULT.md`
  (Session/Fallback/Exo/VLC/Diagnostics/Settings inkl. `STATE_IDLE`-Mapping,
  `useController=false`, Cleartext-HTTP, `user_agent`-Key ohne UI-Feld).
- Kein Quellcode-Diff; `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` um dritte Runde ergänzt.
- `STATE.md` → `ACTIVE_AGENT=CLAUDE` (weiterhin Claude an der Reihe).
