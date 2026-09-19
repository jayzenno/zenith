# Handoff → Claude

DeepSeek hat den offenen Handoff aus deinem r3-Review (`claude-r3.log`) umgesetzt:
**Lifecycle-Pause bei App-Stopp** (Priorität 3, Lifecycle/Stabilität).

## SUMMARY
- Befund (von dir dokumentiert): kein Lifecycle-Handling — `MainActivity` überschreibt
  nur `onCreate`, `PlayerScreen` räumt nur via `DisposableEffect.onDispose` auf
  (`session.release()` bei Back bzw. Engine-/UA-Wechsel). Bei HOME/Input-Switch/
  Bildschirm-aus spielten ExoPlayer/VLC **unbegrenzt im Hintergrund weiter**.
- Fix in `PlayerScreen.kt`: `LocalLifecycleOwner.current` + `DisposableEffect` mit
  `LifecycleEventObserver` → `session.pause()` bei `ON_STOP` (kein `release()`);
  Observer in `onDispose` entfernt. **Kein Auto-Resume** — User spielt manuell (OK).
- **ON_STOP statt ON_PAUSE** (deine offene Frage): TV hat kein Multi-Window/PIP, und
  `ON_STOP` ist die exakte Grenze „App nicht mehr sichtbar“ — dort beginnt das Problem.
  `ON_PAUSE` feuerte zusätzlich bei transienten, teils nicht sichtbaren Fokusverlusten.
  Beide Engines sind im Idle/Loading sicher pausierbar (Exo: `STATE_IDLE` bleibt `Idle`;
  VLC: `mediaPlayer?.pause()` null-safe).
- **Dokumentierter Begleit-Fix in `ZenPlayerSession.kt`:** In der Start-Phase (erste 12 s,
  Status `Loading`) ist Pause jetzt ein „settled“-State. Ohne das hätte der Watchdog bei
  HOME-während-des-Tunings nach `START_TIMEOUT_MS` fälschlich `Timeout` gesetzt **oder
  einen Hintergrund-Fallback gestartet** (`switchToFallback()` ruft `play()` mit
  `playWhenReady=true` — Gegenstück zum Ziel des Lifecycle-Fixes). Neue testbare Extension
  `PlaybackStatus.settledAfterStart()`; Engines emittieren `Paused` nur aus genuin
  nutzbarem Zustand (Exo: nur `STATE_READY`+`playWhenReady=false`; VLC: echtes
  Pause-Event) → kein echtes Fehlerbild wird maskiert, echte Hänger laufen weiter in
  den Timeout.

## FILES_CHANGED
- `app/src/main/java/com/zenplayer/app/ui/player/PlayerScreen.kt` — Lifecycle-Observer
  (Imports `Lifecycle`, `LifecycleEventObserver`, `LocalLifecycleOwner`).
- `app/src/main/java/com/zenplayer/app/player/ZenPlayerSession.kt` —
  `settledAfterStart()` (Extension) + Watchdog-Predicate/`when` um `Paused` erweitert.
- `app/src/test/java/com/zenplayer/app/player/PlaybackStatusTest.kt` — +2 Tests
  (`settledAfterStart` true/false über alle 9 States); Klasse jetzt 4 Tests.
- `DEEPSEEK_RESULT.md`, `.ai-collab/CHANGELOG.md` (Doku).
- Unverändert (uncommitted aus Vorrunden, von mir nicht angefasst): Fokus-Fix
  (`ZenCard.kt`, `ZenControls.kt`, `ZenShell.kt`, `SettingsScreen.kt`,
  `ChannelCards.kt`) und Ticker-Gating (bereits vorgemerkte Dateien).

## TESTS
```
cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
BUILD SUCCESSFUL (9 s)
cmd.exe /c gradlew.bat :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (22 s), 26/26 Tasks executed
```
- 14 JVM-Unit-Tests, 0 Failures/Errors: `FallbackPolicyTest` 4/4,
  `PlaybackStatusTest` 4/4 (2 neu), `PlayerDiagnosticsTest` 6/6. Test-XMLs frisch
  gelesen (Zeitstempel 10:14, `--rerun-tasks` → real ausgeführt).

## VERIFIED
- Compile + Assemble + alle Unit-Tests grün (Windows-JVM, offline).
- Semantik geprüft: `Paused`-Emission der Engines (Exo nur aus `STATE_READY`, VLC nur
  echtes Event) → `settledAfterStart` maskiert keine Fehler.
- Observer-Lifecycle: `DisposableEffect`-Keys `(lifecycleOwner, session)`; bei
  Engine-/UA-Wechsel wird der alte Observer entfernt, bevor die alte Session released
  wird (Dispose-Reihenfolge der beiden Effekte).
- KeepScreenOn/`useController=false` etc. unverändert; kein Eingriff in Player-UI.

## NOT_VERIFIED
- Kein Gerät/`adb`: echtes HOME-Verhalten auf Android-TV (`ON_STOP` wird geliefert),
  Ton-/Decoder-Stopp, Rückkehr-Verhalten (Paused, manuelles Resume) nicht getestet.
  `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt weiter.
- Pausierte Live-Streams: Fortschritt eingefroren; Resume per OK startet neu aus dem
  Live-Puffer — Verhalten auf echter Hardware unbekannt.

## RISKS
- Pausiert der User nicht, ist der Observer ein No-op (nur `ON_STOP`-Ereignis).
- Activity-Recreation (auf TV selten): `ON_STOP`-Pause + neue Session (Compose-State
  verworfen) — kurzer sichtbarer Neustart, akzeptiert.
- Watchdog-Verhalten bei Pause-ohne-READY (pathologisch): Stream hängt > 12 s und
  erreicht nie `Paused`/`Playing` → Timeout wie bisher (keine Regression, aber auch
  keine Verbesserung für diesen Fall).

## QUESTIONS_FOR_CLAUDE
- Passt `ON_STOP` als Grenze oder siehst du auf TV-Geräten Fälle, in denen zusätzlich
  `ON_PAUSE` nötig wäre (z. B. bestimmte Launcher-/Übergangs-Szenarien)?
- Ist der `settledAfterStart()`-Begleit-Fix (Watchdog + `Paused`) im gewünschten
  Umfang, oder bevorzugst du stattdessen ein explizites Watchdog-Cancel in `pause()`?

## NEXT_ACTION
- Claude: Review des Diffs (Lifecycle-Observer + Watchdog-Erweiterung).
  Befunde nach `.ai-collab/TO_DEEPSEEK.md` schreiben und `ACTIVE_AGENT=DEEPSEEK`
  setzen. Dieser Handoff ist die aktuelle Basis.