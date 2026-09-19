# AI Collaboration Changelog

## 2026-09-19 — ⚠ Parallele Implementer-Session im selben Working Tree (Home←EpgRepository-Bindung)
- Eine zweite `opencode run --agent implementer`-Session (PID 22402, 14:52) schrieb während
  dieser Runde zusätzliche uncommittete Dateien: `Daos.kt` (`observeForWindow`), `EpgRepository.kt`
  (`programsForWindowFlow`), `HomeData.kt` (`homeNowPrograms`) — bekannte Folgepunkt-Bindung.
- Diese Dateien kamen NACH dem frischen Build (15:00) — nicht compiliert/getestet; HomeScreen
  ist noch nicht umgehängt (Mitte der Arbeit). Keine von mir erzeugten Dateien wurden überschrieben.
- Beide Sessions laufen noch; der Gesamt-Working-Tree (12 uncommittete Dateien) gehört gemeinsam
  in den nächsten Claude-Review. Keine destruktive Aktion; kein Commit/Push/Reset.

## 2026-09-19 — Core-TV: EPG-„Now“-Konsumenten auf Slot-Basis + effektiver Anzeige-Tag vor 05:00
- Befund: Die Slot-Erzeugung war seit Runde 4/6 korrekt, aber die „Now“-Konsumenten
  verglichen 00:00–04:59 (Wanduhr-Minuten 0–299) roh gegen Slots ≥ 300 → laufendes
  Morgenprogramm lag im Grid-Tail (1440–1739), „JETZT“ zeigte aber das erste 05:00-Programm,
  die Now-Linie klemmte links, `day()` klammerte auf 0 → das Fenster mit dem aktuellen
  Moment (Vortages-05:00–05:00) war morgens unerreichbar.
- Fix: pure `wallClockToSlot(min)` + `effectiveDay(pref, now)`; `epgProgAt` auf Slot-Basis
  (alle Aufrufer inkl. `nowProg`/Home), `nowLineX`/`isNow` auf Slot-Basis, `day()` effektiv,
  `setDayStep` pref-basiert, Init-Anker auf `day()`; Home 4× mit `effectiveDay(0, nowMin())`.
- Neue JVM-Tests (EpgDataTest 22 → 26): Tail-Mapping, Tag-Shift, laufendes Nachtprogramm
  statt Index 0, Tageszeit-Auswahl im Misch-Tag.
- Build frisch: `BUILD SUCCESSFUL in 37 s` (45 Tasks, offline, `--rerun-tasks`), **69
  JVM-Tests, 0 Failures/Errors** (XMLs 15:00). `git diff --check` sauber.
- Kein Commit/Push/Reset; `CORE_TV_ACCEPTED` bleibt Claude-Verdikt.

## 2026-09-19 — Recovery-Runde 6: laufende EPG-Zeitbasis DST-korrekt abgeschlossen
- Ausschließlich den angefangenen EPG-Zeitbasis-/Cache-Diff übernommen und dessen
  DST-Inkonsistenz repariert: lokale 05:00–05:00-Fenster statt `+24h`, lokale
  Wanduhr-Slots statt Millisekunden-Differenzen.
- Room-Abfragen und Grid-Mapping teilen damit dieselbe Grenze; neuer JVM-Test für den
  nächsten europäischen Frühjahrs-DST-Wechsel (04:30 -> Slot 1710).
- `git diff --check` sauber. Frischer Gradle-Lauf hostseitig vor Gradle blockiert
  (`UtilBindVsockAnyPort`); vorhandene 64 grüne Tests sind Vorzustand, nicht neu behauptet.

## 2026-09-19 — Implementer-Fallback #5: Codex-Build-Blocker final aufgelöst, Runde-4-Stand erneut frisch verifiziert
- Claude weiter ausgefallen; übernommen wurde ausschließlich die laufende Runde-4-Aufgabe
  (EPG-5h-Zeitbasis-Fix + Codex-Cache-Invalidierung). Kein neues Feature, kein Quellcode-Change.
- Der im Codex-Handoff erneut gemeldete WSL-Interop-Blocker (`UtilBindVsockAnyPort`) war
  wieder transient: voller Offline-Build exakt wie dokumentiert jetzt erfolgreich:
  `BUILD SUCCESSFUL in 25 s, 45 Tasks executed` (14:37).
- **64 JVM-Unit-Tests, 0 Failures/Errors** (frische XMLs 14:37): EpgDataTest 21/21 (3
  Wanduhr-Basis-Tests + Codex-Cache-Regressionstest), HomeData 10, ChannelQuality 10,
  RecentWatch 9, FallbackPolicy 4, PlaybackStatus 4, PlayerDiagnostics 6.
