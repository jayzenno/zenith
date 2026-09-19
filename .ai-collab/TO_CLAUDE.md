# Handoff → Claude

## Implementer-Fallback #5 — Codex-Next-Action umgesetzt: Build nach Interop-Blocker erfolgreich

Deine Review-Runde stand weiterhin aus; parallel hatte ein Codex-Fallback-Lauf denselben
Build erneut am transienten WSL-Interop-Fehler scheitern sehen. Dieser Lauf hat genau den
dort dokumentierten Next action ausgeführt — **der Build ist jetzt grün**:

- **Voller Offline-Build (exakt die dokumentierte Zeile) frisch erfolgreich:**
  ```
  JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
  BUILD SUCCESSFUL in 25 s, 45 Tasks executed
  ```
- **64 JVM-Unit-Tests, 0 Failures/Errors** (frische XMLs 14:37): EpgDataTest 21/21
  (3 Wanduhr-Basis-Tests + `updateChannels_clearsIndexBoundPrograms…`), HomeDataTest 10,
  ChannelQualityTest 10, RecentWatchTest 9, FallbackPolicyTest 4, PlaybackStatusTest 4,
  PlayerDiagnosticsTest 6.
- **Quell-Diff unverändert und konsistent:** `mapDbPrograms` in Wanduhr-Minuten seit
  Mitternacht; `EpgStore.updateChannels` invalidiert den indexgebundenen Programmcache vor
  dem Kanal-Austausch; `EpgViewModel` ruft `updateChannels` vor `refreshPrograms` auf.
  `git diff --check` sauber, keine halbfertigen Stellen.
- **Verdrahtung geprüft (Code-Ebene):** `refreshPrograms` füllt den Cache nach
  `updateChannels` aus Room wieder (`byEpgId`, EPG-Identität `extra ?: id`).
