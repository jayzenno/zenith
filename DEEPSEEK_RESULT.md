# DeepSeek Ergebnis — Zenith Player (Phase 2)

Umsetzung von `DEEPSEEK_TASK.md`, zweiter Durchlauf. Fokus: echte ExoPlayer-Diagnose,
VLC/Umschalt-Synchronität, kontrollierter Engine-Fallback, Retry, zentrale User-Agent-
Konfiguration, VOD-Seeking und Finalisierung der TV-Player-UI.

Weiterhin gilt: **Build erfolgreich ≠ echte Wiedergabe verifiziert.** Ohne Gerät/Stream
wurde keine reale Wiedergabe getestet.

## Build & Test

```
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
BUILD SUCCESSFUL
```

- 10 JVM-Unit-Tests, 0 Failures (`PlayerDiagnosticsTest`, `FallbackPolicyTest`).
- Keine Compilerfehler, keine Warnungen im finalen Build (eine bestehende
  `ArrowBack`-Deprecation in `ZenControls.kt` nebenbei bereinigt).

---

## Behoben

### ExoPlayer (Ursachenanalyse + Instrumentierung)
- Analyseverhalten: `PlayerView` mit `useController=false`; `DefaultMediaSourceFactory` mit
  `DefaultHttpDataSource`; Content-Typ wird von media3 aus URL und Response-`Content-Type`
  bestimmt (Progressive/TS, HLS, DASH, SS, RTSP sind als Dependencies vorhanden).
- Cleartext-HTTP geprüft: `AndroidManifest.xml` hat `android:usesCleartextTraffic="true"`,
  HTTP-IPTV ist damit grundsätzlich erlaubt.
- HTTP-DataSource konfiguriert: `setAllowCrossProtocolRedirects(true)`,
  Connect-Timeout `15 s`, Read-Timeout `20 s`, User-Agent.
- Diagnose-Logging ohne Credentials: Host/Port (`PlayerDiagnostics.hostOf`), erkannter
  Streamtyp, UA-Status, Player-State-Übergänge, Fehler als
  `code=… cause=… http=<responseCode> msg=<redigiert>`. Fehlermeldungen werden über
  `sanitizeMessage` URL-bereinigt, sodass Xtream-Pfade mit `user/pass` nicht im Log landen.
- Retryzähler: jeder Streamstart wird als `start #N engine=… fallbackAvailable=…` geloggt.

### VLC — Loading synchron zur echten Wiedergabe
- State ausschließlich aus VLC-Events: `Playing → Playing`, `Buffering → Buffering`,
  `Paused → Paused`, `EndReached → Ended`, `EncounteredError → Error`.
- `Buffering` mit `buffering >= 100` und bereits gestartet führt zurück auf `Playing`.
- Der separate UI-Timer ist entfernt. Ein Timeout ist jetzt ein expliziter
  `PlaybackStatus.Timeout`, der von der Session gesetzt wird — nicht mehr von der UI.

### Automatischer Fallback (kein Ping-Pong)
- Neue `ZenPlayerSession` koordiniert beide Engines, besitzt einen stabilen
  `status`/`engine`/`progress`-StateFlow und stellt das richtige Host-View.
- `FallbackPolicy`: bevorzugter Engine zuerst. Bei hartem Fehler **oder** Start-Timeout
  genau **ein** Wechsel auf die andere Engine. Danach kein weiterer automatischer Wechsel.
- Ist der Primär-Engine in dieser Session bereits gescheitert, starten spätere Sender
  direkt auf dem Fallback (vermeidet wiederholte 12-s-Wartezeiten beim Zappen).
- Scheitert auch die Fallback-Engine, bleibt ein echter `Error`/`Timeout`-State stehen.

### Retry
- `ZenPlayerSession.retry()` startet den aktuellen Request neu über die bestehende
  Fallback-Entscheidung.
- Kein neuer Listener, kein neuer MediaPlayer/ExoPlayer pro Retry; VLC `play()` räumt vorher
  Media/MediaPlayer über `releaseMediaPlayer()` auf; die `generation`-Logik verwirft späte
  Events eines alten VLC-Players.
