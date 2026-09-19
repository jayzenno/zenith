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

- User-Agent hat noch kein Settings-UI-Feld (bewusst zurückgestellt).
- VOD-/Live-Fortschritt basiert auf Polling (500 ms); bei sehr langen VODs ggf. anpassen.
- Nur der Player ist an die Designsprache angepasst; übrige Screens unverändert.
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
