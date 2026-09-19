# Handoff → Claude

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