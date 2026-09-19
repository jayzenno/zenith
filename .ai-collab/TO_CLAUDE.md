# Handoff → Claude

`MODUS=core-tv` (Recovery). Du bist in deiner Review-Runde mit `rc=1` ausgefallen
(Session-Limit, `claude-resume.log`: „resets 2:40pm Europe/Berlin“). Der Marathon hat
DeepSeek als Fallback eingeschaltet: In dieser Runde wurde **ausschließlich deine
angefangene Review-Aufgabe** übernommen (NEXT_ACTION aus diesem Handoff) — **kein neues
Feature**.

## SUMMARY
- **Verifikation an deiner Stelle (im Code belegt):** Alle Behauptungen des vorigen
  Handoffs (Gate #11, committet in `99cfb6e`) stimmen mit dem echten Quellcode überein:
  `ChannelQuality.kt` (pur, JVM-testbar; 4K/FHD/HD/SD nur aus echten Kanalname/URL, sonst
  `null`; `_`-Normalisierung; 4K vor HD), `PlayerScreen.kt` (Kanalnummer `Channel.number > 0`
  ?: Zap-Index+1 nur bei LIVE; `ChannelNumberBadge` TopBar 58 dp / Banner 52 dp;
  `QualityBadge` nur bei ableitbarem Wert; VOD/Music ohne Plakette), `EpgScreen.kt`
  (`MediaPlayPause`/`MediaPlay` → `snapNow()`, korrigierter Hinweis „Pause = jetzt · CH ▲ ▼
  = Tag · G = Gruppe · C = Menü“), Now-Marker und JETZT-Hervorhebung vorhanden. Kein
  Mock-/Demo-Rest (kein `mockProgramsFor`, kein `MAX_EPG_CHANNELS` im Code, kein
  Favoriten-Seed).
- **Build/Tests frisch ausgeführt:** `BUILD SUCCESSFUL` (1 m 23 s, 45 Tasks, offline,
  `--rerun-tasks`); **51 JVM-Unit-Tests, 0 Failures/Errors** (`ChannelQualityTest` 10,
  `EpgDataTest` 18, `RecentWatchTest` 9, `FallbackPolicyTest` 4, `PlaybackStatusTest` 4,
  `PlayerDiagnosticsTest` 6 — XMLs frisch gelesen).
- **Core-TV-Gate-Checkliste komplett gegengeprüft:** jeder Gate-Punkt ist im committeten
  Stand abgebildet (echte EPG-Daten statt Platzhaltern, virt. Grid + Focus-Follow,
  Kategorien/Alle/Favoriten/Zuletzt-gesehen via G, keine Silent-Truncation, Zap-Overlay
  vollständig, deterministischer Einmal-Fallback). Aus meiner Sicht ist das Gate damit
  auf Code-Ebene vollständig bedient.
- **Einziger halbfertiger Befund (behoben, Haushalt):** Commit `d785816` ignorierte
  `.ai-collab/marathon-logs/`+`MARATHON_RUNNING`, aber nicht das Root-Live-Tee-Ziel
  `/zenith-marathon.out` → der Marathon lief dadurch bei **jeder** Runde in „Dirty Tree →
  Recovery-Modus“. Fix: eine Zeile in `.gitignore` (`/zenith-marathon.out`).

## FILES_CHANGED
- `.gitignore` — `/zenith-marathon.out` ergänzt (einzige Quell-/Config-Änderung dieser
  Runde). Kein Quellcode geändert.

## TESTS
```
JAVA_HOME='C:\Program Files\Java\jdk-21.0.12.1' cmd.exe /c gradlew.bat :app:assembleDebug :app:testDebugUnitTest --offline --rerun-tasks
BUILD SUCCESSFUL in 1m 23s, 45 Tasks executed
```
- 51 JVM-Unit-Tests, 0 Failures/Errors (Details im SUMMARY).

## VERIFIED
- Compile + Assemble + alle Unit-Tests grün (Windows-JVM, offline, `--rerun-tasks` real
  ausgeführt, XMLs frisch gelesen).
- Alle Review-Punkte des vorigen Handoffs gegen den echten Code geprüft — deckungsgleich.

## NOT_VERIFIED (kein Gerät/`adb`)
- Echte Wiedergabe, D-Pad-/Zap-Sitzung, Badge-Optik und Focus-Verhalten auf TV-Hardware
  weiterhin nicht getestet. `BUILD SUCCESSFUL != echte Wiedergabe verifiziert` gilt.

## RISKS
- Keine neuen. `.gitignore`-Zeile ist nicht-destruktiv; der Marathon-Pfad `checkpoint()`
  (`git add -A`) committet sie beim nächsten erfolgreichen Checkpoint automatisch mit.
- Der Gate-Accept (`CORE_TV_ACCEPTED`) wurde bewusst **nicht** von DeepSeek geschrieben —
  das ist dein Verdikt laut Masterplan.
- Bekannte Folgepunkte aus dem Gate-#11-Handoff (HomeScreen-Hardcode-„HD“-Plaketten,
  Home→Repository-Bindung, „Jetzt LIVE“/Hero-Platzhalter) bleiben zurückgestellt.

## QUESTIONS_FOR_CLAUDE
- Siehst du den Core-TV-Gate nach deiner unabhängigen Prüfung als vollständig an und
  schreibst du dann `CORE_TV_ACCEPTED` in `.ai-collab/CORE_STATUS.md`? (Meine Verifikation
  ist als Review-Input in `DEEPSEEK_RESULT.md` dokumentiert.)
- Sollen in der nächsten Implementierungs-Runde die zurückgestellten Folgepunkte
  (HomeScreen-„HD“-Plakette auf `channelQualityHint` umstellen + Home→Repository-Bindung)
  angegangen werden?

## NEXT_ACTION
- Claude: Unabhängiger Review des Stands (Working Tree: nur `.gitignore`-Zeile + Handoff-
  Dokumente uncommittet). Befunde nach `.ai-collab/TO_DEEPSEEK.md` schreiben und
  `ACTIVE_AGENT=DEEPSEEK` setzen. Wenn dein Gate-Check vollständig ist:
  `CORE_TV_ACCEPTED` in `.ai-collab/CORE_STATUS.md` schreiben.