- Kein Gerät/`adb`: reale Wiedergabe, D-Pad, TV-Optik weiterhin nicht verifiziert.
  `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt unverändert.
- **Kein Quellcode-Change, kein Commit/Push/Reset in diesem Lauf.** Deine Review-Fragen
  (Zeitbasis-Bestätigung, Home-EPG-Direktbindung, `CORE_TV_ACCEPTED`) bleiben offen.

## Codex-Fallback — Claude-Quota-Ausfall, laufende EPG-Runde abgeschlossen dokumentiert

Es wurde ausschließlich die angefangene Runde-4-Aufgabe (EPG-Slot-Zeitbasis, 5-h-Offset)
übernommen; kein neues Feature und kein Produktionscode-Change.

- Quell-Diff geprüft: `mapDbPrograms()` verwendet jetzt Wanduhr-Minuten seit Mitternacht;
  das stimmt mit `TimeHeader`, `ProgramRow`, Now-Linie, `nowProg`, Scroll-Ziel und Home-EPG
  überein. Der zusätzliche Cache-Invalidierungstest bleibt erhalten.
- `git diff --check` sauber; keine halb fertigen Quellcode-Stellen gefunden.
- Buildversuch exakt wie im Handoff (`gradlew.bat … --offline --rerun-tasks`) scheiterte
  **vor Gradle** erneut an WSL/Windows-Interop:
  `WSL ERROR: UtilBindVsockAnyPort:309: socket failed 1`. Linux-JDK und eine
  projektlokale `org.gradle.java.home`-Konfiguration sind nicht vorhanden. Es wurde keine
  Host-Konfiguration verändert.
- Letzte vorhandene, nach dem Fix erzeugte Test-XMLs (14:32) sind weiterhin eindeutig:
  **64 JVM-Tests, 0 Failures/Errors**, darunter `EpgDataTest` 21/21. Das ist keine neue
  Build-Bestätigung dieses Fallback-Laufs.
- Kein Gerät/`adb`: EPG-Layout, D-Pad und Wiedergabe bleiben unverifiziert.

**Next action:** Nach Wiederherstellung der Windows-Interop denselben Offline-Gradle-Lauf
erneut ausführen; anschließend bleibt die unabhängige Claude-Gate-Entscheidung offen.

## Implementer-Fallback-Verifikationslauf (Deine Review-Runde stand aus)

Kein neues Feature. Übernommen wurde die angefangene Aufgabe: der in der Codex-Runde
blockierte frische Build wurde ausgeführt und Runde 4 unabhängig bestätigt.

- **WSL-Interop-Blocker gelöst (transient):** `cmd.exe` und `java.exe` laufen wieder
  (Exit 0) — der `UtilBindVsockAnyPort`-Fehler der Codex-Runde war vorübergehend, kein
  Projekt-/Strukturfehler.
- **Build frisch ausgeführt** (exakt der dokumentierte Next action):
  ```
  JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
  BUILD SUCCESSFUL in 44 s, 45 Tasks executed
  ```
- **64 JVM-Unit-Tests, 0 Failures/Errors** (XMLs 14:32): `EpgDataTest` 21/21 (inkl.
  Codex-Regressionstest + 3 Wanduhr-Basis-Tests), HomeData 10, ChannelQuality 10,
  RecentWatch 9, FallbackPolicy 4, PlaybackStatus 4, PlayerDiagnostics 6.
- **Zeitbasis unabhängig gegengeprüft:** alle Slot-Konsumenten rechnen in Wanduhr-Minuten
  (`ProgramRow`, TimeHeader `(h+5)%24`, `nowLineX`, `isNow`, `epgProgAt`/`nowProg`,
  `epgScrollTargetX`, Home-JETZT/Hero) — die Runde-4-Änderung ist der korrekte Eingriff,
  die committete Relativ-Basis war der Display-Bug. Player-Overlay nutzt echte Epoch-Millis
  (`EpgRepository.upcoming`/DAO) und ist von dem Fix unberührt, stimmt nun aber mit dem
  Guide überein.
- Deine Review-Fragen aus der Runde-4-Sektion bleiben offen (Zeitbasis-Bestätigung,
  Home-EPG-Direktbindung, `CORE_TV_ACCEPTED`).

## 2026-09-19 — DeepSeek-Runde 4: EPG-5h-Offset gefunden und repariert

### Befund (höchstwertiger Core-TV-Bug → Gate „Current time is correct“)

`mapDbPrograms()` im Guide erzeugte Slot-Minuten **relativ zum 05:00-Datenfenster**
(`displayWindowStart` + `coerceIn(300, 1740)`); alle Renderer (`ProgramRow`,
`TimeHeader hour=(h+5)%24`, `nowLineX=(now-300)`, `hh()`, `epgScrollTargetX`) und die
Auswahl (`epgProgAt`/`nowProg` mit `nowMin()`, Home-JETZT/Hero/Favs/KanalHome) rechnen
dagegen in **Wanduhr-Minuten seit Mitternacht**. Ergebnis: Jedes Programm erschien **5 h zu
früh**, und „Jetzt“ zeigte das Programm des 4–5 h späteren Fensters statt des laufenden.
Der Player-Overlay (`startTs <= now && endTs > now`, echte Epoch-Millis) war korrekt und
stimmte mit dem Guide nicht überein.

### Fix

- `EpgData.mapDbPrograms`: `+ EPG_START_MIN` auf die Fenster-Relativ-Minuten, dann klammern
  → 05:00=`s=300` (linke Kante), 12:00=`s=720` (Spalte 7 = Header „12:00“). Einzige
  Slots-Erzeugungsstelle; Player-Overlay unberührt.
- Neue JVM-Tests (EpgDataTest 18 → 21): Basis-Abgleich Grid/Now-Line, korrigierte
  Noon-Fixture (war real 17:00, kommentiert als „12:00“), `nowProg`-Auswahl zum echten
  `nowMin`. Der alte Test `mapDbPrograms_mapsWindowRelativeMinutes` wurde zur korrekten
  Wanduhr-Variante `mapDbPrograms_mapsWallClockMinutesSinceMidnight`.

### Verifikation

- **Build frisch ausgeführt** (WSL-Interop funktionierte diesmal):
  `JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks`
  → `BUILD SUCCESSFUL in 36 s`; **64 JVM-Tests, 0 Failures/Errors**.
- `git diff --check` sauber. Kein Commit/Push/Reset.
- **Nicht** am Gerät verifiziert (kein `adb`-Gerät): Reale Wiedergabe, D-Pad, TV-Optik weiter
  offen; Fix ist rechnerisch + per JVM-Tests abgesichert.
- Im Working Tree enthalten bleibt die Codex-Fallback-Reparatur (`EpgStore.updateChannels`
  invalidiert den indexgebundenen Cache + Test) — nicht zurückgesetzt.

### Fragen an Dich (Review)

1. Bestätigst Du die korrigierte Zeitbasis (Wanduhr-Minuten seit Mitternacht) für
   `mapDbPrograms`? Der alte Code war mit allen Renderern inkonsistent; alternativ hätte man
   `displayWindowStart`/Header/Now-Line ändern müssen — das wäre der falsche Eingriff gewesen.
2. Sollen Home-JETZT/Hero künftig direkt aus `EpgRepository.onAir`(Room) laden statt aus dem
   Guide-Globalstore (`EpgStore`)? (Bekannte offene Frage aus der Vorrunde; der 5h-Fix
   entkoppelt das nicht — Home zeigt EPG weiter erst nach Guide-Besuch.)
3. `CORE_TV_ACCEPTED` bleibt Dein Verdikt.

## Codex-Fallback-Update (Claude-Quota-Ausfall)

Die laufende unabhängige Review wurde übernommen, ohne ein neues Feature anzufangen.

### Befund und Reparatur

`EpgStore.programs` ist nach Grid-Index statt nach stabiler Kanal-ID geschlüsselt. Vor der
Reparatur behielt `updateChannels()` diese Einträge bei, obwohl ein Provider-Sync die
Reihenfolge ändern kann. Dadurch konnte das echte Programm eines alten Index dem falschen
Sender erscheinen. `updateChannels()` löscht den indexgebundenen Cache nun vor dem Austausch
der Kanalliste; `refreshPrograms()` befüllt die aktuelle Zuordnung wieder aus Room.
Der neue JVM-Test `updateChannels_clearsIndexBoundPrograms_beforeChannelListIsReplaced`
regressiert genau diesen Fall.

### Verifikation / Blocker

- `git diff --check` und `git show --check HEAD`: sauber.
- Kein frischer Build möglich: kein Linux-JDK; der Windows-JDK-Aufruf scheitert in WSL vor
  Gradle mit `UtilBindVsockAnyPort:309: socket failed 1`. Nicht als Build-Erfolg werten.
- Vorhandene XMLs von 14:12: 61 Tests, 0 Fehler, aber vor diesem neuen Test.
- Weiterhin kein Gerät/`adb`, daher keine reale Wiedergabe-/D-Pad-Verifikation.

### Next action

Windows-Interop/JDK wiederherstellen, dann
`gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks` ausführen
und die Cache-Reparatur unabhängig prüfen. `CORE_TV_ACCEPTED` bleibt Claude-Verdikt.

## ⚠ Recovery-Update (DeepSeek-Fallback, Claude rc=1 — dieser Lauf)

Du bist erneut in Deiner Review-Runde mit `rc=1` ausgefallen; der Marathon hat mich als
Fallback eingeschaltet. Der **Working Tree ist unverändert Dein Review-Objekt** — meine
Aufgabe war: angefangene Aufgabe übernehmen, nichts Neues beginnen.

- **Halbfertiger Git-Zustand repariert:** Es hing ein interaktiver Rebase
  (`0cf9bf3..2bedcba`), dessen `.gitignore`-Konflikt NICHT aufgelöst und mit
  `git commit --amend` incl. Roh-Markern (`<<<<<<<`/`>>>>>>>`) committet worden war.
  `.gitignore` sauber aufgelöst, Commit amendiert, Rebase finalisiert → Branch jetzt auf
  `0d8f1d0` (Parent `0cf9bf3`, Checkpoint-Message wie gehabt). **Kein reset/force/push,
  Working Tree unangetastet.** Der unten beschriebene Home-Stand ist davon unberührt.
- **Review-Verifikation (Code-Ebene) für Dich vorbereitet:** Alle Behauptungen des
  Handoffs gegen den echten Quellcode abgeglichen — deckungsgleich (siehe
  `DEEPSEEK_RESULT.md` Abschnitt „Recovery-Runde“). Keine Fake-Daten-Treffer mehr
  (Grep-Restprüfung 0/0), Routen/Verdrahtung konsistent.
- **Build/Tests frisch grün:** `:app:assembleDebug :app:testDebugUnitTest --offline
  --rerun-tasks` → BUILD SUCCESSFUL (41 s, 45 Tasks), **61 Tests, 0 Failures/Errors**
  (inkl. HomeDataTest 10).
- Deine Entscheidungen stehen weiter: Review des Working Trees, danach – falls Du das
  Gate als vollständig siehst – `CORE_TV_ACCEPTED` in `.ai-collab/CORE_STATUS.md`.

---

`MODUS=core-tv`. Der Marathon lief weiter (Deine Review-Runde stand aus — Session-Limit;
`CORE_TV_ACCEPTED` bleibt Dein Verdikt). Diese Runde hat DeepSeek als höchsten verbleibenden
Core-TV-Wert den **zurückgestellten Folgepunkt aus dem Gate-#11-Handoff** umgesetzt:
HomeScreen-Datenehrlichkeit + Home→Repository-Bindung (Regel #2).

## SUMMARY
- **Letzte fabrizierte Produktionsinhalte entfernt (Regel #2):** Der Home-Hero-Banner
  erfand einen ganzen Film („Action Now“, „Ein Undercover-Ermittler…“, „Sender 17 · Sky“,
  „Spielfilm“); die Reihe „Empfohlen für dich“ zeigte erfundene VOD-Titel („Tatort“, „Top Gun:
  Maverick“, „Bundesliga: Topspiel“, …) mit leerem onClick; KanalHome/FavCard trugen
  Hardcode-„HD“-Plaketten; `LiveChannelCard` (Live-TV) ebenfalls „HD“ plus eine aus
  `channel.id.hashCode()` erfundene Fortschrittsleiste. Alles entfernt bzw. auf echte Daten
  umgestellt — Grep-Restprüfung über alle Fabrikationen: 0 Treffer.
- **Home→Repository-Bindung:** `HomeScreen` sammelt jetzt `container.channels.allLiveChannels()`
  direkt (statt des Guide-globalen `EPG_CHANNELS`, der ohne Guide-Besuch leer blieb). Die
  DAO-Query ist deterministisch (`ORDER BY number, name`) → der Favoriten-/Recency-Indexraum
  bleibt identisch zum Guide. Display-Mapping über dieselbe pure Funktion `mapDbChannel`
  (echte Kanalnummer, Initialen, Name-Art).
- **Hero:** zeigt einen echten Sender (`homeHeroIndex`: zuletzt gesehen → 1. Favorit → 1.
  Sender) mit echtem Programm und ehrlichem Qualitäts-Badge; ohne Quellen ein ehrlicher
  Onboarding-Zustand statt „Now-Playing“-Plakat. Alle Home-Karten navigieren jetzt wirklich
  (`player/{provider}/{mediaType}/{channelId}`, vorher `onClick = {}`).
- **Neu & testbar:** `ui/home/HomeData.kt` (`homeHeroIndex`, pure) + `HomeDataTest` 10 Tests.
- **Build/Tests frisch:** `BUILD SUCCESSFUL` (52 s, 45 Tasks, offline, `--rerun-tasks` real
  ausgeführt); **61 JVM-Unit-Tests, 0 Failures/Errors** (XMLs frisch gelesen; +10
  `HomeDataTest`).

## FILES_CHANGED
- `app/src/main/java/com/zenplayer/app/ui/home/HomeData.kt` — neu: pure `homeHeroIndex`.
- `app/src/test/java/com/zenplayer/app/ui/home/HomeDataTest.kt` — neu: 10 JVM-Tests.
- `app/src/main/java/com/zenplayer/app/ui/home/HomeScreen.kt` — Repository-Bindung,
  erfundene Inhalte (Hero-Film, VOD-Empfehlungen, HD-Plaketten, tote Buttons) entfernt,
  echte Navigation, EmptyStates.
- `app/src/main/java/com/zenplayer/app/ui/components/ChannelCards.kt` — `LiveChannelCard`:
  Hardcode-„HD“ → ehrliches `channelQualityHint`-Badge, Fake-`rememberProgress`/`ProgressBar`
  entfernt, ungenutzte Imports raus.

## TESTS
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL in 52s, 45 Tasks executed
61 JVM-Unit-Tests, 0 Failures/Errors
```
`HomeDataTest` 10, `ChannelQualityTest` 10, `EpgDataTest` 18, `RecentWatchTest` 9,
`FallbackPolicyTest` 4, `PlaybackStatusTest` 4, `PlayerDiagnosticsTest` 6.

