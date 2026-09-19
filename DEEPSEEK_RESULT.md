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

## Core-TV Runde (Code-Change): Guide zeigt nur noch echte Anbieterdaten (kein Produktions-Mock)

`MODUS=core-tv`, `TO_DEEPSEEK.md` war weiterhin nur Platzhalter. Als genau EIN in sich
geschlossenes Vorhaben wurde der höchste unerfüllte Gate-Punkt angegangen:
**Gate #2 „Real provider/XMLTV/Xtream EPG is displayed; no production placeholders“**
(Masterplan-Regel #2: „No fake data in production“).

### Befund (im realen Code verifiziert)
1. **Fake-Programme:** `epgProgramsFor()` in `EpgData.kt` fiel bei fehlenden DB-Daten auf
   `mockProgramsFor()` zurück → der Guide zeigte erfundene Sendepläne („Tagesschau“,
   „Tatort“, „Top Gun: Maverick“ …), sobald ein Sender/Tag keine echten EPG-Daten hatte.
2. **Demo-Sender:** `EPG_CHANNELS` startete mit 26 hartcodierten deutschen Sendern
   (Das Erste, ZDF, Sky …). Diese flossen bis zum ersten DB-Load in Guide **und**
   Home-Screen („Jetzt LIVE“, „Deine Favoriten“, Kanalliste) — Demo-Daten in Produktion.
3. **Guide→Player-Pfad kaputt (Playback-Fehler):** `buildEpgChannel(vi)` baute einen
   fabrizierten Channel (`id="epg_$vi"`, `providerId=-1`, `url=""`). OK im Guide
   navigierte zu `player/-1/LIVE/epg_…` → `observeByType(-1, "LIVE")` liefert nichts →
   schwarzer, leerer Player. Der echte Stream war aus dem Guide nicht startbar.
4. **Player-EPG-Identität falsch:** `PlayerViewModel.upcoming(channel.id)` suchte mit der
   URL-basierten DB-Id; XMLTV speichert aber unter `tvg-id`/`epg_channel_id`
   (`Channel.extra`) → „JETZT/DANACH“ im Player-Overlay blieb für M3U/Xtream leer.

### Änderungen
- **`EpgData.kt`:** `EPG_CHANNELS` startet **leer** (Kommentar dokumentiert das);
  `epgProgramsFor()` liefert **nur gespeicherte echte Daten**, kein Mock-Fallback.
  Komplette Mock-Maschinerie entfernt (`CHCATS`, `POOL`, `PROGRAM_CATEGORIES`,
  `MOCK_PROGS`, `mockProgramsFor`, `seedRand`, `progressCategory` …) — −154/+11 Zeilen.
- **`EpgScreen.kt`:**
  - `EpgBody`: bei `vis.isEmpty()` ehrlicher Full-Size-Hinweis („Keine Sender vorhanden —
    Anbieter in den Einstellungen hinzufügen“) statt leerem Raster.
  - `ProgramRow`: ohne echte Programmdaten eine ehrliche Zelle über das Fenster
    („Keine Programmdaten für diesen Sender“) statt erfundener Sendepläne.
- **`EpgViewModel.kt`:** neues `channelAt(vi): Channel?` liefert den **echten** DB-Kanal
  an Rasterposition `vi`.
- **`EpgScreen.kt`:** `buildEpgChannel` entfernt; OK/Play nutzt `vm.channelAt(vi)` →
   der Player startet den echten Provider-Stream (richtige `providerId`/`url`/`id`).
- **`PlayerScreen.kt`:** EPG-Lookup im Live-Overlay über die EPG-Identität
  (`extra ?: id`), konsistent zum Guide (`refreshPrograms` nutzt dieselbe Auflösung).
- **Neu `EpgDataTest.kt` (8 JVM-Tests):** `epgProgramsFor`/`nowProg` liefern leer bei
  leerem Store (kein Fake-Fallback), gespeicherte echte Programme werden geliefert,
  `displayWindowStart` = 05:00 lokal, `mapDbPrograms`/`mapDbChannel`-Math, Fenster-Filter.

### Build & Tests
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (29 s, 45 Tasks executed)
```
- 22 JVM-Tests, 0 Failures/Errors: `EpgDataTest` 8/8 (neu), `FallbackPolicyTest` 4/4,
  `PlaybackStatusTest` 4/4, `PlayerDiagnosticsTest` 6/6. XMLs frisch gelesen.

### Gate-Bezug
- Gate #2 (echte EPG, keine Platzhalter): Kern dieser Runde — behoben im Datenteil.
- Regel #2 (keine Fake-Daten): Demo-Kanäle + Mock-Sendespläne aus dem Produktpfad entfernt.
- Priorität-1-Nebenwirkung: Guide→Player-Start über echten Kanal repariert.

### Nicht verifiziert (kein Gerät/Stream)
- Reale Guide-Darstellung mit echtem XMLTV/Xtream-Datenbestand auf TV-Hardware nicht
  getestet (`adb` nicht verfügbar). `BUILD SUCCESSFUL != echte Wiedergabe verifiziert`.
- D-Pad-Verhalten im Empty-Grid (keine Programme) auf echter Fernbedienung nicht getestet;
  Logik ist selection-basiert und auf leere Listen abgesichert (clamps, `getOrNull`).
- Home-Screen: `KanalHome`/„Jetzt LIVE“/Favoriten-Tiles hängen an `EPG_CHANNELS`, das erst
  nach Guide-Besuch vom DB-Flow gefüllt wird — vorher jetzt ehrlich leer statt Demo-Sender.

### Bekannte Folgepunkte (bewusst NICHT in dieser Runde)
- `MAX_EPG_CHANNELS = 200` kappt Senderlisten weiterhin stillschweigend (Gate #9,
  „no silent truncation“) — braucht reale Grid-Virtualisierung; separat.
- „Recently Watched“ existiert noch nicht als First-Class-Gruppe (Gate #8); bevorzugte
  Favoriten-/Kanal-Gruppen sind EPG-Index-basiert.
- Home-Bindung an echtes Kanal-Repository (statt In-Memory-`EPG_CHANNELS`).
- Home-Hero/„Empfohlen“-Reihen sind dekorative Platzhalter (Designreferenz).

---

## Core-TV Runde (Code-Change): EPG-Grid virtualisiert — Focus-Follow + keine Silent-Truncation

`MODUS=core-tv`, `TO_DEEPSEEK.md` weiterhin nur Platzhalter. Als genau EIN geschlossenes
Vorhaben wurden die zwei höchsten noch offenen Gate-Punkte angegangen, die beide an der
**fehlenden Grid-Virtualisierung** hingen:

- **Gate „Focus is always visible and scrolls the grid“:** Vorher nutzte der Guide
  `Column` + `verticalScroll` **ohne jedes Auto-Scroll** — D-Pad Up/Down bewegte die
  Auswahl nach ~10 Schritten aus dem sichtbaren Bereich (Fokus unsichtbar),
  Links/Rechts ebenso horizontal.
- **Gate „Full channel counts are accounted for; no unexplained missing channels“ /
  Masterplan-Regel #4 „No silent truncation“:** `MAX_EPG_CHANNELS = 200` kappte die
  Senderliste stillschweigend; Kanal 201+ war weder im Guide noch in der virtuellen
  `KanalHome`-Liste erreichbar. Der Cap existierte nur, weil der Grid nicht
  virtualisiert war (sonst ANR/OOM bei Playlists mit Tausenden Kanälen).

### Änderungen
- **`EpgScreen.kt` — Grid auf `LazyColumn` umgestellt:**
  - **Programm-Grid:** `LazyColumn` komponiert nur die sichtbaren Zeilen; jede Zeile
    behält ihre volle 24-h-Breite (`horizontalScroll`). **Kanalspalte:** eigene
    `LazyColumn` mit identischem Zeilen-Pitch (`rowH + 5.dp`), folgt dem Grid über
    `snapshotFlow { firstVisibleItemIndex/ScrollOffset } → scrollToItem` — das
    bewährte Shared-`ScrollState`-Muster des bestehenden Codes bleibt für die
    horizontale Achse erhalten (sticky Time-Header + alle komponierten Zeilen teilen
    einen `hScroll`).
  - **Vertikaler Focus-Follow:** `LaunchedEffect(vm.row)` → `animateScrollToItem(row)`
    — die D-pad-Auswahl bleibt immer sichtbar; der Guide öffnet dadurch auch gezielt
    um die aktuelle Zeile („opens around now“).
  - **Horizontaler Focus-Follow:** `LaunchedEffect(vm.row, vm.ecol, …)` → berechnet
    über die neue pure Funktion `EpgData.epgScrollTargetX(prog, gridWidthPx,
    timeColWpx, padPx)` das Scroll-Ziel und animiert `hScroll` dorthin (Grid-Breite
    wird per `onSizeChanged` gemessen → kein Over-Scroll).
  - **Ehrliche Kanalzahlen:** Die Kanalspalten-Headline zeigt bei Filteraktivierung
    „X von N“ (Favoriten), sonst die volle Zahl — nie mehr eine stillschweigend
    gekappte Zahl.
  - Kein `verticalScroll`/`vScroll` mehr; `ProgramRow` bekommt die Zeilenzahl als
    Parameter (statt `vm.currentVis()` pro Zeile neu zu berechnen — O(N)-Allokation
    pro komponierter Zeile entfernt).
- **`EpgData.kt`:** `MAX_EPG_CHANNELS` **entfernt**; neue pure, JVM-testbare Funktion
  `epgScrollTargetX()` (Clamping gegen `[0, totalW − gridW]`, Start-Snap bei leerer
  Zeile).
- **`EpgViewModel.kt`:** beide `take(MAX_EPG_CHANNELS)` entfernt (DB-Collector und
  `currentVis()`); Kommentar dokumentiert die bewusste Caps-Freiheit.

### Build & Tests
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (20 s, 45 Tasks executed)
```
- 25 JVM-Unit-Tests, 0 Failures/Errors: `EpgDataTest` **11/11** (8 alte + 3 neue
  `epgScrollTargetX`-Tests: Clamping, kein Over-Scroll, Start-Snap), `FallbackPolicyTest`
  4/4, `PlaybackStatusTest` 4/4, `PlayerDiagnosticsTest` 6/6. XMLs frisch gelesen
  (`--rerun-tasks` → real ausgeführt).

### Gate-Bezug
- „Focus is always visible and scrolls the grid“ — umgesetzt (vertikal + horizontal).
- Gate #9 „Full channel counts accounted for, no silent truncation“ — Cap entfernt,
  ehrliche Zahlen in der Kanalspalte.
- Nebeneffekt: Guide öffnet gezielt um die aktuelle Zeile (`LaunchedEffect(vm.row)`
  beim ersten Compose).
- Regel #4 „No silent truncation“ — erfüllt für den EPG-Grid (Home `KanalHome` ist
  bereits `LazyColumn`-basiert und iteriert die jetzt ungekappte Liste).