- Code-Ebene erneut geprüft: `updateChannels` (Cache-Clear) läuft vor `refreshPrograms`
  (Room-Neuaufbau), `mapDbPrograms` in konsistenter Wanduhr-Basis, `git diff --check` sauber.
- Kein Gerät/`adb`: reale Wiedergabe/D-Pad weiterhin unverifiziert. Kein Commit/Push/Reset;
  `CORE_TV_ACCEPTED` bleibt Claude-Verdikt.

## 2026-09-19 — Codex-Fallback: angefangene EPG-Runde geprüft, kein neuer Code
- Ausschließlich den uncommitteten EPG-5-h-Zeitbasis-Fix und seinen Testdiff geprüft;
  Wanduhr-Slotbasis ist mit Guide-/Home-Konsumenten konsistent, `git diff --check` sauber.
- Voller Offline-Build erneut versucht, aber vor Gradle durch den bekannten transienten
  WSL-Interop-Fehler `UtilBindVsockAnyPort:309: socket failed 1` blockiert. Kein Linux-JDK
  oder Workspace-Override vorhanden; keine Host-Änderung vorgenommen.
- Bestehende frische XML-Berichte von 14:32 bestätigen weiterhin 64 JVM-Tests ohne Fehler.
  Keine reale Geräte-/Stream-Verifikation, kein Feature, kein Commit/Push/Reset.

## 2026-09-19 — Implementer-Fallback-Verifikationslauf: WSL-Build-Blocker aufgelöst, Runde 4 unabhängig bestätigt
- Claude-Runde ausgefallen; übernommen wurde nur die angefangene Aufgabe (Runde-4-EPG-Fix +
  Codex-Cache-Reparatur verifizieren, blockierten Build ausführen). Kein neues Feature.
- WSL-Interop wieder funktionsfähig (`cmd.exe`/`java.exe` Exit 0) — der gemeldete
  `UtilBindVsockAnyPort`-Blocker war transient. Voller Build frisch ausgeführt:
  `BUILD SUCCESSFUL in 44 s` (45 Tasks, offline, `--rerun-tasks`); **64 JVM-Tests,
  0 Failures/Errors** (XMLs 14:32, EpgDataTest 21).
- Unabhängig verifiziert: Wanduhr-Zeitbasis stimmt mit allen Slot-Konsumenten überein
  (`ProgramRow`, TimeHeader, nowLineX, `epgProgAt`/`nowProg`, `epgScrollTargetX`, Home);
  Player-Overlay nutzt echte Epoch-Millis und ist unberührt. Codex-Cache-Invalidierung
  bestätigt. Kein Commit/Push/Reset; `CORE_TV_ACCEPTED` bleibt Claude-Verdikt.

## 2026-09-19 — DeepSeek-Runde 4: EPG-5h-Offset im Guide/Home repariert (Core-TV-Gate „Current time is correct“)
- Befund: `mapDbPrograms()` erzeugte Slot-Minuten relativ zum 05:00-Fenster; alle Renderer &
  die Auswahl (`nowMin()`, Header `(h+5)%24`, `nowLineX`, `hh()`, `epgScrollTargetX` und
  Home-JETZT/Hero) rechnen in Wanduhr-Minuten seit Mitternacht → jedes Programm erschien 5 h
  zu früh, „Jetzt“ zeigte das 4–5 h spätere Programm. Der Player-Overlay (echte Epoch-Millis)
  war korrekt und stimmte mit dem Guide nicht überein.
- Fix: `+ EPG_START_MIN` in `mapDbPrograms` (05:00→300, 12:00→720); Tests: Noon-Fixture
  korrigiert (war real 17:00), Grid/Now-Line-Basis-Test, `nowProg`-Auswahl-Test.
- Build frisch: `BUILD SUCCESSFUL in 36 s` (45 Tasks, offline, `--rerun-tasks`), **64
  JVM-Tests, 0 Failures/Errors** (EpgDataTest 18→21). WSL-Interop funktionierte diesmal.
- Kein Commit/Push/Reset; kein `CORE_TV_ACCEPTED` (Claude-Verdikt); Codex-Fallback-Reparatur
  (EpgStore-Cache-Invalidierung) blieb im Working Tree erhalten.

## 2026-09-19 — Codex-Fallback: EPG-Cache-Konsistenz im laufenden Core-TV-Review repariert
- Ausschließlich die angefangene Claude-Review übernommen, kein neues Feature.
- `EpgStore.updateChannels()` löscht nun Programme, die nach transientem Grid-Index
  geschlüsselt sind, vor einer Kanal-Neuordnung; verhindert echte EPG-Zuordnungen zum falschen
  Sender nach einem Provider-Sync.
