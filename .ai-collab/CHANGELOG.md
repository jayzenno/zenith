# AI Collaboration Changelog

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