### Nicht verifiziert (kein Gerät/Stream)
- Reale Performance-/Scroll-Fluency mit einer Playlist von tausenden Kanälen auf
  TV-Hardware nicht gemessen (`adb` nicht verfügbar).
- D-Pad-Zapp-Verhalten des virtualisierten Grids (animierte Follow-Scrolls) auf echter
  Fernbedienung nicht getestet.
- Sync der Kanalspalte bei Touch-Scroll auf TV-Geräten nicht getestet (TV hat i. d. R.
  keinen Touch; die Logik ist über `snapshotFlow` touch-agnostisch).
- `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt unverändert.

---

## Core-TV Runde (Code-Change): „Zuletzt gesehen“ als First-Class-Gruppe (Gate #8)

`MODUS=core-tv`, `TO_DEEPSEEK.md` weiterhin nur Platzhalter. Als genau EIN geschlossenes
Vorhaben wurde der letzte noch fehlende Gate-Punkt #8 **„Favorites, All Channels and
Recently Watched exist as first-class virtual groups“** angegangen — im Code verifiziert:

### Befund (im realen Code verifiziert)
1. **„Recently Watched“ fehlte komplett:** Es gab keine Wiedergabe-Historie
   (kein Feld/Key/Recorder im `SettingsRepository`, kein Schreibpfad im Player,
   keine Gruppe im Guide). Favoriten (♥) und „Alle Sender“ existierten bereits als
   EPG-Index-Gruppen.
2. **Der Guide hatte keinen Gruppenumschalter:** Der Favoriten-Filter war im Guide
   gar nicht erreichbar (nur der „Nur Favoriten“-Toggle in den Einstellungen), und
   `toggleFavsOnly` setzte nur das eine Flag — ein zweiter Gruppenmodus hätte ohne
   gemeinsamen Weg einen Torn-State erzeugt (beide Filter gleichzeitig aktiv).

### Änderungen
- **Neu `data/settings/RecentWatch.kt`** (Data-Ebene, rein, JVM-testbar — kein
  UI-/Compose-Import in `SettingsRepository`):
  - `recentWatchKey(ch) = "${providerId}:${id}"` — kollisionssicher über Anbieter.
  - `pushRecentWatch(current, key, limit=24)` — Dedupe + Move-to-Front + Cap.
  - `recentWatchDecode(raw: String?)` / `recentWatchEncode(list)` — newline-joined
    Persistenz (Reihenfolge bleibt erhalten; ein `Set` würde sie verlieren).
  - `recentOnlyVis(recent, channels)` — Recency-Order → globale Guide-Indizes;
    unbekannte/gelöschte Kanäle werden übersprungen (nie fabriziert).
- **`SettingsRepository.kt`:** `ZenSettings.epgRecent: List<String>` +
  `epgRecentOnly: Boolean`, Keys `epg_recent`/`epg_recentonly` (geladen im Factory).
  `pushRecentChannel(key)` als **atomarer** Read-Modify-Write in EINEM `edit`
  (Zap-Rennen sicher, Fast-Path wenn Key schon vorn); `setEpgActiveGroup(mode)`
  setzt exakt EINE Gruppe („all“/„favs“/„recent“) in einem Edit — kein Torn-State.
- **`PlayerScreen.kt`:** im Tune-`LaunchedEffect` nach `session.play(...)` wird
  `container.settings.pushRecentChannel(recentWatchKey(current))` für
  `mediaType == LIVE && providerId >= 0` geschrieben — ein Funnel für alle Einstiege
  (Guide-OK, Live-TV-Logo, Home-Karte). VOD/Music bewusst ausgeschlossen.
- **`EpgViewModel.kt`:** `currentVis()` unterstützt die Recent-Gruppe (Base =
  Favoriten-Subset bzw. alle; Recency-Order bleibt, Schnittmenge bei Alt-Daten mit
  beiden Flags → ehrlich); neu `cycleGroup()` (Alle Sender → Favoriten → Zuletzt
  gesehen → …), `activeGroupLabel()`, `snapTo()` (re-anchort Auswahl synchron aus dem
  aktuellen Snapshot, da der Settings-Flow async ist); `toggleFavsOnly` nutzt jetzt
  `setEpgActiveGroup`.
- **`EpgScreen.kt`:** `Key.G` → `cycleGroup()`; Spalten-Header zeigt die aktive Gruppe
  (`uppercase()`, ellipsiert) + **ehrliche Zählung** „X von N“ bei Filter;
  Empty-State pro Gruppe („Keine Sender in ‚Zuletzt gesehen‘ — Schau Live-TV oder öffne
  Kanäle aus dem Guide…“ statt irreführendem Provider-Hinweis); Hint „G = Gruppe“.
- **`SettingsViewModel.kt`:** Favoriten-Toggle in den Einstellungen nutzt jetzt
  ebenfalls `setEpgActiveGroup` → alle Schreibpfade exklusiv.
- **Neu `RecentWatchTest.kt` (9 JVM-Tests):** `pushRecentWatch` Move-to-Front/Dedupe/
  Idempotenz/Cap, `recentWatchKey` provider-gescopt, Encode/Decode-Roundtrip
  (inkl. null/blank), `recentOnlyVis` Recency-Order + Skip unbekannter/gelöschter
  Kanäle + leere Eingaben.

### Build & Tests
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (23 s, 45 Tasks executed)
```
- **34 JVM-Unit-Tests, 0 Failures/Errors:** `RecentWatchTest` **9/9** (neu),
  `EpgDataTest` 11/11, `FallbackPolicyTest` 4/4, `PlaybackStatusTest` 4/4,
  `PlayerDiagnosticsTest` 6/6. XMLs frisch gelesen (`--rerun-tasks`).