- `bind()` cancelt den vorigen Status-Collector, bevor ein neuer startet (keine doppelten
  Collector/Coroutines).

### User-Agent
- Zentral in `PlayerDefaults.USER_AGENT`; `ZenPlayerSession` löst
  `settings.userAgent ?: PlayerDefaults.USER_AGENT` auf und übergibt den Wert an beide
  Engines. Der Player-Code hängt nicht mehr an der Settings-Quelle.
- Bewusst **kein** neues UI-Feld: eine Einstellung ist funktional nicht nötig; die
  Architektur erlaubt es später, `settings.userAgent` zu setzen, ohne Player-Code zu ändern.
- VLC: `--http-user-agent` (LibVLC + Media-Option). ExoPlayer: `DefaultHttpDataSource`.

### VOD-Seeking vs. Live
- Controller liefern `positionMs()`/`durationMs()`; die Session tickert `PlaybackProgress`.
- Live: ▲/▼ zappt Sender, ◀/▶ wechselt den Control-Slot. Kein Seek, keine Seekbar-Funktion.
- VOD/Musik/Serie: ◀/▶ spult ±10 s (per OK auf `−10 s`/`+10 s` ebenfalls), ▲/▼ wechselt den
  Slot. Seekbar nur bei VOD mit bekannter Dauer.
- Media-Remote: `MediaRewind`/`MediaFastForward` nur bei seekbarem Inhalt.

### Player-UI (Finalisierung, keine Mobile-Optik)
- Normalzustand: praktisch nur Video + dezenter, transienter Sender-Banner.
- Overlay bei D-Pad/OK: Senderlogo (`AsyncImage`), Name, Kategorie, LIVE-Chip, Uhr,
  Engine/Position, Now/Next mit EPG-Fortschrittsbalken (Live) bzw. Positions-/Dauerbalken
  (VOD), Play/Pause und Sender-/Seek-Navigation.
- Fade + Slide-Animationen, Auto-Hide nach 5 s Inaktivität während der Wiedergabe.
- Focus/Selection: Accent-Gradient, subtile Skalierung, klare Border; kein reiner weißer Rand.
- Liquid-Glass-Richtung über transluzente Gradients/Borders, ohne teuren Blur über dem Video.

### Immersiv (beibehalten)
- Player-Route ohne Nav-Chrome, ohne Padding, ohne animierten Hintergrund (`ZenShell`,
  `ZenTheme`) — Video dominiert den Bildschirm.

### Testbarkeit
- Reine, Android-freie Logik (`PlayerDiagnostics`, `FallbackPolicy`) ist als JVM-Unit-Test
  abgedeckt. Damit sind URL-Redaktion und Fallback-Semantik ohne Gerät verifizierbar.

---

## Getestet

- `:app:assembleDebug` (offline) erfolgreich.
- `:app:testDebugUnitTest` (offline): `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6,
  keine Failures/Errors.
- Code-Review der Abläufe:
  - Start → bevorzugter Engine; Watching-Status über `first { Playing|Ready|Error|Ended }`.
  - Harter Fehler → genau ein Fallback; zweiter Fehler → Error (kein Ping-Pong).
  - Start-Timeout → Fallback, danach `Timeout` statt Endlosschleife.
  - Retry ohne doppelte Listener/Collector/Player.
  - `key(activeEngine)` tauscht das Host-View deterministisch bei Fallback.
- Manifest auf Cleartext-HTTP geprüft (erlaubt).

## Nicht getestet wegen fehlendem Gerät/Netz

- Keine echte Wiedergabe eines IPTV-Streams (MPEG-TS, HLS, DASH) — `adb devices` leer.
- Kein Verifikations dieses Verhaltens: tatsächlicher HTTP-Responsecode, Buffering-Dauer,
  tatsächliches VLC-`Playing`-Event, Verhalten des automatischen Fallbacks mit realen Streams.
- Keine Prüfung von Decoder-/Audio-Verhalten und D-Pad-Focus auf echter Fernbedienung.

## Noch offen

- User-Agent-UI-Feld ist inzwischen vorhanden (Commit `90f4f5d`, Playback-Tab) und korrekt
  auf `SettingsRepository.Keys.USER_AGENT`/`ZenPlayerSession.resolvedUserAgent` verdrahtet.
- VOD-/Live-Fortschritt basiert auf Polling (500 ms); bei sehr langen VODs ggf. anpassen.
- Player, Home-Karten, EPG, Settings und jetzt auch die Browse-Karten (Live/VOD/Music)
  nutzen die Designsprache; verbleibende Detailunterschiede pro Screen möglich.
- Kein Timeshift/Recording.

## Verifikationsrunde (kein Code-Change)

Durchlauf ohne neuen Auftrag: `TO_DEEPSEEK.md` enthielt nur „Warte auf den nächsten
Claude-Review“ und keinen konkreten neuen Befund. Stattdessen wurde der aktuelle Stand
gegen den Bericht geprüft:

- Working Tree sauber (`git status`), kein Diff, Branch `claude/charming-fermi-jhbd3i`.
- `:app:assembleDebug` und `:app:testDebugUnitTest --offline` erneut ausgeführt:
  `BUILD SUCCESSFUL`. Tests per `--rerun-tasks` real ausgeführt:
  `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6, 0 Failures/Errors (10 Tests).