- Regressionstest für Re-Sync-Invaliderung ergänzt; Diff-/Commit-Whitespace geprüft.
- Frischer Build blockiert durch WSL-Interop (`UtilBindVsockAnyPort` vor Gradle) und fehlendes
  Linux-JDK; kein Erfolg behauptet. Keine destruktive Git-Aktion, kein Commit/Push.

## 2026-09-19 — DeepSeek Zweit-Fallback-Lauf (Claude rc=1): unabhängige Bestätigung des Checkpoints `6a5fe63`
- Die angefangene Aufgabe (Home-Runde + Rebase-Reparatur) war bereits committet; dieser Lauf
  verifizierte den committeten Stand unabhängig (kein neues Feature).
- Git-Zustand konsolidiert (Worktree sauber, HEAD `6a5fe63`, kein Rebase/keine Marker mehr).
- API-/Signatur-Abgleich aller Home-Referenzen gegen echten Code, `playRoute` ↔
  `ZenNavHost`-Route zeichengleich, Fake-Daten-Grep (Regel #2) 0 Treffer.
- Build/Tests frisch: `BUILD SUCCESSFUL` (38 s, 45 Tasks, offline, `--rerun-tasks`), 61
  JVM-Unit-Tests, 0 Failures/Errors (XMLs 14:12 frisch gelesen).
- `DEEPSEEK_RESULT.md` additiv ergänzt; kein `CORE_TV_ACCEPTED` (Claude-Verdikt), kein
  Commit/Push/Reset; `STATE.md` bleibt `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Recovery-Runde #2 (Claude rc=1 erneut ausgefallen): Rebase-Reparatur + Review-Verifikation der Home-Runde
- Claude fiel erneut in seiner Review-Runde aus (Session-Limit). Übernommen wurde
  ausschließlich die angefangene Aufgabe (Review des Working Trees + angetroffener
  halbfertiger Git-Zustand); **kein neues Feature**.
- **Halbfertiger Rebase repariert:** `0cf9bf3..2bedcba` hing an einem `.gitignore`-Konflikt;
  ein externes `git commit --amend` hatte die Konfliktmarker (`<<<<<<<`/`>>>>>>>`) IN den
  Commit committet. `.gitignore` aufgelöst (Kommentar + `/zenith-marathon.out`), Commit
  amendiert (Checkpoint-Message), Rebase über `git rebase --quit` + `git branch -f` sauber
  finalisiert → Branch auf `0d8f1d0` (Parent `0cf9bf3`, exakt Rebase-Ziel). Keine
  destruktive Aktion; Working Tree (alle uncommitteten Home-Änderungen) nie angetastet.
- **Review-Verifikation der Home-Runde an Stelle des ausgefallenen Claude-Reviews**
  (Code-belegt): `allLiveChannels()`-Binding, `playRoute` deckungsgleich mit `ZenNavHost`,
  `homeHeroIndex` pure korrekt, alle referenzierten Symbole vorhanden; Grep-Restprüfung
  aller ehemaligen Fake-Inhalte → 0 Treffer; `LiveChannelCard` ohne Fake-Progress/HD.
- **Build/Tests frisch:**
  `JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks`
  → `BUILD SUCCESSFUL` (41 s, 45 Tasks), **61 JVM-Unit-Tests, 0 Failures/Errors**
  (HomeData 10, ChannelQuality 10, EpgData 18, RecentWatch 9, FallbackPolicy 4,
  PlaybackStatus 4, PlayerDiagnostics 6; XMLs frisch gelesen).
- Kein `CORE_TV_ACCEPTED` geschrieben (Claude-Verdikt), kein Push/Commit der Quellcode-Arbeit
  (bleibt uncommittet für Claude-Review), kein Gerät/`adb` → echte Wiedergabe weiterhin
  nicht verifiziert. `STATE.md` → `ACTIVE_AGENT=CLAUDE`.

## 2026-09-19 — DeepSeek Core-TV-Runde (Code-Change: Home daten-ehrlich + Home→Repository-Bindung, Regel #2)
- `MODUS=core-tv`, `TO_DEEPSEEK.md` weiterhin nur Platzhalter → höchster verbleibender
  Rückstand aus dem Gate-#11-Handoff umgesetzt: **HomeScreen zeigte die letzten erfundenen
  Produktionsinhalte** — das ist jetzt beseitigt (Regel #2).
- **Fake raus:** Hero-Banner erfand einen Film („Action Now“, „Ein Undercover-Ermittler…“,
  „Sender 17 · Sky“, „Spielfilm“); Reihe „Empfohlen für dich“ zeigte erfundene VOD-Titel
  („Tatort“, „Top Gun: Maverick“, „Bundesliga: Topspiel“, …) mit leerem onClick;
  KanalHome/FavCard + `LiveChannelCard` trugen Hardcode-„HD“-Plaketten; `LiveChannelCard`
  zusätzlich eine aus `channel.id.hashCode()` erfundene Fortschrittsleiste. Alles entfernt.
- **Repository-Bindung:** Home sammelt `container.channels.allLiveChannels()` direkt (statt
  des Guide-Globalstores, der ohne Guide-Besuch leer blieb); DAO deterministisch
  (`ORDER BY number, name`) → Favoriten-/Recency-Indexraum identisch zum Guide; Display über
  dieselbe pure `mapDbChannel` (echte Kanalnummer > sonst Position+1, Initialen, Name-Art).
- **Hero:** echter Sender via purer `homeHeroIndex` (zuletzt gesehen → 1. Favorit → 1.
  Sender) + echtes Programm + ehrliches Qualitäts-Badge; ohne Quellen ehrlicher
  Onboarding-`EmptyState` („Einstellungen öffnen“) statt erfundener Plakate. Alle Home-Karten
  spielen jetzt wirklich (`playRoute` → Player; vorher `onClick = {}`).
- **Ehrliche Qualität:** `channelQualityHint(name, url)` als Badge — nur bei ableitbarem Wert
  (4K/FHD/HD/SD); Sender ohne Hinweis bekommen keine Plakette.
- Neu `ui/home/HomeData.kt` (pure) + `HomeDataTest` +10 Tests; **61 JVM-Unit-Tests grün**
  (HomeData 10, ChannelQuality 10, EpgData 18, RecentWatch 9, FallbackPolicy 4,
  PlaybackStatus 4, PlayerDiagnostics 6), `BUILD SUCCESSFUL` (52 s, 45 Tasks, offline,
  `--rerun-tasks` real).
- Grep-Restprüfung: keine `Text("HD")`, keine fabrizierten Titel, kein `rememberProgress`,
  kein `listOf(16,17,18,19)` → 0 Treffer. `CORE_TV_ACCEPTED` bewusst **nicht** geschrieben
  (Claude-Verdikt laut Masterplan).
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert; `STATE.md` bleibt `ACTIVE_AGENT=CLAUDE`
  (Claude: unabhängiger Review + Gate-Entscheid; Working Tree uncommittet: 2 geänderte,
  2 neue Dateien).

## 2026-09-19 — DeepSeek Recovery-Runde (Claude rc=1 ausgefallen): Review-Verifikation statt neuem Feature
- Claude fiel in seiner Review-Runde aus (Session-Limit, `claude-resume.log`); Marathon
  schaltete DeepSeek als Fallback ein. Übernommen wurde ausschließlich die angefangene
  **Review-Aufgabe** (Gate-#11-Handoff verifizieren) — kein neues Feature, kein Reset/Push.
- Review-Verifikation gegen den committeten Stand (`99cfb6e`): `ChannelQuality.kt`
  deckungsgleich (nur echte Kanalname/URL-Daten, `_`-Normalisierung, 4K vor HD),
  `PlayerScreen.kt` (echte Kanalnummer + `QualityBadge` nur bei ableitbarem Wert, VOD/Music
  ohne Plakette), `EpgScreen.kt` (`MediaPlayPause`/`MediaPlay` → `snapNow()`, korrigierter
  Tasten-Hinweis), Now-Marker vorhanden; keine Mock-/Demo-Reste
  (`mockProgramsFor`/`MAX_EPG_CHANNELS`/Favoriten-Seed weg).
- Build/Tests frisch: `BUILD SUCCESSFUL` (1 m 23 s, 45 Tasks, offline, `--rerun-tasks`),
  **51 JVM-Unit-Tests, 0 Failures/Errors** (ChannelQuality 10, EpgData 18, RecentWatch 9,
  FallbackPolicy 4, PlaybackStatus 4, PlayerDiagnostics 6).
- Static-Fix: `/zenith-marathon.out` (Live-Tee-Ziel des Marathons, Root) fehlte in
  `.gitignore` → Marathon lief bei jeder Runde in „Dirty Tree → Recovery-Modus“ und hätte
  die Mitschrift per `git add -A` eingecheckt. Eine Zeile ergänzt.
- `CORE_TV_ACCEPTED` bewusst **nicht** geschrieben (Claude-Verdikt laut Masterplan);
  Gate-Checkliste auf Code-Ebene als vollständig bedient dokumentiert.
- `DEEPSEEK_RESULT.md`/`TO_CLAUDE.md` aktualisiert; `STATE.md` bleibt
  `ACTIVE_AGENT=CLAUDE` (als Nächstes Claude: unabhängiger Review + Gate-Entscheid).

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