### Gate-Bezug
- Gate #8 „Favorites, All Channels and Recently Watched as first-class virtual
  groups“ — „Zuletzt gesehen“ ist jetzt eine First-Class-Gruppe (eigene Daten,
  eigener Guide-Modus, eigener Empty-State, sichtbare Gruppen-Zählung). Alle drei
  Gruppen sind über `G` im Guide erreichbar.

### Nicht verifiziert (kein Gerät/Stream)
- Kein `adb`/Gerät: reales D-Pad-Verhalten (G-Taste, Gruppenschleife), Guide-
  Darstellung der Recent-Gruppe und Datenbank-/Datastore-Verhalten auf Hardware
  nicht getestet. `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt weiterhin.
- `pushRecentChannel` (ein DataStore-`edit` pro Zap) nicht auf I/O-Last bei
  Dauermashing gemessen — Logik ist serialisiert und klein, Wert im Rahmen.

### Bekannte Folgepunkte (bewusst NICHT in dieser Runde)
- Recent-Einträge referenzieren `providerId:id`; tote Keys nach Provider-Löschung
  bleiben liegen (werden beim Rendern übersprungen; Aufräumen z. B. beim Sync separat).
- Eine „Zuletzt gesehen“-Reihe im Home-Screen ist noch keine eigene Sektion
  (Guide-Gruppe gesetzt; Home-Reihe als Folgeschritt möglich).

---

## Core-TV Runde (Code-Change): Anbieter-Kategorien im Guide (Gate #7) + ehrliche Favoriten

`MODUS=core-tv`; `TO_DEEPSEEK.md` war weiterhin nur Platzhalter, `STATE.md` stand auf
`ACTIVE_AGENT=CLAUDE` (voriger Handoff wartet auf Review). Orchester-Auftrag war explizit
eine Implementierungs-Runde → höchster unerfüllter Gate-Punkt selbst gewählt:
**Gate #7 „All provider categories/groups are reachable from Live TV and Guide“**.

### Befund (im realen Code verifiziert)
1. **Live-TV erreicht Kategorien bereits:** `MediaListViewModel.rows` gruppiert alle
   Provider-Kanäle per `groupBy { it.category ?: "Alle Kanäle" }` in `GridRow`s —
   die Kategorien selbst sind also real vorhanden und im Live-TV erreichbar.
2. **Der Guide hatte KEINEN Kategorien-Zugang:** `Key.G` zyklte nur durch die virtuellen
   Gruppen Alle Sender → Favoriten → Zuletzt gesehen. Eine Anbieter-Kategorie
   (M3U `group-title` / Xtream `category_name` / Stalker `tv_genre_name` → `Channel.category`)
   war im TV-Programm weder erreichbar noch sichtbar.
3. **Fake-Daten im Favoritenpfad (Masterplan-Regel #2):** `ZenSettings.epgFavs` startete
   mit `setOf(0, 2, 4, 6)` und `EpgViewModel.toggleFav` seedete bei leerer Liste erneut
   `{0,2,4,6}` — ein Favoriten-Toggle auf einem frischen Gerät schrieb ohne Nutzerwunsch
   Kanäle 0/2/4/6 als „Favoriten“ (♥ + Home-Reihe + Guide-Gruppe zeigten erfundene Daten).
   `HomeScreen` seedete denselben Fallback.

### Änderungen
- **`EpgData.kt` — neue pure, JVM-testbare Gruppen-Logik:**
  - `epgCategories(channels)`: distinct, nicht-leere `Channel.category`-Werte in
    Playlist-Reihenfolge (nur echte Providerdaten; blank/null wird übersprungen).
  - `epgCategoryVis(cat, channels)`: globale Guide-Indizes aller Kanäle der Kategorie
    (exakter Match, konsistent zu Live-TV).
  - `epgGroupCycle(categories)`: `all → favs → recent → cat:X → …`; Kategorie-Ids sind
    mit `cat:` präfixiert (Kollision mit virtuellen Gruppen unmöglich).
  - `epgNextGroup(state, categories, includeFavs, includeRecent)`: nächste Gruppe im
    Zyklus; leere virtuelle Gruppen werden übersprungen (Frischinstallation
    springt direkt in die Kategorien); eine aktive, inzwischen verschwundene Kategorie
    fällt auf `all` zurück statt den Zyklus zu hängen.
  - `epgGroupCategoryName(id)`: dekodiert `cat:X`→`X` (Roundtrip auch bei `cat:`-haltigen
    Kategorienamen).
- **`SettingsRepository.kt`:**
  - `ZenSettings.epgCategoryGroup: String?` + Key `epg_category`; `setEpgCategoryGroup(cat)`
    aktiviert exakt EINE Kategorie und löscht beide Gruppen-Flags in einem Edit.
  - `setEpgActiveGroup(mode)` löscht jetzt zusätzlich `epg_category` („nur EINE Gruppe“-
    Vertrag gilt für alle drei Arten); die Low-Level-Setter `setEpgFavsOnly`/
    `setEpgRecentOnly` löschen beim Aktivieren ebenfalls die Kategorie.
  - **Favoriten ehrlich:** `epgFavs`-Default und Flow-Fallback `emptySet()` statt
    Demo-Seed.
- **`EpgViewModel.kt`:** `currentVis()` mit Kategorie-Zweig (Favoriten ohne Einträge zeigen
  ehrlich leer statt „alle“); `cycleGroup()` zyklisch durch Alle → Favoriten → Zuletzt →
  jede Anbieter-Kategorie → Alle (synchrones `snapTo` wie gehabt, Toast nennt die
  Kategorie); `activeGroupLabel()` zeigt den Kategorienamen; `toggleFav()` ohne Seed.
- **`EpgScreen.kt`:** Empty-State-Untertitel für eine leere Kategorie (ehrlich statt
  „Anbieter hinzufügen“).
- **`HomeScreen.kt`:** Favoriten-Fallback-Seed entfernt (`settings.epgFavs` direkt) —
  die „Deine Favoriten“-Reihe zeigt bei leerem Set wie vorgesehen nichts.
- **`EpgDataTest.kt`:** +7 Tests (18 gesamt in der Klasse): `epgCategories` (distinct/
  Reihenfolge/blank-Skip), `epgCategoryVis` (exakte Indizes/Unbekannt), `epgGroupCycle`,
  `epgNextGroup` (voller Zyklus + Skip leerer Gruppen + Wrap + verschwundene Kategorie),
  `epgGroupCategoryName`-Roundtrip.

### Build & Tests
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (15 s, 45 Tasks executed)
```
- **41 JVM-Unit-Tests, 0 Failures/Errors:** `EpgDataTest` **18/18** (11 alte + 7 neue),
  `RecentWatchTest` 9/9, `FallbackPolicyTest` 4/4, `PlaybackStatusTest` 4/4,
  `PlayerDiagnosticsTest` 6/6. XMLs frisch gelesen (`--rerun-tasks`).