- Code-Abgleich mit dem Bericht bestätigt: `ZenPlayerSession` mit
  `resolvedUserAgent` (`PlayerDefaults.USER_AGENT`), `START_TIMEOUT_MS = 12_000`,
  `FallbackPolicy` (Einmal-Fallback), `PlayerDiagnostics` (sanitize), VLC-Event-Mapping.
- Settings besitzen bereits einen `user_agent`-Key (`SettingsRepository`), ein
  UI-Feld existiert wie dokumentiert noch nicht (bewusst offen).

**Ergebnis: kein Code-Change in dieser Runde.** Offene Punkte aus „Noch offen“
(User-Agent-UI-Feld, übrige Screens, Timeshift/Recording) sind bewusst bzw. außerhalb
des Player-Fokus zurückgestellt.

## Zweite Verifikationsrunde (erneut kein Code-Change)

Wiederholter Durchlauf ohne neuen Auftrag: `TO_DEEPSEEK.md` war weiterhin nur der
Platzhalter „Warte auf den nächsten Claude-Review“, kein neuer Befund von Claude.
Stand erneut verifiziert:

- Branch `claude/charming-fermi-jhbd3i`; Working Tree enthält nur die (uncommitteten)
  Handoff-Dokumente der Vorrunde, **keinen** Quellcode-Diff.
- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks` via
  Windows-JVM erneut ausgeführt: `BUILD SUCCESSFUL` in 29 s, 45 Tasks.
- Test-XMLs gelesen: `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6, 0 Failures
  (10 Tests) — deckungsgleich mit dem Bericht.
- Code-Abgleich: `ZenPlayerSession.resolvedUserAgent` (`PlayerDefaults.USER_AGENT`),
  `START_TIMEOUT_MS = 12_000`, Einmal-Fallback (`FallbackPolicy`), VLC-Event-Mapping,
  `SettingsRepository.Keys.USER_AGENT` ohne UI-Feld — unverändert.
- Kein Gerät/Stream verfügbar (kein `adb`, keine reale Wiedergabe) — Lücke aus
  „Nicht getestet“ bleibt bestehen.

**Ergebnis: kein Code-Change, keine neuen offenen Punkte.** Der Bericht bleibt gültig.

## Dritte Verifikationsrunde (erneut kein Code-Change)

Wiederholter Durchlauf auf Anweisung des Orchestrators; `TO_DEEPSEEK.md` enthielt
weiterhin nur den Platzhalter „Warte auf den nächsten Claude-Review“, keinen neuen
Befund von Claude. Stand erneut verifiziert:

- Branch `claude/charming-fermi-jhbd3i`; Working Tree ohne Quellcode-Diff — uncommitted
  sind nur die Handoff-Dokumente (`.ai-collab/*`, `DEEPSEEK_RESULT.md`).
- Build über Windows-JVM (`cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest
  --offline --rerun-tasks`): `BUILD SUCCESSFUL` in 28 s, 45 Tasks (davon 45 executed).