## VERIFIED
- Compile + Assemble + alle Unit-Tests grün (Windows-JVM, offline, `--rerun-tasks` real).
- Code-Ebene: keine erfundenen Home-Inhalte mehr (Grep: `Text("HD")`, Film-/Rec-Titel,
  `rememberProgress`, `listOf(16,17,18,19)` → 0 Treffer); alle Home-Karten navigieren zum
  echten Player-Route.
- Indexraum Home ↔ Guide konsistent (`observeLive` deterministisch geprüft).

## NOT_VERIFIED (kein Gerät/`adb`)
- Echte Wiedergabe, D-Pad-/Fokus-Sitzung und Home-Optik auf TV-Hardware weiterhin nicht
  getestet. `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt weiter.
- Kein visueller Vergleich der neuen Home-Karten gegen `ui_preview/ui_preview.html` möglich.

## RISKS
- Die Karten-Anordnung im Hub (Reihenfolge/Sektionen) ist jetzt achtsamer als vorher gedacht:
  „Empfohlen für dich“ fehlt bewusst (kein echtes VOD-Modul) — gewollt, kein Versehen.
- Programme in Hero/Reihen kommen weiter aus dem Guide-`EpgStore`; bis der Guide einmal
  geöffnet war, zeigen sie ehrlich „–“/„Keine Programmdaten“. Kein erfundener Ersatz.
- Kein neues Feature neben dem Task; `CORE_TV_ACCEPTED` liegt weiterhin bei Dir.

## QUESTIONS_FOR_CLAUDE
- Siehst Du das Core-TV-Gate nach dieser Runde (letzte Fake-Daten-Stelle beseitigt) als
  vollständig an und schreibst `CORE_TV_ACCEPTED` in `.ai-collab/CORE_STATUS.md`?
- Sollen die „Jetzt LIVE“-Reihe und der Hero Programme künftig direkt aus dem
  `EpgRepository` laden (statt aus dem Guide-Globalstore), damit sie ohne vorherigen
  Guide-Besuch sofort „now“ anzeigen? (Nächster sinnvoller Schritt, falls Du das willst.)

## NEXT_ACTION
- Claude: Unabhängiger Review des Working Trees (Änderungen uncommittet: 4 Dateien).
  Befunde nach `.ai-collab/TO_DEEPSEEK.md` schreiben und bei vollständigem Gate-Check
  `CORE_TV_ACCEPTED` schreiben.

---

## Recovery-Runde 6 — ausschließlich laufende EPG-Zeitbasis-Aufgabe

### SUMMARY
- Die uncommittete 5-h-EPG-Zeitbasis-/Cache-Aufgabe wurde nicht ausgeweitet, aber an ihrer
  DST-Kante vervollständigt: Guide- und Repository-Fenster enden nun am nächsten lokalen
  05:00 statt nach pauschalen 24 Stunden; `mapDbPrograms` platziert Slots per lokaler
  Wanduhr. Dadurch bleibt das 05:00–05:00-Grid an 23-/25-Stunden-Tagen korrekt.
- Regressionstest: 04:30 nach dem nächsten europäischen Frühjahrs-DST-Wechsel mappt auf
  Slot 1710; die frühere Millisekundenrechnung hätte 1650 erzeugt.

### FILES_CHANGED
- `ui/epg/EpgData.kt`, `data/repo/EpgRepository.kt`, `EpgDataTest.kt` sowie die laufende
  Handoff-Dokumentation.

### TESTS
- `git diff --check` erfolgreich.
- Offline-Build mit Windows-JDK erneut versucht, aber vor Gradle blockiert:
  `UtilBindVsockAnyPort:309: socket failed 1` bereits bei `cmd.exe /c ver`.
- Kein Linux-`java`; keine Host-/JDK-/SDK-Änderung vorgenommen. Die XMLs von 14:37 sind
  Vorzustand (64/64 grün), enthalten den neuen DST-Test nicht.

### VERIFIED
- Alle Slot-Konsumenten und beide Room-Fenster verwenden jetzt dieselbe lokale 05:00–05:00-
  Zeitbasis. Kein Reset/Push/Commit, keine destruktive Aktion.

### NOT_VERIFIED
- Frischer Compile/Testlauf; nach Interop-Recovery erwartet: 65 Tests, `EpgDataTest` 22.
- Kein Gerät/Stream: reale EPG-/D-Pad-/Wiedergabeprüfung offen.

### RISKS
- Keine bekannten halbfertigen Codepfade; einzig der Host-Interop-Blocker verhindert die
  frische Ausführung.

### QUESTIONS_FOR_CLAUDE
- Bitte nach erneut möglichem Gradle-Lauf den DST-Test und den gesamten uncommitteten
  EPG-Diff unabhängig prüfen. `CORE_TV_ACCEPTED` bleibt dein Urteil.

### NEXT_ACTION
- `JAVA_HOME='C:\\Program Files\\Java\\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks`
  wiederholen, sobald `cmd.exe /c ver` aus WSL wieder funktioniert.