### Gate-Bezug
- **Gate #7 „All provider categories/groups are reachable from Live TV and Guide“** —
  Live-TV war bereits vollständig; der Guide erreicht jetzt über `G` jede echte
  Anbieter-Kategorie (M3U/Xtream/Stalker) mit ehrlicher Zählung „X von N“ im Spalten-Header.
- **Regel #2 (keine Fake-Daten):** Favoriten-Seeds aus Guide/Home entfernt; Favoriten
  verhalten sich ab jetzt daten-ehrlich (leerer Zustand statt erfundener ♥-Sender).

### Nicht verifiziert (kein Gerät/Stream)
- Echte D-Pad-Sitzung (G durch viele Kategorien), Guide-Darstellung einer echten
  Kategorie-Playlist und der neue Favoriten-Leerzustand auf TV-Hardware nicht getestet
  (`adb` nicht verfügbar). `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt weiterhin.
- Kategorien mit gleichem Namen verschiedener Anbieter werden im Guide zu EINER Gruppe
  zusammengeführt (exakter `category`-Match wie in Live-TV) — bewusst, alle Kanäle bleiben
  über „Alle Sender“ erreichbar.

### Bekannte Folgepunkte (bewusst NICHT in dieser Runde)
- Ein Kategorien-Picker/Rail (Liste statt G-Einzelzyklus) wäre für Playlists mit sehr vielen
  Kategorien komfortabler — Datenlogik (`epgGroupCycle`) ist dafür bereits vorbereitet.
- Auf Geräten, die den alten Favoriten-Seed bereits persistiert haben, bleiben die
  geschriebenen Einträge bis zum manuellen Entfernen bestehen (kein migratorisches Aufräumen
  in dieser Runde; niedrige Priorität).

## Core-TV Runde (Code-Change): Premium-Zap-Overlay — echte Kanalnummer + ehrliche Qualitäts-Badges (Gate #11)

`MODUS=core-tv`; `TO_DEEPSEEK.md` war weiterhin nur Platzhalter, `STATE.md` stand auf
`ACTIVE_AGENT=CLAUDE` (vorige Handoffs warten auf Review). Orchester-Auftrag war explizit
eine Implementierungs-Runde → als genau EIN geschlossenes Vorhaben wurde der letzte noch
nicht vollständig erfüllte Gate-Punkt angegangen:

**Gate #11 „Live player has premium zap overlay: logo, channel number, logical channel,
Now/Next, progress, useful badges“** — Logo, Senderinfo, JETZT/DANACH + Fortschritt waren
vorhanden; **die Kanalnummer und nützliche Badges fehlten**.

### Befund (im realen Code verifiziert)
1. **Kanalnummer fehlte im Zap-Overlay:** `PlayerTopBar` und `ChannelBanner` zeigten Logo,
   Name, Kategorie, LIVE-Chip, Uhr, Engine+Position — aber nie die **echte Kanalnummer**.
   Das Datenmodell hat sie (`Channel.number`, real befüllt: M3U Playlist-Zähler,
   Xtream `num`, Stalker `index+1`; DB sortiert `ORDER BY number, name`).
2. **„Useful badges“ waren Fake oder fehlten:** Der HomeScreen (KanalHome/FavCard) zeigte
   hartcodierte „HD“-Plaketten ohne Datenbasis, und der Player hatte gar kein
   Qualitäts-Badge. Erfundene Werte wären Regel-#2-Verstoß gewesen → der Qualitäts-Hint
   wird jetzt **ausschließlich aus echten Kanalname/URL-Daten** abgeleitet.
3. **Nebenbefund (Guide-Bedienung, Priorität 3):** Der Shortcut-Hinweis in der Guide-TopBar
   war falsch („◄ ► = Tag“ — Tagwechsel passiert tatsächlich per PageUp/PageDown, und
   „Pause = jetzt“ behauptete einen Shortcut, den es gar nicht gab). Die fertige
   `EpgViewModel.snapNow()`-Funktion („Springe zu jetzt“) war **ungebunden**.

### Änderungen
- **Neu `ui/player/ChannelQuality.kt`** (rein, JVM-testbar): `channelQualityHint(name, url)`
  erkennt **4K** (`4k|uhd|2160|ultra hd`), **FHD** (`fhd|1080`), **HD** (`hd|720`), **SD**
  (`sd|576|480`) aus Kanalname + Stream-URL; sonst `null` — unbekannte Qualität wird nie
  erfunden. Unterstriche werden normalisiert („stream_1080p“ in echten URLs wäre sonst
  durch `\b`-Wortgrenzen unsichtbar — im Test entdeckt).
- **`PlayerScreen.kt`:** Kanalnummer (`Channel.number > 0`, sonst Position im Zap-Stapel)
  nur bei LIVE; neue **Kanalnummer-Plakette** (accent, breite skaliert mit Ziffernzahl) in
  `PlayerTopBar` (58 dp) und `ChannelBanner` (52 dp); neues dezentes **`QualityBadge`**
  (white-glass Pill) neben dem LIVE-Chip — nur wenn der Hint ehrlich ableitbar ist.
  VOD/Music: kein Badge, kein Nummernzettel.
- **`EpgScreen.kt` (Nebenbefund):** `snapNow()` an `MediaPlayPause`/`MediaPlay` gebunden
  („Springe zu jetzt“ war im Masterplan-Schichtenset vorgesehen, nur unerreichbar);
  Hinweis korrigiert auf die echten Tasten: „Pause = jetzt · CH ▲ ▼ = Tag · G = Gruppe ·
  C = Menü“.
- **Neu `ChannelQualityTest.kt` (10 JVM-Tests):** Kürzel pro Klasse, 4K vorrangig vor HD
  („Ultra HD“/„4K Ultra HD“ → 4K), URL-Varianten inkl. `2160p`/`1080p`/`720`, Wortgrenzen
  („Derbys HDKabel“/„HDMI“ → null), Normalisierung (case/Leerzeichen), Einzelwert-Pflicht
  bei proprietären Namen („NDR“ → **null**, nie erfunden), widersprüchliche „4K SD“ → 4K.

### Build & Tests
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL (16 s, 45 Tasks executed)
```
- **51 JVM-Unit-Tests, 0 Failures/Errors:** `ChannelQualityTest` **10/10** (neu),
  `EpgDataTest` 18/18, `RecentWatchTest` 9/9, `FallbackPolicyTest` 4/4,
  `PlaybackStatusTest` 4/4, `PlayerDiagnosticsTest` 6/6. XMLs frisch gelesen
  (`--rerun-tasks`). Ein erster Testlauf deckte den `_`-Wortgrenzen-Bug auf
  (`stream_1080p`) und wurde durch die `_`-Normalisierung behoben — der Test blieb als
  Regressionsschutz stehen.