- Test-XMLs gelesen: `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6, 0 Failures,
  0 Errors (10 Tests) — deckungsgleich mit dem Bericht.
- Code-Abgleich (alle Kern-Dateien frisch gelesen):
  - `ZenPlayerSession.kt`: `resolvedUserAgent` (`PlayerDefaults.USER_AGENT`-Fallback),
    `START_TIMEOUT_MS = 12_000`, Watchdog mit `first { Playing|Ready|Error|Ended }`,
    `retry()` ohne neue Listener/Player, `bind()` cancelt vorigen Collector.
  - `FallbackPolicy.kt`: Einmal-Fallback, kein Ping-Pong, `primaryFailed` merkt Primär-
    Fehler sessionweit.
  - `PlayerDiagnostics.kt`/`PlayerDefaults.kt`: `sanitizeMessage`/`hostOf` ohne
    Credentials, zentrale Defaults (UA, 15 s Connect, 20 s Read, 12 s Start-Timeout).
  - `PlayerEngines.kt` (`ExoPlayerController`): `DefaultHttpDataSource.Factory` mit
    `setAllowCrossProtocolRedirects(true)`, Timeouts, UA; `useController=false` —
    PlayerView ohne eingebaute Controls (kein Mobile-Look, kein Focus-Klau);
    `STATE_IDLE`-Mapping auf `Loading`/`Idle`; `hasStarted` trennt Loading/Buffering.
  - `VlcPlayerController.kt`: State nur aus Events (`Playing`, `EndReached`,
    `EncounteredError`, `Buffering` mit `buffering >= 100 && hasStarted`).
  - `SettingsRepository.kt`: `Keys.USER_AGENT = "user_agent"` vorhanden, bewusst
    weiterhin ohne UI-Feld.
- Manifest geprüft: `INTERNET`-Permission, `usesCleartextTraffic="true"` — HTTP-IPTV
  bleibt grundsätzlich erlaubt.
- Kein Gerät/Stream verfügbar (`adb` nicht im PATH) — echte Wiedergabe weiterhin nicht
  verifiziert.

**Ergebnis: kein Code-Change, keine neuen offenen Punkte.** Der Bericht bleibt gültig;
`BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt unverändert.

## Improve-Runde (Code-Change): TV-Fokus-Indikation für Browse-Karten

Der Orchestrator hat `MODUS=improve` vorgegeben (zuerst offene Handoffs lösen, danach
höchstens EINEN abgegrenzten Verbesserungsschritt nach Priorität). `TO_DEEPSEEK.md`
enthielt auch in dieser Runde keine konkreten Review-Befunde, daher wurde als genau
ein Schritt Priorität 5 (TV-UX/D-pad) gewählt:

### Befund
`LiveChannelCard` (Live-TV) und `PosterChannelCard` (VOD + Music) hatten nur nacktes
`.clickable(...)` ohne jegliche Fokus-Indikation. Bei D-Pad-Navigation war der fokussierte
Eintrag auf den drei Haupt-Browse-Screens praktisch unsichtbar. `ZenCard`, `ZenChip`,
EPG- und Settings-Screens nutzen dagegen bereits `zenFocusEffect` + `focusable()` +
`onFocusChanged`.

### Änderung (nur `ChannelCards.kt`)
- Beide Karten übernehmen das app-weite, theme-getriebene Fokusmuster:
  `LocalFocusEffect.current`, `focused`-State via `onFocusChanged` (`isFocused && hasFocus`),
  `zenFocusEffect(focused, focusEffect, shape)`, `focusable()`, klickbar mit
  `MutableInteractionSource`/`indication = null`.
- `FocusEffect.ZOOM` skaliert die Karte (1.05, `tween(220)`) über `graphicsLayer` —
  konsistent mit `ZenCard`.
- Die Fokus-Indikation wird als **Overlay über dem Inhalt** gezeichnet
  (`Box(Modifier.matchParentSize().zenFocusEffect(...))`, letztes Kind): Bei
  opaken Poster-Artworks bleibt der Ring/Gradient garantiert sichtbar (bei `ZenCard`
  läge er unter opakem Inhalt).