### Gate-Bezug
- **Gate #11 „premium zap overlay“** — jetzt vollständig: Logo, **echte Kanalnummer**,
  logischer Kanalname, JETZT/DANACH mit Zeiten, **Live-Fortschritt**, nützliche
  **Qualitäts-Badges** (ehrlich aus Anbieterdaten), Engine/Positions-Info, Uhr. Kein
  erfundenes Catch-up-/Empfehlungs-Badge (Shared Catchup ist Phase B; erst dann ehrlich).

### Nicht verifiziert (kein Gerät/Stream)
- Optik/Anordnung von Kanalnummer + Badges auf echter TV-Hardware nicht getestet
  (`adb` nicht verfügbar). `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt weiterhin.
- Reale Verteilung der Qualitäts-Kürzel (wie viele Playlists liefern tatsächlich „FHD“/
  „HD“ im Kanalnamen bzw. Auflösungen in der URL) nicht gemessen — die Funktion ist
  konservativ (unbekannt → kein Badge).

### Bekannte Folgepunkte (bewusst NICHT in dieser Runde)
- HomeScreen-Karten zeigen weiterhin hartcodierte „HD“-Plaketten (KanalHome/FavCard) statt
  `channelQualityHint` — Folgepunkt, hängt an der Home→Repository-Bindung.
- „Replay/Catch-up“-Badge erst mit Phase B (Shared Catchup) möglich; bis dahin kein Badge.
- Home „Jetzt LIVE“-Kacheln (feste Indizes 16–19) und Hero/„Empfohlen“-Reihe sind weiterhin
  Design-Platzhalter ohne echte Datenbasis (Regel-#2-Risiko, bewusst zurückgestellt).

---

## Recovery-Runde (Claude-Ausfall rc=1): Review-Verifikation statt neuem Feature

`Claude` ist in seiner Review-Runde mit `rc=1` ausgefallen (Session-Limit, Log
`claude-resume.log`). Der Marathon hat DeepSeek als Fallback eingeschaltet. Übernommen
wurde **ausschließlich die angefangene Aufgabe** (Review der Core-TV-Runden gemäß
`TO_CLAUDE.md` NEXT_ACTION). **Kein neues Feature.**

### Verifikation (an Stelle des ausgefallenen Claude-Reviews, im Code belegt)
- Branch `claude/charming-fermi-jhbd3i`, Working Tree vor dieser Runde sauber; alle
  Core-TV-Runden sind in `99cfb6e` committet (inkl. Gate-#11-Material
  `ChannelQuality.kt`, `ChannelQualityTest.kt`, `PlayerScreen.kt`, `EpgScreen.kt`,
  `EpgViewModel.kt`).
- Handoff-Behauptungen gegen den echten Quellcode abgeglichen:
  - `channelQualityHint(name, url)` (pure, JVM-testbar) — 4K/FHD/HD/SD nur aus echten
    Kanalname/URL, sonst `null`, `_`-Normalisierung, 4K-vor-HD-Reihenfolge — **deckungsgleich**.
  - `PlayerScreen.kt`: `channelNumber = Channel.number > 0 ?: Zap-Index+1` nur bei LIVE;
    `ChannelNumberBadge` in `PlayerTopBar` (58 dp) und `ChannelBanner` (52 dp);
    `QualityBadge` neben `LiveChip` nur bei ableitbarem Hint; VOD/Music ohne Plakette.
  - `EpgScreen.kt`: `MediaPlayPause`/`MediaPlay` → `vm.snapNow()`; Hinweis „Pause = jetzt ·
    CH ▲ ▼ = Tag · G = Gruppe · C = Menü“ korrekt gesetzt; Now-Marker (`nowLineX`) und
    JETZT-Zellen-Hervorhebung vorhanden.
  - Kein Rest von Mock-/Demo-Daten: `MAX_EPG_CHANNELS` nur noch im Kommentar, kein
    `mockProgramsFor`, kein Favoriten-Seed `{0,2,4,6}` mehr.
- **Build/Tests frisch ausgeführt** (Windows-JVM, offline, `--rerun-tasks`):
  ```
  JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
  BUILD SUCCESSFUL in 1m 23s, 45 Tasks executed
  ```
  **51 JVM-Unit-Tests, 0 Failures/Errors** (XMLs frisch gelesen, Zeitstempel der Läufe):
  `ChannelQualityTest` 10/10, `EpgDataTest` 18/18, `RecentWatchTest` 9/9,
  `FallbackPolicyTest` 4/4, `PlaybackStatusTest` 4/4, `PlayerDiagnosticsTest` 6/6.
- Core-TV-Gate-Checkliste einmal komplett gegen den committeten Stand geprüft: alle
  Gate-Punkte sind im Code abgebildet (echte EPG-Daten, Now-Marker, Grid-Virtualisierung +
  Focus-Follow, Anbieter-Kategorien und Alle/Favoriten/Zuletzt-gesehen im Guide, keine
  Silent-Truncation, Zap-Overlay komplett, deterministischer Einmal-Fallback).

### Einziger halbfertiger Befund (behoben, Haushalt statt Feature)
- Commit `d785816` („ignore marathon runtime files“) ignorierte `.ai-collab/marathon-logs/`
  und `MARATHON_RUNNING`, **aber nicht** `/zenith-marathon.out` — das Root-Live-Tee-Ziel
  des Marathons. Dadurch sah `ai-marathon.sh` bei **jeder** Runde einen „Dirty Tree“ und
  lief in den Recovery-Modus, und `git add -A` hätte die wachsende Konsolenmitschrift in
  Checkpoints gespült.
- Fix: `.gitignore` um `/zenith-marathon.out` ergänzt (eine Zeile, nicht-destruktiv).

### Bewusst NICHT getan
- **Kein `CORE_TV_ACCEPTED`** in `.ai-collab/CORE_STATUS.md` geschrieben: der
  Gate-Accept ist laut Masterplan Claude-Verdikt („When Claude independently verifies“).
  Meine Verifikation ist als Review-Input dokumentiert; Claude trifft die Entscheidung.
- Kein neues Feature, kein Reset/Push, keine destruktive Git-Aktion.

### Noch offen (unverändert)
- Kein Gerät/`adb`: echte Wiedergabe, D-Pad-/Zap-Sitzung und Badge-Optik auf TV-Hardware
  weiterhin nicht verifiziert. `BUILD SUCCESSFUL != echte Wiedergabe verifiziert`.
- Bekannte Folgepunkte aus dem Gate-#11-Handoff (HomeScreen-Hardcode-„HD“-Plaketten,
  Home→Repository-Bindung, „Jetzt LIVE“-/Hero-Platzhalter) bleiben bewusst zurückgestellt.

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