- Layout/Maße/Inhalte unverändert; keine Mobile-Optik, kein Blur über dem Inhalt.

### Build & Tests
```
cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
BUILD SUCCESSFUL (Assemble 8 s, Tests 3 s)
```
- 10 JVM-Unit-Tests weiterhin grün (`FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6).

### Nicht verifiziert (kein Gerät)
- Reale D-Pad-Fokus-Wiedergabe auf TV-Hardware, Sichtbarkeit aller
  `FocusEffect`-Varianten (`RING`, `GLOW`, `BAR`, `CORNERS`, `HALO`, `SWEEP`, `ZOOM`)
  auf echter Fernbedienung nicht getestet. Wirkt nur durch Code-Muster-Abgleich mit
  den bereits etablierten (`ZenCard`/`ZenChip`/EPG/Settings).

---

## Review-Fix (Code-Change): Systemischer Fokus-Bug — onFocusChanged-Reihenfolge

Claude hat in seinem Review (`claude-r1.log`, konnte mangels Schreibrechten keinen
Diff setzen) einen **systemischen Fokus-Bug** gemeldet. Dieser Handoff wurde in dieser
Runde zuerst gelöst.

### Befund (verifiziert)
Alle sieben Fokus-Call-Sites nutzten die Modifier-Reihenfolge
`.clickable(...).focusable().onFocusChanged { ... }`. Verifikation gegen Primärquellen:

- Offizielle Doku (API-Referenz `onFocusChanged`, `FocusState`):
  *"The `onFocusChanged` modifier listens to the state of the first `focusTarget`
  **following** this modifier."* und *"The `onFocusChanged` should be added **BEFORE**
  the `focusable` that is being observed."*
- Compose-Quellcode `FocusTargetNode.dispatchFocusCallbacks`:
  Fokus-Events werden an `visitSelfAndAncestors(Nodes.FocusEvent, untilType =
  Nodes.FocusTarget)` verteilt — d. h. vom aktiven `FocusTargetNode` **nach außen**
  (zu den früheren Modifiern der Kette), gestoppt am nächsten außen liegenden
  `FocusTarget`.

**Konsequenz:** Mit `.onFocusChanged{}` am Kettenende (innen liegend) wurde es nie
erreicht → `focused` blieb dauerhaft `false` → **kein Fokus-Indikator in der App hat
auf D-Pad reagiert**, auch nicht die in der Vorrunde ergänzten Browse-Karten. Zudem
erzeugten `clickable()` **und** `focusable()` zwei Focus-Targets an einem Element, was
die Reihenfolge der Event-Zustellung zusätzlich mehrdeutig macht.

### Fix (7 Sites, 5 Dateien)
Garantiert korrekt ist: **genau ein Focus-Target pro Element** und
`.onFocusChanged { ... }` **vor** diesem Target in der Kette. Da `clickable()` bereits
ein Focus-Target liefert (dokumentiert: klickbare Composables sind fokussierbar),
wurde das redundante `.focusable()` entfernt und `.onFocusChanged{}` unmittelbar vor
`.clickable(...)` gesetzt:

| Datei | Composable |
|---|---|
| `ui/components/ZenCard.kt` | `ZenCard` |
| `ui/components/ZenControls.kt` | `ZenChip` |
| `ui/navigation/ZenShell.kt` | `NavItemBox` (Sidebar) |
| `ui/settings/SettingsScreen.kt` | `ChoiceBtn`, `SmallBtn` |
| `ui/components/ChannelCards.kt` | `LiveChannelCard`, `PosterChannelCard` |

Unbenutzte `focusable`-Imports entfernt. `PlayerScreen` (`.focusable()` als Key-Handler-
Root ohne `onFocusChanged`) und `EpgScreen` (selection-basierter Fokus) sind korrekt
und blieben unverändert. Keine Layout-/Logikänderungen; `clickable`-Parameter
(`interactionSource`/`indication = null`) unverändert.

### Build & Tests
```
cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
BUILD SUCCESSFUL (22 s)
cmd.exe /c gradlew.bat :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (47 s) — FallbackPolicyTest 4/4, PlayerDiagnosticsTest 6/6, 0 Failures
```

### Nicht verifiziert (kein Gerät)
- Echte D-Pad-Fokus-Wiedergabe auf TV-Hardware weiterhin nicht getestet (kein `adb`).
  Der Nachweis stützt sich auf offizielle Doku + Compose-Quellcode-Semantik.
- Kein neuer JVM-Unit-Test möglich: Compose-Fokus erfordert UI-Test-Infrastruktur
  (Robolectric/ui-test), die offline nicht nachladbar ist.

---

## Improve-Runde (Code-Change): 500-ms-Ticker nur bei aktiver Wiedergabe

Offener Handoff aus Claude-Review `claude-r2.log` gelöst (Priorität 3, Performance):
`ZenPlayerSession.startTicker()` pollte **unabhängig vom Player-State** alle 500 ms
`positionMs()/durationMs()` — auch in `Idle`, `Loading`, `Paused`, `Ended`, `Error`,
`Timeout` (teilweise Leben lang der Session, ohne dass je gestartet wurde).

### Änderung (`ZenPlayerSession.kt` + neuer Unit-Test)
- Ticker fragt die Engines **nur noch in `Playing`/`Buffering`**:
  `val PlaybackStatus.progressLive` (neue, testbare Erweiterungseigenschaft) gated den
  Poll; in allen anderen States wird der letzte bekannte Fortschritt **eingefroren**
  (nicht auf 0 zurückgesetzt — `startStream()`/`switchToFallback()` setzen weiterhin
  explizit auf 0).
- `snapProgress()` nach `seekBy()`: verhindert einen sichtbaren Rückschritt beim
  VOD-Seek während Pause — der Balken im Overlay würde sonst stale bleiben, weil der
  Ticker eingefroren ist. `StateFlow` dedupliziert gleiche Werte → im laufenden Poll
  praktisch No-op.
- `PROGRESS_POLL_MS = 500L` als benannte Konstante.

### Build & Tests
```
cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
BUILD SUCCESSFUL (23 s)
```
- 12 JVM-Unit-Tests grün, 0 Failures: neu `PlaybackStatusTest` 2/2 (pollt genau bei
  `Playing`/`Buffering`, friert bei `Idle`/`Loading`/`Ready`/`Paused`/`Ended`/
  `Timeout`/`Error`), plus `FallbackPolicyTest` 4/4, `PlayerDiagnosticsTest` 6/6.
- Test-XMLs frisch gelesen (Testzeitstempel der aktuellen Ausführung).

### Nicht verifiziert (kein Gerät)
- Reduzierte CPU-/Engines-Last auf echter Hardware nicht messbar; Verhalten bei
  reifem Seek-while-paused auf TV nicht getestet (kein `adb`).

---

## Improve-Runde (Code-Change): Lifecycle-Pause bei App-Stopp (HOME)

Offener Handoff aus `claude-r3.log` gelöst (Priorität 3, Lifecycle/Stabilität). Claude
hatte den Ticker-Fix der Vorrunde **approved** und als nächsten Schritt die fehlende
Lifecycle-Behandlung übergeben: `MainActivity` überschreibt nur `onCreate`, `PlayerScreen`
räumt nur in `DisposableEffect.onDispose` auf (`session.release()` bei Back-Navigation bzw.
Engine-/UA-Wechsel) — nicht aber, wenn die Activity gestoppt wird. Folge auf echter
TV-Hardware: HOME-Taste / Input-Switch / Bildschirm-aus → ExoPlayer/VLC spielten
**unbegrenzt im Hintergrund weiter** (Ton über dem Home-Screen, Netz-/CPU-/Batterieverbrauch,
Decoder an möglicherweise losgelöstem `SurfaceView`).

### Änderung (`PlayerScreen.kt` + `ZenPlayerSession.kt` + Test)
- **`PlayerScreen.kt`:** `LocalLifecycleOwner.current` + zweites `DisposableEffect`, das
  einen `LifecycleEventObserver` registriert, der bei `ON_STOP` → `session.pause()` aufruft.
  Observer wird in `onDispose` entfernt. **Kein Auto-Resume** bei Rückkehr — der User
  spielt manuell weiter (OK-Taste), bewusst und wie von Claude vorgeschlagen.
- **`ON_STOP` statt `ON_PAUSE`** (Claude bat um den Check): TV kennt kein Multi-Window/PIP;
  HOME-/Input-Wechsel lösen `onPause`→`onStop` aus. `ON_STOP` ist die exakte Grenze „App
  nicht mehr sichtbar“ — dort beginnt das Problem. `ON_PAUSE` feuert zusätzlich bei
  transienten, teils gar nicht sichtbaren Fokusverlusten → mehr unnötige Pausen.
  Beide Engines sind im Idle/Loading sicher pausierbar (Exo: `player.pause()` lässt
  `STATE_IDLE` → Status bleibt `Idle`; VLC: `mediaPlayer?.pause()` null-safe).
- **`ZenPlayerSession.kt` — Watchdog-Interaktion (dokumentierter Begleit-Fix):**
  Pausiert der User/System während der Start-Phase (HOME in den ersten 12 s, Status noch
  `Loading`), wartete der Watchdog weiter auf `Playing|Ready|Error|Ended` → nach
  `START_TIMEOUT_MS` wäre fälschlich `Timeout` gesetzt **oder** ein **Hintergrund-Fallback**
  gestartet worden (`switchToFallback()` ruft `play()` mit `playWhenReady=true` — also genau
  die unerwünschte Hintergrund-Wiedergabe, die der Lifecycle-Fix verhindern soll).
  Fix: neue testbare Extension `PlaybackStatus.settledAfterStart()` — `Paused` zählt jetzt
  als „gesettelt“. Die Engines emittieren `Paused` nur aus genuin nutzbarem Zustand
  (Exo: nur bei `STATE_READY` + `playWhenReady=false`; VLC: nur echtes Pause-Event) → kein
  echtes Fehlerbild wird maskiert; ein wirklich hängender Stream läuft weiter in den Timeout.
- **`PlaybackStatusTest.kt`:** 2 neue Tests (`settledAfterStart` true/false, alle 9 States),
  Gesamt 4 Tests in dieser Klasse.

### Build & Tests
```
cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline
BUILD SUCCESSFUL (9 s)
cmd.exe /c gradlew.bat :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (22 s) — 26/26 Tasks executed
```
- 14 JVM-Unit-Tests, 0 Failures/Errors: `FallbackPolicyTest` 4/4, `PlaybackStatusTest` 4/4,
  `PlayerDiagnosticsTest` 6/6. Test-XMLs frisch gelesen (Zeitstempel + `--rerun-tasks`).

### Nicht verifiziert (kein Gerät)
- Echtes HOME-Verhalten auf TV-Hardware (ob die Activity auf Android-TV bei HOME
  tatsächlich `ON_STOP` liefert, ob Ton/Decoder wirklich gestoppt werden) nicht
  getestet — kein `adb`.
- Kein Gerätetest für: pausierte Live-Streams nach Rückkehr (Fortschritt eingefroren,
  Resume per OK).

### Risiko-Rest
- Pausiert der User **nicht**, ändert sich nichts (Observer feuert nur bei `ON_STOP`).
- Bei Activity-Recreation (config change, selten auf TV) → `ON_STOP`-Pause + neue Session
  durch `remember`-Verlust; alter Session-Release via `onDispose`. Akzeptiert.

---

## Bekannte Einschränkungen

- Der Start-Timeout für den Fallback beträgt 12 s (`PlayerDefaults.START_TIMEOUT_MS`). Ein
  legitim langsamer Stream kann dadurch einmalig auf die andere Engine wechseln. Wert ist
  zentral konfigurierbar.
- Ein automatischer Engine-Wechsel startet den Stream neu (kurzer sichtbarer Neustart).
- `Build erfolgreich` und `echte Wiedergabe verifiziert` bleiben strikt getrennt; letzteres
  steht aus.
- `FallbackPolicy` merkt sich einen Primär-Fehler nur für die Lebensdauer der Session
  (Engine-/UA-Wechsel in den Einstellungen erzeugt eine neue Session).
